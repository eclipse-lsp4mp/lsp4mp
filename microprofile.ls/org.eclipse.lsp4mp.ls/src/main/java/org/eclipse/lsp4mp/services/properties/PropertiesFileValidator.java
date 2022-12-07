/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.services.properties;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4mp.commons.ConfigSourceInfo;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ConfigurationMetadata;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.utils.StringUtils;
import org.eclipse.lsp4mp.model.Node;
import org.eclipse.lsp4mp.model.Node.NodeType;
import org.eclipse.lsp4mp.model.PropertiesModel;
import org.eclipse.lsp4mp.model.Property;
import org.eclipse.lsp4mp.model.PropertyValueExpression;
import org.eclipse.lsp4mp.settings.MicroProfileValidationSettings;
import org.eclipse.lsp4mp.utils.EnvUtils;
import org.eclipse.lsp4mp.utils.PositionUtils;
import org.eclipse.lsp4mp.utils.PropertiesFileUtils;

/**
 * The properties file validator.
 *
 * @author Angelo ZERR
 *
 */
class PropertiesFileValidator {

	private static final String MICROPROFILE_DIAGNOSTIC_SOURCE = "microprofile";

	private final MicroProfileProjectInfo projectInfo;

	private final IPropertiesModelProvider propertiesModelProvider;

	private final List<Diagnostic> diagnostics;

	private final MicroProfileValidationSettings validationSettings;
	private Map<String, List<Property>> allPropertiesFromFiles;
	private Set<String> allPropertiesFromJava;

	public PropertiesFileValidator(MicroProfileProjectInfo projectInfo,
			IPropertiesModelProvider propertiesModelProvider, List<Diagnostic> diagnostics,
			MicroProfileValidationSettings validationSettings) {
		this.projectInfo = projectInfo;
		this.propertiesModelProvider = propertiesModelProvider;
		this.diagnostics = diagnostics;
		this.validationSettings = validationSettings;
		// to be lazily init
		this.allPropertiesFromFiles = null;
		this.allPropertiesFromJava = null;
	}

	public void validate(PropertiesModel document, CancelChecker cancelChecker) {
		List<Node> nodes = document.getChildren();

		for (Node node : nodes) {
			checkCanceled(cancelChecker);
			if (node.getNodeType() == NodeType.PROPERTY) {
				validateProperty((Property) node, cancelChecker);
			}
		}

		addDiagnosticsForDuplicates(document, cancelChecker);
		addDiagnosticsForMissingRequired(document, cancelChecker);
	}

	private void validateProperty(Property property, CancelChecker cancelChecker) {
		String propertyNameWithProfile = property.getPropertyNameWithProfile();
		if (!StringUtils.isEmpty(propertyNameWithProfile)) {
			// Validate Syntax property
			validateSyntaxProperty(propertyNameWithProfile, property);
		}

		String propertyName = property.getPropertyName();
		if (!StringUtils.isEmpty(propertyName)) {
			ItemMetadata metadata = PropertiesFileUtils.getProperty(propertyName, projectInfo);
			if (metadata == null) {
				// Validate Unknown property
				validateUnknownProperty(propertyName, property);
			}
			if (!property.isPropertyValueExpression()) {
				// Validate simple property Value
				validateSimplePropertyValue(propertyNameWithProfile, metadata, property);
			} else {
				validatePropertyValueExpressions(propertyNameWithProfile, metadata, property, cancelChecker);
			}
		}
	}

	private void validateSyntaxProperty(String propertyName, Property property) {
		DiagnosticSeverity severity = validationSettings.getSyntax().getDiagnosticSeverity(propertyName);
		if (severity == null) {
			// The syntax validation must be ignored for this property name
			return;
		}
		if (property.getDelimiterAssign() == null) {
			addDiagnostic("Missing equals sign after '" + propertyName + "'", property.getKey(), severity,
					ValidationType.syntax.name());
		}
	}

	private void validateUnknownProperty(String propertyName, Property property) {
		DiagnosticSeverity severity = validationSettings.getUnknown().getDiagnosticSeverity(propertyName);
		if (severity == null) {
			// The unknown validation must be ignored for this property name
			return;
		}
		addDiagnostic("Unknown property '" + propertyName + "'", property.getKey(), severity,
				ValidationType.unknown.name());
	}

