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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.utils.StringUtils;
import org.eclipse.lsp4mp.extensions.ExtendedMicroProfileProjectInfo;
import org.eclipse.lsp4mp.model.Node;
import org.eclipse.lsp4mp.model.Node.NodeType;
import org.eclipse.lsp4mp.model.PropertiesModel;
import org.eclipse.lsp4mp.model.Property;
import org.eclipse.lsp4mp.model.PropertyValueExpression;
import org.eclipse.lsp4mp.services.properties.extensions.PropertiesFileExtensionRegistry;
import org.eclipse.lsp4mp.services.properties.extensions.participants.IPropertyValidatorParticipant;
import org.eclipse.lsp4mp.settings.MicroProfileExecutionSettings;
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

	private static final Logger LOGGER = Logger.getLogger(PropertiesFileValidator.class.getName());
	private static final String MICROPROFILE_DIAGNOSTIC_SOURCE = "microprofile";

	private final MicroProfileProjectInfo projectInfo;

	private final List<Diagnostic> diagnostics;

	private final MicroProfileExecutionSettings executionSettings;

	private final MicroProfileValidationSettings validationSettings;
	private final PropertiesFileExtensionRegistry extensionRegistry;
	private final Map<String, List<Property>> existingProperties;
	private Set<String> declaredProperties;
	private Map<String, ItemMetadata> availableProperties;

	private ValidationKeyContext validationKeyContext;
	private ValidationValueContext validationValueContext;

	public PropertiesFileValidator(MicroProfileProjectInfo projectInfo, List<Diagnostic> diagnostics,
			MicroProfileExecutionSettings executionSettings, MicroProfileValidationSettings validationSettings,
			PropertiesFileExtensionRegistry extensionRegistry) {
		this.projectInfo = projectInfo;
		this.diagnostics = diagnostics;
		this.executionSettings = executionSettings;
		this.validationSettings = validationSettings;
		this.extensionRegistry = extensionRegistry;
		this.existingProperties = new HashMap<String, List<Property>>();
		// to be lazily init
		this.declaredProperties = null;
		this.availableProperties = null;
	}

	public void validate(PropertiesModel document, CancelChecker cancelChecker) {
		List<Node> nodes = document.getChildren();

		for (Node node : nodes) {
			if (cancelChecker != null) {
				cancelChecker.checkCanceled();
			}
			if (node.getNodeType() == NodeType.PROPERTY) {
				validateProperty((Property) node, cancelChecker);
			}
		}

		addDiagnosticsForDuplicates();
		addDiagnosticsForMissingRequired(document);
	}

	private void validateProperty(Property property, CancelChecker cancelChecker) {
		String propertyNameWithProfile = property.getPropertyNameWithProfile();
		if (!StringUtils.isEmpty(propertyNameWithProfile)) {
			// Validate Syntax property
			validateSyntaxProperty(propertyNameWithProfile, property);
			// Validate Duplicate property
			validateDuplicateProperty(propertyNameWithProfile, property);
		}

		String propertyName = property.getPropertyName();
		if (!StringUtils.isEmpty(propertyName)) {
			ItemMetadata metadata = PropertiesFileUtils.getProperty(propertyName, projectInfo);
			validatePropertyKey(property, propertyName, metadata, cancelChecker);
			// Validate simple / expression property value
			validatePropertyValue(property, propertyNameWithProfile, metadata, cancelChecker);
		}
	}

	// ---------------- Property syntax/duplicate validation

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

	private void validateDuplicateProperty(String propertyName, Property property) {
		DiagnosticSeverity severity = validationSettings.getDuplicate().getDiagnosticSeverity(propertyName);
		if (severity == null) {
			// The duplicate validation must be ignored for this property name
			return;
		}

		if (!existingProperties.containsKey(propertyName)) {
			existingProperties.put(propertyName, new ArrayList<Property>());
		}

		existingProperties.get(propertyName).add(property);
	}

	// ---------------- Property key validation

	private void validatePropertyKey(Property property, String propertyName, ItemMetadata metadata,
			CancelChecker cancelChecker) {
		// 1. Custom property key validation
		ValidationKeyContext context = getValidationKeyContext();
		context.setProperty(property);
		context.setPropertyName(propertyName);
		context.setMetadata(metadata);
		context.setPropertiesModel(property.getOwnerModel());
		if (validatePropertyKeyWithParticipant(context, cancelChecker)) {
			// There is a custom validator which validates the property key which overrides
			// the standard property key validation.
			return;
		}

		// 2. Standard property key validation
		if (metadata != null) {
			return;
		}
		validateUnknownProperty(propertyName, property);

	}

	private boolean validatePropertyKeyWithParticipant(ValidationKeyContext context, CancelChecker cancelChecker) {
		boolean override = false;
		for (IPropertyValidatorParticipant propertyValidatorParticipant : extensionRegistry
				.getPropertyValidatorParticipants()) {
			cancelChecker.checkCanceled();
			try {
				override = override | propertyValidatorParticipant.validatePropertyKey(context, cancelChecker);
			} catch (CancellationException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error while validating property key for the participant '"
						+ propertyValidatorParticipant.getClass().getName() + "'.", e);
			}
		}
		return override;
	}

	private void validateUnknownProperty(String propertyName, Property property) {
		DiagnosticSeverity severity = validationSettings.getUnknown().getDiagnosticSeverity(propertyName);
		if (severity == null) {
			// The unknown validation must be ignored for this property name
			return;
		}
		addDiagnostic("Unrecognized property '" + propertyName + "', it is not referenced in any Java files",
				property.getKey(), severity, ValidationType.unknown.name());
	}

	// ---------------- Property value validation

	private void validatePropertyValue(Property property, String propertyNameWithProfile, ItemMetadata metadata,
			CancelChecker cancelChecker) {
		if (!property.isPropertyValueExpression()) {
			// Validate simple property Value
			validateSimplePropertyValue(propertyNameWithProfile, metadata, property, cancelChecker);
		} else {
			validatePropertyValueExpressions(propertyNameWithProfile, metadata, property, cancelChecker);
		}
	}

	private void validateSimplePropertyValue(String propertyName, ItemMetadata metadata, Property property,
			CancelChecker cancelChecker) {
		Node propertyValue = property.getValue();
		if (propertyValue == null) {
			return;
		}
		int start = propertyValue.getStart();
		int end = propertyValue.getEnd();
		validatePropertyValue(propertyName, metadata, property.getPropertyValue(), start, end, property.getOwnerModel(),
				cancelChecker);
	}

	private void validatePropertyValue(String propertyName, ItemMetadata metadata, String value, int start, int end,
			PropertiesModel propertiesModel, CancelChecker cancelChecker) {
		DiagnosticSeverity severity = validationSettings.getValue().getDiagnosticSeverity(propertyName);
		if (severity == null) {
			// The value validation must be ignored for this property name
			return;
		}

		// 1. Custom property value validation
		ValidationValueContext context = getValidationValueContext();
		context.setPropertyName(propertyName);
		context.setMetadata(metadata);
		context.setValue(value);
		context.setStart(start);
		context.setEnd(end);
		context.setPropertiesModel(propertiesModel);
		if (validatePropertyValueWithParticipant(context, cancelChecker)) {
			// There is a custom validator which validates the property value which
			// overrides
			// the standard property value validation.
			return;
		}

		// 2. Standard property value validation
		if (metadata == null || StringUtils.isEmpty(value)) {
			return;
		}

		// 3. Validate with the real MicroProfile converter from the MicroProfile
		// application classpath
		if (projectInfo instanceof ExtendedMicroProfileProjectInfo) {
			MicroProfileProjectRuntime projectRuntime = ((ExtendedMicroProfileProjectInfo) projectInfo)
					.getProjectRuntime();
			if (projectRuntime != null) {
				ExecutionMode preferredMode = executionSettings.getExecutionMode();
				projectRuntime.validateValue(value, metadata.getType(), preferredMode,
						(errorMessage, source, code, converterStart, converterEnd) -> {
							Range range = PositionUtils.createRange(start + converterStart,
									start + converterStart + converterEnd, propertiesModel.getDocument());
							addDiagnostic(errorMessage, range, severity, code);
						});
			}
		}
	}

	private boolean validatePropertyValueWithParticipant(ValidationValueContext context, CancelChecker cancelChecker) {
		boolean override = false;
		for (IPropertyValidatorParticipant propertyValidatorParticipant : extensionRegistry
				.getPropertyValidatorParticipants()) {
			cancelChecker.checkCanceled();
			try {
				override = override | propertyValidatorParticipant.validatePropertyValue(context, cancelChecker);
			} catch (CancellationException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error while validating property value for the participant '"
						+ propertyValidatorParticipant.getClass().getName() + "'.", e);
			}
		}
		return override;
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
					if (declaredProperties == null) {
						// Collect names of all properties defined in the configuration file and the
						// project information
						declaredProperties = property.getOwnerModel().getChildren().stream().filter(n -> {
							return n.getNodeType() == NodeType.PROPERTY;
						}).map(prop -> {
							return ((Property) prop).getPropertyNameWithProfile();
						}).collect(Collectors.toSet());

						availableProperties = projectInfo.getProperties()//
								.stream() //
								.collect(Collectors.toMap(ItemMetadata::getName, Function.identity(), (i1, i2) -> i1));
					}

					String refdProp = propValExpr.getReferencedPropertyName();
					if (!declaredProperties.contains(refdProp)) {
						// The referenced property name doesn't reference a property inside the file
						ItemMetadata availableProperty = availableProperties.get(refdProp);
						if (availableProperty != null) {
							// The property is declared in a Java file, System/Environment variables, etc
							if (availableProperty.isJavaOrigin()) {
								// The property is declared in a Java file
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
										addDiagnostic(
												"The referenced property '" + refdProp + "' has no default value.",
												range, expressionSeverity, ValidationType.expression.name());
									}
								}
							}
						} else {
							if (propValExpr.hasDefaultValue()) {
								// The expression has default value (ex : ${DBUSER:sa})
								int start = propValExpr.getDefaultValueStartOffset();
								int end = propValExpr.getDefaultValueEndOffset();
								validatePropertyValue(propertyName, metadata, propValExpr.getDefaultValue(), start, end,
										propValExpr.getOwnerModel(), cancelChecker);
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

	private static boolean isBuildtimePlaceholder(String str) {
		return str.startsWith("${") && str.endsWith("}");
	}

	private void addDiagnosticsForDuplicates() {
		existingProperties.forEach((propertyName, propertyList) -> {
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

	private void addDiagnosticsForMissingRequired(PropertiesModel document) {
		for (ItemMetadata property : projectInfo.getProperties()) {

			String propertyName = property.getName();

			DiagnosticSeverity severity = validationSettings.getRequired().getDiagnosticSeverity(propertyName);

			if (severity != null && property.isRequired()) {
				if (!existingProperties.containsKey(propertyName)) {
					addDiagnostic("Missing required property '" + propertyName + "'", document, severity,
							ValidationType.required.name());
				} else {
					addDiagnosticsForRequiredIfNoValue(propertyName, severity);
				}
			}
		}
	}

	private void addDiagnosticsForRequiredIfNoValue(String propertyName, DiagnosticSeverity severity) {
		List<Property> propertyList = existingProperties.get(propertyName);

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

	Diagnostic addDiagnostic(String message, Node node, DiagnosticSeverity severity, String code) {
		Range range = PositionUtils.createRange(node);
		return addDiagnostic(message, range, severity, code);
	}

	/**
	 * Add diagnostic.
	 * 
	 * @param message  the diagnostic message.
	 * @param range    the diagnostic range.
	 * @param severity the diagnostic severity.
	 * @param code     the diagnostic code.
	 * 
	 * @return the diagnostic.
	 */
	Diagnostic addDiagnostic(String message, Range range, DiagnosticSeverity severity, String code) {
		Diagnostic d = new Diagnostic(range, message, severity, MICROPROFILE_DIAGNOSTIC_SOURCE, code);
		diagnostics.add(d);
		return d;
	}

	public MicroProfileValidationSettings getValidationSettings() {
		return validationSettings;
	}

	private ValidationKeyContext getValidationKeyContext() {
		if (validationKeyContext == null) {
			validationKeyContext = new ValidationKeyContext(this);
		}
		return validationKeyContext;
	}

	private ValidationValueContext getValidationValueContext() {
		if (validationValueContext == null) {
			validationValueContext = new ValidationValueContext(this);
		}
		return validationValueContext;
	}

}
