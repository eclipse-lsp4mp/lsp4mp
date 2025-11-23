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
 * Unit tests for {@link LibertyProjectRuntime} in SAFE execution mode.
 *
 * <p>
 * In SAFE mode, MicroProfile Config does not have access to the project
 * classpath or its JAR dependencies. SmallRye Config from the MicroProfile
 * Language Server is used instead for validation. As a result, enums, classes,
 * and other project-specific types cannot be fully validated.
 * </p>
 *
 * <p>
 * Scenarios tested:
 * </p>
 * <ul>
 * <li>Enum values defined in project classes cannot be fully validated.</li>
 * <li>Enum values defined in JAR dependencies are not accessible.</li>
 * </ul>
 *
 * <p>
 * This test ensures that property validation in SAFE mode behaves correctly
 * even when the MicroProfile Config implementation is not present in the
 * project classpath.
 * </p>
 * 
 * @author Angelo
 */
public class LibertyProjectRuntimeInSafeModeTest extends AbstractMicroProfileProjectRuntimeTest {

	/**
	 * Creates a new test instance for the Liberty project runtime in SAFE mode.
	 */
	public LibertyProjectRuntimeInSafeModeTest() {
		super(LIBERTY_PROJECT_RUNTIME, ExecutionMode.SAFE);
	}

	/**
	 * Tests enum conversion for enums defined in project classes.
	 * <p>
	 * In SAFE mode, project enums cannot be fully validated because the project
	 * classpath is not used. Validation relies solely on the SmallRye Config from
	 * the MicroProfile LS.
	 * </p>
	 */
	@Test
	public void testEnumFromClasses() {
		// Enum value valid
		assertValiateWithConverter("FOO", "org.acme.MyEnum");

		// Invalid enum value cannot be validated properly in SAFE mode
		assertValiateWithConverter("FOOX", "org.acme.MyEnum");
	}

}
