/*******************************************************************************
* Copyright (c) 2019-2020 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.settings.capabilities;

import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.CODE_ACTION_ID;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.CODE_LENS_ID;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.COMPLETION_ID_FOR_JAVA;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.COMPLETION_ID_FOR_PROPERTIES;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.DEFAULT_CODEACTION_OPTIONS;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.DEFAULT_WORKSPACE_SYMBOL_OPTIONS;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.DEFINITION_ID;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.DOCUMENT_HIGHLIGHT_ID;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.DOCUMENT_SYMBOL_ID;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.FORMATTING_ID;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.HOVER_ID;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.INLAY_HINT_ID;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.RANGE_FORMATTING_ID;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_CODE_ACTION;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_CODE_LENS;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_COMPLETION;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_DEFINITION;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_DOCUMENT_SYMBOL;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_FORMATTING;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_HOVER;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_INLAY_HINT;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.TEXT_DOCUMENT_RANGE_FORMATTING;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.WORKSPACE_SYMBOLS;
import static org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesConstants.WORKSPACE_SYMBOL_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CompletionRegistrationOptions;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.DocumentFormattingRegistrationOptions;
import org.eclipse.lsp4j.DocumentHighlightRegistrationOptions;
import org.eclipse.lsp4j.DocumentRangeFormattingRegistrationOptions;
import org.eclipse.lsp4j.DocumentSymbolRegistrationOptions;
import org.eclipse.lsp4j.InlayHintRegistrationOptions;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.TextDocumentRegistrationOptions;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4mp.MicroProfileLanguageIds;
import org.eclipse.lsp4mp.ls.commons.client.ExtendedClientCapabilities;

/**
 * Manages dynamic capabilities
 */
public class MicroProfileCapabilityManager {

	private final Set<String> registeredCapabilities = new HashSet<>(3);
	private final LanguageClient languageClient;

	private ClientCapabilitiesWrapper clientWrapper;

	private final List<IMicroProfileRegistrationConfiguration> registrationConfigurations;
	private boolean registrationConfigurationsInitialized;

	public MicroProfileCapabilityManager(LanguageClient languageClient) {
		this.languageClient = languageClient;
		this.registrationConfigurations = new ArrayList<>();
	}

	/**
	 * Registers all dynamic capabilities that the server does not support client
	 * side preferences turning on/off
	 */
	public void initializeCapabilities() {
		if (this.getClientCapabilities().isCodeActionDynamicRegistered()) {
			registerCapability(CODE_ACTION_ID, TEXT_DOCUMENT_CODE_ACTION, DEFAULT_CODEACTION_OPTIONS);
		}
		if (this.getClientCapabilities().isCodeLensDynamicRegistered()) {
			registerCapability(CODE_LENS_ID, TEXT_DOCUMENT_CODE_LENS);
		}
		if (this.getClientCapabilities().isCompletionDynamicRegistrationSupported()) {

			registerCapability(COMPLETION_ID_FOR_PROPERTIES, TEXT_DOCUMENT_COMPLETION,
					new CompletionRegistrationOptions(
							Arrays.asList(".", "%", "=", "$", "{", ":" /* triggered characters for properties file */),
							true),
					MicroProfileLanguageIds.MICROPROFILE_PROPERTIES);

			registerCapability(COMPLETION_ID_FOR_JAVA, TEXT_DOCUMENT_COMPLETION,
					new CompletionRegistrationOptions(
							Arrays.asList("@" /* triggered characters for java snippets annotation */,
									"\"" /* trigger characters for annotation property value completion */),
							false),
					MicroProfileLanguageIds.JAVA);
		}
		if (this.getClientCapabilities().isHoverDynamicRegistered()) {
			registerCapability(HOVER_ID, TEXT_DOCUMENT_HOVER);
		}
		if (this.getClientCapabilities().isDocumentSymbolDynamicRegistrationSupported()) {
			registerCapability(DOCUMENT_SYMBOL_ID, TEXT_DOCUMENT_DOCUMENT_SYMBOL,
					new DocumentSymbolRegistrationOptions(), MicroProfileLanguageIds.MICROPROFILE_PROPERTIES);
		}
		if (this.getClientCapabilities().isDefinitionDynamicRegistered()) {
			registerCapability(DEFINITION_ID, TEXT_DOCUMENT_DEFINITION);
		}
		if (this.getClientCapabilities().isFormattingDynamicRegistered()) {
			// The MP language server manages properties and Java files, but for formatting
			// and range formatting
			// feature, only properties file are supported.
			// We need to inform to the client that only properties are supported for format
			// feature with register options:
			/**
			 * <pre>
			 * "registerOptions": {
			 *  "documentSelector": [
			 *      { "language": "microprofile-properties" },
			 *      { "language": "quarkus-properties" }
			 *  ]
			 * }
			 * </pre>
			 */
			registerCapability(FORMATTING_ID, TEXT_DOCUMENT_FORMATTING, new DocumentFormattingRegistrationOptions(),
					MicroProfileLanguageIds.MICROPROFILE_PROPERTIES);
		}
		if (this.getClientCapabilities().isRangeFormattingDynamicRegistered()) {
			registerCapability(RANGE_FORMATTING_ID, TEXT_DOCUMENT_RANGE_FORMATTING,
					new DocumentRangeFormattingRegistrationOptions(), MicroProfileLanguageIds.MICROPROFILE_PROPERTIES);
		}
		if (this.getClientCapabilities().isDocumentHighlightSupported()) {
			registerCapability(DOCUMENT_HIGHLIGHT_ID, TEXT_DOCUMENT_DOCUMENT_HIGHLIGHT,
					new DocumentHighlightRegistrationOptions(), MicroProfileLanguageIds.MICROPROFILE_PROPERTIES);
		}
		if (this.getClientCapabilities().isInlayHintDynamicRegistered()) {
			registerCapability(INLAY_HINT_ID, TEXT_DOCUMENT_INLAY_HINT, new InlayHintRegistrationOptions());
		}
		if (this.getClientCapabilities().isWorkspaceSymbolDynamicRegistered()) {
			registerCapability(WORKSPACE_SYMBOL_ID, WORKSPACE_SYMBOLS, DEFAULT_WORKSPACE_SYMBOL_OPTIONS);
		}
	}

