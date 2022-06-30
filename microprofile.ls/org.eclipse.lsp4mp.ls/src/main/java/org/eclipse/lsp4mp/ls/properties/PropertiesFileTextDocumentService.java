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
package org.eclipse.lsp4mp.ls.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Tuple;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesChangeEvent;
import org.eclipse.lsp4mp.ls.AbstractTextDocumentService;
import org.eclipse.lsp4mp.ls.MicroProfileLanguageServer;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI.JsonSchemaForProjectInfo;
import org.eclipse.lsp4mp.ls.commons.ModelTextDocument;
import org.eclipse.lsp4mp.ls.commons.ModelTextDocuments;
import org.eclipse.lsp4mp.ls.commons.ValidatorDelayer;
import org.eclipse.lsp4mp.model.PropertiesModel;
import org.eclipse.lsp4mp.services.properties.PropertiesFileLanguageService;
import org.eclipse.lsp4mp.settings.MicroProfileFormattingSettings;
import org.eclipse.lsp4mp.settings.MicroProfileSymbolSettings;
import org.eclipse.lsp4mp.settings.MicroProfileValidationSettings;
import org.eclipse.lsp4mp.settings.SharedSettings;
import org.eclipse.lsp4mp.utils.JSONSchemaUtils;
import org.eclipse.lsp4mp.utils.URIUtils;

/**
 * LSP text document service for 'microprofile-config.properties',
 * 'application.properties' file.
 *
 */
public class PropertiesFileTextDocumentService extends AbstractTextDocumentService implements IPropertiesModelProvider {

	private final ModelTextDocuments<PropertiesModel> documents;

	private MicroProfileProjectInfoCache projectInfoCache;

	private final ValidatorDelayer<ModelTextDocument<PropertiesModel>> validatorDelayer;

