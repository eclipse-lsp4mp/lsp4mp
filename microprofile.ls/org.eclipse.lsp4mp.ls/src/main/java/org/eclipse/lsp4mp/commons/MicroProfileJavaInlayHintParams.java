/*******************************************************************************
* Copyright (c) 2025 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.commons;

/**
 * MicroProfile Java inlay hint parameters.
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileJavaInlayHintParams {

	private String uri;

	private MicroProfileJavaInlayHintSettings settings;

	public MicroProfileJavaInlayHintParams() {

	}

	public MicroProfileJavaInlayHintParams(String uri, MicroProfileJavaInlayHintSettings settings) {
		this();
		setUri(uri);
		setSettings(settings);
	}

	/**
	 * Returns the java file uri.
	 *
	 * @return the java file uri.
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Set the java file uri.
	 *
	 * @param uri the java file uri.
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	public MicroProfileJavaInlayHintSettings getSettings() {
		return settings;
	}

	public void setSettings(MicroProfileJavaInlayHintSettings settings) {
		this.settings = settings;
	}

}
