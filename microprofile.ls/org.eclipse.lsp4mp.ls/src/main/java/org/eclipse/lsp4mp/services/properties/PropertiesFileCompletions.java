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

import static org.eclipse.lsp4mp.services.properties.PropertiesInfoPropertiesProvider.createConfigSourcePropertiesProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ConverterKind;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.metadata.ValueHint;
import org.eclipse.lsp4mp.commons.utils.IConfigSourcePropertiesProvider;
import org.eclipse.lsp4mp.commons.utils.PropertyValueExpander;
import org.eclipse.lsp4mp.commons.utils.StringUtils;
import org.eclipse.lsp4mp.ls.commons.BadLocationException;
import org.eclipse.lsp4mp.ls.commons.SnippetsBuilder;
import org.eclipse.lsp4mp.ls.commons.TextDocument;
import org.eclipse.lsp4mp.ls.commons.snippets.TextDocumentSnippetRegistry;
import org.eclipse.lsp4mp.model.Assign;
import org.eclipse.lsp4mp.model.BasePropertyValue;
import org.eclipse.lsp4mp.model.Node;
import org.eclipse.lsp4mp.model.Node.NodeType;
import org.eclipse.lsp4mp.model.PropertiesModel;
import org.eclipse.lsp4mp.model.Property;
import org.eclipse.lsp4mp.model.PropertyKey;
import org.eclipse.lsp4mp.model.PropertyValueExpression;
import org.eclipse.lsp4mp.settings.MicroProfileCompletionCapabilities;
import org.eclipse.lsp4mp.settings.MicroProfileFormattingSettings;
import org.eclipse.lsp4mp.snippets.LanguageId;
import org.eclipse.lsp4mp.snippets.SnippetContextForProperties;
import org.eclipse.lsp4mp.utils.DocumentationUtils;
import org.eclipse.lsp4mp.utils.PropertiesFileUtils;
import org.eclipse.lsp4mp.utils.PropertiesFileUtils.FormattedPropertyResult;

/**
 * The properties file completions support.
 *
 * @author Angelo ZERR
 *
 */
class PropertiesFileCompletions {

	private static final Logger LOGGER = Logger.getLogger(PropertiesFileCompletions.class.getName());
	private TextDocumentSnippetRegistry snippetRegistry;

	/**
	 * Returns completion list for the given position
	 *
	 * @param document                the properties model document
	 * @param position                the position where completion was triggered
	 * @param projectInfo             the MicroProfile project information
	 * @param propertiesModelProvider
	 * @param completionCapabilities  the completion capabilities
	 * @param cancelChecker           the cancel checker
	 * @return completion list for the given position
	 */
	public CompletionList doComplete(PropertiesModel document, Position position, MicroProfileProjectInfo projectInfo,
			IPropertiesModelProvider propertiesModelProvider, MicroProfileCompletionCapabilities completionCapabilities,
			MicroProfileFormattingSettings formattingSettings, CancelChecker cancelChecker) {
		CompletionList list = new CompletionList();
		int offset = -1;
		Node node = null;
		try {
			offset = document.offsetAt(position);
			node = document.findNodeAt(offset);
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "In MicroProfileCompletions, position error", e);
			return list;
		}
		if (node == null) {
			return list;
		}

