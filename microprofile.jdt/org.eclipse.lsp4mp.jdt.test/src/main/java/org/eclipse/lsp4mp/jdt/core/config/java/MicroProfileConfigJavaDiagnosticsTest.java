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
package org.eclipse.lsp4mp.jdt.core.config.java;

import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.assertJavaCodeAction;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.ca;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.createCodeActionParams;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.d;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.te;
import static org.eclipse.lsp4mp.jdt.internal.config.java.MicroProfileConfigASTValidator.setDataForUnassigned;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ls.core.internal.ProjectUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionFactory;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest;
import org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants;
import org.eclipse.lsp4mp.jdt.core.utils.IJDTUtils;
import org.eclipse.lsp4mp.jdt.internal.config.java.MicroProfileConfigErrorCode;
import org.eclipse.lsp4mp.jdt.internal.core.providers.MicroProfileConfigSourceProvider;
import org.junit.BeforeClass;
import org.junit.Test;

public class MicroProfileConfigJavaDiagnosticsTest extends BasePropertiesManagerTest {

	@BeforeClass
	public static void setupTests() throws Exception {
		BasePropertiesManagerTest.loadJavaProjects(new String[] { //
				"maven/" + MicroProfileMavenProjectName.config_quickstart,
				"maven/" + MicroProfileMavenProjectName.microprofile_configproperties });
	}

