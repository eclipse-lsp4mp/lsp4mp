/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.services.properties;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.ca;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.d;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.getDefaultMicroProfileProjectInfo;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.te;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.testCodeActionsFor;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.wrapWithQuarkusProject;

import java.util.Arrays;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.codeaction.MicroProfileCodeActionId;
import org.eclipse.lsp4mp.ls.commons.BadLocationException;
import org.eclipse.lsp4mp.ls.commons.client.CommandKind;
import org.eclipse.lsp4mp.ls.commons.client.ConfigurationItemEdit;
import org.eclipse.lsp4mp.ls.commons.client.ConfigurationItemEditType;
import org.junit.Test;

/**
 * Test with code actions in 'microprofile-config.properties' file.
 *
 * @author Angelo ZERR
 *
 */
public class PropertiesFileCodeActionsTest {

	@Test
	public void codeActionsForUnknownProperties() throws BadLocationException {
		String value = "# quarkus.datasource.url=jdbc:postgresql:quarkus_test\n" + //
				"quarkus.application.nme=X\n" + // <-- error
				"\n" + //
				"";
		Diagnostic d = d(1, 0, 23,
				"Unrecognized property 'quarkus.application.nme', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d,
				ca("Did you mean 'quarkus.application.name' ?",
						MicroProfileCodeActionId.UnknownPropertySimilarTextSuggestion,
						te(1, 0, 1, 23, "quarkus.application.name"), d),
				caAddToExcluded("quarkus.application.nme", d), caAddToExcluded("quarkus.application.*", d));
	}

