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
package org.eclipse.lsp4mp.commons.runtime.converter;

import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileRuntimeSupport;

/**
 * Runtime API for performing MicroProfile Config value conversion and
 * validation.
 *
 * <p>
 * This API behaves differently depending on the active execution mode:
 * </p>
 *
 * <ul>
 * <li><strong>SAFE</strong> – Fully isolated runtime based on the embedded
 * SmallRye Config implementation. No access to the project classpath or
 * classes.</li>
 *
 * <li><strong>FULL</strong> – Uses the project's classpath and its actual
 * MicroProfile Config implementation.</li>
 * </ul>
 */
public interface ConverterRuntimeSupportApi extends MicroProfileRuntimeSupport {

	/**
	 * Returns the current execution mode.
	 *
	 * @return the execution mode ({@link ExecutionMode#SAFE} or
	 *         {@link ExecutionMode#FULL})
	 */
	ExecutionMode getExecutionMode();

	/**
	 * Returns whether the classpath contains an implementation of
	 * {@code ConfigProviderResolver}.
	 *
	 * <p>
	 * Behavior by mode:
	 * </p>
	 *
	 * <ul>
	 * <li><strong>SAFE mode</strong>: always returns {@code false}, since the
	 * project's classpath and classes are never consulted. The embedded SmallRye
	 * Config runtime is used.</li>
	 *
	 * <li><strong>FULL mode</strong>: returns {@code true} if a MicroProfile Config
	 * implementation is present on the project classpath (e.g. SmallRye, Helidon,
	 * DeltaSpike).</li>
	 * </ul>
	 *
	 * @return {@code true} if a {@code ConfigProviderResolver} implementation is
	 *         found, {@code false} otherwise
	 */
	boolean hasConfigProviderResolver();

	/**
	 * Validates a raw string value by converting it to the specified target type.
	 * Conversion failures are reported through the given diagnostics collector.
	 *
	 * <p>
	 * Behavior by execution mode:
	 * </p>
	 *
	 * <ul>
	 * <li><strong>SAFE mode</strong>:
	 * <ul>
	 * <li>Uses only the embedded SmallRye Config runtime.</li>
	 * <li>Does not load or inspect any project classes.</li>
	 * <li>Ignores any custom converters defined by the project.</li>
	 * </ul>
	 * </li>
	 *
	 * <li><strong>FULL mode</strong>:
	 * <ul>
	 * <li>Uses the project's {@code ConfigProviderResolver} implementation.</li>
	 * <li>Loads and converts using project's classes and custom converters.</li>
	 * </ul>
	 * </li>
	 * </ul>
	 *
	 * @param value     the raw configuration value to validate
	 * @param type      the fully-qualified type name expected
	 * @param collector the diagnostics collector used to report validation errors
	 */
	void validate(String value, String type, DiagnosticsCollector collector);
}
