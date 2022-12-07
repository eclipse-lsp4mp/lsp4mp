/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.services.properties;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintLabelPart;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.json.adapters.EnumTypeAdapter;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.extensions.ExtendedMicroProfileProjectInfo;
import org.eclipse.lsp4mp.ls.MockMicroProfilePropertyDefinitionProvider;
import org.eclipse.lsp4mp.ls.api.MicroProfilePropertyDefinitionProvider;
import org.eclipse.lsp4mp.ls.commons.BadLocationException;
import org.eclipse.lsp4mp.ls.commons.TextDocument;
import org.eclipse.lsp4mp.ls.commons.client.CommandCapabilities;
import org.eclipse.lsp4mp.ls.commons.client.CommandKind;
import org.eclipse.lsp4mp.ls.commons.client.CommandKindCapabilities;
import org.eclipse.lsp4mp.ls.commons.snippets.TextDocumentSnippetRegistry;
import org.eclipse.lsp4mp.model.PropertiesModel;
import org.eclipse.lsp4mp.settings.MicroProfileCommandCapabilities;
import org.eclipse.lsp4mp.settings.MicroProfileCompletionCapabilities;
import org.eclipse.lsp4mp.settings.MicroProfileFormattingSettings;
import org.eclipse.lsp4mp.settings.MicroProfileHoverSettings;
import org.eclipse.lsp4mp.settings.MicroProfileInlayHintSettings;
import org.eclipse.lsp4mp.settings.MicroProfileValidationSettings;
import org.eclipse.lsp4mp.snippets.LanguageId;
import org.eclipse.lsp4mp.snippets.SnippetContextForProperties;
import org.eclipse.lsp4mp.utils.DocumentationUtils;
import org.eclipse.lsp4mp.utils.PositionUtils;
import org.junit.Assert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The properties file assert
 *
 * @author Angelo ZERR
 *
 */
public class PropertiesFileAssert {

	private static MicroProfileProjectInfo DEFAULT_PROJECT;

	private static MicroProfilePropertyDefinitionProvider DEFAULT_DEFINITION_PROVIDER;

	private static final String MICROPROFILE_DIAGNOSTIC_SOURCE = "microprofile";

	private static final CancelChecker NOOP_CHECKER = () -> {
	};

	public static MicroProfileProjectInfo getDefaultMicroProfileProjectInfo() {
		if (DEFAULT_PROJECT == null) {
			DEFAULT_PROJECT = load(PropertiesFileAssert.class.getResourceAsStream("all-quarkus-properties.json"));
		}
		return DEFAULT_PROJECT;
	}

	public static MicroProfileProjectInfo load(InputStream input) {
		return new ExtendedMicroProfileProjectInfo(
				createGson().fromJson(new InputStreamReader(input), ExtendedMicroProfileProjectInfo.class));
	}

	private static Gson createGson() {
		return new GsonBuilder().registerTypeAdapterFactory(new EnumTypeAdapter.Factory()).create();
	}

	public static MicroProfilePropertyDefinitionProvider getDefaultMicroProfilePropertyDefinitionProvider() {
		if (DEFAULT_DEFINITION_PROVIDER == null) {
			DEFAULT_DEFINITION_PROVIDER = new MockMicroProfilePropertyDefinitionProvider();
		}
		return DEFAULT_DEFINITION_PROVIDER;
	}

	// ------------------- Completion assert

	public static void testCompletionFor(String value, boolean snippetSupport, Integer expectedCount)
			throws BadLocationException {
		testCompletionFor(value, snippetSupport, false, expectedCount);
	}

	public static void testCompletionFor(String value, boolean snippetSupport, CompletionItem... expectedItems)
			throws BadLocationException {
		testCompletionFor(value, snippetSupport, false, null, expectedItems);
	}