	@Test
	public void codeActionsForUnknownPropertiesParentKey() throws BadLocationException {
		String value = "abcdefghij.group=myUser\n" + //
				"abcdefghij.registry=http://my.docker-registry.net\n" + //
				"abcdefghij.labels[0].key=foo\n" + //
				"abcdefghij.labels[0].value=bar\n" + //
				"abcdefghij.readiness-probe.initial-delay-seconds=20\n" + //
				"abcdefghij.readiness-probe.period-seconds=45";

		Diagnostic d1 = d(0, 0, 16, "Unrecognized property 'abcdefghij.group', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);
		Diagnostic d2 = d(1, 0, 19,
				"Unrecognized property 'abcdefghij.registry', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);
		Diagnostic d3 = d(2, 0, 24,
				"Unrecognized property 'abcdefghij.labels[0].key', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);
		Diagnostic d4 = d(3, 0, 26,
				"Unrecognized property 'abcdefghij.labels[0].value', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);
		Diagnostic d5 = d(4, 0, 48,
				"Unrecognized property 'abcdefghij.readiness-probe.initial-delay-seconds', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);
		Diagnostic d6 = d(5, 0, 41,
				"Unrecognized property 'abcdefghij.readiness-probe.period-seconds', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);

		testDiagnosticsFor(value, d1, d2, d3, d4, d5, d6);
		testCodeActionsFor(value, d1, caAddToExcluded("abcdefghij.group", d1), caAddToExcluded("abcdefghij.*", d1));

		testCodeActionsFor(value, d2, caAddToExcluded("abcdefghij.registry", d2), caAddToExcluded("abcdefghij.*", d2));

		testCodeActionsFor(value, d3, caAddToExcluded("abcdefghij.labels[0].key", d3),
				caAddToExcluded("abcdefghij.labels[0].*", d3), caAddToExcluded("abcdefghij.*", d3));

		testCodeActionsFor(value, d4, caAddToExcluded("abcdefghij.labels[0].value", d4),
				caAddToExcluded("abcdefghij.labels[0].*", d4), caAddToExcluded("abcdefghij.*", d4));

		testCodeActionsFor(value, d5, caAddToExcluded("abcdefghij.readiness-probe.initial-delay-seconds", d5),
				caAddToExcluded("abcdefghij.readiness-probe.*", d5), caAddToExcluded("abcdefghij.*", d5));

		testCodeActionsFor(value, d6, caAddToExcluded("abcdefghij.readiness-probe.period-seconds", d6),
				caAddToExcluded("abcdefghij.readiness-probe.*", d6), caAddToExcluded("abcdefghij.*", d6));
	}

	@Test
	public void codeActionsForUnknownPropertiesParentKey2() throws BadLocationException {

		String value = "a.b.c.d=123\n" + //
				"a.c.d=123";

		Diagnostic d1 = d(0, 0, 7, "Unrecognized property 'a.b.c.d', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);
		Diagnostic d2 = d(1, 0, 5, "Unrecognized property 'a.c.d', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);

		testDiagnosticsFor(value, d1, d2);

		testCodeActionsFor(value, d1, caAddToExcluded("a.b.c.d", d1), caAddToExcluded("a.b.c.*", d1),
				caAddToExcluded("a.b.*", d1), caAddToExcluded("a.*", d1));

		testCodeActionsFor(value, d2, caAddToExcluded("a.c.d", d2), caAddToExcluded("a.c.*", d2),
				caAddToExcluded("a.*", d2));
	}

	@Test
	public void codeActionsForUnknownPropertiesParentKey3() throws BadLocationException {

		String value = "quarkus.a.b.c.d=123";

		Diagnostic d = d(0, 0, 15, "Unrecognized property 'quarkus.a.b.c.d', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);

		testDiagnosticsFor(value, d);

		testCodeActionsFor(value, d, caAddToExcluded("quarkus.a.b.c.d", d), caAddToExcluded("quarkus.a.b.c.*", d),
				caAddToExcluded("quarkus.a.b.*", d), caAddToExcluded("quarkus.a.*", d));
	}

	@Test
	public void codeActionsForUnknownPropertiesParentKey4() throws BadLocationException {

		String value = "a.b.c.d=123";

		Diagnostic d1 = d(0, 0, 7, "Unrecognized property 'a.b.c.d', it is not referenced in any Java files",
				DiagnosticSeverity.Warning, ValidationType.unknown);

		testDiagnosticsFor(value, d1);

		testCodeActionsFor(value, d1, caAddToExcluded("a.b.c.d", d1), caAddToExcluded("a.b.c.*", d1),
				caAddToExcluded("a.b.*", d1), caAddToExcluded("a.*", d1));
	}

	@Test
	public void codeActionsForUnknownLogLevelStartsWith() throws BadLocationException {
		String value = "quarkus.log.level=F";
		Diagnostic d = d(0, 18, 19, "Bad level \"F\"", DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d,
				ca("Did you mean 'FINE'?", MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion,
						te(0, 18, 0, 19, "FINE"), d),
				ca("Did you mean 'FINER'?", MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion,
						te(0, 18, 0, 19, "FINER"), d),
				ca("Did you mean 'FINEST'?", MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion,
						te(0, 18, 0, 19, "FINEST"), d),
				ca("Did you mean 'FATAL'?", MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion,
						te(0, 18, 0, 19, "FATAL"), d));
	}

	@Test
	public void codeActionsForUnknownEnum() throws BadLocationException {
		String value = "quarkus.log.syslog.async.overflow=BLACK";
		Diagnostic d = d(0, 34, 39,
				"SRCFG00049: Cannot convert BLACK to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block",
				DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d, ca("Did you mean 'BLOCK'?",
				MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion, te(0, 34, 0, 39, "BLOCK"), d));
	}

	@Test
	public void codeActionsForUnknownEnumStartsWith() throws BadLocationException {
		// verbatim
		String value = "quarkus.log.syslog.async.overflow=B";
		Diagnostic d = d(0, 34, 35,
				"SRCFG00049: Cannot convert B to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block",
				DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d, ca("Did you mean 'BLOCK'?",
				MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion, te(0, 34, 0, 35, "BLOCK"), d));

		// kebab_case
		value = "quarkus.log.syslog.async.overflow=b";
		d = d(0, 34, 35,
				"SRCFG00049: Cannot convert b to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block",
				DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d, ca("Did you mean 'block'?",
				MicroProfileCodeActionId.UnknownEnumValueSimilarTextSuggestion, te(0, 34, 0, 35, "block"), d));
	}

	@Test
	public void codeActionsForReplaceUnknown() throws BadLocationException {
		String value = "quarkus.log.syslog.async.overflow=unknown-value";
		Diagnostic d = d(0, 34, 47,
				"SRCFG00049: Cannot convert unknown-value to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block",
				DiagnosticSeverity.Error, ValidationType.value);

		testDiagnosticsFor(value, d);
		testCodeActionsFor(value, d,
				ca("Replace with 'block'?", MicroProfileCodeActionId.UnknownEnumValueAllEnumsSuggestion,
						te(0, 34, 0, 47, "block"), d),
				ca("Replace with 'discard'?", MicroProfileCodeActionId.UnknownEnumValueAllEnumsSuggestion,
						te(0, 34, 0, 47, "discard"), d));

	}

	/**
	 * Returns a code action for <code>diagnostic</code> that causes
	 * <code>item</code> to be added to
	 * <code>microprofile.tools.validation.unknown.excluded</code> client
	 * configuration
	 *
	 * @param item       the item to add to the client configuration array
	 * @param diagnostic the diagnostic for the <code>CodeAction</code>
	 * @return a code action that causes <code>item</code> to be added to
	 *         <code>microprofile.tools.validation.unknown.excluded</code> client
	 *         configuration
	 */
	private CodeAction caAddToExcluded(String item, Diagnostic diagnostic) {
		ConfigurationItemEdit configItemEdit = new ConfigurationItemEdit(
				"microprofile.tools.validation.unknown.excluded", ConfigurationItemEditType.add, item);

		Command command = new Command("Add '" + item + "' to unknown excluded array",
				CommandKind.COMMAND_CONFIGURATION_UPDATE, Arrays.asList(configItemEdit));

		return ca("Exclude '" + item + "' from unknown property validation?",
				MicroProfileCodeActionId.IgnoreUnknownProperty, command, diagnostic);
	}

	public static void testDiagnosticsFor(String value, Diagnostic... expected) {
		MicroProfileProjectInfo projectInfo = wrapWithQuarkusProject(getDefaultMicroProfileProjectInfo());
		PropertiesFileAssert.testDiagnosticsFor(value, projectInfo, expected);
	}
}