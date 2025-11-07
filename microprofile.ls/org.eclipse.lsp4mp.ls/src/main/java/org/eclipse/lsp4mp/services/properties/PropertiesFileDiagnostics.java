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
package org.eclipse.lsp4mp.services.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.model.PropertiesModel;
import org.eclipse.lsp4mp.services.properties.extensions.PropertiesFileExtensionRegistry;
import org.eclipse.lsp4mp.settings.MicroProfileExecutionSettings;
import org.eclipse.lsp4mp.settings.MicroProfileValidationSettings;

/**
 * The properties file diagnostics support.
 *
 */
class PropertiesFileDiagnostics {

	private final PropertiesFileExtensionRegistry extensionRegistry;

	public PropertiesFileDiagnostics(PropertiesFileExtensionRegistry extensionRegistry) {
		this.extensionRegistry = extensionRegistry;
	}

	/**
	 * Validate the given application.properties <code>document</code> by using the
	 * given MicroProfile properties metadata <code>projectInfo</code>.
	 *
	 * @param document           the properties model.
	 * @param projectInfo        the MicroProfile properties
	 * @param executionSettings  the execution settings.
	 * @param validationSettings the validation settings.
	 * @param cancelChecker      the cancel checker.
	 * @return the result of the validation.
	 */
	public List<Diagnostic> doDiagnostics(PropertiesModel document, MicroProfileProjectInfo projectInfo,
			MicroProfileExecutionSettings executionSettings, MicroProfileValidationSettings validationSettings,
			CancelChecker cancelChecker) {
		if (validationSettings == null) {
			validationSettings = MicroProfileValidationSettings.DEFAULT;
		}
		List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
		if (validationSettings.isEnabled()) {
			PropertiesFileValidator validator = new PropertiesFileValidator(projectInfo, diagnostics, executionSettings,
					validationSettings, extensionRegistry);
			validator.validate(document, cancelChecker);
		}
		return diagnostics;
	}

}
