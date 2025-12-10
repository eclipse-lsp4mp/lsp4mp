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
package org.eclipse.lsp4mp.ls;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
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
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4mp.commons.MicroProfileInlayHintSettings;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesChangeEvent;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.commons.utils.JSONUtility;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI.JsonSchemaForProjectInfo;
import org.eclipse.lsp4mp.ls.commons.client.ExtendedClientCapabilities;
import org.eclipse.lsp4mp.ls.java.JavaFileTextDocumentService;
import org.eclipse.lsp4mp.ls.java.JavaTextDocuments;
import org.eclipse.lsp4mp.ls.properties.PropertiesFileTextDocumentService;
import org.eclipse.lsp4mp.services.properties.CompletionData;
import org.eclipse.lsp4mp.settings.MicroProfileCodeLensSettings;
import org.eclipse.lsp4mp.settings.MicroProfileExecutionSettings;
import org.eclipse.lsp4mp.settings.MicroProfileFormattingSettings;
import org.eclipse.lsp4mp.settings.MicroProfileGeneralClientSettings;
import org.eclipse.lsp4mp.settings.MicroProfileSymbolSettings;
import org.eclipse.lsp4mp.settings.MicroProfileValidationSettings;
import org.eclipse.lsp4mp.settings.SharedSettings;

/**
 * MicroProfile text document service.
 *
 */
public class MicroProfileTextDocumentService implements TextDocumentService {

	private final MicroProfileLanguageServer microprofileLanguageServer;
	private final Map<String, TextDocumentService> textDocumentServicesMap;
	private final PropertiesFileTextDocumentService propertiesTextDocumentService;
	private final JavaFileTextDocumentService javaTextDocumentService;
	private final SharedSettings sharedSettings;

	public MicroProfileTextDocumentService(MicroProfileLanguageServer microprofileLanguageServer,
			SharedSettings sharedSettings, JavaTextDocuments javaTextDocuments) {
		this.microprofileLanguageServer = microprofileLanguageServer;
		textDocumentServicesMap = new HashMap<>();
		this.sharedSettings = sharedSettings;
		propertiesTextDocumentService = new PropertiesFileTextDocumentService(microprofileLanguageServer,
				sharedSettings);
		javaTextDocumentService = new JavaFileTextDocumentService(microprofileLanguageServer,
				propertiesTextDocumentService, sharedSettings, javaTextDocuments);
		textDocumentServicesMap.put("properties", propertiesTextDocumentService);
		textDocumentServicesMap.put("java", javaTextDocumentService);
		textDocumentServicesMap.put("class", javaTextDocumentService);
		textDocumentServicesMap.put("kt", javaTextDocumentService);
	}

