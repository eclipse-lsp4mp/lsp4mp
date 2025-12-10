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

import org.eclipse.lsp4mp.commons.MicroProfileInlayHintSettings;
import org.eclipse.lsp4mp.commons.utils.JSONUtility;

/**
 * Class to hold all settings from the client side.
 *
 *
 * This class is created through the deserialization of a JSON object. Each
 * internal setting must be represented by a class and have:
 *
 * 1) A constructor with no parameters
 *
 * 2) The JSON key/parent for the settings must have the same name as a
 * variable.
 *
 * eg: {"symbols" : {...}, "validation" : {...}}
 *
 */
public class MicroProfileGeneralClientSettings {

	private MicroProfileSymbolSettings symbols;

	private MicroProfileExecutionSettings execution;
	private MicroProfileValidationSettings validation;

	private MicroProfileFormattingSettings formatting;

	private MicroProfileCodeLensSettings codeLens;

	private MicroProfileInlayHintSettings inlayHint;

	/**
	 * Returns the symbols settings.
	 *
	 * @return the symbols settings.
	 */
	public MicroProfileSymbolSettings getSymbols() {
		return symbols;
	}

	/**
	 * Set the symbols settings.
	 *
	 * @param symbols the symbols settings.
	 */
	public void setSymbols(MicroProfileSymbolSettings symbols) {
		this.symbols = symbols;
	}

	/**
	 * Returns the execution settings.
	 *
	 * @return the execution settings.
	 */
	public MicroProfileExecutionSettings getExecution() {
		return execution;
	}

	/**
	 * Set the execution settings.
	 *
	 * @param execution the execution settings.
	 */
	public void setExecution(MicroProfileExecutionSettings execution) {
		this.execution = execution;
	}

	/**
	 * Returns the validation settings.
	 *
	 * @return the validation settings.
	 */
	public MicroProfileValidationSettings getValidation() {
		return validation;
	}

	/**
	 * Set the validation settings.
	 *
	 * @param validation the validation settings.
	 */
	public void setValidation(MicroProfileValidationSettings validation) {
		this.validation = validation;
	}

	/**
	 * Returns the formatting settings
	 *
	 * @return the formatting settings
	 */
	public MicroProfileFormattingSettings getFormatting() {
		return formatting;
	}

	/**
	 * Sets the formatting settings
	 *
	 * @param formatting the formatting settings
	 */
	public void setFormatting(MicroProfileFormattingSettings formatting) {
		this.formatting = formatting;
	}

	/**
	 * Returns the code lens settings.
	 *
	 * @return the code lens settings.
	 */
	public MicroProfileCodeLensSettings getCodeLens() {
		return codeLens;
	}

	/**
	 * Sets the code lens settings.
	 *
	 * @param codeLens the code lens settings.
	 */
	public void setCodeLens(MicroProfileCodeLensSettings codeLens) {
		this.codeLens = codeLens;
	}

	public MicroProfileInlayHintSettings getInlayHint() {
		return inlayHint;
	}

	public void setInlayHint(MicroProfileInlayHintSettings inlayHint) {
		this.inlayHint = inlayHint;
	}

	/**
	 * Returns the general settings from the given initialization options
	 *
	 * @param initializationOptionsSettings the initialization options
	 * @return the general settings from the given initialization options
	 */
	public static MicroProfileGeneralClientSettings getGeneralMicroProfileSettings(
			Object initializationOptionsSettings) {
		return JSONUtility.toModel(initializationOptionsSettings, MicroProfileGeneralClientSettings.class);
	}
}