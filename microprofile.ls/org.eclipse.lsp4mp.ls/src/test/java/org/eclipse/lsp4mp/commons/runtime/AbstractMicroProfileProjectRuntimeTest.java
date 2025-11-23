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
package org.eclipse.lsp4mp.commons.runtime;

import java.util.ArrayList;

import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.metadata.ValueHint;
import org.junit.Test;

/**
 * Base test class for validating MicroProfile Config converters across
 * different project runtimes and execution modes (SAFE or FULL).
 *
 * <p>
 * This abstract class provides common tests for primitive types, arrays, lists,
 * and classes. Subclasses specify the actual {@link MicroProfileProjectRuntime}
 * and {@link ExecutionMode}, as well as whether the project classpath includes
 * a MicroProfile Config implementation (e.g., Quarkus) or not (e.g., Liberty).
 * </p>
 *
 * <p>
 * The {@link #assertValiateWithConverter(String, String)} and its overloads are
 * used to validate string values against a target type, checking that valid
 * values pass and invalid values produce appropriate diagnostic messages.
 * </p>
 * 
 *
 * @author Angelo ZERR
 */
public abstract class AbstractMicroProfileProjectRuntimeTest extends AbstractProjectRuntimeTest {

	/**
	 * Creates a new base test instance for the given MicroProfile project runtime
	 * and execution mode.
	 *
	 * @param projectRuntime the MicroProfile project runtime (e.g., Quarkus,
	 *                       Liberty)
	 * @param executionMode  the execution mode (SAFE or FULL)
	 */
	public AbstractMicroProfileProjectRuntimeTest(MicroProfileProjectRuntime projectRuntime,
			ExecutionMode executionMode) {
		super(projectRuntime, executionMode);
	}

	@Test
	public void testInt() {
		assertValiateWithConverter("1", "int");
		assertValiateWithConverter("1X", "int", "SRCFG00029: Expected an integer value, got \"1X\"");
	}

	@Test
	public void testInteger() {
		assertValiateWithConverter("1", "java.lang.Integer");
		assertValiateWithConverter("1X", "java.lang.Integer", "SRCFG00029: Expected an integer value, got \"1X\"");
	}

	@Test
	public void testOptionalInteger() {
		assertValiateWithConverter("1", "java.util.Optional<java.lang.Integer>");
		assertValiateWithConverter("1X", "java.util.Optional<java.lang.Integer>",
				"SRCFG00029: Expected an integer value, got \"1X\"");
	}

	@Test
	public void testOptionalInt() {
		assertValiateWithConverter("1", "java.util.OptionalInt");
		assertValiateWithConverter("1X", "java.util.OptionalInt", "SRCFG00029: Expected an integer value, got \"1X\"");
	}

	@Test
	public void testSupplierInteger() {
		assertValiateWithConverter("1", "java.util.function.Supplier<java.lang.Integer>");
		assertValiateWithConverter("1X", "java.util.function.Supplier<java.lang.Integer>",
				"SRCFG00029: Expected an integer value, got \"1X\"");
	}

	@Test
	public void testJakartaInjectProviderInteger() {
		assertValiateWithConverter("1", "jakarta.inject.Provider<java.lang.Integer>");
		assertValiateWithConverter("1X", "jakarta.inject.Provider<java.lang.Integer>",
				"SRCFG00029: Expected an integer value, got \"1X\"");
	}

	@Test
	public void testIntArray() {
		assertValiateWithConverter("1,2,3,4", "int[]");
		assertValiateWithConverter("1,2X,3x,4", "int[]", "SRCFG00029: Expected an integer value, got \"2X\"",
				"SRCFG00029: Expected an integer value, got \"3x\"");
	}

	@Test
	public void testIntegerArray() {
		assertValiateWithConverter("1,2,3,4", "java.lang.Integer[]");
		assertValiateWithConverter("1,2X,3x,4", "java.lang.Integer[]",
				"SRCFG00029: Expected an integer value, got \"2X\"",
				"SRCFG00029: Expected an integer value, got \"3x\"");
	}

