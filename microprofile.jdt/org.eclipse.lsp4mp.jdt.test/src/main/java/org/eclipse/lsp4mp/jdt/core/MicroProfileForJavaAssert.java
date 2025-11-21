/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.jdt.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDefinitionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionData;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.IJavaErrorCode;
import org.eclipse.lsp4mp.jdt.core.utils.IJDTUtils;
import org.eclipse.lsp4mp.jdt.core.utils.JDTMicroProfileUtils;
import org.junit.Assert;

/**
 * MicroProfile assert for java files for JUnit tests.
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileForJavaAssert {

	// ------------------- Assert for CodeAction

	public static MicroProfileJavaCodeActionParams createCodeActionParams(String uri, Diagnostic d) {
		return createCodeActionParams(uri, d, true);
	}

	public static MicroProfileJavaCodeActionParams createCodeActionParams(String uri, Diagnostic d,
			boolean commandSupported) {
		TextDocumentIdentifier textDocument = new TextDocumentIdentifier(uri);
		Range range = d.getRange();
		CodeActionContext context = new CodeActionContext();
		context.setDiagnostics(Arrays.asList(d));
		MicroProfileJavaCodeActionParams codeActionParams = new MicroProfileJavaCodeActionParams(textDocument, range,
				context);
		codeActionParams.setResourceOperationSupported(true);
		codeActionParams.setCommandConfigurationUpdateSupported(commandSupported);
		codeActionParams.setResolveSupported(false);
		return codeActionParams;
	}

	public static void assertJavaCodeAction(MicroProfileJavaCodeActionParams params, IJDTUtils utils,
			CodeAction... expected) throws JavaModelException {
		List<? extends CodeAction> actual = PropertiesManagerForJava.getInstance().codeAction(params, utils,
				new NullProgressMonitor());
		assertCodeActions(actual != null && actual.size() > 0 ? actual : Collections.emptyList(), expected);
	}

	public static void assertCodeActions(List<? extends CodeAction> actual, CodeAction... expected) {
		actual.stream().forEach(ca -> {
			// we don't want to compare title, etc
			ca.setCommand(null);
			ca.setKind(null);

			if (ca.getEdit() != null && ca.getEdit().getChanges() != null) {
				assertTrue(ca.getEdit().getChanges().isEmpty());
				ca.getEdit().setChanges(null);
			}
			if (ca.getDiagnostics() != null) {
				ca.getDiagnostics().forEach(d -> {
					d.setSeverity(null);
					d.setMessage("");
					d.setSource(null);
				});
			}
		});

		Assert.assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			Assert.assertEquals("Assert title [" + i + "]", expected[i].getTitle(), actual.get(i).getTitle());
			Assert.assertEquals("Assert edit [" + i + "]", expected[i].getEdit(), actual.get(i).getEdit());
			Assert.assertEquals("Assert id [" + i + "]", ((CodeActionData)(expected[i].getData())).getCodeActionId(), ((CodeActionData)(actual.get(i).getData())).getCodeActionId());
		}
	}

	public static CodeAction ca(String uri, String title, MicroProfileCodeActionId id, Diagnostic d, TextEdit... te) {
		CodeAction codeAction = new CodeAction();
		codeAction.setTitle(title);
		codeAction.setDiagnostics(Arrays.asList(d));

		VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(uri, 0);

		TextDocumentEdit textDocumentEdit = new TextDocumentEdit(versionedTextDocumentIdentifier, Arrays.asList(te));
		WorkspaceEdit workspaceEdit = new WorkspaceEdit(Arrays.asList(Either.forLeft(textDocumentEdit)));
		codeAction.setEdit(workspaceEdit);
		codeAction.setData(new CodeActionData(id));
		return codeAction;
	}

	public static TextEdit te(int startLine, int startCharacter, int endLine, int endCharacter, String newText) {
		TextEdit textEdit = new TextEdit();
		textEdit.setNewText(newText);
		textEdit.setRange(r(startLine, startCharacter, endLine, endCharacter));
		return textEdit;
	}

	// ------------------- Assert for Completion

	public static void assertJavaCompletion(MicroProfileJavaCompletionParams params, IJDTUtils utils,
			CompletionItem... expected) throws JavaModelException {
		CompletionList actual = PropertiesManagerForJava.getInstance().completion(params, utils,
				new NullProgressMonitor());
		assertCompletion(actual != null && actual.getItems() != null && actual.getItems().size() > 0 ? actual.getItems()
				: Collections.emptyList(), expected);
	}

	public static void assertCompletion(List<? extends CompletionItem> actual, CompletionItem... expected) {
		actual.stream().forEach(completionItem -> {
			completionItem.setDetail(null);
			completionItem.setFilterText(null);
			completionItem.setDocumentation((String) null);
		});

		Assert.assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			Assert.assertEquals("Assert TextEdit [" + i + "]", expected[i].getTextEdit(), actual.get(i).getTextEdit());
			Assert.assertEquals("Assert label [" + i + "]", expected[i].getLabel(), actual.get(i).getLabel());
			Assert.assertEquals("Assert Kind [" + i + "]", expected[i].getKind(), actual.get(i).getKind());
		}
	}

	public static CompletionItem c(TextEdit textEdit, String label, CompletionItemKind kind) {
		CompletionItem completionItem = new CompletionItem();
		completionItem.setTextEdit(Either.forLeft(textEdit));
		completionItem.setKind(kind);
		completionItem.setLabel(label);
		return completionItem;
	}

	// Assert for diagnostics

	public static Diagnostic d(int line, int startCharacter, int endCharacter, String message,
			DiagnosticSeverity severity, final String source, IJavaErrorCode code) {
		return d(line, startCharacter, line, endCharacter, message, severity, source, code);
	}

	public static Diagnostic d(int startLine, int startCharacter, int endLine, int endCharacter, String message,
			DiagnosticSeverity severity, final String source, IJavaErrorCode code) {
		// Diagnostic on 1 line
		return new Diagnostic(r(startLine, startCharacter, endLine, endCharacter), message, severity, source,
				code != null ? code.getCode() : null);
	}

	public static Range r(int line, int startCharacter, int endCharacter) {
		return r(line, startCharacter, line, endCharacter);
	}

	public static Range r(int startLine, int startCharacter, int endLine, int endCharacter) {
		return new Range(p(startLine, startCharacter), p(endLine, endCharacter));
	}

	public static Position p(int line, int character) {
		return new Position(line, character);
	}

	public static void assertJavaDiagnostics(MicroProfileJavaDiagnosticsParams params, IJDTUtils utils,
			Diagnostic... expected) throws JavaModelException {
		List<PublishDiagnosticsParams> actual = PropertiesManagerForJava.getInstance().diagnostics(params, utils,
				new NullProgressMonitor());
		assertDiagnostics(
				actual != null && actual.size() > 0 ? actual.get(0).getDiagnostics() : Collections.emptyList(),
				expected);
	}

	public static void assertDiagnostics(List<Diagnostic> actual, Diagnostic... expected) {
		assertDiagnostics(actual, Arrays.asList(expected), false);
	}

	public static void assertDiagnostics(List<Diagnostic> actual, List<Diagnostic> expected, boolean filter) {
		List<Diagnostic> received = actual;
		final boolean filterMessage;
		if (expected != null && !expected.isEmpty()
				&& (expected.get(0).getMessage() == null || expected.get(0).getMessage().isEmpty())) {
			filterMessage = true;
		} else {
			filterMessage = false;
		}
		if (filter) {
			received = actual.stream().map(d -> {
				Diagnostic simpler = new Diagnostic(d.getRange(), "");
				simpler.setCode(d.getCode());
				if (filterMessage) {
					simpler.setMessage(d.getMessage());
				}
				return simpler;
			}).collect(Collectors.toList());
		}
		Assert.assertArrayEquals("Unexpected diagnostics:", expected.toArray(), received.toArray());
	}

	// Assert for Hover

	public static void assertJavaHover(Position hoverPosition, String javaFileUri, IJDTUtils utils, Hover expected)
			throws JavaModelException {
		MicroProfileJavaHoverParams params = new MicroProfileJavaHoverParams();
		params.setDocumentFormat(DocumentFormat.Markdown);
		params.setPosition(hoverPosition);
		params.setUri(javaFileUri);
		params.setSurroundEqualsWithSpaces(true);
		assertJavaHover(params, utils, expected);
	}

	public static void assertJavaHover(MicroProfileJavaHoverParams params, IJDTUtils utils, Hover expected)
			throws JavaModelException {
		Hover actual = PropertiesManagerForJava.getInstance().hover(params, utils, new NullProgressMonitor());
		assertHover(expected, actual);
	}

	public static void assertHover(Hover expected, Hover actual) {
		if (expected == null || actual == null) {
			assertEquals(expected, actual);
		} else {
			assertEquals(expected.getContents().getRight(), actual.getContents().getRight());
			assertEquals(expected.getRange(), actual.getRange());
		}
	}

	public static Hover h(String hoverContent, int startLine, int startCharacter, int endLine, int endCharacter) {
		Range range = r(startLine, startCharacter, endLine, endCharacter);
		Hover hover = new Hover();
		hover.setContents(Either.forRight(new MarkupContent(MarkupKind.MARKDOWN, hoverContent)));
		hover.setRange(range);
		return hover;
	}

	public static Hover h(String hoverContent, int line, int startCharacter, int endCharacter) {
		return h(hoverContent, line, startCharacter, line, endCharacter);
	}

	// Assert for Definition

	public static void assertJavaDefinitions(Position position, String javaFileUri, IJDTUtils utils,
			MicroProfileDefinition... expected) throws JavaModelException {
		MicroProfileJavaDefinitionParams params = new MicroProfileJavaDefinitionParams();
		params.setPosition(position);
		params.setUri(javaFileUri);
		List<MicroProfileDefinition> actual = PropertiesManagerForJava.getInstance().definition(params, utils,
				new NullProgressMonitor());
		assertDefinitions(actual, expected);
	}

	public static void assertDefinitions(List<MicroProfileDefinition> actual, MicroProfileDefinition... expected) {
		Assert.assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			Assert.assertEquals("Assert selectPropertyName [" + i + "]", expected[i].getSelectPropertyName(),
					actual.get(i).getSelectPropertyName());
			Assert.assertEquals("Assert location [" + i + "]", expected[i].getLocation(), actual.get(i).getLocation());
		}
	}

	public static MicroProfileDefinition def(Range originSelectionRange, String targetUri, Range targetRange) {
		return def(originSelectionRange, targetUri, targetRange, null);
	}

	public static MicroProfileDefinition def(Range originSelectionRange, String targetUri, String selectPropertyName) {
		return def(originSelectionRange, targetUri, null, selectPropertyName);
	}

	private static MicroProfileDefinition def(Range originSelectionRange, String targetUri, Range targetRange,
			String selectPropertyName) {
		MicroProfileDefinition definition = new MicroProfileDefinition();
		LocationLink location = new LocationLink();
		location.setOriginSelectionRange(originSelectionRange);
		location.setTargetUri(targetUri);
		if (targetRange != null) {
			location.setTargetRange(targetRange);
			location.setTargetSelectionRange(targetRange);
		}
		definition.setLocation(location);
		definition.setSelectPropertyName(selectPropertyName);
		return definition;
	}

	public static String fixURI(URI uri) {
		String uriString = uri.toString();
		return uriString.replaceFirst("file:/([^/])", "file:///$1");
	}

	// Assert for WorkspaceSymbol

	/**
	 * Returns a new symbol information.
	 *
	 * @param name  the name of the symbol
	 * @param range the range of the symbol
	 * @return a new symbol information
	 */
	public static SymbolInformation si(String name, Range range) {
		SymbolInformation symbolInformation = new SymbolInformation();
		symbolInformation.setName(name);
		Location location = new Location("", range);
		symbolInformation.setLocation(location);
		return symbolInformation;
	}

	/**
	 * Asserts that the actual workspace symbols for the given project are the same
	 * as the list of expected workspace symbols.
	 *
	 * @param javaProject the project to check the workspace symbols of
	 * @param utils       the jdt utils
	 * @param expected    the expected workspace symbols
	 * @throws JavaModelException
	 */
	public static void assertWorkspaceSymbols(IJavaProject javaProject, IJDTUtils utils, SymbolInformation... expected)
			throws JavaModelException {
		List<SymbolInformation> actual = PropertiesManagerForJava.getInstance()
				.workspaceSymbols(JDTMicroProfileUtils.getProjectURI(javaProject), utils, new NullProgressMonitor());
		MicroProfileForJavaAssert.assertWorkspaceSymbols(Arrays.asList(expected), actual);
	}

	/**
	 * Asserts that the given lists of workspace symbols are the same.
	 *
	 * @param expected the expected symbols
	 * @param actual   the actual symbols
	 */
	public static void assertWorkspaceSymbols(List<SymbolInformation> expected, List<SymbolInformation> actual) {
		assertEquals(expected.size(), actual.size());
		Collections.sort(expected, (si1, si2) -> si1.getName().compareTo(si2.getName()));
		Collections.sort(actual, (si1, si2) -> si1.getName().compareTo(si2.getName()));
		for (int i = 0; i < expected.size(); i++) {
			assertSymbolInformation(expected.get(i), actual.get(i));
		}
	}

	/**
	 * Asserts that the expected and actual symbol informations' name and range are
	 * the same.
	 *
	 * Doesn't check any of the other properties. For instance, the URI is avoided
	 * since this will change between systems
	 *
	 * @param expected the expected symbol information
	 * @param actual   the actual symbol information
	 */
	public static void assertSymbolInformation(SymbolInformation expected, SymbolInformation actual) {
		assertEquals(expected.getName(), actual.getName());
		assertEquals(expected.getLocation().getRange(), actual.getLocation().getRange());
	}

	// Assert for CodeLens

	/**
	 * Asserts that the expected code lens are in the document specified by the
	 * params.
	 *
	 * @param params   the parameters specifying the document to get the code lens
	 *                 for
	 * @param utils    the jdt utils
	 * @param expected the list of expected code lens
	 * @throws JavaModelException
	 */
	public static void assertCodeLens(MicroProfileJavaCodeLensParams params, IJDTUtils utils, CodeLens... expected)
			throws JavaModelException {
		List<? extends CodeLens> actual = PropertiesManagerForJava.getInstance().codeLens(params, utils,
				new NullProgressMonitor());
		assertEquals(expected.length, actual.size());

		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], actual.get(i));
		}
	}

	/**
	 * Returns a new code lens.
	 *
	 * @param title     the title of the code lens
	 * @param commandId the id of the command to run when the code lens is clicked
	 * @param range     the range of the code lens
	 * @return a new code lens
	 */
	public static CodeLens cl(String title, String commandId, Range range) {
		CodeLens codeLens = new CodeLens(range);
		codeLens.setCommand(new Command(title, commandId, Collections.singletonList(title)));
		return codeLens;
	}

}
