/*******************************************************************************
* Copyright (c) 2018 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.ls.commons;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SnippetTextEdit;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionData;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;

/**
 * Factory for simple {@link CodeAction}
 *
 */
public class CodeActionFactory {

	/**
	 * Create a CodeAction to remove the content from the given range.
	 *
	 * @param title
	 * @param range
	 * @param document
	 * @param diagnostic
	 * @return
	 */
	public static CodeAction remove(String title, MicroProfileCodeActionId id, Range range, TextDocumentItem document, Diagnostic diagnostic) {
		return replace(title, id, range, "", document, diagnostic);
	}

	/**
	 * Create a CodeAction to insert a new content at the end of the given range.
	 *
	 * @param title
	 * @param range
	 * @param insertText
	 * @param document
	 * @param diagnostic
	 * @return
	 */
	public static CodeAction insert(String title, MicroProfileCodeActionId id, Position position, String insertText, TextDocumentItem document,
			Diagnostic diagnostic) {
		return insert(title, id, position, insertText, document, Arrays.asList(diagnostic));
	}

	/**
	 * Create a CodeAction to insert a new content at the end of the given range.
	 *
	 * @param title
	 * @param range
	 * @param insertText
	 * @param document
	 * @param diagnostics
	 * @return
	 */
	public static CodeAction insert(String title, MicroProfileCodeActionId id, Position position, String insertText, TextDocumentItem document,
			List<Diagnostic> diagnostics) {
		CodeAction insertContentAction = new CodeAction(title);
		insertContentAction.setKind(CodeActionKind.QuickFix);
		insertContentAction.setDiagnostics(diagnostics);
		insertContentAction.setData(new CodeActionData(id));
		TextEdit edit = new TextEdit(new Range(position, position), insertText);
		VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(
				document.getUri(), document.getVersion());

		TextDocumentEdit textDocumentEdit = new TextDocumentEdit(versionedTextDocumentIdentifier,
				Collections.singletonList(Either.<TextEdit, SnippetTextEdit>forLeft(edit)));
		WorkspaceEdit workspaceEdit = new WorkspaceEdit(Collections.singletonList(Either.forLeft(textDocumentEdit)));
		insertContentAction.setEdit(workspaceEdit);
		return insertContentAction;
	}

	public static CodeAction replace(String title, MicroProfileCodeActionId id, Range range, String replaceText, TextDocumentItem document,
			Diagnostic diagnostic) {
		CodeAction replaceContentAction = new CodeAction(title);
		replaceContentAction.setKind(CodeActionKind.QuickFix);
		replaceContentAction.setDiagnostics(Arrays.asList(diagnostic));
		replaceContentAction.setData(new CodeActionData(id));
		TextEdit edit = new TextEdit(range, replaceText);
		VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(
				document.getUri(), document.getVersion());

		TextDocumentEdit textDocumentEdit = new TextDocumentEdit(versionedTextDocumentIdentifier,
				Collections.singletonList(Either.<TextEdit, SnippetTextEdit>forLeft(edit)));
		WorkspaceEdit workspaceEdit = new WorkspaceEdit(Collections.singletonList(Either.forLeft(textDocumentEdit)));
		replaceContentAction.setEdit(workspaceEdit);
		return replaceContentAction;
	}

	public static boolean isDiagnosticCode(Either<String, Integer> diagnosticCode, String code) {
		if (diagnosticCode == null || diagnosticCode.isRight()) {
			return false;
		}
		return code.equals(diagnosticCode.getLeft());
	}
}