	@Test
	public void testIntegerList() {
		assertValiateWithConverter("1,2,3,4", "java.util.List<java.lang.Integer>");
		assertValiateWithConverter("1,2X,3x,4", "java.lang.Integer[]",
				"SRCFG00029: Expected an integer value, got \"2X\"",
				"SRCFG00029: Expected an integer value, got \"3x\"");
	}

	@Test
	public void testIntegerSet() {
		assertValiateWithConverter("1,2,3,4", "java.util.Set<java.lang.Integer>");
		assertValiateWithConverter("1,2X,3x,4", "java.lang.Integer[]",
				"SRCFG00029: Expected an integer value, got \"2X\"",
				"SRCFG00029: Expected an integer value, got \"3x\"");
	}

	@Test
	public void testClassObject() {
		assertValiateWithConverter("foo", "java.lang.Class<java.lang.Object>");
	}

	@Test
	public void testWrongClass() {
		assertValiateWithConverter("foo", "java.lang.WrongClass<java.lang.Object>");
	}

	@Test
	public void testEnum() {
		// Enum value valid
		assertValiateWithConverter("BLOCK", "org.jboss.logmanager.handlers.AsyncHandler$OverflowAction");

		// Invalid enum value triggers validation error
		assertValiateWithConverter("BLACK", "org.jboss.logmanager.handlers.AsyncHandler$OverflowAction",
				"SRCFG00049: Cannot convert BLACK to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block");
	}

	@Test
	public void testJakartaInjectProviderEnum() {
		// Enum value valid
		assertValiateWithConverter("BLOCK",
				"jakarta.inject.Provider<org.jboss.logmanager.handlers.AsyncHandler$OverflowAction>");

		// Invalid enum value triggers validation error
		assertValiateWithConverter("BLACK",
				"jakarta.inject.Provider<org.jboss.logmanager.handlers.AsyncHandler$OverflowAction>",
				"SRCFG00049: Cannot convert BLACK to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block");
	}

	@Test
	public void testRefreshEnumValues() {
		MicroProfileProjectInfo info = new MicroProfileProjectInfo();
		info.setProperties(new ArrayList<>());
		info.setHints(new ArrayList<>());

		// Create enum io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType

		// fill properties
		ItemMetadata p = new ItemMetadata();
		p.setName("io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType");
		p.setSourceType("io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType");
		info.getProperties().add(p);

		// fill hints
		ItemHint hint = new ItemHint();
		hint.setName("io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType");
		hint.setValues(new ArrayList<>());
		info.getHints().add(hint);

		ValueHint webAppValue = new ValueHint();
		webAppValue.setValue("WEB_APP");
		hint.getValues().add(webAppValue);

		ValueHint value = new ValueHint();
		value.setValue("SERVICE");
		hint.getValues().add(value);

		// Test 1) FOO doesn't exist
		assertValiateWithConverter("WEB_APP", "io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType", info);
		assertValiateWithConverter("SERVICE", "io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType", info);
		assertValiateWithConverter("FOO", "io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType", info,
				"SRCFG00049: Cannot convert FOO to enum class io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType, allowed values: web-app,service");

		// Test 2) Add FOO
		value = new ValueHint();
		value.setValue("FOO");
		hint.getValues().add(value);

		assertValiateWithConverter("WEB_APP", "io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType", info);
		assertValiateWithConverter("SERVICE", "io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType", info);
		assertValiateWithConverter("FOO", "io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType", info);

		// Test 3) Remove WEB_APP
		hint.getValues().remove(webAppValue);

		assertValiateWithConverter("WEB_APP", "io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType", info,
				"SRCFG00049: Cannot convert WEB_APP to enum class io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType, allowed values: service,foo");
		assertValiateWithConverter("SERVICE", "io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType", info);
		assertValiateWithConverter("FOO", "io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType", info);

	}

}
