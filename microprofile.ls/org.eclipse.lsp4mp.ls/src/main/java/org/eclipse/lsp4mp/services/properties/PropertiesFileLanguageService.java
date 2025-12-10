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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4mp.commons.MicroProfileInlayHintSettings;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.extensions.ExtendedMicroProfileProjectInfo;
import org.eclipse.lsp4mp.ls.api.MicroProfilePropertyDefinitionProvider;
import org.eclipse.lsp4mp.ls.api.MicroProfilePropertyDocumentationProvider;
import org.eclipse.lsp4mp.model.PropertiesModel;
import org.eclipse.lsp4mp.services.properties.extensions.PropertiesFileExtensionRegistry;
import org.eclipse.lsp4mp.settings.MicroProfileCommandCapabilities;
import org.eclipse.lsp4mp.settings.MicroProfileCompletionCapabilities;
import org.eclipse.lsp4mp.settings.MicroProfileExecutionSettings;
import org.eclipse.lsp4mp.settings.MicroProfileFormattingSettings;
import org.eclipse.lsp4mp.settings.MicroProfileHoverSettings;
import org.eclipse.lsp4mp.settings.MicroProfileValidationSettings;

/**
 * The properties file language service.
 *
 * @author Angelo ZERR
 *
 */
public class PropertiesFileLanguageService extends PropertiesFileExtensionRegistry {

	private final PropertiesFileCompletions completions;
	private final PropertiesFileSymbolsProvider symbolsProvider;
	private final PropertiesFileHover hover;
	private final PropertiesFileDefinition definition;
	private final PropertiesFileDiagnostics diagnostics;
	private final PropertiesFileFormatter formatter;
	private final PropertiesFileCodeActions codeActions;
	private final PropertiesFileDocumentHighlight documentHighlight;
	private final PropertiesFileInlayHint inlayHint;

	public PropertiesFileLanguageService() {
		this.completions = new PropertiesFileCompletions();
		this.symbolsProvider = new PropertiesFileSymbolsProvider();
		this.hover = new PropertiesFileHover();
		this.definition = new PropertiesFileDefinition();
		this.diagnostics = new PropertiesFileDiagnostics(this);
		this.formatter = new PropertiesFileFormatter();
		this.codeActions = new PropertiesFileCodeActions();
		this.documentHighlight = new PropertiesFileDocumentHighlight();
		this.inlayHint = new PropertiesFileInlayHint();
	}

	/**
	 * Returns completion list for the given position
	 *
	 * @param document               the properties model document
	 * @param position               the position where completion was triggered
	 * @param projectInfo            the MicroProfile project information
	 * @param completionCapabilities the completion capabilities
	 * @param cancelChecker          the cancel checker
	 * @return completion list for the given position
	 */
	public CompletionList doComplete(PropertiesModel document, Position position, MicroProfileProjectInfo projectInfo,
			MicroProfileCompletionCapabilities completionCapabilities,
			MicroProfileFormattingSettings formattingSettings, CancelChecker cancelChecker) {
		updateProperties(projectInfo, document);
		return completions.doComplete(document, position, projectInfo, completionCapabilities, formattingSettings,
				cancelChecker);
	}

	/**
	 * Returns the completion item with the empty fields resolved.
	 *
	 * @param unresolved             the unresolved completion item
	 * @param projectInfo            the MicroProfile project information
	 * @param completionCapabilities the completion capabilities
	 * @param cancelChecker          the cancel checker
	 * @return the completion item with the empty fields resolved.
	 */
	public CompletionItem resolveCompletionItem(CompletionItem unresolved, MicroProfileProjectInfo projectInfo,
			MicroProfileCompletionCapabilities completionCapabilities, CancelChecker cancelChecker) {
		return completions.resolveCompletionItem(unresolved, projectInfo, completionCapabilities, cancelChecker);
	}

	/**
	 * Returns Hover object for the currently hovered token
	 *
	 * @param document              the properties model document
	 * @param position              the hover position
	 * @param projectInfo           the MicroProfile project information
	 * @param hoverSettings         the hover settings
	 * @param documentationProvider the documentation provider
	 * @param cancelChecker         the cancel checker
	 * @return Hover object for the currently hovered token
	 */
	public CompletableFuture<Hover> doHover(PropertiesModel document, Position position,
			MicroProfileProjectInfo projectInfo, MicroProfileHoverSettings hoverSettings,
			MicroProfilePropertyDocumentationProvider documentationProvider, CancelChecker cancelChecker) {
		updateProperties(projectInfo, document);
		return hover.doHover(document, position, projectInfo, hoverSettings, documentationProvider, cancelChecker);
	}

	/**
	 * Returns symbol information list for the given properties model.
	 *
	 * @param document      the properties model document
	 * @param cancelChecker the cancel checker
	 * @return symbol information list for the given properties model.
	 */
	public List<SymbolInformation> findSymbolInformations(PropertiesModel document, CancelChecker cancelChecker) {
		return symbolsProvider.findSymbolInformations(document, cancelChecker);
	}