	@Test
	public void improperDefaultValues() throws Exception {
		IJavaProject javaProject = ProjectUtils.getJavaProject(MicroProfileMavenProjectName.config_quickstart);
		IJDTUtils utils = JDT_UTILS;

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		IFile javaFile = javaProject.getProject()
				.getFile(new Path("src/main/java/org/acme/config/DefaultValueResource.java"));
		diagnosticsParams.setUris(Arrays.asList(javaFile.getLocation().toFile().toURI().toString()));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		// Error message like "For input string: \"foo\"" commes from SmallRyeConfig
		Diagnostic d1 = d(10, 56, 59, "For input string: \"foo\"", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d2 = d(13, 56, 59, "For input string: \"bar\"", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d3 = d(19, 56, 59, "Value out of range. Value:\"128\" Radix:10", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);
		Diagnostic d4 = d(34, 27, 38,
				"The property 'greeting9' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("greeting9", d4);

		Diagnostic d5 = d(37, 57, 59, "AB can not be converted to a Character", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		/*
		 * Diagnostic d6 = d(52, 83, 86,
		 * "No enum constant org.acme.config.DefaultValueResource.ProcessingLevel.FOO",
		 * DiagnosticSeverity.Error,
		 * MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
		 * MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);
		 */
		Diagnostic d7 = d(58, 62, 67, "Text '00-00' could not be parsed at index 2", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d8 = d(64, 65, 70, "java.time.format.DateTimeParseException: Text cannot be parsed to a Duration",
				DiagnosticSeverity.Error, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		assertJavaDiagnostics(diagnosticsParams, utils, //
				d1, d2, d3, d4, d5, /* d6, */ d7, d8);
	}

	@Test
	public void improperDefaultValuesList() throws Exception {
		IJavaProject javaProject = ProjectUtils.getJavaProject(MicroProfileMavenProjectName.config_quickstart);
		IJDTUtils utils = JDT_UTILS;

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		IFile javaFile = javaProject.getProject()
				.getFile(new Path("src/main/java/org/acme/config/DefaultValueListResource.java"));
		diagnosticsParams.setUris(Arrays.asList(javaFile.getLocation().toFile().toURI().toString()));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(13, 57, 60, "For input string: \"13X\"", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d2 = d(19, 57, 60, "For input string: \"13X\"", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d3 = d(22, 57, 60, "For input string: \"13\\\"", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d4 = d(34, 54, 56, "AB can not be converted to a Character", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d5 = d(34, 57, 59, "CD can not be converted to a Character", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d6 = d(40, 54, 56,
				"'defaultValue=\"\"' will behave as if no default value is set, and will not be treated as an empty 'List<String>'.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.EMPTY_LIST_NOT_SUPPORTED);

		assertJavaDiagnostics(diagnosticsParams, utils, //
				d1, d2, d3, d4, d5, d6);
	}

	@Test
	public void noValueAssignedWithIgnore() throws Exception {
		IJavaProject javaProject = ProjectUtils.getJavaProject(MicroProfileMavenProjectName.config_quickstart);
		IJDTUtils utils = JDT_UTILS;

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		IFile javaFile = javaProject.getProject()
				.getFile(new Path("src/main/java/org/acme/config/DefaultValueResource.java"));
		diagnosticsParams.setSettings(new MicroProfileJavaDiagnosticsSettings(Arrays.asList("greeting?"),
				DiagnosticSeverity.Error, ExecutionMode.SAFE));
		diagnosticsParams.setUris(Arrays.asList(javaFile.getLocation().toFile().toURI().toString()));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		// Error message like "For input string: \"foo\"" commes from SmallRyeConfig
		Diagnostic d1 = d(10, 56, 59, "For input string: \"foo\"", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d2 = d(13, 56, 59, "For input string: \"bar\"", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d3 = d(19, 56, 59, "Value out of range. Value:\"128\" Radix:10", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		/*
		 * Diagnostic d4 = d(34, 27, 38,
		 * "The property 'greeting9' is not assigned a value in any config file, and must be assigned at runtime."
		 * , DiagnosticSeverity.Warning,
		 * MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
		 * MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		 * setDataForUnassigned("greeting9", d4);
		 */

		Diagnostic d5 = d(37, 57, 59, "AB can not be converted to a Character", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		/*
		 * Diagnostic d6 = d(52, 83, 86,
		 * "No enum constant org.acme.config.DefaultValueResource.ProcessingLevel.FOO",
		 * DiagnosticSeverity.Error,
		 * MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
		 * MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);
		 */
		Diagnostic d7 = d(58, 62, 67, "Text '00-00' could not be parsed at index 2", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		Diagnostic d8 = d(64, 65, 70, "java.time.format.DateTimeParseException: Text cannot be parsed to a Duration",
				DiagnosticSeverity.Error, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE);

		assertJavaDiagnostics(diagnosticsParams, utils, //
				d1, d2, d3, d5, /* d6, */ d7, d8);

	}

	@Test
	public void unassignedWithConfigProperties() throws Exception {
		IJavaProject javaProject = ProjectUtils
				.getJavaProject(MicroProfileMavenProjectName.microprofile_configproperties);
		IJDTUtils utils = JDT_UTILS;

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/org/acme/Details.java"));
		diagnosticsParams.setUris(Arrays.asList(javaFile.getLocation().toFile().toURI().toString()));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d = d(13, 32, 46,
				"The property 'server.old.location' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("server.old.location", d);

		assertJavaDiagnostics(diagnosticsParams, utils, d);
	}

	@Test
	public void codeActionForUnassigned() throws Exception {
		IJavaProject javaProject = ProjectUtils.getJavaProject(MicroProfileMavenProjectName.config_quickstart);
		IJDTUtils utils = JDT_UTILS;

		saveFile(MicroProfileConfigSourceProvider.MICROPROFILE_CONFIG_PROPERTIES_FILE, "", javaProject);

		IFile propertiesFile = javaProject.getProject()
				.getFile(new Path("src/main/resources/META-INF/microprofile-config.properties"));

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		IFile javaFile = javaProject.getProject()
				.getFile(new Path("src/main/java/org/acme/config/UnassignedValue.java"));
		diagnosticsParams.setUris(Arrays.asList(javaFile.getLocation().toFile().toURI().toString()));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(8, 24, 29,
				"The property 'foo' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("foo", d1);
		Diagnostic d2 = d(14, 25, 30,
				"The property 'server.url' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("server.url", d2);

		assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);

		String javaUri = fixURI(javaFile.getLocation().toFile().toURI().toString());
		String propertiesUri = fixURI(propertiesFile.getLocation().toFile().toURI().toString());

		MicroProfileJavaCodeActionParams codeActionParams1 = createCodeActionParams(javaUri, d1, false);
		assertJavaCodeAction(codeActionParams1, utils, //
				ca(javaUri, "Insert 'defaultValue' attribute",
						MicroProfileCodeActionId.ConfigPropertyInsertDefaultValue, d1, //
						te(8, 29, 8, 29, ", defaultValue = \"\"")),
				ca(propertiesUri, "Insert 'foo' property in 'META-INF/microprofile-config.properties'",
						MicroProfileCodeActionId.AssignValueToProperty, d1, //
						te(0, 0, 0, 0, "foo=\r\n")));

		MicroProfileJavaCodeActionParams codeActionParams2 = createCodeActionParams(javaUri, d2, false);
		assertJavaCodeAction(codeActionParams2, utils, //
				ca(javaUri, "Insert 'defaultValue' attribute",
						MicroProfileCodeActionId.ConfigPropertyInsertDefaultValue, d2, //
						te(14, 30, 14, 30, ", defaultValue = \"\"")),
				ca(propertiesUri, "Insert 'server.url' property in 'META-INF/microprofile-config.properties'",
						MicroProfileCodeActionId.AssignValueToProperty, d2, //
						te(0, 0, 0, 0, "server.url=\r\n")));

		// Same code actions but with exclude
		Diagnostic d1_1 = d(8, 24, 29,
				"The property 'foo' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("foo", d1_1);
		Diagnostic d2_1 = d(14, 25, 30,
				"The property 'server.url' is not assigned a value in any config file, and must be assigned at runtime.",
				DiagnosticSeverity.Warning, MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY);
		setDataForUnassigned("server.url", d2_1);

		MicroProfileJavaCodeActionParams codeActionParams1_1 = createCodeActionParams(javaUri, d1_1);
		codeActionParams1_1.setCommandConfigurationUpdateSupported(true);
		assertJavaCodeAction(codeActionParams1_1, utils, //
				MicroProfileCodeActionFactory.createAddToUnassignedExcludedCodeAction("foo", d1_1),
				ca(javaUri, "Insert 'defaultValue' attribute",
						MicroProfileCodeActionId.ConfigPropertyInsertDefaultValue, d1_1, //
						te(8, 29, 8, 29, ", defaultValue = \"\"")),
				ca(propertiesUri, "Insert 'foo' property in 'META-INF/microprofile-config.properties'",
						MicroProfileCodeActionId.AssignValueToProperty, d1_1, //
						te(0, 0, 0, 0, "foo=\r\n")));

		MicroProfileJavaCodeActionParams codeActionParams2_1 = createCodeActionParams(javaUri, d2_1);
		codeActionParams2_1.setCommandConfigurationUpdateSupported(true);
		assertJavaCodeAction(codeActionParams2_1, utils, //
				MicroProfileCodeActionFactory.createAddToUnassignedExcludedCodeAction("server.url", d2_1),
				ca(javaUri, "Insert 'defaultValue' attribute",
						MicroProfileCodeActionId.ConfigPropertyInsertDefaultValue, d2_1, //
						te(14, 30, 14, 30, ", defaultValue = \"\"")),
				ca(propertiesUri, "Insert 'server.url' property in 'META-INF/microprofile-config.properties'",
						MicroProfileCodeActionId.AssignValueToProperty, d2_1, //
						te(0, 0, 0, 0, "server.url=\r\n")));

	}

	@Test
	public void emptyNameKeyValue() throws Exception {
		IJavaProject javaProject = ProjectUtils
				.getJavaProject(MicroProfileMavenProjectName.microprofile_configproperties);
		IJDTUtils utils = JDT_UTILS;

		MicroProfileJavaDiagnosticsParams diagnosticsParams = new MicroProfileJavaDiagnosticsParams();
		IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/org/acme/EmptyKey.java"));
		diagnosticsParams.setUris(Arrays.asList(javaFile.getLocation().toFile().toURI().toString()));
		diagnosticsParams.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(5, 25, 27, "The member 'name' can't be empty.", DiagnosticSeverity.Error,
				MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE,
				MicroProfileConfigErrorCode.EMPTY_KEY);

		assertJavaDiagnostics(diagnosticsParams, utils, d1);
	}

	private static String fixURI(String uriString) {
		return uriString.replaceFirst("file:/([^/])", "file:///$1");
	}
}
