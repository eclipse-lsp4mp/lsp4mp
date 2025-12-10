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
package org.eclipse.lsp4mp.commons.metadata;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link ItemMetadata#getSimpleType()}.
 * <p>
 * This test class verifies that the {@code getSimpleType()} method correctly
 * converts fully qualified Java types to a readable simple form, including:
 * <ul>
 * <li>Simple types (String, int, Boolean, double)</li>
 * <li>Optional types (Optional&lt;T&gt;)</li>
 * <li>Collection types (List&lt;T&gt;, Map&lt;K,V&gt;)</li>
 * <li>Nested and complex generics with multiple levels of nesting</li>
 * </ul>
 */
public class ItemMetadataSimpleTypeTest {

	@Test
	public void testSimpleTypes() {
		check("java.lang.String", "String");
		check("int", "int");
		check("java.lang.Boolean", "Boolean");
		check("double", "double");
	}

	@Test
	public void testOptionalTypes() {
		check("java.util.Optional<java.lang.String>", "Optional<String>");
		check("java.util.Optional<java.lang.Integer>", "Optional<Integer>");
		check("java.util.Optional<java.lang.Double>", "Optional<Double>");
	}

	@Test
	public void testListTypes() {
		check("java.util.List<java.lang.String>", "List<String>");
		check("java.util.List<java.lang.Integer>", "List<Integer>");
		check("java.util.List<java.util.Optional<java.lang.Boolean>>", "List<Optional<Boolean>>");
	}

	@Test
	public void testMapTypes() {
		check("java.util.Map<java.lang.String, java.lang.Integer>", "Map<String, Integer>");
		check("java.util.Map<java.lang.String, java.util.List<java.lang.String>>", "Map<String, List<String>>");
		check("java.util.Map<java.lang.String, java.util.Optional<java.lang.Double>>", "Map<String, Optional<Double>>");
	}

	@Test
	public void testNestedGenerics() {
		check("java.util.Optional<java.util.List<java.lang.String>>", "Optional<List<String>>");
		check("java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Integer>>",
				"Map<String, Map<String, Integer>>");
		check("java.util.List<java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>>",
				"List<Map<String, List<Integer>>>");
	}

	@Test
	public void testComplexGenerics() {
		check("java.util.Optional<java.util.Map<java.lang.String, java.util.List<java.util.Optional<java.lang.Double>>>>",
				"Optional<Map<String, List<Optional<Double>>>>");
		check("java.util.Map<java.lang.String, java.util.List<java.util.Map<java.lang.Integer, java.lang.Double>>>",
				"Map<String, List<Map<Integer, Double>>>");
	}

	private static void check(String inputType, String expected) {
		ItemMetadata item = new ItemMetadata();
		item.setType(inputType);
		String result = item.getSimpleType();
		assertEquals("Type conversion failed for: " + inputType, expected, result);
	}

}
