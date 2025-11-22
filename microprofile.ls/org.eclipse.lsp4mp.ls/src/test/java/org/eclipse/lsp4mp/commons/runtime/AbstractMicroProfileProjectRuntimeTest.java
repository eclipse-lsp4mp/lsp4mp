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
 * <p>
 * Example types tested include:
 * </p>
 * <ul>
 * <li>Primitive types: int, Integer</li>
 * <li>Arrays: int[], Integer[]</li>
 * <li>Collections: List&lt;Integer&gt;</li>
 * <li>Class references: Class&lt;Object&gt;</li>
 * </ul>
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
	public void testClassObject() {
		assertValiateWithConverter("java.lang.String", "java.lang.Class<java.lang.Object>");
	}
}
