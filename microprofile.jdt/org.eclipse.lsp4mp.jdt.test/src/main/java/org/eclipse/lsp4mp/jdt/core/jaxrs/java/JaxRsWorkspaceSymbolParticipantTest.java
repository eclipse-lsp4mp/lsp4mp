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
package org.eclipse.lsp4mp.jdt.core.jaxrs.java;

import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.assertWorkspaceSymbols;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.r;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.si;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest;
import org.junit.Test;

/**
 * Tests for <code>JaxRsWorkspaceSymbolParticipantTest</code>.
 */
public class JaxRsWorkspaceSymbolParticipantTest extends BasePropertiesManagerTest {

	@Test
	public void testConfigQuickstart() throws Exception {
		IJavaProject javaProject = loadMavenProject(MicroProfileMavenProjectName.config_quickstart);

		assertWorkspaceSymbols(javaProject, JDT_UTILS, //
				si("@/greeting/hello4: GET", r(44, 18, 24)), //
				si("@/greeting/constructor: GET", r(34, 18, 23)), //
				si("@/greeting/hello: GET", r(37, 18, 24)), //
				si("@/greeting: GET", r(30, 18, 23)), //
				si("@/greeting/method: GET", r(38, 18, 23)), //
				si("@/greeting/hello5: PATCH", r(50, 18, 24)));
	}

	@Test
	public void testOpenLiberty() throws Exception {
		IJavaProject javaProject = loadMavenProject(MicroProfileMavenProjectName.open_liberty);

		assertWorkspaceSymbols(javaProject, JDT_UTILS, //
				si("@/api/api/resource: GET", r(13, 15, 20)));
	}

}