	/**
	 * Returns document symbol list for the given properties model.
	 *
	 * @param document      the properties model document
	 * @param cancelChecker the cancel checker
	 * @return document symbol list for the given properties model.
	 */
	public List<DocumentSymbol> findDocumentSymbols(PropertiesModel document, CancelChecker cancelChecker) {
		return symbolsProvider.findDocumentSymbols(document, cancelChecker);
	}

	/**
	 * Returns as promise the Java field definition location of the property at the
	 * given <code>position</code> of the given application.properties
	 * <code>document</code>.
	 *
	 * @param document              the properties model.
	 * @param position              the position where definition was triggered
	 * @param projectInfo           the MicroProfile project info
	 * @param provider              the MicroProfile property definition provider.
	 * @param definitionLinkSupport true if {@link LocationLink} must be returned
	 *                              and false otherwise.
	 * @param cancelChecker         the cancel checker
	 * @return as promise the Java field definition location of the property at the
	 *         given <code>position</code> of the given application.properties
	 *         <code>document</code>.
	 */
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> findDefinition(
			PropertiesModel document, Position position, MicroProfileProjectInfo projectInfo,
			MicroProfilePropertyDefinitionProvider provider, boolean definitionLinkSupport,
			CancelChecker cancelChecker) {
		updateProperties(projectInfo, document);
		CompletableFuture<List<LocationLink>> definitionLocationLinks = definition.findDefinition(document, position,
				projectInfo, provider, cancelChecker);
		if (definitionLinkSupport) {
			return definitionLocationLinks.thenApply((List<LocationLink> resolvedLinks) -> {
				return Either.forRight(resolvedLinks);
			});
		}
		cancelChecker.checkCanceled();
		return definitionLocationLinks.thenApply((List<LocationLink> resolvedLinks) -> {
			return Either.forLeft(resolvedLinks.stream().map((link) -> {
				return new Location(link.getTargetUri(), link.getTargetRange());
			}).collect(Collectors.toList()));
		});
	}

	/**
	 * Returns a <code>List<TextEdit></code> that formats the application.properties
	 * file represented by <code>document</code>
	 *
	 * @param document           the properties model
	 * @param formattingSettings the client's formatting settings
	 * @return a <code>List<TextEdit></code> that formats the application.properties
	 *         file represented by <code>document</code>
	 */
	public List<? extends TextEdit> doFormat(PropertiesModel document,
			MicroProfileFormattingSettings formattingSettings) {
		return formatter.format(document, formattingSettings);
	}

	/**
	 * Returns a <code>List<TextEdit></code> that formats the application.properties
	 * file represented by <code>document</code>, for the given <code>range</code>
	 *
	 * @param document           the properties model
	 * @param range              the range specifying the lines to format
	 * @param formattingSettings the client's formatting settings
	 * @return
	 */
	public List<? extends TextEdit> doRangeFormat(PropertiesModel document, Range range,
			MicroProfileFormattingSettings formattingSettings) {
		return formatter.format(document, range, formattingSettings);
	}

	/**
	 * Validate the given application.properties <code>document</code> by using the
	 * given MicroProfile properties metadata <code>projectInfo</code>.
	 *
	 * @param document           the properties model.
	 * @param projectInfo        the MicroProfile project info.
	 * @param validationSettings the validation settings.
	 * @param cancelChecker      the cancel checker.
	 * @return the result of the validation.
	 */
	public List<Diagnostic> doDiagnostics(PropertiesModel document, MicroProfileProjectInfo projectInfo,
			MicroProfileExecutionSettings executionSettings, MicroProfileValidationSettings validationSettings,
			CancelChecker cancelChecker) {
		updateProperties(projectInfo, document);
		return diagnostics.doDiagnostics(document, projectInfo, executionSettings, validationSettings, cancelChecker);
	}

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
		updateProperties(projectInfo, document);
		return codeActions.doCodeActions(context, range, document, projectInfo, formattingSettings, commandCapabilities,
				cancelChecker);
	}

	/**
	 * Returns highlights to apply to the document based off the current cursor
	 * position in the document.
	 *
	 * @param document      the parsed model of the document where highlights were
	 *                      requested
	 * @param position      the position of the cursor in the document
	 * @param cancelChecker the cancel checker
	 * @return highlights to apply to the document based off the current cursor
	 *         position in the document
	 */
	public List<? extends DocumentHighlight> findDocumentHighlight(PropertiesModel document, Position position,
			CancelChecker cancelChecker) {
		return documentHighlight.findDocumentHighlight(document, position, cancelChecker);
	}

	private void updateProperties(MicroProfileProjectInfo projectInfo, PropertiesModel document) {
		if (projectInfo instanceof ExtendedMicroProfileProjectInfo) {
			((ExtendedMicroProfileProjectInfo) projectInfo).updateCustomProperties(document);
		}
	}

	public List<InlayHint> getInlayHint(PropertiesModel document, MicroProfileProjectInfo projectInfo, Range range,
			MicroProfileInlayHintSettings inlayHintSettings, MicroProfileExecutionSettings executionSettings,
			CancelChecker cancelChecker) {
		return inlayHint.getInlayHint(document, projectInfo, range, inlayHintSettings, executionSettings,
				cancelChecker);
	}

}
