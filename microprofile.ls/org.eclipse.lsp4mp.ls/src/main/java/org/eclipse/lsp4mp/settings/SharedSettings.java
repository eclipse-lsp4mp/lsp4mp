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

/**
 * Shared settings.
 *
 * @author Angelo ZERR
 *
 */
public class SharedSettings {

	private final MicroProfileCompletionCapabilities completionCapabilities;
	private final MicroProfileHoverSettings hoverSettings;
	private final MicroProfileSymbolSettings symbolSettings;
	private final MicroProfileValidationSettings validationSettings;
	private final MicroProfileFormattingSettings formattingSettings;
	private final MicroProfileCommandCapabilities commandCapabilities;
	private final MicroProfileCodeLensSettings codeLensSettings;

	public SharedSettings() {
		this.completionCapabilities = new MicroProfileCompletionCapabilities();
		this.hoverSettings = new MicroProfileHoverSettings();
		this.symbolSettings = new MicroProfileSymbolSettings();
		this.validationSettings = new MicroProfileValidationSettings();
		this.formattingSettings = new MicroProfileFormattingSettings();
		this.commandCapabilities = new MicroProfileCommandCapabilities();
		this.codeLensSettings = new MicroProfileCodeLensSettings();
	}

	/**
	 * Returns the completion capabilities.
	 *
	 * @return the completion capabilities.
	 */
	public MicroProfileCompletionCapabilities getCompletionCapabilities() {
		return completionCapabilities;
	}

	/**
	 * Returns the hover settings.
	 *
	 * @return the hover settings.
	 */
	public MicroProfileHoverSettings getHoverSettings() {
		return hoverSettings;
	}

	/**
	 * Returns the symbol settings.
	 *
	 * @return the symbol settings.
	 */
	public MicroProfileSymbolSettings getSymbolSettings() {
		return symbolSettings;
	}

	/**
	 * Returns the validation settings.
	 *
	 * @return the validation settings.
	 */
	public MicroProfileValidationSettings getValidationSettings() {
		return validationSettings;
	}

	/**
	 * Returns the formatting settings.
	 *
	 * @return the formatting settings.
	 */
	public MicroProfileFormattingSettings getFormattingSettings() {
		return formattingSettings;
	}

	/**
	 * Returns the command capabilities.
	 *
	 * @return the command capabilities.
	 */
	public MicroProfileCommandCapabilities getCommandCapabilities() {
		return commandCapabilities;
	}

	/**
	 * Returns the code lens settings.
	 *
	 * @return the code lens settings.
	 */
	public MicroProfileCodeLensSettings getCodeLensSettings() {
		return codeLensSettings;
	}
}
