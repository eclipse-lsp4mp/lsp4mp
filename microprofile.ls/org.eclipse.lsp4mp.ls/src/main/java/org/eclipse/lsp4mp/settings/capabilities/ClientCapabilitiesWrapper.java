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
package org.eclipse.lsp4mp.settings.capabilities;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DynamicRegistrationCapabilities;
import org.eclipse.lsp4j.ResourceOperationKind;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4mp.ls.commons.client.ExtendedClientCapabilities;

/**
 * Determines if a client supports a specific capability dynamically
 */
public class ClientCapabilitiesWrapper {

	private boolean v3Supported;

	private ClientCapabilities capabilities;

	private final ExtendedClientCapabilities extendedCapabilities;

	public ClientCapabilitiesWrapper() {
		this(new ClientCapabilities(), null);
	}

	public ClientCapabilitiesWrapper(ClientCapabilities capabilities, ExtendedClientCapabilities extendedCapabilities) {
		this.capabilities = capabilities;
		this.v3Supported = capabilities != null ? capabilities.getTextDocument() != null : false;
		this.extendedCapabilities = extendedCapabilities;
	}

	/**
	 * IMPORTANT
	 *
	 * This should be up to date with all Server supported capabilities
	 *
	 */

	public boolean isCodeActionDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getCodeAction());
	}

	public boolean isCodeLensDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getCodeLens());
	}

	public boolean isCompletionDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getCompletion());
	}

	public boolean isHoverDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getHover());
	}

	public boolean isDocumentSymbolDynamicRegistrationSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getDocumentSymbol());
	}

	public boolean isDefinitionDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getDefinition());
	}

	public boolean isFormattingDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getFormatting());
	}

	public boolean isRangeFormattingDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getRangeFormatting());
	}

	public boolean isWorkspaceSymbolDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getWorkspace().getSymbol());
	}

	public boolean isDocumentHighlightSupported() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getDocumentHighlight());
	}

	private boolean isDynamicRegistrationSupported(DynamicRegistrationCapabilities capability) {
		return capability != null && capability.getDynamicRegistration() != null
				&& capability.getDynamicRegistration().booleanValue();
	}

	public TextDocumentClientCapabilities getTextDocument() {
		return this.capabilities.getTextDocument();
	}

	public WorkspaceClientCapabilities getWorkspace() {
		return this.capabilities.getWorkspace();
	}

	/**
	 * Returns true if the client should exit on shutdown() request and avoid
	 * waiting for an exit() request
	 *
	 * @return true if the language server should exit on shutdown() request
	 */
	public boolean shouldLanguageServerExitOnShutdown() {
		if (extendedCapabilities == null) {
			return false;
		}
		return extendedCapabilities.shouldLanguageServerExitOnShutdown();
	}

	public boolean isResourceOperationSupported() {
		// @formatter:off
		return capabilities.getWorkspace() != null && capabilities.getWorkspace().getWorkspaceEdit() != null
				&& capabilities.getWorkspace().getWorkspaceEdit().getResourceOperations() != null
				&& capabilities.getWorkspace().getWorkspaceEdit().getResourceOperations()
						.contains(ResourceOperationKind.Create)
				&& capabilities.getWorkspace().getWorkspaceEdit().getResourceOperations()
						.contains(ResourceOperationKind.Rename)
				&& capabilities.getWorkspace().getWorkspaceEdit().getResourceOperations()
						.contains(ResourceOperationKind.Delete);
		// @formatter:on
	}

	public boolean isInlayHintDynamicRegistered() {
		return v3Supported && isDynamicRegistrationSupported(getTextDocument().getInlayHint());
	}

	/**
	 * Returns true if the client supports both code action data and resolving
	 * workspace edits for code actions, and false otherwise.
	 *
	 * Both of these feature must be present in order to implement code action
	 * resolve effectively.
	 *
	 * @return true if the client supports both code action data and resolving
	 *         workspace edits for code actions, and false otherwise
	 */
	public boolean isCodeActionResolveSupported() {
		return capabilities.getTextDocument() != null && capabilities.getTextDocument().getCodeAction() != null
				&& capabilities.getTextDocument().getCodeAction().getDataSupport() != null
				&& capabilities.getTextDocument().getCodeAction().getDataSupport().booleanValue()
				&& capabilities.getTextDocument().getCodeAction().getResolveSupport() != null
				&& capabilities.getTextDocument().getCodeAction().getResolveSupport().getProperties() != null
				&& capabilities.getTextDocument().getCodeAction().getResolveSupport().getProperties().contains("edit");
	}

	public boolean isCodeLensesRefreshSupported() {
		return capabilities.getWorkspace() != null && capabilities.getWorkspace().getInlayHint() != null
				&& capabilities.getWorkspace().getCodeLens().getRefreshSupport() != null
				&& capabilities.getWorkspace().getCodeLens().getRefreshSupport().booleanValue();
	}
	
	public boolean isInlayHintsRefreshSupported() {
		return capabilities.getWorkspace() != null && capabilities.getWorkspace().getInlayHint() != null
				&& capabilities.getWorkspace().getInlayHint().getRefreshSupport() != null
				&& capabilities.getWorkspace().getInlayHint().getRefreshSupport().booleanValue();
	}
}