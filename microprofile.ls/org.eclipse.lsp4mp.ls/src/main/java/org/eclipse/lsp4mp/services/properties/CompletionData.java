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
package org.eclipse.lsp4mp.services.properties;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4mp.commons.utils.JSONUtility;

/**
 * Represents data sent to for completionItem/resolve
 *
 * { 'data': { 'propertyName': {...}, 'uri': {...} } }
 */
public class CompletionData {

	private String propertyName;

	private String uri;

	public CompletionData() {
		this.propertyName = null;
		this.uri = null;
	}

	public CompletionData(String uri) {
		this.uri = uri;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public static CompletionData getCompletionData(CompletionItem unresolved) {
		return JSONUtility.toModel(unresolved.getData(), CompletionData.class);
	}
}
