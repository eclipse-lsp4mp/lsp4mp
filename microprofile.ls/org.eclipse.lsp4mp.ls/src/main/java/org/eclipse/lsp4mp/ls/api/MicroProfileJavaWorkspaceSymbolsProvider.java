/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.ls.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

/**
 * Provides the workspace symbols given the project uri.
 *
 * @author datho7561
 */
public interface MicroProfileJavaWorkspaceSymbolsProvider {

	@JsonRequest("microprofile/java/workspaceSymbols")
	CompletableFuture<List<WorkspaceSymbol>> getJavaWorkspaceSymbols(String projectUri);

}
