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
package org.eclipse.lsp4mp.commons.runtime.liberty;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.LIBERTY_PROJECT_RUNTIME;

import org.eclipse.lsp4mp.commons.runtime.AbstractMicroProfileProjectRuntimeTest;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.junit.Test;

/**
 * Unit tests for {@link LibertyProjectRuntime} in FULL execution mode.
 *
 * <p>
 * Liberty projects do not host a MicroProfile Config implementation in their
 * classpath. As a result, FULL mode is not fully available, and validation
 * falls back to SAFE mode using SmallRye Config provided by the MicroProfile
 * Language Server.
 * </p>
 *
 * <p>
 * This test ensures that property validation behaves consistently when FULL
 * mode cannot access the project classpath, particularly for enums and classes
 * defined in project sources or external JARs.
 * </p>
 *
 * <p>
 * Scenarios tested:
 * </p>
 * <ul>
 * <li>Enum values defined in project classes are partially validated via
 * fallback SAFE mode.</li>
 * <li>Enum values defined in JAR dependencies cannot be validated, because the
 * JARs are not on the classpath.</li>
 * </ul>
 * 
 * <p>
 * This mirrors the behavior of SAFE mode, highlighting the limitations of
 * Liberty projects for FULL mode validation.
 * </p>
 * 
 * Author: Angelo ZERR
 */
public class LibertyProjectRuntimeInFullModeTest extends AbstractMicroProfileProjectRuntimeTest {

	/**
	 * Creates a new test instance for the Liberty project runtime in FULL mode.
	 */
	public LibertyProjectRuntimeInFullModeTest() {
		super(LIBERTY_PROJECT_RUNTIME, ExecutionMode.FULL);
	}

	/**
	 * Tests enum conversion for enums defined in project classes.
	 * <p>
	 * Because FULL mode cannot use the Liberty project classpath, validation falls
	 * back to SAFE mode and may only partially validate enums.
	 * </p>
	 */
	@Test
	public void testEnumFromClasses() {
		// Enum value valid
		assertValiateWithConverter("FOO", "org.acme.MyEnum");

		// Invalid enum value is reported as in SAFE mode
		assertValiateWithConverter("FOOX", "org.acme.MyEnum",
				"SRCFG00049: Cannot convert FOOX to enum class org.acme.MyEnum, allowed values: bar,foo");
	}

}