	private void validateSimplePropertyValue(String propertyName, ItemMetadata metadata, Property property) {
		Node propertyValue = property.getValue();
		if (propertyValue == null) {
			return;
		}
		int start = propertyValue.getStart();
		int end = propertyValue.getEnd();
		validatePropertyValue(propertyName, metadata, property.getPropertyValue(), start, end,
				property.getOwnerModel());
	}

	private void validatePropertyValue(String propertyName, ItemMetadata metadata, String value, int start, int end,
			PropertiesModel propertiesModel) {
		if (metadata == null || StringUtils.isEmpty(value)) {
			return;
		}

		DiagnosticSeverity severity = validationSettings.getValue().getDiagnosticSeverity(propertyName);
		if (severity == null) {
			// The value validation must be ignored for this property name
			return;
		}

		String errorMessage = getErrorIfInvalidEnum(metadata, projectInfo, propertiesModel, value);
		if (errorMessage == null) {
			errorMessage = getErrorIfValueTypeMismatch(metadata, value);
		}

		if (errorMessage != null) {
			Range range = PositionUtils.createRange(start, end, propertiesModel.getDocument());
			addDiagnostic(errorMessage, range, severity, ValidationType.value.name());
		}
	}

	/**
	 * Validates the property value expressions (${other.property}) of the given
	 * property.
	 *
	 * Checks if the property expression is closed, and if the referenced property
	 * exists.
	 *
	 * @param property      The property to validate
	 * @param cancelChecker
	 */
	private void validatePropertyValueExpressions(String propertyName, ItemMetadata metadata, Property property,
			CancelChecker cancelChecker) {
		if (property.getValue() == null) {
			return;
		}
		DiagnosticSeverity expressionSeverity = validationSettings.getExpression()
				.getDiagnosticSeverity(property.getPropertyName());
		DiagnosticSeverity syntaxSeverity = validationSettings.getSyntax()
				.getDiagnosticSeverity(property.getPropertyName());
		if (expressionSeverity == null || syntaxSeverity == null) {
			return;
		}
		for (Node child : property.getValue().getChildren()) {
			if (child != null && child.getNodeType() == NodeType.PROPERTY_VALUE_EXPRESSION) {
				PropertyValueExpression propValExpr = (PropertyValueExpression) child;
				if (expressionSeverity != null) {
					String refdProp = propValExpr.getReferencedPropertyName();
					if (!getAllPropertiesFromFiles(property.getOwnerModel(), cancelChecker).containsKey(refdProp)) {
						if (allPropertiesFromJava == null) {
							allPropertiesFromJava = projectInfo.getProperties().stream().map((ItemMetadata info) -> {
								return info.getName();
							}).collect(Collectors.toSet());
						}
						// The referenced property name doesn't reference a property inside the file
						if (allPropertiesFromJava.contains(refdProp)) {
							Range range = PositionUtils.createRange(propValExpr.getReferenceStartOffset(),
									propValExpr.getReferenceEndOffset(), propValExpr.getDocument());
							if (range != null) {
								ItemMetadata referencedProperty = PropertiesFileUtils.getProperty(refdProp,
										projectInfo);
								if (referencedProperty.getDefaultValue() != null) {
									// The referenced property has a default value.
									addDiagnostic("Cannot reference the property '" + refdProp
											+ "'. A default value defined via annotation like ConfigProperty is not eligible to be expanded since multiple candidates may be available.",
											range, expressionSeverity, ValidationType.expression.name());
								} else if (!propValExpr.hasDefaultValue()) {
									// The referenced property and the property expression have not a default value.
									addDiagnostic("The referenced property '" + refdProp + "' has no default value.",
											range, expressionSeverity, ValidationType.expression.name());
								}
							}
						} else {
							if (propValExpr.hasDefaultValue()) {
								// The expression has default value (ex : ${DBUSER:sa})
								int start = propValExpr.getDefaultValueStartOffset();
								int end = propValExpr.getDefaultValueEndOffset();
								validatePropertyValue(propertyName, metadata, propValExpr.getDefaultValue(), start, end,
										propValExpr.getOwnerModel());
							} else {
								if (!(EnvUtils.isEnvVariable(refdProp))) {
									// or the expression is an ENV variable
									// otherwise the error is reported
									Range range = PositionUtils.createRange(propValExpr.getReferenceStartOffset(),
											propValExpr.getReferenceEndOffset(), propValExpr.getDocument());
									if (range != null) {
										addDiagnostic("Unknown referenced property value expression '" + refdProp + "'",
												range, expressionSeverity, ValidationType.expression.name());
									}
								}
							}
						}
					}
				}
				if (syntaxSeverity != null && !propValExpr.isClosed()) {
					addDiagnostic("Missing '}'", propValExpr, syntaxSeverity, ValidationType.syntax.name());
				}
			}
		}
	}

