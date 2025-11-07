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

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.c;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.r;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.extensions.ExtendedMicroProfileProjectInfo;
import org.eclipse.lsp4mp.ls.commons.BadLocationException;
import org.eclipse.lsp4mp.services.properties.PropertiesFileAssert;
import org.junit.Test;

/**
 * Test System / Environment variables completion in
 * 'microprofile-config.properties' file.
 * 
 * @author Angelo ZERR
 *
 */
public class SysEnvCompletionTest {

	private static final String PATH_ENV = (System.getenv().keySet().stream().anyMatch(key -> key.equals("Path"))
			? "Path"
			: "PATH");

	@Test
	public void systemPropertiesInKey() throws BadLocationException {
		String value = "|";
		testCompletionFor(value, true,
				c("user.name", "user.name=${0:" + System.getProperty("user.name") + "}", r(0, 0, 0)));
	}

	@Test
	public void systemPropertiesInExpression() throws BadLocationException {
		String value = "foo=$|";
		testCompletionFor(value, false, c("${user.name}", "${user.name}", r(0, 4, 5)));
	}

	@Test
	public void environmentVariablesInKey() throws BadLocationException {
		String value = "|";
		testCompletionFor(value, true, c(PATH_ENV, PATH_ENV + "=${0:" + System.getenv("PATH") + "}", r(0, 0, 0)));
	}

	@Test
	public void environmentVariablesInExpression() throws BadLocationException {
		String value = "foo=$|";
		testCompletionFor(value, false, c("${" + PATH_ENV + "}", "${" + PATH_ENV + "}", r(0, 4, 5)));
	}

	private static void testCompletionFor(String value, boolean snippetSupport, CompletionItem... expectedItems)
			throws BadLocationException {
		PropertiesFileAssert.testCompletionFor(value, snippetSupport, null, null,
				new ExtendedMicroProfileProjectInfo(new MicroProfileProjectInfo()), expectedItems);
	}
}