	public PropertiesFileTextDocumentService(MicroProfileLanguageServer microprofileLanguageServer,
			SharedSettings sharedSettings) {
		super(microprofileLanguageServer, sharedSettings);
		this.documents = new ModelTextDocuments<PropertiesModel>((document, cancelChecker) -> {
			return PropertiesModel.parse(document, cancelChecker);
		});
		this.validatorDelayer = new ValidatorDelayer<ModelTextDocument<PropertiesModel>>((document) -> {
			triggerValidationFor(document);
		});
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		ModelTextDocument<PropertiesModel> document = documents.onDidOpenTextDocument(params);
		validate(document, false);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		ModelTextDocument<PropertiesModel> document = documents.onDidChangeTextDocument(params);
		validate(document, true);
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		documents.onDidCloseTextDocument(params);
		TextDocumentIdentifier document = params.getTextDocument();
		String uri = document.getUri();
		validatorDelayer.cleanPendingValidation(uri);
		microprofileLanguageServer.getLanguageClient()
				.publishDiagnostics(new PublishDiagnosticsParams(uri, new ArrayList<Diagnostic>()));
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {

	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		return getPropertiesModel(params.getTextDocument(), (document, cancelChecker) -> {
			// Get MicroProfile project information which stores all available MicroProfile
			// properties
			MicroProfileProjectInfoParams projectInfoParams = createProjectInfoParams(params.getTextDocument());
			MicroProfileProjectInfo projectInfo = getProjectInfoCache().getProjectInfo(projectInfoParams).getNow(null);
			if (projectInfo == null || projectInfo.getProperties().isEmpty()) {
				return Either.forRight(new CompletionList());
			}
			cancelChecker.checkCanceled();

			// then return completion by using the MicroProfile project information and the
			// Properties model document
			CompletionList list = getPropertiesFileLanguageService().doComplete(document, params.getPosition(),
					projectInfo, sharedSettings.getCompletionCapabilities(), sharedSettings.getFormattingSettings(),
					cancelChecker);
			return Either.forRight(list);
		});
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return getPropertiesModel(params.getTextDocument(), (document, cancelChecker) -> {
			// Get MicroProfile project information which stores all available MicroProfile
			// properties
			// Don't block if it hasn't been computed yet
			MicroProfileProjectInfoParams projectInfoParams = createProjectInfoParams(params.getTextDocument());
			MicroProfileProjectInfo projectInfo = getProjectInfoCache().getProjectInfo(projectInfoParams).getNow(null);
			if (projectInfo == null || projectInfo.getProperties().isEmpty()) {
				return new Hover();
			}
			cancelChecker.checkCanceled();

			// then return hover by using the MicroProfile project information and the
			// Properties model document
			return getPropertiesFileLanguageService().doHover(document, params.getPosition(), projectInfo,
					sharedSettings.getHoverSettings(), cancelChecker);
		});
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			DocumentSymbolParams params) {
		return getPropertiesModel(params.getTextDocument(), (document, cancelChecker) -> {
			if (isHierarchicalDocumentSymbolSupport() && sharedSettings.getSymbolSettings().isShowAsTree()) {
				return getPropertiesFileLanguageService().findDocumentSymbols(document, cancelChecker) //
						.stream() //
						.map(s -> {
							Either<SymbolInformation, DocumentSymbol> e = Either.forRight(s);
							return e;
						}) //
						.collect(Collectors.toList());
			}
			return getPropertiesFileLanguageService().findSymbolInformations(document, cancelChecker) //
					.stream() //
					.map(s -> {
						Either<SymbolInformation, DocumentSymbol> e = Either.forLeft(s);
						return e;
					}) //
					.collect(Collectors.toList());
		});
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {
		return getPropertiesModel(params.getTextDocument(), (document, cancelChecker) -> {
			MicroProfileProjectInfoParams projectInfoParams = createProjectInfoParams(params.getTextDocument());
			MicroProfileProjectInfo projectInfo = getProjectInfoCache().getProjectInfo(projectInfoParams).getNow(null);
			if (projectInfo == null || projectInfo.getProperties().isEmpty()) {
				return null;
			}
			return Tuple.two(Tuple.two(document, projectInfo), cancelChecker);
		}).thenCompose((modelAndProjectInfoAndCancelChecker) -> {
			return getPropertiesFileLanguageService().findDefinition(
					modelAndProjectInfoAndCancelChecker.getFirst().getFirst(),
					params.getPosition(), modelAndProjectInfoAndCancelChecker.getFirst().getSecond(),
					microprofileLanguageServer.getLanguageClient(), isDefinitionLinkSupport(),
					modelAndProjectInfoAndCancelChecker.getSecond());
		});
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return getPropertiesModel(params.getTextDocument(), (document, cancelChecker) -> {
			return getPropertiesFileLanguageService().doFormat(document, sharedSettings.getFormattingSettings());
		});
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		return getPropertiesModel(params.getTextDocument(), (document, cancelChecker) -> {
			return getPropertiesFileLanguageService().doRangeFormat(document, params.getRange(),
					sharedSettings.getFormattingSettings());
		});
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		return getPropertiesModel(params.getTextDocument(), (document, cancelChecker) -> {
			MicroProfileProjectInfoParams projectInfoParams = createProjectInfoParams(params.getTextDocument());
			MicroProfileProjectInfo projectInfo = getProjectInfoCache().getProjectInfo(projectInfoParams).getNow(null);
			if (projectInfo == null || projectInfo.getProperties().isEmpty()) {
				return null;
			}
			return getPropertiesFileLanguageService()
					.doCodeActions(params.getContext(), params.getRange(), document, projectInfo,
							sharedSettings.getFormattingSettings(), sharedSettings.getCommandCapabilities(), cancelChecker) //
					.stream() //
					.map(ca -> {
						Either<Command, CodeAction> e = Either.forRight(ca);
						return e;
					}) //
					.collect(Collectors.toList());
		});
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		return getPropertiesModel(params.getTextDocument(), (document, cancelChecker) -> {
			return getPropertiesFileLanguageService().findDocumentHighlight(document, params.getPosition(), cancelChecker);
		});
	}

	private MicroProfileProjectInfoParams createProjectInfoParams(TextDocumentIdentifier id) {
		return createProjectInfoParams(id.getUri());
	}

	private MicroProfileProjectInfoParams createProjectInfoParams(String uri) {
		MicroProfileProjectInfoParams params = new MicroProfileProjectInfoParams(uri);
		params.setDocumentFormat(getDocumentFormat());
		return params;
	}

	private PropertiesFileLanguageService getPropertiesFileLanguageService() {
		return microprofileLanguageServer.getPropertiesFileLanguageService();
	}

	private void validate(ModelTextDocument<PropertiesModel> model, boolean delay) {
		if (delay) {
			this.validatorDelayer.validateWithDelay(model);
		} else {
			CompletableFuture.runAsync(() -> {
				triggerValidationFor(model);
			});
		}
	}

	private void triggerValidationFor(ModelTextDocument<PropertiesModel> model) {
		PropertiesModel propertiesModel = model.getModel();
		CancelChecker cancelChecker = propertiesModel.getCancelChecker();
		cancelChecker.checkCanceled();

		// Get MicroProfile project information which stores all available
		// MicroProfile properties
		MicroProfileProjectInfoParams projectInfoParams = createProjectInfoParams(propertiesModel.getDocumentURI());
		getProjectInfoCache().getProjectInfo(projectInfoParams).thenComposeAsync(projectInfo -> {
			cancelChecker.checkCanceled();
			if (projectInfo.getProperties().isEmpty()) {
				return CompletableFuture.completedFuture(null);
			}

			List<Diagnostic> diagnostics = getPropertiesFileLanguageService().doDiagnostics(propertiesModel,
					projectInfo, getSharedSettings().getValidationSettings(), cancelChecker);
			cancelChecker.checkCanceled();
			microprofileLanguageServer.getLanguageClient()
					.publishDiagnostics(new PublishDiagnosticsParams(propertiesModel.getDocumentURI(), diagnostics));
			return null;
		});
	}

	/**
	 * Returns the text document from the given uri.
	 *
	 * @param uri the uri
	 * @return the text document from the given uri.
	 */
	public ModelTextDocument<PropertiesModel> getDocument(String uri) {
		return documents.get(uri);
	}

	/**
	 * Returns the properties model for a given uri in a future and then apply the
	 * given function.
	 *
	 * @param <R>
	 * @param documentIdentifier the document identifier.
	 * @param code               a bi function that accepts a {@link CancelChecker}
	 *                           and parsed {@link PropertiesModel} and returns the
	 *                           to be computed value
	 * @return the properties model for a given uri in a future and then apply the
	 *         given function.
	 */
	public <R> CompletableFuture<R> getPropertiesModel(TextDocumentIdentifier documentIdentifier,
			BiFunction<PropertiesModel, CancelChecker, R> code) {
		return documents.computeModelAsync(documentIdentifier, code);
	}

	public void propertiesChanged(MicroProfilePropertiesChangeEvent event) {
		Collection<String> uris = getProjectInfoCache().propertiesChanged(event);
		for (String uri : uris) {
			ModelTextDocument<PropertiesModel> document = getDocument(uri);
			if (document != null) {
				triggerValidationFor(document);
			}
		}
	}

	public void updateSymbolSettings(MicroProfileSymbolSettings newSettings) {
		MicroProfileSymbolSettings symbolSettings = sharedSettings.getSymbolSettings();
		symbolSettings.setShowAsTree(newSettings.isShowAsTree());
	}

	public void updateValidationSettings(MicroProfileValidationSettings newValidation) {
		// Update validation settings
		MicroProfileValidationSettings validation = sharedSettings.getValidationSettings();
		validation.update(newValidation);
		// trigger validation for all opened application.properties
		documents.all().stream().forEach(document -> {
			triggerValidationFor(document);
		});
	}

	/**
	 * Updates MicroProfile formatting settings configured from the client.
	 *
	 * @param newFormatting the new MicroProfile formatting settings
	 */
	public void updateFormattingSettings(MicroProfileFormattingSettings newFormatting) {
		MicroProfileFormattingSettings formatting = sharedSettings.getFormattingSettings();
		formatting.setSurroundEqualsWithSpaces(newFormatting.isSurroundEqualsWithSpaces());
	}

	public SharedSettings getSharedSettings() {
		return sharedSettings;
	}

	private MicroProfileProjectInfoCache getProjectInfoCache() {
		if (projectInfoCache == null) {
			createProjectInfoCache();
		}
		return projectInfoCache;
	}

	private synchronized void createProjectInfoCache() {
		if (projectInfoCache != null) {
			return;
		}
		projectInfoCache = new MicroProfileProjectInfoCache(microprofileLanguageServer.getLanguageClient());
	}

	public CompletableFuture<JsonSchemaForProjectInfo> getJsonSchemaForProjectInfo(
			MicroProfileProjectInfoParams params) {
		return getProjectInfoCache().getProjectInfo(params).thenApply(info -> {
			String jsonSchema = JSONSchemaUtils.toJSONSchema(info, true);
			return new JsonSchemaForProjectInfo(info.getProjectURI(), jsonSchema);
		});
	}

	@Override
	public PropertiesModel getPropertiesModel(String documentURI) {
		// Try to get the document from the Map.
		ModelTextDocument<PropertiesModel> document = documents.get(documentURI);
		if (document != null) {
			return document.getModel();
		}
		// vscode opens the file by encoding the file URI and the 'C' of hard drive
		// lower case
		// 'c'.
		// for --> file:///C:/Users/a folder/application.properties
		// vscode didOpen --> file:///c%3A/Users/a%20folder/application.properties
		String encodedFileURI = URIUtils.encodeFileURI(documentURI).toUpperCase();
		// We loop for all properties files which are opened and we compare the encoded
		// file URI with upper case
		for (ModelTextDocument<PropertiesModel> textDocument : documents.all()) {
			if (textDocument.getUri().toUpperCase().equals(encodedFileURI)) {
				return textDocument.getModel();
			}
		}
		return null;
	}

}