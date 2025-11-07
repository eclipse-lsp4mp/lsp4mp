/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.services.properties.expressions;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.ca;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.d;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.getDefaultMicroProfileProjectInfo;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.te;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.testCodeActionsFor;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.wrapWithQuarkusProject;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;
import org.eclipse.lsp4mp.ls.commons.BadLocationException;
import org.eclipse.lsp4mp.services.properties.PropertiesFileAssert;
import org.eclipse.lsp4mp.services.properties.ValidationType;
import org.junit.Test;

/**
 * Test with property expression code actions in
 * 'microprofile-config.properties' file.
 *
 * @author Angelo ZERR
 *
 */
public class PropertiesFileExpressionCodeActionsTest {

	@Test
	public void codeActionsForUnknownLogLevelValue() throws BadLocationException {
		String value = "quarkus.log.level=${ENV:WARNIN}";
		Diagnostic d = d(0, 24, 30, "Bad level \"WARNIN\"", DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d, ca("Did you mean 'WARNING'?",
				MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion, te(0, 24, 0, 30, "WARNING"), d));
	}

	@Test
	public void codeActionsForUnknownLogLevelStartsWith() throws BadLocationException {
		String value = "quarkus.log.level=${ENV:F}";
		Diagnostic d = d(0, 24, 25, "Bad level \"F\"", DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d,
				ca("Did you mean 'FINE'?", MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion,
						te(0, 24, 0, 25, "FINE"), d),
				ca("Did you mean 'FINER'?", MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion,
						te(0, 24, 0, 25, "FINER"), d),
				ca("Did you mean 'FINEST'?", MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion,
						te(0, 24, 0, 25, "FINEST"), d),
				ca("Did you mean 'FATAL'?", MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion,
						te(0, 24, 0, 25, "FATAL"), d));
	}

	@Test
	public void codeActionsForUnknownEnum() throws BadLocationException {
		String value = "quarkus.log.syslog.async.overflow=${ENV:BLACK}";
		Diagnostic d = d(0, 40, 45,
				"SRCFG00049: Cannot convert BLACK to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block",
				DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d, ca("Did you mean 'BLOCK'?",
				MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion, te(0, 40, 0, 45, "BLOCK"), d));
	}

	@Test
	public void codeActionsForUnknownEnumStartsWith() throws BadLocationException {
		// verbatim
		String value = "quarkus.log.syslog.async.overflow=${ENV:B}";
		Diagnostic d = d(0, 40, 41,
				"SRCFG00049: Cannot convert B to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block",
				DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d, ca("Did you mean 'BLOCK'?",
				MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion, te(0, 40, 0, 41, "BLOCK"), d));

		// kebab_case
		value = "quarkus.log.syslog.async.overflow=${ENV:b}";
		d = d(0, 40, 41,
				"SRCFG00049: Cannot convert b to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block",
				DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d, ca("Did you mean 'block'?",
				MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion, te(0, 40, 0, 41, "block"), d));
	}

	@Test
	public void codeActionsForUnknownBoolean() throws BadLocationException {
		String value = "quarkus.http.cors=${ENV:fals}";
		Diagnostic d = d(0, 24, 28,
				"Type mismatch: boolean expected. By default, this value will be interpreted as 'false'",
				DiagnosticSeverity.Error, ValidationType.value);
		// SmallRye doesn't throw an error when boolena is validated.
		testDiagnosticsFor(value); //, d);
		//testCodeActionsFor(value, d, ca("Did you mean 'false'?",
		//		MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion, te(0, 24, 0, 28, "false"), d));
	}

	@Test
	public void codeActionsForReplaceUnknown() throws BadLocationException {
		String value = "quarkus.log.syslog.async.overflow=${ENV:unknown-value}";
		Diagnostic d = d(0, 40, 53,
				"SRCFG00049: Cannot convert unknown-value to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block",
				DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d,
				ca("Replace with 'block'?", MicroProfileCodeActionId.UnknownEnumValueAllEnumsSuggestion,
						te(0, 40, 0, 53, "block"), d),
				ca("Replace with 'discard'?", MicroProfileCodeActionId.UnknownEnumValueAllEnumsSuggestion,
						te(0, 40, 0, 53, "discard"), d));
	}

	public static void testDiagnosticsFor(String value, Diagnostic... expected) {
		MicroProfileProjectInfo projectInfo = wrapWithQuarkusProject(getDefaultMicroProfileProjectInfo());
		PropertiesFileAssert.testDiagnosticsFor(value, projectInfo, expected);
	}

}