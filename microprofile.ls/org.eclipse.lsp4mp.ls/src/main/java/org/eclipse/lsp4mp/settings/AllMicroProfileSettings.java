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
package org.eclipse.lsp4mp.settings;

import com.google.gson.annotations.JsonAdapter;

import org.eclipse.lsp4j.jsonrpc.json.adapters.JsonElementTypeAdapter;
import org.eclipse.lsp4mp.utils.JSONUtility;

/**
 * Represents all settings under the 'quarkus' key
 *
 * { 'quarkus': {...} }
 */
public class AllMicroProfileSettings {

	private static class ToolsSettings {

		@JsonAdapter(JsonElementTypeAdapter.Factory.class)
		private Object tools;

		public Object getTools() {
			return tools;
		}

	}

	@JsonAdapter(JsonElementTypeAdapter.Factory.class)
	private Object quarkus;

	/**
	 * @return the quarkus
	 */
	public Object getQuarkus() {
		return quarkus;
	}

	/**
	 * @param quarkus the quarkus to set
	 */
	public void setQuarkus(Object quarkus) {
		this.quarkus = quarkus;
	}

	public static Object getMicroProfileToolsSettings(Object initializationOptionsSettings) {
		AllMicroProfileSettings rootSettings = JSONUtility.toModel(initializationOptionsSettings,
				AllMicroProfileSettings.class);
		if (rootSettings == null) {
			return null;
		}
		ToolsSettings quarkusSettings = JSONUtility.toModel(rootSettings.getQuarkus(), ToolsSettings.class);
		return quarkusSettings != null ? quarkusSettings.getTools() : null;
	}
}