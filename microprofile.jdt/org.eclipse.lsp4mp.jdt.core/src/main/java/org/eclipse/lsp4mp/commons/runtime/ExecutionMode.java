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

/**
 * Defines the execution mode used when performing MicroProfile Config
 * conversion and validation.
 *
 * <p>
 * Two modes are supported:
 * </p>
 *
 * <ul>
 * <li><strong>SAFE</strong> – Fully isolated mode. The project classpath is
 * never used. Project JARs and classes are not loaded. No reflection is
 * performed on user classes. Validation relies exclusively on the embedded
 * SmallRye Config runtime shipped with the MicroProfile Language Server or
 * MicroProfile IDE integration (JDT/IntelliJ).</li>
 *
 * <li><strong>FULL</strong> – Uses the project's full classpath. User classes
 * may be loaded, reflection is allowed, and the project's actual
 * {@code ConfigProviderResolver} implementation is used (SmallRye, Helidon,
 * DeltaSpike, etc.). Supports custom converters provided by the project.</li>
 * </ul>
 */
public enum ExecutionMode {

	/**
	 * SAFE mode:
	 *
	 * <ul>
	 * <li>No access to the project's classpath.</li>
	 * <li>No project JARs or classes are ever loaded.</li>
	 * <li>No reflection on user-defined classes.</li>
	 * <li>Uses only the embedded SmallRye Config runtime.</li>
	 * <li>Stable, deterministic, sandboxed validation.</li>
	 * </ul>
	 */
	SAFE(1, "safe"),

	/**
	 * FULL mode:
	 *
	 * <ul>
	 * <li>Uses the project's full classpath.</li>
	 * <li>Loads project JARs and compiled classes.</li>
	 * <li>Reflection on project classes is allowed.</li>
	 * <li>Uses the project's {@code ConfigProviderResolver} implementation.</li>
	 * <li>Supports all converters—including project-specific custom ones.</li>
	 * </ul>
	 */

	FULL(2, "full");

	private final int value;
	private final String name;

	ExecutionMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	/**
	 * Returns the enum corresponding to the integer value. Throws
	 * IllegalArgumentException if value is invalid.
	 */
	public static ExecutionMode forValue(int value) {
		ExecutionMode[] allValues = ExecutionMode.values();
		if (value < 1 || value > allValues.length) {
			throw new IllegalArgumentException("Illegal enum value: " + value);
		}
		return allValues[value - 1];
	}

	/**
	 * Returns the enum corresponding to the string value (name). Defaults to SAFE
	 * if null or unrecognized.
	 */
	public static ExecutionMode forValue(String value) {
		if (value == null)
			return SAFE;
		for (ExecutionMode mode : values()) {
			if (mode.name.equalsIgnoreCase(value)) {
				return mode;
			}
		}
		return SAFE; // default
	}
}
