/*******************************************************************************
* Copyright (c) 2025 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.services.properties;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.getDefaultMicroProfileProjectInfo;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.ih;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.ihLabel;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.p;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.testInlayHintFor;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.wrapWithQuarkusProject;

import org.eclipse.lsp4mp.commons.MicroProfileInlayHintSettings;
import org.eclipse.lsp4mp.commons.MicroProfileInlayHintTypeSettings;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.junit.Test;

/**
 * Test Types+Converters inlay hint for the 'microprofile-config.properties'
 * file.
 *
 */
public class PropertiesFileInlayHintTest {

	@Test
	public void showTypesAndConvertersWithNullProjectRuntime() throws Exception {
		// In this test converters cannot be loaded because there is no project runtime
		MicroProfileInlayHintSettings inlayHintSettings = createInlayHintSettings(true, true);
		String value = "quarkus.log.file.async.overflow = BLOCK";
		testInlayHintFor(value, //
				inlayHintSettings, //
				ih(p(0, 31), //
						ihLabel(" :"), //
						ihLabel("AsyncHandler$OverflowAction")));
	}

	@Test
	public void showTypesAndConvertersWithProjectRuntime() throws Exception {
		// In this test converters can be loaded because it uses Quarkus project runtime
		MicroProfileProjectInfo projectInfo = wrapWithQuarkusProject(getDefaultMicroProfileProjectInfo());
		MicroProfileInlayHintSettings inlayHintSettings = createInlayHintSettings(true, true);

		// Show converters + types
		String value = "quarkus.log.file.async.overflow = BLOCK";
		testInlayHintFor(value, //
				inlayHintSettings, //
				projectInfo, //
				ih(p(0, 31), //
						ihLabel(" :"), //
						ihLabel("AsyncHandler$OverflowAction"), //
						ihLabel(" - "), //
						ihLabel("HyphenateEnumConverter")));

		// Show only converters
		inlayHintSettings = createInlayHintSettings(true, false);
		testInlayHintFor(value, //
				inlayHintSettings, //
				projectInfo, //
				ih(p(0, 31), //
						ihLabel(" :"), //
						ihLabel("HyphenateEnumConverter")));

		// Show only types
		inlayHintSettings = createInlayHintSettings(false, true);
		testInlayHintFor(value, //
				inlayHintSettings, //
				projectInfo, //
				ih(p(0, 31), //
						ihLabel(" :"), //
						ihLabel("AsyncHandler$OverflowAction")));
	}

	private static MicroProfileInlayHintSettings createInlayHintSettings(boolean showConverters, boolean showTypes) {
		MicroProfileInlayHintSettings inlayHintSettings = new MicroProfileInlayHintSettings();
		MicroProfileInlayHintTypeSettings converterSettings = new MicroProfileInlayHintTypeSettings();
		converterSettings.setEnabled(showConverters);
		inlayHintSettings.setConverters(converterSettings);
		MicroProfileInlayHintTypeSettings typesSettings = new MicroProfileInlayHintTypeSettings();
		typesSettings.setEnabled(showTypes);
		inlayHintSettings.setTypes(typesSettings);
		return inlayHintSettings;
	}

}