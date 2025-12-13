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

import static org.eclipse.lsp4j.jsonrpc.CompletableFutures.computeAsync;
import static org.eclipse.lsp4mp.utils.VersionHelper.getVersion;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.SetTraceParams;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
import org.eclipse.lsp4mp.commons.MicroProfileJavaFileInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaProjectLabelsParams;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesChangeEvent;
import org.eclipse.lsp4mp.commons.ProjectLabelInfoEntry;
import org.eclipse.lsp4mp.ls.api.MicroProfileJavaFileInfoProvider;
import org.eclipse.lsp4mp.ls.api.MicroProfileJavaProjectLabelsProvider;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageClientAPI;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI;
import org.eclipse.lsp4mp.ls.commons.ParentProcessWatcher.ProcessLanguageServer;
import org.eclipse.lsp4mp.ls.commons.client.ExtendedClientCapabilities;
import org.eclipse.lsp4mp.ls.commons.client.InitializationOptionsExtendedClientCapabilities;
import org.eclipse.lsp4mp.ls.java.JavaTextDocuments;
import org.eclipse.lsp4mp.services.properties.PropertiesFileLanguageService;
import org.eclipse.lsp4mp.settings.AllMicroProfileSettings;
import org.eclipse.lsp4mp.settings.InitializationOptionsSettings;
import org.eclipse.lsp4mp.settings.MicroProfileExtensionSettings;
import org.eclipse.lsp4mp.settings.MicroProfileGeneralClientSettings;
import org.eclipse.lsp4mp.settings.SharedSettings;
import org.eclipse.lsp4mp.settings.capabilities.MicroProfileCapabilityManager;
import org.eclipse.lsp4mp.settings.capabilities.ServerCapabilitiesInitializer;

/**
 * MicroProfile language server.
 *
 */
public class MicroProfileLanguageServer implements LanguageServer, ProcessLanguageServer, MicroProfileLanguageServerAPI,
		MicroProfileJavaProjectLabelsProvider, MicroProfileJavaFileInfoProvider {

	private static final Logger LOGGER = Logger.getLogger(MicroProfileLanguageServer.class.getName());

	private final PropertiesFileLanguageService propertiesFileLanguageService;
	private final MicroProfileTextDocumentService textDocumentService;
	private final WorkspaceService workspaceService;
	private final SharedSettings sharedSettings;
	private final JavaTextDocuments javaDocuments;

	private final MicroProfileExtensionSettings extensionSettings;

	private Integer parentProcessId;
	private MicroProfileLanguageClientAPI languageClient;
	private MicroProfileCapabilityManager capabilityManager;

	public MicroProfileLanguageServer() {
		sharedSettings = new SharedSettings();
		javaDocuments = new JavaTextDocuments(this, this);
		propertiesFileLanguageService = new PropertiesFileLanguageService();
		textDocumentService = new MicroProfileTextDocumentService(this, sharedSettings, javaDocuments);
		workspaceService = new MicroProfileWorkspaceService(this, javaDocuments);
		this.extensionSettings = new MicroProfileExtensionSettings();
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		LOGGER.info("Initializing MicroProfile server " + getVersion() + " with " + System.getProperty("java.home"));

		propertiesFileLanguageService.initializeParams(params);

		this.parentProcessId = params.getProcessId();

		ExtendedClientCapabilities extendedClientCapabilities = InitializationOptionsExtendedClientCapabilities
				.getExtendedClientCapabilities(params);
		capabilityManager.setClientCapabilities(params.getCapabilities(), extendedClientCapabilities);
		updateSettings(InitializationOptionsSettings.getSettings(params));

		textDocumentService.updateClientCapabilities(params.getCapabilities(), extendedClientCapabilities);
		ServerCapabilities serverCapabilities = ServerCapabilitiesInitializer
				.getNonDynamicServerCapabilities(capabilityManager.getClientCapabilities());

		InitializeResult initializeResult = new InitializeResult(serverCapabilities);
		return CompletableFuture.completedFuture(initializeResult);
	}

	/*
	 * Registers all capabilities that do not support client side preferences to
	 * turn on/off
	 *
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.lsp4j.services.LanguageServer#initialized(org.eclipse.lsp4j.
	 * InitializedParams)
	 */
	@Override
	public void initialized(InitializedParams params) {
		capabilityManager.initializeCapabilities();
	}

	/**
	 * Update MicroProfile settings configured from the client.
	 *
	 * @param initializationOptionsSettings the MicroProfile settings
	 */
	public synchronized void updateSettings(Object initializationOptionsSettings) {
		if (initializationOptionsSettings == null) {
			return;
		}
		// Update client settings
		initializationOptionsSettings = AllMicroProfileSettings
				.getMicroProfileToolsSettings(initializationOptionsSettings);
		MicroProfileGeneralClientSettings clientSettings = MicroProfileGeneralClientSettings
				.getGeneralMicroProfileSettings(initializationOptionsSettings);
		if (clientSettings != null) {
			// Merge client settings with extension settings
			extensionSettings.merge(clientSettings);
			// Update client settings
			textDocumentService.updateClientSettings(clientSettings);
		}
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		propertiesFileLanguageService.dispose();
		if (capabilityManager.getClientCapabilities().shouldLanguageServerExitOnShutdown()) {
			ScheduledExecutorService delayer = Executors.newScheduledThreadPool(1);
			delayer.schedule(() -> exit(0), 1, TimeUnit.SECONDS);
		}
		return computeAsync(cc -> new Object());
	}

	@Override
	public void exit() {
		exit(0);
	}

	@Override
	public void exit(int exitCode) {
		System.exit(exitCode);
	}

	@Override
	public void setTrace(SetTraceParams params) {
		// to avoid having error in vscode, the method is implemented
		// FIXME: implement the behavior of this method.
	}

	public TextDocumentService getTextDocumentService() {
		return this.textDocumentService;
	}

	public WorkspaceService getWorkspaceService() {
		return this.workspaceService;
	}

	public MicroProfileLanguageClientAPI getLanguageClient() {
		return languageClient;
	}

	public MicroProfileCapabilityManager getCapabilityManager() {
		return capabilityManager;
	}

	public void setClient(LanguageClient languageClient) {
		this.languageClient = (MicroProfileLanguageClientAPI) languageClient;
		this.capabilityManager = new MicroProfileCapabilityManager(languageClient);
	}

	@Override
	public long getParentProcessId() {
		return parentProcessId != null ? parentProcessId : 0;
	}

	public PropertiesFileLanguageService getPropertiesFileLanguageService() {
		return propertiesFileLanguageService;
	}

	@Override
	public void propertiesChanged(MicroProfilePropertiesChangeEvent event) {
		textDocumentService.propertiesChanged(event);
	}

	@Override
	public CompletableFuture<JsonSchemaForProjectInfo> getJsonSchemaForProjectInfo(
			MicroProfileProjectInfoParams params) {
		return textDocumentService.getJsonSchemaForProjectInfo(params);
	}

	@Override
	public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectLabels(
			MicroProfileJavaProjectLabelsParams javaParams) {
		return getLanguageClient().getJavaProjectLabels(javaParams);
	}

	@Override
	public CompletableFuture<List<ProjectLabelInfoEntry>> getAllJavaProjectLabels() {
		return getLanguageClient().getAllJavaProjectLabels();
	}

	@Override
	public CompletableFuture<JavaFileInfo> getJavaFileInfo(MicroProfileJavaFileInfoParams javaParams) {
		return getLanguageClient().getJavaFileInfo(javaParams);
	}
}
