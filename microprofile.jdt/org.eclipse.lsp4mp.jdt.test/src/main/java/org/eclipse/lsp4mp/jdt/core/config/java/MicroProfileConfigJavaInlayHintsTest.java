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
package org.eclipse.lsp4mp.jdt.core.config.java;

import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.assertInlayHints;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.ih;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.p;

import java.io.InputStream;
import java.util.Scanner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
import org.eclipse.lsp4mp.commons.MicroProfileInlayHintTypeSettings;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintSettings;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest;
import org.eclipse.lsp4mp.jdt.core.utils.IJDTUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class MicroProfileConfigJavaInlayHintsTest extends BasePropertiesManagerTest {

	@BeforeClass
	public static void setupTests() throws Exception {
		BasePropertiesManagerTest.loadJavaProjects(new String[] { //
				"maven/" + MicroProfileMavenProjectName.config_quickstart });
	}

	@Test
	@Ignore("Fix flacky Java inlay hint test")
	public void defaultValuesAndConverters() throws Exception {
		IJavaProject javaProject = ProjectUtils.getJavaProject(MicroProfileMavenProjectName.config_quickstart);
		IJDTUtils utils = JDT_UTILS;

		// It seems build is not done when test is executed and
		// target/microprofile-config.properties doesn't exist
		// Before executing the test, we copy/paste the
		// "src/main/resources/microprofile-config.properties" in
		// "target/microprofile-config.properties"
		IFile propertiesSourceFile = javaProject.getProject()
				.getFile(new Path("src/main/resources/META-INF/microprofile-config.properties"));
		saveFile("META-INF/microprofile-config.properties", convertStreamToString(propertiesSourceFile.getContents()),
				javaProject);

		MicroProfileJavaInlayHintParams inlayHintsParams = new MicroProfileJavaInlayHintParams();
		IFile javaFile = javaProject.getProject()
				.getFile(new Path("src/main/java/org/acme/config/GreetingResource.java"));
		inlayHintsParams.setUri(javaFile.getLocation().toFile().toURI().toString());

		// Default values + converters
		inlayHintsParams.setSettings(createInlayHintSettings(true, true));

		assertInlayHints(inlayHintsParams, utils, //
				ih(p(17, 11), "BuiltInConverter "), //
				ih(p(20, 11), "BuiltInConverter "), //
				ih(p(23, 21), "BuiltInConverter "), //
				ih(p(25, 50), ", defaultValue=\"PT15M\""), //
				ih(p(26, 13), "StaticMethodConverter "));

		// Only converters
		inlayHintsParams.setSettings(createInlayHintSettings(true, false));

		assertInlayHints(inlayHintsParams, utils, //
				ih(p(17, 11), "BuiltInConverter "), //
				ih(p(20, 11), "BuiltInConverter "), //
				ih(p(23, 21), "BuiltInConverter "), //
				ih(p(26, 13), "StaticMethodConverter "));

		// Only default values
		inlayHintsParams.setSettings(createInlayHintSettings(false, true));

		assertInlayHints(inlayHintsParams, utils, //
				ih(p(25, 50), ", defaultValue=\"PT15M\""));

	}

	private static MicroProfileJavaInlayHintSettings createInlayHintSettings(boolean showConverters,
			boolean showDefaultValues) {
		MicroProfileJavaInlayHintSettings inlayHintSettings = new MicroProfileJavaInlayHintSettings(ExecutionMode.SAFE);
		MicroProfileInlayHintTypeSettings converterSettings = new MicroProfileInlayHintTypeSettings();
		converterSettings.setEnabled(showConverters);
		inlayHintSettings.setConverters(converterSettings);
		MicroProfileInlayHintTypeSettings defaultValuesSettings = new MicroProfileInlayHintTypeSettings();
		defaultValuesSettings.setEnabled(showDefaultValues);
		inlayHintSettings.setDefaultValues(defaultValuesSettings);
		return inlayHintSettings;
	}

	/**
	 * Convert the given {@link InputStream} into a String. The source InputStream
	 * will then be closed.
	 * 
	 * @param is the input stream
	 * @return the given input stream in a String.
	 */
	private static String convertStreamToString(InputStream is) {
		try (Scanner s = new java.util.Scanner(is)) {
			s.useDelimiter("\\A");
			return s.hasNext() ? s.next() : "";
		}
	}

}
