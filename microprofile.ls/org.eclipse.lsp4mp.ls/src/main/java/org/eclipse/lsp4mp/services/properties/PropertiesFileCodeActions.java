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

import static org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionFactory.createAddToUnknownExcludedCodeAction;
import static org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId.AddAllMissingRequiredProperties;
import static org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId.UnknownEnumValueAllEnumsSuggestion;
import static org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;
import org.eclipse.lsp4mp.commons.metadata.ConverterKind;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.metadata.ValueHint;
import org.eclipse.lsp4mp.commons.utils.StringUtils;
import org.eclipse.lsp4mp.ls.commons.BadLocationException;
import org.eclipse.lsp4mp.ls.commons.CodeActionFactory;
import org.eclipse.lsp4mp.ls.commons.TextDocument;
import org.eclipse.lsp4mp.ls.commons.client.CommandKind;
import org.eclipse.lsp4mp.model.BasePropertyValue;
import org.eclipse.lsp4mp.model.Node;
import org.eclipse.lsp4mp.model.Node.NodeType;
import org.eclipse.lsp4mp.model.PropertiesModel;
import org.eclipse.lsp4mp.model.PropertyKey;
import org.eclipse.lsp4mp.settings.MicroProfileCommandCapabilities;
import org.eclipse.lsp4mp.settings.MicroProfileFormattingSettings;
import org.eclipse.lsp4mp.utils.PositionUtils;
import org.eclipse.lsp4mp.utils.PropertiesFileUtils;

/**
 * The properties file code actions support.
 *
 * @author Angelo ZERR
 *
 */
class PropertiesFileCodeActions {

	private static final float MAX_DISTANCE_DIFF_RATIO = 0.1f;

	private static final Logger LOGGER = Logger.getLogger(PropertiesFileCodeActions.class.getName());

	private static final String UNKNOWN_PROPERTY_SIMILAR_TEXT_SUGGESTION = "Did you mean ''{0}'' ?";
	private static final String UNKNOWN_ENUM_VALUE_SIMILAR_TEXT_SUGGESTION = "Did you mean ''{0}''?";
	private static final String UNKNOWN_ENUM_VALUE_ALL_ENUMS_SUGGESTION = "Replace with ''{0}''?";
	private static final String ADD_ALL_MISSING_REQUIRED_PROPERTIES = "Add all missing required properties?";

	/**
	 * Returns code actions for the given diagnostics of the application.properties
	 * <code>document</code> by using the given MicroProfile properties metadata
	 * <code>projectInfo</code>.
	 *
	 * @param context             the code action context
	 * @param range               the range
	 * @param document            the properties model.
	 * @param projectInfo         the MicroProfile project info
	 * @param formattingSettings  the formatting settings.
	 * @param commandCapabilities the command capabilities
	 * @param cancelChecker       the cancel checker
	 * @return the result of the code actions.
	 */
	public List<CodeAction> doCodeActions(CodeActionContext context, Range range, PropertiesModel document,
			MicroProfileProjectInfo projectInfo, MicroProfileFormattingSettings formattingSettings,
			MicroProfileCommandCapabilities commandCapabilities, CancelChecker cancelChecker) {
		List<CodeAction> codeActions = new ArrayList<>();
		if (context.getDiagnostics() != null) {
			doCodeActionForAllRequired(context.getDiagnostics(), document, formattingSettings, codeActions);
			// Loop for all diagnostics
			for (Diagnostic diagnostic : context.getDiagnostics()) {
				cancelChecker.checkCanceled();
				if (ValidationType.unknown.isValidationType(diagnostic.getCode())) {
					// Manage code action for unknown
					doCodeActionsForUnknown(diagnostic, document, projectInfo, commandCapabilities, codeActions);
				} else if (ValidationType.value.isValidationType(diagnostic.getCode())) {
					doCodeActionsForUnknownEnumValue(diagnostic, document, projectInfo, codeActions);
				}
			}
		}
		return codeActions;
	}