		switch (node.getNodeType()) {
		case COMMENTS:
			// no completions
			break;

		case PROPERTY_VALUE_EXPRESSION:
			PropertyValueExpression propExpr = (PropertyValueExpression) node;
			boolean inDefautlValue = propExpr.isInDefaultValue(offset);
			if (inDefautlValue || offset == propExpr.getStart()
					|| (propExpr.isClosed() && propExpr.getEnd() == offset)) {
				// other.test.property = ${}|
				// quarkus.log.level = ${ENV_LEVEL:|}
				collectPropertyValueSuggestions(node, document, inDefautlValue, projectInfo, completionCapabilities,
						list);
			} else {
				// other.test.property = ${|}
				collectPropertyValueExpressionSuggestions(propExpr, document, projectInfo, propertiesModelProvider,
						completionCapabilities, list, cancelChecker);
			}
			break;

		case ASSIGN:
			// Only collect if on right side of =
			if (offset >= node.getEnd()) {
				// quarkus.datasource.transaction-isolation-level=|
				collectPropertyValueSuggestions(node, document, false, projectInfo, completionCapabilities, list);
			} else {
				// completion on property key if on the left side of =
				collectPropertyKeySuggestions(offset, node, document, projectInfo, completionCapabilities,
						formattingSettings, list);
			}
			break;
		case PROPERTY_VALUE:
		case PROPERTY_VALUE_LITERAL:
			// completion on property value
			// quarkus.log.console.async.overflow=B|L
			collectPropertyValueSuggestions(node, document, false, projectInfo, completionCapabilities, list);
			break;

		default:
			// completion on property key
			collectPropertyKeySuggestions(offset, node, document, projectInfo, completionCapabilities,
					formattingSettings, list);
			// Collect completion items with snippet
			collectSnippetSuggestions(offset, node, document, projectInfo, completionCapabilities, getSnippetRegistry(),
					list);
			break;
		}
		cancelChecker.checkCanceled();
		return list;
	}

	/**
	 * Collect property keys.
	 *
	 * @param offset                 the offset where completion was invoked
	 * @param node                   the property key node
	 * @param projectInfo            the MicroProfile project information
	 * @param completionCapabilities the completion capabilities
	 * @param list                   the completion list to fill
	 */
	private static void collectPropertyKeySuggestions(int offset, Node node, PropertiesModel model,
			MicroProfileProjectInfo projectInfo, MicroProfileCompletionCapabilities completionCapabilities,
			MicroProfileFormattingSettings formattingSettings, CompletionList list) {

		boolean snippetsSupported = completionCapabilities.isCompletionSnippetsSupported();
		boolean markdownSupported = completionCapabilities.isDocumentationFormatSupported(MarkupKind.MARKDOWN);

		Range range = null;
		try {
			range = model.getDocument().lineRangeAt(offset);
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "In MicroProfileCompletion#collectPropertyKeySuggestions, position error", e);
			return;
		}

		String profile = null;
		if (node != null && node.getNodeType() == NodeType.PROPERTY_KEY) {
			PropertyKey key = (PropertyKey) node;
			if (key.isBeforeProfile(offset)) {
				collectProfileSuggestions(offset, key, model, markdownSupported, list);
				return;
			}
			profile = key.getProfile();
		}

		Set<String> existingProperties = getExistingProperties(model);

		String propertyValue = null;
		if (node != null && node.getNodeType() == NodeType.PROPERTY_KEY) {
			propertyValue = ((PropertyKey) node).getProperty().getPropertyValue();
		} else if (node != null && node.getNodeType() == NodeType.ASSIGN) {
			propertyValue = ((Assign) node).getProperty().getPropertyValue();
		}

		// Completion on MicroProfile properties
		for (ItemMetadata property : projectInfo.getProperties()) {
			if (property == null) {
				continue;
			}
			String propertyName = property.getName();
			if (profile != null) {
				propertyName = "%" + profile + "." + propertyName;
			}
			if (existingProperties.contains(propertyName) && node.getNodeType() == NodeType.PROPERTY_KEY
					&& !((PropertyKey) node).getPropertyNameWithProfile().equals(propertyName)) {
				// don't add completion items for properties that already exist
				// unless current node has a key equal to current property name
				continue;
			}

			String name = property.getName();
			CompletionItem item = new CompletionItem(name);
			item.setKind(CompletionItemKind.Property);

			String defaultValue = null;
			if (propertyValue == null || propertyValue.isEmpty()) {
				defaultValue = property.getDefaultValue();
			} else {
				defaultValue = propertyValue;
			}

			Collection<ValueHint> enums = PropertiesFileUtils.getEnums(property, projectInfo);

			StringBuilder insertText = new StringBuilder();
			if (profile != null) {
				insertText.append('%');
				insertText.append(profile);
				insertText.append('.');
			}
			FormattedPropertyResult formattedProperty = getPropertyName(name, snippetsSupported);
			insertText.append(formattedProperty.getPropertyName());

			String filterText = insertText.toString();
			item.setFilterText(filterText);

			if (formattingSettings.isSurroundEqualsWithSpaces()) {
				insertText.append(' ');
			}
			insertText.append('=');
			if (formattingSettings.isSurroundEqualsWithSpaces()) {
				insertText.append(' ');
			}

			if (enums != null && enums.size() > 0) {
				// Enumerations
				if (snippetsSupported) {
					// Because of LSP limitation, we cannot use default value with choice.
					SnippetsBuilder.choice(formattedProperty.getParameterCount() + 1,
							enums.stream().map(valueHint -> valueHint.getPreferredValue(property.getConverterKinds()))
									.collect(Collectors.toList()),
							insertText);
				} else {
					// Plaintext: use default value or the first enum if no default value.
					String defaultEnumValue = defaultValue != null ? defaultValue : enums.iterator().next().getValue();
					insertText.append(defaultEnumValue);
				}
			} else if (defaultValue != null) {
				// Default value
				if (snippetsSupported) {
					SnippetsBuilder.placeholders(0, defaultValue, insertText);
				} else {
					insertText.append(defaultValue);
				}
			} else {
				if (snippetsSupported) {
					SnippetsBuilder.tabstops(0, insertText);
				}
			}

			item.setTextEdit(Either.forLeft(new TextEdit(range, insertText.toString())));

			item.setInsertTextFormat(snippetsSupported ? InsertTextFormat.Snippet : InsertTextFormat.PlainText);
			item.setDocumentation(DocumentationUtils.getDocumentation(property, profile, null, markdownSupported));
			list.getItems().add(item);
		}
	}

	/**
	 * Collect Quarkus profiles
	 *
	 * @param offset            the offset where completion was invoked
	 * @param key               the property key
	 * @param model             the properties model
	 * @param markdownSupported boolean determining if markdown is supported
	 * @param list              the completion list
	 */
	private static void collectProfileSuggestions(int offset, PropertyKey key, PropertiesModel model,
			boolean markdownSupported, CompletionList list) {

		Range range = null;
		Position currPosition = null;
		boolean addPeriod = false;
		String line = null;
		TextDocument textDocument = model.getDocument();
		try {
			range = textDocument.lineRangeAt(offset);
			currPosition = textDocument.positionAt(offset);
			line = textDocument.lineText(currPosition.getLine());
			addPeriod = currPosition.getCharacter() < line.length() && line.charAt(currPosition.getCharacter()) != '.';
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "In MicroProfileCompletion#collectPropertyKeySuggestions, position error", e);
			return;
		}

		// Collect all existing profiles declared in application.properties
		Set<String> profiles = model.getChildren().stream().filter(n -> n.getNodeType() == NodeType.PROPERTY).map(n -> {
			Property property = (Property) n;
			return property.getProfile();
		}).filter(Objects::nonNull).filter(not(String::isEmpty)).distinct().collect(Collectors.toSet());
		// merge existings profiles with default profiles.
		profiles.addAll(QuarkusModel.getDefaultProfileNames());
		// Completion on profiles
		for (String p : profiles) {
			if (p.equals(key.getProfile())) {
				continue;
			}

			CompletionItem item = new CompletionItem(p);
			item.setKind(CompletionItemKind.Struct);

			String insertText = new StringBuilder("%").append(p).append(addPeriod ? "." : "").toString();
			range.setEnd(currPosition);
			item.setTextEdit(Either.forLeft(new TextEdit(range, insertText)));
			item.setInsertTextFormat(InsertTextFormat.PlainText);
			item.setFilterText(insertText);
			addDocumentationIfDefaultProfile(item, markdownSupported);
			list.getItems().add(item);
		}
	}

	/**
	 * Adds documentation to <code>item</code> if <code>item</code> represents a
	 * default profile
	 *
	 * @param item
	 * @param markdown
	 */
	private static void addDocumentationIfDefaultProfile(CompletionItem item, boolean markdown) {

		for (ValueHint profile : QuarkusModel.DEFAULT_PROFILES.getValues()) {
			if (profile.getValue().equals(item.getLabel())) {
				item.setDocumentation(DocumentationUtils.getDocumentation(profile, markdown));
				break;
			}
		}
	}

	private static <T> Predicate<T> not(Predicate<T> t) {
		return t.negate();
	}

	/**
	 * Returns a set of property names for the properties in <code>model</code>.
	 *
	 * @param model the <code>PropertiesModel</code> to get property names from
	 * @return set of property names for the properties in <code>model</code>
	 */
	private static Set<String> getExistingProperties(PropertiesModel model) {
		Set<String> set = new HashSet<String>();
		for (Node child : model.getChildren()) {
			if (child.getNodeType() == NodeType.PROPERTY) {
				String name = ((Property) child).getPropertyNameWithProfile();
				if (!StringUtils.isEmpty(name)) {
					set.add(name);
				}
			}
		}
		return set;
	}

	/**
	 * Returns the property name to insert when completion is applied.
	 *
	 * @param propertyName      the property name
	 * @param snippetsSupported true if snippet is supported and false otherwise.
	 * @return the property name to insert when completion is applied.
	 */
	private static FormattedPropertyResult getPropertyName(String propertyName, boolean snippetsSupported) {
		if (!snippetsSupported) {
			return new FormattedPropertyResult(propertyName, 0);
		}
		return PropertiesFileUtils.formatPropertyForCompletion(propertyName);
	}

	/**
	 * Collect property values.
	 *
	 * @param node                   the property value node
	 * @param inDefautlValue         true if the offset is in the default value.
	 * @param projectInfo            the MicroProfile project information
	 * @param completionCapabilities the completion capabilities
	 * @param list                   the completion list to fill
	 */
	private static void collectPropertyValueSuggestions(Node node, PropertiesModel model, boolean inDefautlValue,
			MicroProfileProjectInfo projectInfo, MicroProfileCompletionCapabilities completionCapabilities,
			CompletionList list) {

		Property property = null;

		switch (node.getNodeType()) {
		case ASSIGN:
			property = ((Assign) node).getProperty();
			break;
		case PROPERTY_VALUE:
		case PROPERTY_VALUE_LITERAL:
		case PROPERTY_VALUE_EXPRESSION:
			property = ((BasePropertyValue) node).getProperty();
			break;
		default:
			assert false;
		}

		String propertyName = property.getPropertyName();

		ItemMetadata item = PropertiesFileUtils.getProperty(propertyName, projectInfo);
		if (item != null) {
			Collection<ValueHint> enums = PropertiesFileUtils.getEnums(item, projectInfo);
			if (enums != null && !enums.isEmpty()) {

				Range range = null;
				try {
					TextDocument doc = model.getDocument();
					int startOffset;
					int endOffset = -1;
					if (inDefautlValue) {
						PropertyValueExpression propExpr = (PropertyValueExpression) node;
						startOffset = propExpr.getDefaultValueStartOffset();
						endOffset = propExpr.getDefaultValueEndOffset();
					} else if (node.getNodeType() == NodeType.ASSIGN) {
						startOffset = node.getEnd();
					} else {
						startOffset = node.getStart();
					}
					range = doc.lineRangeAt(startOffset);
					range.setStart(doc.positionAt(startOffset));
					if (endOffset != -1) {
						range.setEnd(doc.positionAt(endOffset));
					}
				} catch (BadLocationException e) {
					LOGGER.log(Level.SEVERE, "In MicroProfileCompletion#getEnumCompletionItem, position error", e);
				}

				boolean markdownSupported = completionCapabilities.isDocumentationFormatSupported(MarkupKind.MARKDOWN);
				for (ValueHint e : enums) {
					list.getItems()
							.add(getValueCompletionItem(e, item.getConverterKinds(), range, model, markdownSupported));
				}
			}
		}
	}

	private static void collectPropertyValueExpressionSuggestions(PropertyValueExpression node, PropertiesModel model,
			MicroProfileProjectInfo projectInfo, IPropertiesModelProvider propertiesModelProvider,
			MicroProfileCompletionCapabilities completionCapabilities, CompletionList list,
			CancelChecker cancelChecker) {

		IConfigSourcePropertiesProvider properties = createConfigSourcePropertiesProvider(model, projectInfo,
				propertiesModelProvider, cancelChecker);
		PropertyValueExpander expander = new PropertyValueExpander(properties);
		cancelChecker.checkCanceled();

		// Find properties that won't make a circular dependency and suggest them for
		// completion
		String completionPropertyName = node.getProperty().getPropertyKey();
		List<String> independentProperties = expander.getIndependentProperties(completionPropertyName);
		cancelChecker.checkCanceled();
		// Add all independent properties as completion items
		for (String independentProperty : independentProperties) {
			list.getItems().add(getPropertyCompletionItem(independentProperty, node, model));
		}

		// Add all properties not referenced in the properties file as completion
		// options only the property has no default value
		for (ItemMetadata candidateCompletion : projectInfo.getProperties()) {
			if (candidateCompletion.getDefaultValue() == null) {
				String candidateCompletionName = candidateCompletion.getName();
				if (!model.hasKey(candidateCompletionName)) {
					list.getItems().add(getPropertyCompletionItem(candidateCompletionName, node, model));
				}
			}
		}
	}

	/**
	 * Returns the <code>CompletionItem</code> which offers completion for value
	 * completion for <code>value</code> at the start offset of <code>node</code>.
	 *
	 * @param item              the value item.
	 * @param converterKinds    the converter kinds.
	 * @param range             the range for completion
	 * @param model             the property model
	 * @param markdownSupported true if markdown is supported and false otherwise.
	 * @return the value completion item
	 */
	private static CompletionItem getValueCompletionItem(ValueHint item, List<ConverterKind> converterKinds,
			Range range, PropertiesModel model, boolean markdownSupported) {
		String value = item.getPreferredValue(converterKinds);
		CompletionItem completionItem = new CompletionItem(value);
		completionItem.setKind(CompletionItemKind.Value);

		completionItem.setTextEdit(Either.forLeft(new TextEdit(range, value)));
		completionItem.setDocumentation(DocumentationUtils.getDocumentation(item, markdownSupported));

		return completionItem;
	}

	/**
	 * Make a completion item for a property given its metadata
	 *
	 * @param property the metadata of the property to create a completion item for
	 */
	private static CompletionItem getPropertyCompletionItem(String propertyName,
			PropertyValueExpression propertyValueExpression, PropertiesModel model) {
		String completionText = "${" + propertyName + "}";
		CompletionItem completionItem = new CompletionItem(completionText);
		completionItem.setKind(CompletionItemKind.Value);
		try {
			Range range = new Range(model.getDocument().positionAt(propertyValueExpression.getStart()),
					model.getDocument().positionAt(propertyValueExpression.getEnd()));
			completionItem.setTextEdit(Either.forLeft(new TextEdit(range, completionText)));
			return completionItem;
		} catch (BadLocationException e) {
			return null;
		}
	}

	private static void collectSnippetSuggestions(int completionOffset, Node node, PropertiesModel document,
			MicroProfileProjectInfo projectInfo, MicroProfileCompletionCapabilities completionCapabilities,
			TextDocumentSnippetRegistry snippetRegistry, CompletionList list) {
		boolean markdownSupported = completionCapabilities.isDocumentationFormatSupported(MarkupKind.MARKDOWN);
		boolean snippetsSupported = completionCapabilities.isCompletionSnippetsSupported();
		snippetRegistry.getCompletionItems(document.getDocument(), completionOffset, markdownSupported,
				snippetsSupported, (context, model) -> {
					if (context instanceof SnippetContextForProperties) {
						SnippetContextForProperties contextProperties = (SnippetContextForProperties) context;
						return contextProperties.isMatch(projectInfo);
					}
					return false;
				}, Collections.emptyMap()).forEach(item -> {
					list.getItems().add(item);
				});
	}

	private TextDocumentSnippetRegistry getSnippetRegistry() {
		if (snippetRegistry == null) {
			snippetRegistry = new TextDocumentSnippetRegistry(LanguageId.properties.name()) {

				@Override
				protected String getExpr(TextDocument document, int completionOffset) {
					String expr = super.getExpr(document, completionOffset);
					if (expr.length() > 0 && expr.charAt(0) == '%') {
						return null;
					}
					return expr;
				}
			};
		}
		return snippetRegistry;
	}

}