	/**
	 * Update shared settings from the client capabilities.
	 *
	 * @param capabilities               the client capabilities
	 * @param extendedClientCapabilities the extended client capabilities
	 */
	public void updateClientCapabilities(ClientCapabilities capabilities,
			ExtendedClientCapabilities extendedClientCapabilities) {
		TextDocumentClientCapabilities textDocumentClientCapabilities = capabilities.getTextDocument();
		if (textDocumentClientCapabilities != null) {
			sharedSettings.getCompletionCapabilities().setCapabilities(textDocumentClientCapabilities.getCompletion());
			sharedSettings.getHoverSettings().setCapabilities(textDocumentClientCapabilities.getHover());
		}
		if (extendedClientCapabilities != null) {
			sharedSettings.getCommandCapabilities().setCapabilities(extendedClientCapabilities.getCommands());
		}
		propertiesTextDocumentService.updateClientCapabilities(capabilities);
		javaTextDocumentService.updateClientCapabilities(capabilities);
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			service.didOpen(params);
		}
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			service.didChange(params);
		}
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			service.didClose(params);
		}
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			service.didSave(params);
		}
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		TextDocumentService service = getTextDocumentService(position.getTextDocument());
		if (service != null) {
			return service.completion(position);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		CompletionData data = CompletionData.getCompletionData(unresolved);
		if (data != null) {
			String uri = data.getUri();
			TextDocumentService service = getTextDocumentService(new TextDocumentIdentifier(uri));
			if (service != null) {
				return service.resolveCompletionItem(unresolved);
			}
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			return service.hover(params);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			DocumentSymbolParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			return service.documentSymbol(params);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			return service.definition(params);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			return service.formatting(params);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			return service.rangeFormatting(params);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			return service.codeAction(params);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
		CodeActionResolveData data = JSONUtility.toModel(unresolved.getData(), CodeActionResolveData.class);
		if (data == null) {
			return CompletableFuture.completedFuture(unresolved);
		}
		TextDocumentService service = getTextDocumentService(new TextDocumentIdentifier(data.getDocumentUri()));
		if (service != null) {
			return service.resolveCodeAction(unresolved);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			return service.codeLens(params);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			return service.inlayHint(params);
		}
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		TextDocumentService service = getTextDocumentService(params.getTextDocument());
		if (service != null) {
			return service.documentHighlight(params);
		}
		return CompletableFuture.completedFuture(null);
	}

	public void propertiesChanged(MicroProfilePropertiesChangeEvent event) {
		propertiesTextDocumentService.propertiesChanged(event);
		javaTextDocumentService.propertiesChanged(event);
	}

	public void updateClientSettings(MicroProfileGeneralClientSettings clientSettings) {
		MicroProfileSymbolSettings newSymbols = clientSettings.getSymbols();
		if (newSymbols != null) {
			updateSymbolSettings(newSymbols);
		}
		MicroProfileFormattingSettings newFormatting = clientSettings.getFormatting();
		if (newFormatting != null) {
			updateFormattingSettings(newFormatting);
		}

		boolean executionSettingsChanged = updateExecutionSettings(clientSettings.getExecution());
		boolean validationSettingsChanged = updateValidationSettings(clientSettings.getValidation());
		boolean codeLensSettingsChanged = updateCodeLensSettings(clientSettings.getCodeLens());
		boolean inlayHintSettingsChanged = updateInlayHintSettings(clientSettings.getInlayHint());

		if (validationSettingsChanged || executionSettingsChanged) {
			propertiesTextDocumentService.triggerValidationAll();
			javaTextDocumentService.triggerValidationAll();
		}

		if (codeLensSettingsChanged) {
			// Codelens settings has changed, refresh codelens of all opened editors
			if (microprofileLanguageServer.getCapabilityManager().getClientCapabilities()
					.isCodeLensesRefreshSupported()) {
				microprofileLanguageServer.getLanguageClient().refreshCodeLenses();
			}
		}

		if (inlayHintSettingsChanged || executionSettingsChanged) {
			// Inlay hint settings has changed, refresh inlay hints of all opened editors
			if (microprofileLanguageServer.getCapabilityManager().getClientCapabilities()
					.isInlayHintsRefreshSupported()) {
				microprofileLanguageServer.getLanguageClient().refreshInlayHints();
			}
		}

	}

	private void updateSymbolSettings(MicroProfileSymbolSettings newSettings) {
		propertiesTextDocumentService.updateSymbolSettings(newSettings);
	}

	private void updateFormattingSettings(MicroProfileFormattingSettings newFormatting) {
		propertiesTextDocumentService.updateFormattingSettings(newFormatting);
	}

	private boolean updateExecutionSettings(MicroProfileExecutionSettings newExecution) {
		return sharedSettings.getExecutionSettings().update(newExecution);
	}

	private boolean updateValidationSettings(MicroProfileValidationSettings newValidation) {
		return sharedSettings.getValidationSettings().update(newValidation);
	}

	private boolean updateCodeLensSettings(MicroProfileCodeLensSettings newCodeLens) {
		return sharedSettings.getCodeLensSettings().update(newCodeLens);
	}

	private boolean updateInlayHintSettings(MicroProfileInlayHintSettings newInlayHint) {
		// Update inlay hint settings
		return sharedSettings.getInlayHintSettings().update(newInlayHint);
	}

	private TextDocumentService getTextDocumentService(TextDocumentIdentifier document) {
		String fileExtension = getFileExtension(document);
		return fileExtension != null ? textDocumentServicesMap.get(fileExtension) : null;
	}

	private TextDocumentService getTextDocumentService(TextDocumentItem document) {
		String fileExtension = getFileExtension(document);
		return fileExtension != null ? textDocumentServicesMap.get(fileExtension) : null;
	}

	private static String getFileExtension(TextDocumentIdentifier document) {
		return getFileExtension(document.getUri());
	}

	private static String getFileExtension(TextDocumentItem document) {
		return getFileExtension(document.getUri());
	}

	private static String getFileExtension(String uri) {
		int index = uri != null ? uri.lastIndexOf('.') : -1;
		return index != -1 ? uri.substring(index + 1, uri.length()) : null;
	}

	public CompletableFuture<JsonSchemaForProjectInfo> getJsonSchemaForProjectInfo(
			MicroProfileProjectInfoParams params) {
		return propertiesTextDocumentService.getJsonSchemaForProjectInfo(params);
	}

}