	public static void testCompletionFor(String value, boolean snippetSupport, boolean insertSpacing,
			CompletionItem... expectedItems) throws BadLocationException {
		testCompletionFor(value, snippetSupport, insertSpacing, null, expectedItems);
	}

	public static void testCompletionFor(String value, boolean snippetSupport, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		testCompletionFor(value, snippetSupport, false, null, expectedCount, getDefaultMicroProfileProjectInfo(),
				expectedItems);
	}

	public static void testCompletionFor(String value, boolean snippetSupport, boolean insertSpacing,
			Integer expectedCount, CompletionItem... expectedItems) throws BadLocationException {
		testCompletionFor(value, snippetSupport, insertSpacing, null, expectedCount,
				getDefaultMicroProfileProjectInfo(), expectedItems);
	}

	public static void testCompletionFor(String value, boolean snippetSupport, Integer expectedCount,
			MicroProfileProjectInfo projectInfo, CompletionItem... expectedItems) throws BadLocationException {
		testCompletionFor(value, snippetSupport, false, null, expectedCount, projectInfo, expectedItems);
	}

	public static void testCompletionFor(String value, boolean snippetSupport, String fileURI, Integer expectedCount,
			MicroProfileProjectInfo projectInfo, CompletionItem... expectedItems) throws BadLocationException {
		testCompletionFor(value, snippetSupport, false, fileURI, expectedCount, projectInfo, expectedItems);
	}

	public static void testCompletionFor(String value, MicroProfileProjectInfo projectInfo,
			CompletionItem... expectedItems) throws BadLocationException {
		testCompletionFor(value, true, false, null, expectedItems.length, projectInfo, expectedItems);
	}

	public static void testCompletionFor(String value, MicroProfileProjectInfo projectInfo, Integer expectedCount)
			throws BadLocationException {
		testCompletionFor(value, true, null, expectedCount, projectInfo);
	}

	public static void testCompletionFor(String value, boolean snippetSupport, boolean insertSpacing, String fileURI,
			Integer expectedCount, MicroProfileProjectInfo projectInfo, CompletionItem... expectedItems)
			throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		PropertiesModel model = parse(value, fileURI);
		Position position = model.positionAt(offset);

		// Add snippet support for completion
		MicroProfileCompletionCapabilities microProfileCompletionCapabilities = new MicroProfileCompletionCapabilities();
		CompletionItemCapabilities completionItemCapabilities = new CompletionItemCapabilities();
		completionItemCapabilities.setSnippetSupport(snippetSupport);
		CompletionCapabilities completionCapabilities = new CompletionCapabilities(completionItemCapabilities);
		microProfileCompletionCapabilities.setCapabilities(completionCapabilities);

		MicroProfileFormattingSettings formattingSettings = new MicroProfileFormattingSettings();
		formattingSettings.setSurroundEqualsWithSpaces(insertSpacing);

		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();
		IPropertiesModelProvider propertiesModelProvider = documentURI -> model;
		CompletionList list = languageService.doComplete(model, position, projectInfo, propertiesModelProvider,
				microProfileCompletionCapabilities, formattingSettings, () -> {
				});