	public void setClientCapabilities(ClientCapabilities clientCapabilities,
			ExtendedClientCapabilities extendedClientCapabilities) {
		this.clientWrapper = new ClientCapabilitiesWrapper(clientCapabilities, extendedClientCapabilities);
	}

	public ClientCapabilitiesWrapper getClientCapabilities() {
		if (this.clientWrapper == null) {
			this.clientWrapper = new ClientCapabilitiesWrapper();
		}
		return this.clientWrapper;
	}

	public Set<String> getRegisteredCapabilities() {
		return registeredCapabilities;
	}

	private void registerCapability(String id, String method) {
		registerCapability(id, method, null);
	}

	private void registerCapability(String id, String method, Object options) {
		registerCapability(id, method, options, null);
	}

	private void registerCapability(String id, String method, Object options, String languageId) {
		if (registeredCapabilities.add(id)) {
			if (languageId != null) {
				List<DocumentFilter> documentSelector = new ArrayList<>();
				documentSelector.add(new DocumentFilter(languageId, null, null));
				((TextDocumentRegistrationOptions) options).setDocumentSelector(documentSelector);
			}
			Registration registration = new Registration(id, method, options);
			RegistrationParams registrationParams = new RegistrationParams(Collections.singletonList(registration));
			if (MicroProfileLanguageIds.MICROPROFILE_PROPERTIES.equals(languageId)) {
				getRegistrationConfigurations().forEach(config -> {
					config.configure(registration);
				});
			}
			languageClient.registerCapability(registrationParams);
		}
	}

	/**
	 * Returns list of registration configuration contributed with Java SPI.
	 *
	 * @return list of registration configuration contributed with Java SPI.
	 */
	private List<IMicroProfileRegistrationConfiguration> getRegistrationConfigurations() {
		if (!registrationConfigurationsInitialized) {
			initializeRegistrationConfigurations();
		}
		return registrationConfigurations;
	}

	private synchronized void initializeRegistrationConfigurations() {
		if (registrationConfigurationsInitialized) {
			return;
		}
		ServiceLoader<IMicroProfileRegistrationConfiguration> extensions = ServiceLoader
				.load(IMicroProfileRegistrationConfiguration.class);
		extensions.forEach(extension -> {
			this.registrationConfigurations.add(extension);
		});
		registrationConfigurationsInitialized = true;
	}

}