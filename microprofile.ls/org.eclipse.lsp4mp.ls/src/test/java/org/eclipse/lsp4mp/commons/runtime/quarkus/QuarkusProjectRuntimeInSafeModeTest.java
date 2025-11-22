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
 * Unit tests for {@link QuarkusProjectRuntime} in SAFE execution mode.
 *
 * <p>
 * In SAFE mode, MicroProfile Config uses the default SmallRye Config from the
 * language server or MicroProfile JDT/IntelliJ classpath. Project classes and
 * project JARs are not accessible, so converters for custom enums or types from
 * project dependencies cannot be used.
 * </p>
 *
 * <p>
 * Scenarios tested:
 * </p>
 * <ul>
 * <li>Valid and invalid enum values from project classes (cannot fully
 * validate).</li>
 * <li>Valid and invalid enum values from JAR dependencies (cannot fully
 * validate).</li>
 * </ul>
 *
 * <p>
 * SAFE mode ensures no reflection or access to the project classpath is
 * performed.
 * </p>
 * 
 * @author Angelo
 */
public class QuarkusProjectRuntimeInSafeModeTest extends AbstractMicroProfileProjectRuntimeTest {

	/**
	 * Creates a new test instance for the Quarkus project runtime in SAFE mode.
	 */
	public QuarkusProjectRuntimeInSafeModeTest() {
		super(QUARKUS_PROJECT_RUNTIME, ExecutionMode.SAFE);
	}

	/**
	 * Tests enum conversion for enums defined in project classes.
	 * <p>
	 * In SAFE mode, custom project enums cannot be fully validated because the
	 * project classpath is not used. The test ensures no exceptions are thrown.
	 * </p>
	 */
	@Test
	public void testEnumFromClasses() {
		// Enum value valid (basic check with default SmallRye Config)
		assertValiateWithConverter("FOO", "org.acme.MyEnum");

		// Custom enum cannot be fully validated in SAFE mode
		assertValiateWithConverter("FOOX", "org.acme.MyEnum");
	}

	/**
	 * Tests enum conversion for enums defined in JAR dependencies.
	 * <p>
	 * In SAFE mode, enums from project JARs cannot be fully validated because
	 * project JARs are not included in the classpath.
	 * </p>
	 */
	@Test
	public void testEnumFromJAR() {
		// Enum value valid (basic check with default SmallRye Config)
		assertValiateWithConverter("BLOCK", "org.jboss.logmanager.handlers.AsyncHandler$OverflowAction");

		// Enum from JAR cannot be fully validated in SAFE mode
		assertValiateWithConverter("BLACK", "org.jboss.logmanager.handlers.AsyncHandler$OverflowAction");
	}
}