		assertCompletions(list, expectedCount, expectedItems);
	}

	public static void assertCompletions(CompletionList actual, Integer expectedCount,
			CompletionItem... expectedItems) {
		// no duplicate labels
		List<String> labels = actual.getItems().stream().map(i -> i.getLabel()).sorted().collect(Collectors.toList());
		String previous = null;
		for (String label : labels) {
			Assert.assertTrue(
					"Duplicate label " + label + " in " + labels.stream().collect(Collectors.joining(",")) + "}",
					previous != label);
			previous = label;
		}
		if (expectedCount != null) {
			assertEquals(expectedCount.intValue(), actual.getItems().size());
		}
		if (expectedItems != null) {
			for (CompletionItem item : expectedItems) {
				assertCompletion(actual, item);
			}
		}
	}

	private static void assertCompletion(CompletionList completions, CompletionItem expected) {
		List<CompletionItem> matches = completions.getItems().stream().filter(completion -> {
			return expected.getLabel().equals(completion.getLabel());
		}).collect(Collectors.toList());

		assertEquals(
				expected.getLabel() + " should only exist once: Actual: "
						+ completions.getItems().stream().map(c -> c.getLabel()).collect(Collectors.joining(",")),
				1, matches.size());

		CompletionItem match = matches.get(0);
		/*
		 * if (expected.documentation != null) {
		 * assertEquals(match.getDocumentation().getRight().getValue(), expected.getd);
		 * } if (expected.kind) { assertEquals(match.kind, expected.kind); }
		 */
		// if (expected.getTextEdit() != null && match.getTextEdit() != null) {
		if (expected.getTextEdit() != null && expected.getTextEdit().getLeft() != null) {
			assertEquals(expected.getTextEdit().getLeft().getNewText(), match.getTextEdit().getLeft().getNewText());
		}
		Range r = expected.getTextEdit() != null && expected.getTextEdit().getLeft() != null
				? expected.getTextEdit().getLeft().getRange()
				: null;
		if (r != null && r.getStart() != null && r.getEnd() != null) {
			assertEquals(expected.getTextEdit().getLeft().getRange(), match.getTextEdit().getLeft().getRange());
		}
		// }
		if (expected.getFilterText() != null && match.getFilterText() != null) {
			assertEquals(expected.getFilterText(), match.getFilterText());
		}

		if (expected.getDocumentation() != null) {
			assertEquals(DocumentationUtils.getDocumentationTextFromEither(expected.getDocumentation()),
					DocumentationUtils.getDocumentationTextFromEither(match.getDocumentation()));
		}

	}

	public static CompletionItem c(String newText, Range range) {
		return c(newText, newText, range);
	}

	public static CompletionItem c(String label, String newText, Range range) {
		return c(label, newText, range, null);
	}

	public static CompletionItem c(String label, String newText, Range range, String documentation) {
		return c(label, new TextEdit(range, newText), null,
				documentation != null ? Either.forLeft(documentation) : null);
	}

	private static CompletionItem c(String label, TextEdit textEdit, String filterText,
			Either<String, MarkupContent> documentation) {
		CompletionItem item = new CompletionItem();
		item.setLabel(label);
		item.setFilterText(filterText);
		item.setTextEdit(Either.forLeft(textEdit));
		item.setDocumentation(documentation);
		return item;
	}

	public static Range r(int line, int startChar, int endChar) {
		return r(line, startChar, line, endChar);
	}

	public static Range r(int startLine, int startChar, int endLine, int endChar) {
		Position start = new Position(startLine, startChar);
		Position end = new Position(endLine, endChar);
		return new Range(start, end);
	}

	// ------------------- Snippet completion assert

	public static void assertCompletion(String value, TextDocumentSnippetRegistry registry,
			CompletionItem... expectedItems) {
		assertCompletion(value, null, registry, expectedItems);
	}

	public static void assertCompletion(String value, Integer expectedCount, TextDocumentSnippetRegistry registry,
			CompletionItem... expectedItems) {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);
		TextDocument document = new TextDocument(value, "microprofile-config.properties");
		List<CompletionItem> items = registry.getCompletionItems(document, offset, true, true, (context, model) -> {
			return true;
		}, new HashMap<>());
		CompletionList actual = new CompletionList(items);
		assertCompletions(actual, expectedCount, expectedItems);
	}

	public static void assertCompletionWithProperties(String value, Integer expectedCount,
			Collection<String> propertyNames, CompletionItem... expectedItems) {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);
		// Create project info from the property names
		MicroProfileProjectInfo projectInfo = new MicroProfileProjectInfo();
		projectInfo.setProperties(propertyNames.stream().map(propertyName -> {
			ItemMetadata metadata = new ItemMetadata();
			metadata.setName(propertyName);
			return metadata;
		}).collect(Collectors.toList()));
		TextDocumentSnippetRegistry registry = new TextDocumentSnippetRegistry(LanguageId.properties.name());
		TextDocument document = new TextDocument(value, "microprofile-config.properties");
		List<CompletionItem> items = registry.getCompletionItems(document, offset, true, true, (context, model) -> {
			if (context instanceof SnippetContextForProperties) {
				SnippetContextForProperties contextProperties = (SnippetContextForProperties) context;
				return contextProperties.isMatch(projectInfo);
			}
			return false;
		}, new HashMap<>());
		CompletionList actual = new CompletionList(items);
		assertCompletions(actual, expectedCount, expectedItems);
	}

	// ------------------- Hover assert

	public static void assertNoHover(String value) throws BadLocationException {
		MicroProfileHoverSettings hoverSettings = new MicroProfileHoverSettings();
		hoverSettings.setCapabilities(new HoverCapabilities(Arrays.asList(MarkupKind.MARKDOWN), false));
		assertHover(value, null, getDefaultMicroProfileProjectInfo(), hoverSettings, null, null);
	}

	public static void assertHoverMarkdown(String value, String expectedHoverLabel, Integer expectedHoverOffset)
			throws BadLocationException {

		MicroProfileHoverSettings hoverSettings = new MicroProfileHoverSettings();
		HoverCapabilities capabilities = new HoverCapabilities(Arrays.asList(MarkupKind.MARKDOWN), false);
		hoverSettings.setCapabilities(capabilities);

		assertHover(value, null, getDefaultMicroProfileProjectInfo(), hoverSettings, expectedHoverLabel,
				expectedHoverOffset);
	}

	public static void assertHoverPlaintext(String value, String expectedHoverLabel, Integer expectedHoverOffset)
			throws BadLocationException {

		MicroProfileHoverSettings hoverSettings = new MicroProfileHoverSettings();
		HoverCapabilities capabilities = new HoverCapabilities(Arrays.asList(MarkupKind.PLAINTEXT), false);
		hoverSettings.setCapabilities(capabilities);

		assertHover(value, null, getDefaultMicroProfileProjectInfo(), hoverSettings, expectedHoverLabel,
				expectedHoverOffset);
	}

	public static void assertHover(String value, String fileURI, MicroProfileProjectInfo projectInfo,
			MicroProfileHoverSettings hoverSettings, String expectedHoverLabel, Integer expectedHoverOffset)
			throws BadLocationException {

		int offset = value.indexOf("|");
		value = value.substring(0, offset) + value.substring(offset + 1);

		PropertiesModel model = parse(value, fileURI);
		Position position = model.positionAt(offset);

		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();
		IPropertiesModelProvider propertiesModelProvider = documentURI -> model;

		Hover hover = languageService.doHover(model, position, projectInfo, propertiesModelProvider, hoverSettings,
				() -> {
				});
		if (expectedHoverLabel == null) {
			Assert.assertNull(hover);
		} else {
			String actualHoverLabel = getHoverLabel(hover);
			assertEquals(expectedHoverLabel, actualHoverLabel);
			if (expectedHoverOffset != null) {
				Assert.assertNotNull(hover.getRange());
				Assert.assertNotNull(hover.getRange().getStart());
				assertEquals(expectedHoverOffset.intValue(), hover.getRange().getStart().getCharacter());
			}
		}
	}

	private static String getHoverLabel(Hover hover) {
		Either<List<Either<String, MarkedString>>, MarkupContent> contents = hover != null ? hover.getContents() : null;
		if (contents == null) {
			return null;
		}
		return contents.getRight().getValue();
	}

	// ------------------- SymbolInformation assert

	public static void testSymbolInformationsFor(String value, SymbolInformation... expected) {
		testSymbolInformationsFor(value, null, expected);
	}

	public static void testSymbolInformationsFor(String value, String fileURI, SymbolInformation... expected) {

		PropertiesModel model = parse(value, fileURI);

		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();

		List<SymbolInformation> actual = languageService.findSymbolInformations(model, () -> {
		});
		assertSymbolInformations(actual, expected);

	}

	public static SymbolInformation s(final String name, final SymbolKind kind, final String uri, final Range range) {
		return new SymbolInformation(name, kind, new Location(uri, range));
	}

	public static void assertSymbolInformations(List<SymbolInformation> actual, SymbolInformation... expected) {
		assertEquals(expected.length, actual.size());
		Assert.assertArrayEquals(expected, actual.toArray());
	}

	// ------------------- DocumentSymbol assert

	public static void testDocumentSymbolsFor(String value, DocumentSymbol... expected) {
		testDocumentSymbolsFor(value, null, expected);
	}

	public static void testDocumentSymbolsFor(String value, String fileURI, DocumentSymbol... expected) {
		PropertiesModel model = parse(value, fileURI);
		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();
		List<DocumentSymbol> actual = languageService.findDocumentSymbols(model, () -> {
		});
		assertDocumentSymbols(actual, expected);
	}

	public static DocumentSymbol ds(final String name, final SymbolKind kind, final Range range, final String detail) {
		return ds(name, kind, range, detail, new ArrayList<>());
	}

	public static DocumentSymbol ds(final String name, final SymbolKind kind, final Range range, final String detail,
			final List<DocumentSymbol> children) {
		return new DocumentSymbol(name, kind, range, range, detail, children);
	}

	public static void assertDocumentSymbols(List<DocumentSymbol> actual, DocumentSymbol... expected) {
		assertEquals(expected.length, actual.size());
		Assert.assertArrayEquals(expected, actual.toArray());
	}

	// ------------------- Definition assert

	public static void testDefinitionFor(String value, LocationLink... expected)
			throws BadLocationException, InterruptedException, ExecutionException {
		testDefinitionFor(value, null, expected);
	}

	public static void testDefinitionFor(String value, String documentName, LocationLink... expected)
			throws BadLocationException, InterruptedException, ExecutionException {

		testDefinitionFor(value, documentName, getDefaultMicroProfileProjectInfo(),
				getDefaultMicroProfilePropertyDefinitionProvider(), expected);
	}

	public static void testDefinitionFor(String value, String documentName, MicroProfileProjectInfo projectInfo,
			MicroProfilePropertyDefinitionProvider definitionProvider, LocationLink... expected)
			throws BadLocationException, InterruptedException, ExecutionException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();
		PropertiesModel document = parse(value, documentName);
		Position position = document.positionAt(offset);
		IPropertiesModelProvider propertiesModelProvider = documentURI -> document;

		Either<List<? extends Location>, List<? extends LocationLink>> actual = languageService.findDefinition(document,
				position, projectInfo, propertiesModelProvider, definitionProvider, true, NOOP_CHECKER).get();
		assertLocationLink(actual.getRight(), expected);

	}

	public static LocationLink ll(final String uri, final Range originRange, Range targetRange) {
		return new LocationLink(uri, targetRange, targetRange, originRange);
	}

	public static void assertLocationLink(List<? extends LocationLink> actual, LocationLink... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			actual.get(i).setTargetUri(actual.get(i).getTargetUri().replaceAll("file:///", "file:/"));
			expected[i].setTargetUri(expected[i].getTargetUri().replaceAll("file:///", "file:/"));
		}
		Assert.assertArrayEquals(expected, actual.toArray());
	}

	// ------------------- Diagnostics assert

	public static void testDiagnosticsFor(String value, Diagnostic... expected) {
		testDiagnosticsFor(value, getDefaultMicroProfileProjectInfo(), expected);
	}

	public static void testDiagnosticsFor(String value, MicroProfileProjectInfo projectInfo, Diagnostic... expected) {
		MicroProfileValidationSettings validationSettings = new MicroProfileValidationSettings();
		testDiagnosticsFor(value, projectInfo, validationSettings, expected);
	}

	public static void testDiagnosticsFor(String value, MicroProfileProjectInfo projectInfo,
			MicroProfileValidationSettings validationSettings, Diagnostic... expected) {
		testDiagnosticsFor(value, null, null, projectInfo, validationSettings, expected);
	}

	public static void testDiagnosticsFor(String value, Integer expectedCount, MicroProfileProjectInfo projectInfo,
			MicroProfileValidationSettings validationSettings, Diagnostic... expected) {
		testDiagnosticsFor(value, null, expectedCount, projectInfo, validationSettings, expected);
	}

	public static void testDiagnosticsFor(String value, String fileURI, Integer expectedCount,
			MicroProfileProjectInfo projectInfo, MicroProfileValidationSettings validationSettings,
			Diagnostic... expected) {
		PropertiesModel model = parse(value, fileURI);
		IPropertiesModelProvider propertiesModelProvider = documentURI -> model;
		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();
		List<Diagnostic> actual = languageService.doDiagnostics(model, projectInfo, propertiesModelProvider,
				validationSettings, () -> {
				});
		if (expectedCount != null) {
			assertEquals(expectedCount.intValue(), actual.size());
		}
		assertDiagnostics(actual, expected);
	}

	public static void assertDiagnostics(List<Diagnostic> actual, Diagnostic... expected) {
		assertDiagnostics(actual, Arrays.asList(expected));
	}

	public static void assertDiagnostics(List<Diagnostic> actual, List<Diagnostic> expected) {
		assertEquals("Unexpected diagnostics:\n", expected, actual);
	}

	public static Diagnostic d(int line, int startCharacter, int endCharacter, String message,
			DiagnosticSeverity severity, ValidationType code) {
		return d(line, startCharacter, line, endCharacter, message, severity, code);
	}

	public static Diagnostic d(int startLine, int startCharacter, int endLine, int endCharacter, String message,
			DiagnosticSeverity severity, ValidationType code) {
		return new Diagnostic(r(startLine, startCharacter, endLine, endCharacter), message, severity,
				MICROPROFILE_DIAGNOSTIC_SOURCE, code.name());
	}

	// ------------------- CodeAction assert

	public static void testCodeActionsFor(String value, Diagnostic diagnostic, CodeAction... expected) {
		testCodeActionsFor(value, diagnostic, getDefaultMicroProfileProjectInfo(), new MicroProfileFormattingSettings(),
				expected);
	}

	public static void testCodeActionsFor(String value, Diagnostic diagnostic, MicroProfileProjectInfo projectInfo,
			CodeAction... expected) {
		testCodeActionsFor(value, Collections.singletonList(diagnostic), diagnostic.getRange(), projectInfo,
				new MicroProfileFormattingSettings(), expected);
	}

	public static void testCodeActionsFor(String value, Diagnostic diagnostic, MicroProfileProjectInfo projectInfo,
			MicroProfileFormattingSettings formattingSettings, CodeAction... expected) {
		testCodeActionsFor(value, Collections.singletonList(diagnostic), diagnostic.getRange(), projectInfo,
				formattingSettings, expected);
	}

	public static void testCodeActionsFor(String value, List<Diagnostic> diagnostics, Range range,
			MicroProfileProjectInfo projectInfo, CodeAction... expected) {
		testCodeActionsFor(value, diagnostics, range, projectInfo, new MicroProfileFormattingSettings(), expected);
	}

	public static void testCodeActionsFor(String value, List<Diagnostic> diagnostics, Range range,
			MicroProfileProjectInfo projectInfo, MicroProfileFormattingSettings formattingSettings,
			CodeAction... expected) {
		PropertiesModel model = parse(value, null);
		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();

		CodeActionContext context = new CodeActionContext();
		context.setDiagnostics(diagnostics);

		MicroProfileCommandCapabilities mpCommandCapabilities = new MicroProfileCommandCapabilities();

		List<String> valueSet = Arrays.asList(CommandKind.COMMAND_CONFIGURATION_UPDATE);
		CommandKindCapabilities commandKindCapabilities = new CommandKindCapabilities(valueSet);
		CommandCapabilities commandCapabilities = new CommandCapabilities(commandKindCapabilities);

		mpCommandCapabilities.setCapabilities(commandCapabilities);

		List<CodeAction> actual = languageService.doCodeActions(context, range, model, projectInfo, formattingSettings,
				mpCommandCapabilities, NOOP_CHECKER);
		assertCodeActions(actual, expected);
	}

	public static void assertCodeActions(List<CodeAction> actual, CodeAction... expected) {
		actual.stream().forEach(ca -> {
			// we don't want to compare title, etc
			ca.setKind(null);
			if (ca.getDiagnostics() != null) {
				ca.getDiagnostics().forEach(d -> {
					d.setSeverity(null);
					d.setMessage("");
					d.setSource(null);
				});
			}
		});

		assertEquals(expected.length, actual.size());
		Assert.assertArrayEquals(expected, actual.toArray());
	}

	public static CodeAction ca(String title, TextEdit te, Diagnostic... d) {
		return ca(title, te, null, d);
	}

	public static CodeAction ca(String title, Command command, Diagnostic... d) {
		return ca(title, null, command, d);
	}

	public static CodeAction ca(String title, TextEdit te, Command command, Diagnostic... d) {
		List<Diagnostic> diagnostics = new ArrayList<>();
		for (int i = 0; i < d.length; i++) {
			diagnostics.add(d[i]);
		}
		return ca(title, te, command, diagnostics);
	}

	public static CodeAction ca(String title, TextEdit te, Command command, List<Diagnostic> diagnostics) {
		CodeAction codeAction = new CodeAction();
		codeAction.setTitle(title);
		codeAction.setDiagnostics(diagnostics);
		codeAction.setCommand(command);

		if (te != null) {
			VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(
					"microprofile-config.properties", 0);
			TextDocumentEdit textDocumentEdit = new TextDocumentEdit(versionedTextDocumentIdentifier,
					Collections.singletonList(te));
			WorkspaceEdit workspaceEdit = new WorkspaceEdit(
					Collections.singletonList(Either.forLeft(textDocumentEdit)));
			codeAction.setEdit(workspaceEdit);
		}

		return codeAction;
	}

	public static TextEdit te(int startLine, int startCharacter, int endLine, int endCharacter, String newText) {
		TextEdit textEdit = new TextEdit();
		textEdit.setNewText(newText);
		textEdit.setRange(r(startLine, startCharacter, endLine, endCharacter));
		return textEdit;
	}

	// ------------------- Formatting assert

	public static void assertFormat(String value, String expected, boolean insertSpaces) {
		MicroProfileFormattingSettings formattingSettings = new MicroProfileFormattingSettings();
		formattingSettings.setSurroundEqualsWithSpaces(insertSpaces);
		assertFormat(value, expected, formattingSettings);
	}

	public static void assertFormat(String value, String expected, MicroProfileFormattingSettings formattingSettings) {

		PropertiesModel model = parse(value, null);
		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();
		List<? extends TextEdit> edits = languageService.doFormat(model, formattingSettings);

		String formatted = edits.stream().map(edit -> edit.getNewText()).collect(Collectors.joining(""));
		assertEquals(expected, formatted);
	}

	public static void assertRangeFormat(String value, String expected, boolean insertSpaces)
			throws BadLocationException {
		MicroProfileFormattingSettings formattingSettings = new MicroProfileFormattingSettings();
		formattingSettings.setSurroundEqualsWithSpaces(insertSpaces);
		assertRangeFormat(value, expected, formattingSettings);
	}

	public static void assertRangeFormat(String value, String expected,
			MicroProfileFormattingSettings formattingSettings) throws BadLocationException {

		int startOffset = value.indexOf("|");
		value = value.substring(0, startOffset) + value.substring(startOffset + 1);
		int endOffset = value.indexOf("|");
		value = value.substring(0, endOffset) + value.substring(endOffset + 1);
		TextDocument document = new TextDocument(value, "microprofile-config.properties");
		Range range = PositionUtils.createRange(startOffset, endOffset, document);

		PropertiesModel model = parse(value, null);
		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();
		List<? extends TextEdit> edits = languageService.doRangeFormat(model, range, formattingSettings);

		Range formatRange = edits.get(0).getRange();
		int formatStart = document.offsetAt(formatRange.getStart());
		int formatEnd = document.offsetAt(formatRange.getEnd());

		String formatted = value.substring(0, formatStart)
				+ edits.stream().map(edit -> edit.getNewText()).collect(Collectors.joining(""))
				+ value.substring(formatEnd);
		assertEquals(expected, formatted);
	}

	// ------------------- Document Highlight Assert

	public static void assertDocumentHighlight(String value, Range... expected) throws BadLocationException {
		int offset = value.indexOf("|");
		value = value.substring(0, offset) + value.substring(offset + 1);
		TextDocument document = new TextDocument(value, "microprofile-config.properties");
		PropertiesModel model = parse(value, null);
		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();
		Object[] actual = languageService.findDocumentHighlight(model, document.positionAt(offset), NOOP_CHECKER)
				.stream().map(dh -> {
					return dh.getRange();
				}).collect(Collectors.toList()).toArray();

		Assert.assertArrayEquals(expected, actual);
	}

	private static PropertiesModel parse(String text, String uri) {
		TextDocument document = new TextDocument(text, uri != null ? uri : "microprofile-config.properties");
		return PropertiesModel.parse(document, () -> {
		});
	}

	// ------------------- InlayHint assert

	public static void testInlayHintFor(String value, InlayHint... expected) throws Exception {
		testInlayHintFor(value, null, expected);
	}

	public static void testInlayHintFor(String value, MicroProfileInlayHintSettings inlayHintSettings,
			InlayHint... expected) throws Exception {
		testInlayHintFor(value, inlayHintSettings, getDefaultMicroProfileProjectInfo(), expected);
	}

	public static void testInlayHintFor(String value, MicroProfileInlayHintSettings inlayHintSettings,
			MicroProfileProjectInfo projectInfo, InlayHint... expected) throws Exception {
		PropertiesModel model = parse(value, null);
		Range range = null;
		PropertiesFileLanguageService languageService = new PropertiesFileLanguageService();
		IPropertiesModelProvider propertiesModelProvider = documentURI -> model;
		List<InlayHint> actual = languageService.getInlayHint(model, projectInfo, propertiesModelProvider, range,
				() -> {
				});
		assertInlayHint(actual, expected);
	}

	public static InlayHint ih(Position position, String label) {
		return new InlayHint(position, Either.forLeft(label));
	}

	public static InlayHint ih(Position position, InlayHintLabelPart... parts) {
		return new InlayHint(position, Either.forRight(Arrays.asList(parts)));
	}

	public static InlayHintLabelPart ihLabel(String label) {
		return new InlayHintLabelPart(label);
	}

	public static InlayHintLabelPart ihLabel(String label, String tooltip, Command command) {
		InlayHintLabelPart part = ihLabel(label);
		part.setCommand(command);
		part.setTooltip(tooltip);
		return part;
	}

	public static void assertInlayHint(List<? extends InlayHint> actual, InlayHint... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals("position at " + i, expected[i].getPosition(), actual.get(i).getPosition());
			assertEquals("label at " + i, expected[i].getLabel(), actual.get(i).getLabel());
		}
	}

	// Utilities

	public static Position p(int line, int character) {
		return new Position(line, character);
	}
}