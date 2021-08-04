/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.jdt.core;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4mp.jdt.core.project.IConfigSource;
import org.eclipse.lsp4mp.jdt.core.project.IConfigSourceProvider;
import org.eclipse.lsp4mp.jdt.core.project.PropertiesConfigSource;

/**
 * Defines the config file <code>META-INF/microprofile-config-test.properties</code> for use in tests.
 *
 * The config file has a higher ordinal than <code>META-INF/microprofile-config.properties</code>.
 *
 */
public class TestConfigSourceProvider implements IConfigSourceProvider {

	public static final String MICROPROFILE_CONFIG_TEST = "META-INF/microprofile-config-test.properties";

	@Override
	public List<IConfigSource> getConfigSources(IJavaProject project) {
		return Collections.singletonList(new PropertiesConfigSource(MICROPROFILE_CONFIG_TEST, project, 101));
	}

}
