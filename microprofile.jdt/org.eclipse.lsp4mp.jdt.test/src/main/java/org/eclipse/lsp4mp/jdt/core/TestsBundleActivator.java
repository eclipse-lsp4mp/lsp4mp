/*******************************************************************************
 * Copyright (c) 2026 IBM Corp. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corp. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lsp4mp.jdt.core;

import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Configures jdtls' Maven project importer to use the artifact id as the project name.
 * This was the old behaviour before it was changed to avoid project name collisions.
 */
public class TestsBundleActivator implements BundleActivator {

	String old = null;

	@Override
	public void start(BundleContext context) throws Exception {
		var prefs = JavaLanguageServerPlugin.getPreferencesManager().getPreferences();
		old = prefs.getMavenProjectNameTemplate();
		prefs.setMavenProjectNameTemplate("[artifactId]");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		var prefs = JavaLanguageServerPlugin.getPreferencesManager().getPreferences();
		prefs.setMavenProjectNameTemplate(old);
	}

}
