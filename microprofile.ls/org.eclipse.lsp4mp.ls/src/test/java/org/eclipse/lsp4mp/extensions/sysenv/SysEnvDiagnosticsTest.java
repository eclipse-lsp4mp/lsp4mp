/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.extensions.sysenv;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.d;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.testDiagnosticsFor;

import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.services.properties.ValidationType;
import org.eclipse.lsp4mp.utils.EnvUtils;
import org.junit.Test;

/**
 * Test with System/Environment variables diagnostics in
 * 'microprofile-config.properties' file.
 *
 * @author Angelo ZERR
 *
 */
public class SysEnvDiagnosticsTest {

	@Test
	public void sysemPropertyInKey() {
		// dbuser doesn't exists a System property
		String value = "dbuser = FOO";
		testDiagnosticsFor(value, //
				d(0, 0, 6, "Unrecognized property 'dbuser', it is not referenced in any Java, Properties files",
						DiagnosticSeverity.Warning, ValidationType.unknown));

		// user.name exists as System property
		value = "user.name = FOO";
		testDiagnosticsFor(value);
	}

	@Test
	public void sysemPropertyInPropertyExpression() {
		// dbuser doesn't exists a System property
		String value = "quarkus.datasource.username = ${dbuser}";
		testDiagnosticsFor(value, //
				d(0, 32, 38, "Unknown referenced property value expression 'dbuser'", DiagnosticSeverity.Error,
						ValidationType.expression));

		// user.name exists as System property
		value = "quarkus.datasource.username = ${user.name}";
		testDiagnosticsFor(value);
	}

	@Test
	public void environmentVariableInKey() {
		// DBUSER doesn't exists a Environment variable
		String value = "DBUSER = FOO";
		testDiagnosticsFor(value, //
				d(0, 0, 6, "Unrecognized property 'DBUSER', it is not referenced in any Java, Properties files",
						DiagnosticSeverity.Warning, ValidationType.unknown));

		// PATH exists as Environment variable
		value = "PATH = FOO";
		testDiagnosticsFor(value);
	}

	@Test
	public void environmentVariableInPropertyExpression() {
		// DBUSER doesn't exists a Environment variable
		String value = "quarkus.datasource.username = ${dbuser}";
		testDiagnosticsFor(value, //
				d(0, 32, 38, "Unknown referenced property value expression 'dbuser'", DiagnosticSeverity.Error,
						ValidationType.expression));

		// PATH exists as Environment variable
		value = "quarkus.datasource.username = ${PATH}";
		testDiagnosticsFor(value);

		// Path exists on Windows OS but not for other OS
		value = "quarkus.datasource.username = ${Path}";
		if (EnvUtils.isWindows) {
			testDiagnosticsFor(value);
		} else {
			testDiagnosticsFor(value, //
					d(0, 32, 36, "Unknown referenced property value expression 'Path'", DiagnosticSeverity.Error,
							ValidationType.expression));
		}
	}
}