	private Map<String, List<Property>> getAllPropertiesFromFiles(PropertiesModel current,
			CancelChecker cancelChecker) {
		if (allPropertiesFromFiles != null) {
			return allPropertiesFromFiles;
		}
		Map<String, List<Property>> allPropertiesFromFiles = new HashMap<>();
		boolean fillWithCurrent = false;
		Set<ConfigSourceInfo> configSources = projectInfo != null ? projectInfo.getConfigSources() : null;
		if (configSources != null) {
			for (ConfigSourceInfo configSource : configSources) {
				checkCanceled(cancelChecker);
				PropertiesModel document = propertiesModelProvider.getPropertiesModel(configSource.getUri());
				if (document != null) {
					if (document == current) {
						fillWithCurrent = true;
					}
					fill(document, allPropertiesFromFiles, cancelChecker);
				}
			}
		}
		if (!fillWithCurrent) {
			fill(current, allPropertiesFromFiles, cancelChecker);
		}
		this.allPropertiesFromFiles = allPropertiesFromFiles;
		return this.allPropertiesFromFiles;
	}

	private void fill(PropertiesModel document, Map<String, List<Property>> allPropertiesFromFiles,
			CancelChecker cancelChecker) {
		List<Node> nodes = document.getChildren();
		for (Node node : nodes) {
			checkCanceled(cancelChecker);
			if (node.getNodeType() == NodeType.PROPERTY) {
				Property property = (Property) node;
				String propertyNameWithProfile = property.getPropertyNameWithProfile();
				if (!StringUtils.isEmpty(propertyNameWithProfile)) {
					if (!allPropertiesFromFiles.containsKey(propertyNameWithProfile)) {
						allPropertiesFromFiles.put(propertyNameWithProfile, new ArrayList<Property>());
					}
					allPropertiesFromFiles.get(propertyNameWithProfile).add(property);
				}
			}
		}
	}

	/**
	 * Returns an error message only if <code>value</code> is an invalid enum for
	 * the property defined by <code>metadata</code>
	 *
	 * @param metadata metadata defining a property
	 * @param value    value to check
	 * @return error message only if <code>value</code> is an invalid enum for the
	 *         property defined by <code>metadata</code>
	 */
	private String getErrorIfInvalidEnum(ItemMetadata metadata, ConfigurationMetadata configuration,
			PropertiesModel model, String value) {
		if (!PropertiesFileUtils.isValidEnum(metadata, configuration, value)) {
			return "Invalid enum value: '" + value + "' is invalid for type " + metadata.getType();
		}
		return null;
	}

	/**
	 * Returns an error message only if <code>value</code> is an invalid value type
	 * for the property defined by <code>metadata</code>
	 *
	 * @param metadata metadata defining a property
	 * @param value    value to check
	 * @return error message only if <code>value</code> is an invalid value type for
	 *         the property defined by <code>metadata</code>
	 */
	private static String getErrorIfValueTypeMismatch(ItemMetadata metadata, String value) {

		if (isBuildtimePlaceholder(value)) {
			return null;
		}

		if (metadata.isRegexType()) {
			try {
				Pattern.compile(value);
				return null;
			} catch (PatternSyntaxException e) {
				return e.getMessage() + System.lineSeparator();
			}
		}

		if (metadata.isBooleanType() && !isBooleanString(value)) {
			return "Type mismatch: " + metadata.getType()
					+ " expected. By default, this value will be interpreted as 'false'";
		}

		if ((metadata.isIntegerType() && !isIntegerString(value) || (metadata.isFloatType() && !isFloatString(value))
				|| (metadata.isDoubleType() && !isDoubleString(value))
				|| (metadata.isLongType() && !isLongString(value)) || (metadata.isShortType() && !isShortString(value))
				|| (metadata.isBigDecimalType() && !isBigDecimalString(value))
				|| (metadata.isBigIntegerType() && !isBigIntegerString(value)))) {
			return "Type mismatch: " + metadata.getType() + " expected";
		}
		return null;
	}