	/**
	 * Creation code action for 'unknown' property by searching similar name from
	 * the known MicroProfile properties.
	 *
	 * <p>
	 * LIMITATION: mapped property are not supported.
	 * </p>
	 *
	 * @param diagnostic          the diagnostic
	 * @param document            the properties model.
	 * @param projectInfo         the MicroProfile project info
	 * @param commandCapabilities the command capabilities
	 * @param codeActions         code actions list to fill.
	 */
	private void doCodeActionsForUnknown(Diagnostic diagnostic, PropertiesModel document,
			MicroProfileProjectInfo projectInfo, MicroProfileCommandCapabilities commandCapabilities,
			List<CodeAction> codeActions) {
		try {
			// Get property name by using the diagnostic range
			PropertyKey propertyKey = (PropertyKey) document.findNodeAt(diagnostic.getRange().getStart());
			String propertyName = propertyKey.getPropertyName();
			// Loop for each metadata property
			for (ItemMetadata metaProperty : projectInfo.getProperties()) {
				String name = metaProperty.getName();
				if (PropertiesFileUtils.isMappedProperty(name)) {
					// FIXME: support mapped property
				} else {
					// Check if the property name is similar to the metadata name
					if (isSimilar(metaProperty.getName(), propertyName)) {
						Range range = PositionUtils.createRange(propertyKey);
						CodeAction replaceAction = CodeActionFactory.replace(
								MessageFormat.format(UNKNOWN_PROPERTY_SIMILAR_TEXT_SUGGESTION, name),
								MicroProfileCodeActionId.UnknownPropertySimilarTextSuggestion, range, name,
								document.getDocument(), diagnostic);
						codeActions.add(replaceAction);
					}
				}
			}

			if (commandCapabilities.isCommandSupported(CommandKind.COMMAND_CONFIGURATION_UPDATE)) {
				doCodeActionForIgnoreUnknownValidation(propertyName, diagnostic, document, projectInfo, codeActions);
			}
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "In MicroProfileCodeActions, position error", e);
		}
	}

	/**
	 * Create code action for suggesting similar known enum values for unknown enum
	 * values. If no enum values are similar, code actions are created for each
	 * possible enum value.
	 *
	 *
	 * Code action(s) are created only if the property contained within the
	 * <code>diagnostic</code> range expects an enum value
	 *
	 * @param diagnostic  the diagnostic
	 * @param document    the properties model
	 * @param projectInfo the MicroProfile properties
	 * @param codeActions the code actions list to fill
	 */
	private void doCodeActionsForUnknownEnumValue(Diagnostic diagnostic, PropertiesModel document,
			MicroProfileProjectInfo projectInfo, List<CodeAction> codeActions) {
		try {
			Node node = document.findNodeAt((diagnostic.getRange().getStart()));
			if (node == null || !(node.getNodeType() == NodeType.PROPERTY_VALUE
					|| node.getNodeType() == NodeType.PROPERTY_VALUE_EXPRESSION
					|| node.getNodeType() == NodeType.PROPERTY_VALUE_LITERAL)) {
				// the node is not a property value.
				return;
			}
			BasePropertyValue propertyValue = (BasePropertyValue) node;
			PropertyKey propertyKey = propertyValue.getProperty().getKey();
			String value = propertyValue.getValue();
			String propertyName = propertyKey.getPropertyName();

			ItemMetadata metaProperty = PropertiesFileUtils.getProperty(propertyName, projectInfo);
			if (metaProperty == null) {
				return;
			}

			Collection<ValueHint> enums = PropertiesFileUtils.getEnums(metaProperty, projectInfo);
			if (enums == null || enums.isEmpty()) {
				return;
			}

			List<ConverterKind> converterKinds = metaProperty.getConverterKinds();
			Collection<String> similarEnums = new ArrayList<>();
			for (ValueHint e : enums) {
				if (converterKinds != null && !converterKinds.isEmpty()) {
					// The metadata property has converters, loop for each converter and check for
					// each converted value if it could be a similar value.
					for (ConverterKind converterKind : converterKinds) {
						String convertedValue = e.getValue(converterKind);
						if (isSimilarPropertyValue(convertedValue, value)) {
							similarEnums.add(convertedValue);
						}
					}
				} else {
					// No converter, check if the value if the value hint could be a similar value.
					if (isSimilarPropertyValue(e.getValue(), value)) {
						similarEnums.add(e.getValue());
					}
				}
			}

			Range range = diagnostic.getRange();

			if (!similarEnums.isEmpty()) {
				// add code actions for all similar enums
				for (String similarValue : similarEnums) {
					CodeAction replaceAction = CodeActionFactory.replace(
							MessageFormat.format(UNKNOWN_ENUM_VALUE_SIMILAR_TEXT_SUGGESTION,
									similarValue),
							UnknownEnumValueSimilarTextSuggestion, range, similarValue, document.getDocument(),
							diagnostic);
					codeActions.add(replaceAction);
				}
			} else {
				// add code actions for all enums
				for (ValueHint e : enums) {
					String preferredValue = e.getPreferredValue(converterKinds);
					CodeAction replaceAction = CodeActionFactory.replace(
							MessageFormat.format(UNKNOWN_ENUM_VALUE_ALL_ENUMS_SUGGESTION, preferredValue),
							UnknownEnumValueAllEnumsSuggestion, range, preferredValue, document.getDocument(),
							diagnostic);
					codeActions.add(replaceAction);
				}
			}

		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "In MicroProfileCodeActions, position error", e);
		}
	}

	/**
	 * Create a code action that adds <code>propertyName</code> to user's unknown
	 * validation excluded array
	 *
	 * @param propertyName the property name to add to array for code action
	 * @param diagnostic   the corresponding unknown property diagnostic
	 * @param document     the properties model
	 * @param projectInfo  the MicroProfile properties
	 * @param codeActions  the list of code actions
	 */
	private void doCodeActionForIgnoreUnknownValidation(String propertyName, Diagnostic diagnostic,
			PropertiesModel document, MicroProfileProjectInfo projectInfo, List<CodeAction> codeActions) {

		codeActions.add(createAddToUnknownExcludedCodeAction(propertyName, diagnostic));

		while (hasParentKey(propertyName)) {
			propertyName = getParentKey(propertyName);
			if (!propertyName.equals("quarkus")) {
				String globPattern = propertyName + ".*";
				codeActions.add(createAddToUnknownExcludedCodeAction(globPattern, diagnostic));
			}
		}
	}

	/**
	 * Create a code action that inserts all missing required properties and equals
	 * signs if there are more than one missing required properties.
	 *
	 * @param diagnostics        the diagnostics, one for each missing required
	 *                           property
	 * @param document           the properties model
	 * @param formattingSettings the formatting settings
	 * @param codeActions        the code actions list to fill
	 */
	private void doCodeActionForAllRequired(List<Diagnostic> diagnostics, PropertiesModel document,
			MicroProfileFormattingSettings formattingSettings, List<CodeAction> codeActions) {

		TextDocument textDocument = document.getDocument();
		List<Diagnostic> requiredDiagnostics = diagnostics.stream()
				.filter(d -> ValidationType.required.isValidationType(d.getCode())).collect(Collectors.toList());

		if (requiredDiagnostics.isEmpty()) {
			return;
		}

		try {
			Position position = getPositionForRequiredCodeAction(textDocument);
			String lineDelimiter = document.getDocument().lineDelimiter(0);
			String assign = formattingSettings.isSurroundEqualsWithSpaces() ? " = " : "=";

			StringBuilder stringToInsert = new StringBuilder();

			if (StringUtils.hasText(textDocument.getText())) {
				stringToInsert.append(lineDelimiter);
			}

			for (int i = 0; i < requiredDiagnostics.size(); i++) {
				Diagnostic diagnostic = requiredDiagnostics.get(i);
				stringToInsert.append(getPropertyNameFromRequiredMessage(diagnostic.getMessage().getLeft()));
				stringToInsert.append(assign);

				if (i < requiredDiagnostics.size() - 1) {
					stringToInsert.append(lineDelimiter);
				}
			}

			CodeAction insertAction = CodeActionFactory.insert(
					MessageFormat.format(ADD_ALL_MISSING_REQUIRED_PROPERTIES, new Object[] {}),
					AddAllMissingRequiredProperties, position, stringToInsert.toString(), textDocument,
					requiredDiagnostics);
			codeActions.add(insertAction);
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "In MicroProfileCodeActions, position error", e);
		}

	}

	/**
	 * Returns true if <code>propertyName</code> has a parent key, false otherwise
	 *
	 * For example, the parent key for "quarkus.http.cors" is "quarkus.http"
	 *
	 * @param propertyName the property name to check
	 * @return true if <code>propertyName</code> has a parent key, false otherwise
	 */
	private boolean hasParentKey(String propertyName) {
		return propertyName.lastIndexOf('.') >= 0;
	}

	/**
	 * Returns the parent key for <code>propertyName</code>
	 *
	 * For example, the parent key for "quarkus.http.cors" is "quarkus.http"
	 *
	 * @param propertyName the property name
	 * @return the parent key for <code>propertyName</code>
	 */
	private String getParentKey(String propertyName) {
		return propertyName.substring(0, propertyName.lastIndexOf('.'));
	}

	/**
	 * Returns the <code>Position</code> to insert the missing required code action
	 * property into
	 *
	 * @param textDocument the text document
	 * @return the <code>Position</code> to insert the missing required code action
	 *         property into
	 * @throws BadLocationException
	 */
	private Position getPositionForRequiredCodeAction(TextDocument textDocument) throws BadLocationException {
		String textDocumentText = textDocument.getText();

		if (!StringUtils.hasText(textDocumentText)) {
			return new Position(0, 0);
		}

		for (int i = textDocumentText.length() - 1; i >= 0; i--) {
			if (!Character.isWhitespace(textDocumentText.charAt(i))) {
				return textDocument.positionAt(i + 1);
			}
		}

		// should never happen
		return null;
	}

	/**
	 * Returns the missing required property name from
	 * <code>diagnosticMessage</code>
	 *
	 * @param diagnosticMessage the diagnostic message containing the property name
	 *                          in single quotes
	 * @return the missing required property name from
	 *         <code>diagnosticMessage</code>
	 */
	private String getPropertyNameFromRequiredMessage(String diagnosticMessage) {
		int start = diagnosticMessage.indexOf('\'') + 1;
		int end = diagnosticMessage.indexOf('\'', start);
		return diagnosticMessage.substring(start, end);
	}

	private static boolean isSimilarPropertyValue(String reference, String current) {
		return reference.startsWith(current) ? true : isSimilar(reference, current);
	}

	private static boolean isSimilar(String reference, String current) {
		int threshold = Math.round(MAX_DISTANCE_DIFF_RATIO * reference.length());
		LevenshteinDistance levenshteinDistance = new LevenshteinDistance(threshold);
		return levenshteinDistance.apply(reference, current) != -1;
	}
}
