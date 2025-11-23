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
package org.eclipse.lsp4mp.commons.runtime.quarkus;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.QUARKUS_PROJECT_RUNTIME;

import org.eclipse.lsp4mp.commons.runtime.AbstractMicroProfileProjectRuntimeTest;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.junit.Test;

/**
 * Unit tests for {@link QuarkusProjectRuntime} in FULL execution mode.
 *
 * <p>
 * In FULL mode, MicroProfile Config can access the project classpath and all
 * project JARs. This allows validation of enums, classes, and other types
 * defined in the project or its dependencies using reflection.
 * </p>
 *
 * <p>
 * Scenarios tested:
 * </p>
 * <ul>
 * <li>Enum values defined in project classes (custom enums).</li>
 * <li>Enum values defined in JAR dependencies (external enums).</li>
 * </ul>
 *
 * <p>
 * FULL mode uses reflection to access the project classloader and invokes
 * converters from the project MicroProfile Config implementation.
 * </p>
 * 
 * @author Angelo
 */
public class QuarkusProjectRuntimeInFullModeTest extends AbstractMicroProfileProjectRuntimeTest {

	/**
	 * Creates a new test instance for the Quarkus project runtime in FULL mode.
	 */
	public QuarkusProjectRuntimeInFullModeTest() {
		super(QUARKUS_PROJECT_RUNTIME, ExecutionMode.FULL);
	}

	/**
	 * Tests enum conversion for enums defined in project classes.
	 * <p>
	 * In FULL mode, project enums can be validated using the project's classpath
	 * and converters. Invalid values produce detailed error messages.
	 * </p>
	 */
	@Test
	public void testEnumFromClasses() {
		// Enum value valid
		assertValiateWithConverter("FOO", "org.acme.MyEnum");

		// Invalid enum value triggers validation error
		assertValiateWithConverter("FOOX", "org.acme.MyEnum",
				"SRCFG00049: Cannot convert FOOX to enum class org.acme.MyEnum, allowed values: bar,foo");
	}

}