	private static boolean isBooleanString(String str) {
		if (str == null) {
			return false;
		}
		String strUpper = str.toUpperCase();
		return "TRUE".equals(strUpper) || "FALSE".equals(strUpper) || "Y".equals(strUpper) || "YES".equals(strUpper)
				|| "1".equals(strUpper) || "ON".equals(strUpper);
	}

	private static boolean isIntegerString(String str) {
		if (!StringUtils.hasText(str)) {
			return false;
		}
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isFloatString(String str) {
		if (!StringUtils.hasText(str)) {
			return false;
		}
		try {
			Float.parseFloat(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isLongString(String str) {
		if (!StringUtils.hasText(str)) {
			return false;
		}
		try {
			Long.parseLong(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isDoubleString(String str) {
		if (!StringUtils.hasText(str)) {
			return false;
		}
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isShortString(String str) {
		if (!StringUtils.hasText(str)) {
			return false;
		}
		try {
			Short.parseShort(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isBigDecimalString(String str) {
		if (!StringUtils.hasText(str)) {
			return false;
		}
		try {
			new BigDecimal(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isBigIntegerString(String str) {
		if (!StringUtils.hasText(str)) {
			return false;
		}
		try {
			new BigInteger(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static boolean isBuildtimePlaceholder(String str) {
		return str.startsWith("${") && str.endsWith("}");
	}

	private void addDiagnosticsForDuplicates(PropertiesModel document, CancelChecker cancelChecker) {
		getAllPropertiesFromFiles(document, cancelChecker).forEach((propertyName, propertyList) -> {
			if (propertyList.size() <= 1) {
				return;
			}

			DiagnosticSeverity severity = validationSettings.getDuplicate().getDiagnosticSeverity(propertyName);

			for (Property property : propertyList) {
				addDiagnostic("Duplicate property '" + propertyName + "'", property.getKey(), severity,
						ValidationType.duplicate.name());
			}
		});
	}

	private void addDiagnosticsForMissingRequired(PropertiesModel document, CancelChecker cancelChecker) {
		for (ItemMetadata property : projectInfo.getProperties()) {

			String propertyName = property.getName();

			DiagnosticSeverity severity = validationSettings.getRequired().getDiagnosticSeverity(propertyName);

			if (severity != null && property.isRequired()) {
				if (!getAllPropertiesFromFiles(document, cancelChecker).containsKey(propertyName)) {
					addDiagnostic("Missing required property '" + propertyName + "'", document, severity,
							ValidationType.required.name());
				} else {
					addDiagnosticsForRequiredIfNoValue(document, propertyName, severity, cancelChecker);
				}
			}
		}
	}

	private void addDiagnosticsForRequiredIfNoValue(PropertiesModel document, String propertyName,
			DiagnosticSeverity severity, CancelChecker cancelChecker) {
		List<Property> propertyList = getAllPropertiesFromFiles(document, cancelChecker).get(propertyName);

		for (Property property : propertyList) {
			if (property.getValue() != null && !property.getValue().getValue().isEmpty()) {
				return;
			}
		}

		for (Property property : propertyList) {
			addDiagnostic("Missing required property value for '" + propertyName + "'", property, severity,
					ValidationType.requiredValue.name());
		}
	}

	private void addDiagnostic(String message, Node node, DiagnosticSeverity severity, String code) {
		Range range = PositionUtils.createRange(node);
		addDiagnostic(message, range, severity, code);
	}

	private void addDiagnostic(String message, Range range, DiagnosticSeverity severity, String code) {
		diagnostics.add(new Diagnostic(range, message, severity, MICROPROFILE_DIAGNOSTIC_SOURCE, code));
	}

	public MicroProfileValidationSettings getValidationSettings() {
		return validationSettings;
	}

	private void checkCanceled(CancelChecker cancelChecker) {
		if (cancelChecker != null) {
			cancelChecker.checkCanceled();
		}
	}
}
