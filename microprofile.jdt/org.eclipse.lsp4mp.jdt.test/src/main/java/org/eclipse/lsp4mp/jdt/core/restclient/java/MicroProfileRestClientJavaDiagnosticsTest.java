/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.jdt.core.restclient.java;

import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.assertJavaCodeAction;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.assertJavaDiagnostics;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.ca;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.createCodeActionParams;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.d;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileForJavaAssert.te;

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
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest.MicroProfileMavenProjectName;
import org.eclipse.lsp4mp.jdt.core.utils.IJDTUtils;
import org.eclipse.lsp4mp.jdt.internal.restclient.MicroProfileRestClientConstants;
import org.eclipse.lsp4mp.jdt.internal.restclient.MicroProfileRestClientErrorCode;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Java diagnostics for MicroProfile RestClient.
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileRestClientJavaDiagnosticsTest extends BasePropertiesManagerTest {

	@BeforeClass
	public static void setupTests() throws Exception {
		BasePropertiesManagerTest.loadJavaProjects(new String [] {
				"maven/" + MicroProfileMavenProjectName.rest_client_quickstart,
				"maven/" + MicroProfileMavenProjectName.open_liberty
				});
	}

	@Test
	public void restClientAnnotationMissingForFields() throws Exception {
		IJavaProject javaProject = ProjectUtils.getJavaProject(MicroProfileMavenProjectName.rest_client_quickstart);
		IJDTUtils utils = JDT_UTILS;

		MicroProfileJavaDiagnosticsParams params = new MicroProfileJavaDiagnosticsParams();
		IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/org/acme/restclient/Fields.java"));
		params.setUris(Arrays.asList(javaFile.getLocation().toFile().toURI().toString()));
		params.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(12, 18, 26,
				"The corresponding `org.acme.restclient.MyService` interface does not have the @RegisterRestClient annotation. The field `service1` will not be injected as a CDI bean.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE, null);
		Diagnostic d2 = d(12, 28, 36,
				"The corresponding `org.acme.restclient.MyService` interface does not have the @RegisterRestClient annotation. The field `service2` will not be injected as a CDI bean.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE, null);
		Diagnostic d3 = d(15, 25, 52,
				"The Rest Client object should have the @RestClient annotation to be injected as a CDI bean.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
				MicroProfileRestClientErrorCode.RestClientAnnotationMissing);
		Diagnostic d4 = d(18, 25, 48,
				"The Rest Client object should have the @Inject annotation to be injected as a CDI bean.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
				MicroProfileRestClientErrorCode.InjectAnnotationMissing);
		Diagnostic d5 = d(20, 25, 61,
				"The Rest Client object should have the @Inject and @RestClient annotations to be injected as a CDI bean.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
				MicroProfileRestClientErrorCode.InjectAndRestClientAnnotationMissing);

		assertJavaDiagnostics(params, utils, //
				d1, //
				d2, //
				d3, //
				d4, //
				d5);

		String uri = javaFile.getLocation().toFile().toURI().toString();

		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d3);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Insert @RestClient", MicroProfileCodeActionId.InsertRestClientAnnotation, d3, //
						te(14, 1, 14, 1, "@RestClient\r\n\t")));

		codeActionParams = createCodeActionParams(uri, d4);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Insert @Inject", MicroProfileCodeActionId.InsertInjectAnnotation, d4, //
						te(17, 1, 17, 1, "@Inject\r\n\t")));

		codeActionParams = createCodeActionParams(uri, d5);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Insert @Inject, @RestClient",  MicroProfileCodeActionId.InsertInjectAndRestClientAnnotations, d5, //
						te(20, 1, 20, 1, "@RestClient\r\n\t@Inject\r\n\t")));

	}

	@Test
	public void restClientAnnotationMissingForFieldsJakarta() throws Exception {
		IJavaProject javaProject = ProjectUtils.getJavaProject(MicroProfileMavenProjectName.open_liberty);
		IJDTUtils utils = JDT_UTILS;

		MicroProfileJavaDiagnosticsParams params = new MicroProfileJavaDiagnosticsParams();
		IFile javaFile = javaProject.getProject()
				.getFile(new Path("src/main/java/com/demo/rest/injectAnnotation.java"));
		params.setUris(Arrays.asList(javaFile.getLocation().toFile().toURI().toString()));
		params.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d1 = d(10, 21, 40,
				"The corresponding `com.demo.rest.MyService` interface does not have the @RegisterRestClient annotation. The field `NoAnnotationMissing` will not be injected as a CDI bean.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE, null);
		Diagnostic d2 = d(13, 19, 42,
				"The Rest Client object should have the @Inject annotation to be injected as a CDI bean.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
				MicroProfileRestClientErrorCode.InjectAnnotationMissing);
		Diagnostic d3 = d(15, 19, 55,
				"The Rest Client object should have the @Inject and @RestClient annotations to be injected as a CDI bean.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
				MicroProfileRestClientErrorCode.InjectAndRestClientAnnotationMissing);

		assertJavaDiagnostics(params, utils, //
				d1, //
				d2, //
				d3);

		String uri = javaFile.getLocation().toFile().toURI().toString();

		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d2);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Insert @Inject",  MicroProfileCodeActionId.InsertInjectAnnotation, d2, //
						te(12, 4, 12, 4, "@Inject\n\t")),
				ca(uri, "Generate OpenAPI Annotations for 'injectAnnotation'", MicroProfileCodeActionId.GenerateOpenApiAnnotations, d2, //
						te(0, 0, 0, 0, "")));

		codeActionParams = createCodeActionParams(uri, d3);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Insert @Inject, @RestClient", MicroProfileCodeActionId.InsertInjectAndRestClientAnnotations, d3, //
						te(15, 4, 15, 4, "@RestClient\n\t@Inject\n\t")),
				ca(uri, "Generate OpenAPI Annotations for 'injectAnnotation'", MicroProfileCodeActionId.GenerateOpenApiAnnotations, d3, //
						te(0, 0, 0, 0, "")));

	}

	@Test
	public void restClientAnnotationMissingForInterface() throws Exception {
		IJavaProject javaProject = ProjectUtils.getJavaProject(MicroProfileMavenProjectName.rest_client_quickstart);
		IJDTUtils utils = JDT_UTILS;

		MicroProfileJavaDiagnosticsParams params = new MicroProfileJavaDiagnosticsParams();
		IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/org/acme/restclient/MyService.java"));
		params.setUris(Arrays.asList(javaFile.getLocation().toFile().toURI().toString()));
		params.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d = d(2, 17, 26,
				"The interface `MyService` does not have the @RegisterRestClient annotation. The 1 fields references will not be injected as CDI beans.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
				MicroProfileRestClientErrorCode.RegisterRestClientAnnotationMissing);

		assertJavaDiagnostics(params, utils, //
				d);

		String uri = javaFile.getLocation().toFile().toURI().toString();
		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Insert @RegisterRestClient", MicroProfileCodeActionId.InsertRegisterRestClient, d, //
						te(0, 28, 2, 0,
								"\r\n\r\nimport org.eclipse.microprofile.rest.client.inject.RegisterRestClient;\r\n\r\n@RegisterRestClient\r\n")));
	}

	@Test
	public void restClientAnnotationMissingForInterfaceJakarta() throws Exception {
		IJavaProject javaProject = ProjectUtils.getJavaProject(MicroProfileMavenProjectName.open_liberty);
		IJDTUtils utils = JDT_UTILS;

		MicroProfileJavaDiagnosticsParams params = new MicroProfileJavaDiagnosticsParams();
		IFile javaFile = javaProject.getProject()
				.getFile(new Path("src/main/java/com/demo/rest/MyService.java"));
		params.setUris(Arrays.asList(javaFile.getLocation().toFile().toURI().toString()));
		params.setDocumentFormat(DocumentFormat.Markdown);

		Diagnostic d = d(2, 17, 26,
				"The interface `MyService` does not have the @RegisterRestClient annotation. The 2 fields references will not be injected as CDI beans.",
				DiagnosticSeverity.Warning, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
				MicroProfileRestClientErrorCode.RegisterRestClientAnnotationMissing);

		assertJavaDiagnostics(params, utils, //
				d);

		String uri = javaFile.getLocation().toFile().toURI().toString();
		MicroProfileJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
		assertJavaCodeAction(codeActionParams, utils, //
				ca(uri, "Insert @RegisterRestClient", MicroProfileCodeActionId.InsertRegisterRestClient, d, //
						te(0, 22, 2, 0,
								"\r\n\r\nimport org.eclipse.microprofile.rest.client.inject.RegisterRestClient;\r\n\r\n@RegisterRestClient\r\n")),
				ca(uri, "Generate OpenAPI Annotations for 'MyService'", MicroProfileCodeActionId.GenerateOpenApiAnnotations, d, //
						te(0, 0, 0, 0, "")));
	}
}
