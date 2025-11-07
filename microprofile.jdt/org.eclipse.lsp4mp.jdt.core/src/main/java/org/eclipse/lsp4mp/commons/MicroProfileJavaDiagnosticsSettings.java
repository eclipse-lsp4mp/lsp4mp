/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.commons;

import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;

/**
 * Settings controlling how MicroProfile Java diagnostics validate the values of
 * configuration properties declared in Java sources.
 * <p>
 * These settings are specifically used when inspecting annotations such as
 * {@code @ConfigProperty(defaultValue = "...")}, and when applying
 * MicroProfile-compatible converters to validate that the provided default
 * values can be converted to the expected type.
 * </p>
 *
 * <h2>Ignored Properties</h2> The {@link #patterns} list contains a set of
 * string patterns that match against the <strong>property name</strong>
 * declared in {@code @ConfigProperty(name = "…")}. If a property name matches
 * one of these patterns, then its default value is <strong>excluded from
 * validation</strong>.
 *
 * <h2>Diagnostics Severity</h2> {@link #validationValueSeverity} controls the
 * severity used when reporting validation issues:
 * <ul>
 * <li>value not convertible using MicroProfile converters</li>
 * <li>invalid format (e.g., list, map, boolean, integer, etc.)</li>
 * <li>other type-related validation problems</li>
 * </ul>
 *
 * This enables fine-grained control over how strict the tooling should be when
 * validating default values defined in Java code.
 */
public class MicroProfileJavaDiagnosticsSettings {

	/**
	 * List of patterns used to determine which configuration properties should be
	 * ignored during value validation.
	 * <p>
	 * Each pattern is matched against the {@code name} attribute of
	 * {@code @ConfigProperty}. If the property name matches any pattern, its
	 * {@code defaultValue} will not be validated.
	 * </p>
	 */
	private List<String> patterns;

	/**
	 * The severity level used when reporting validation diagnostics for property
	 * values that cannot be converted using MicroProfile converters.
	 */
	private DiagnosticSeverity validationValueSeverity;

	private ExecutionMode mode;

	/**
	 * Creates new diagnostics settings for validating Java-based MicroProfile
	 * configuration property values.
	 *
	 * @param patterns                the list of patterns applied to the property
	 *                                name to determine whether its default value
	 *                                should be excluded from validation (may be
	 *                                {@code null})
	 * @param validationValueSeverity the severity to use when reporting invalid
	 *                                default values
	 * @param the                     execution mode to use to validate property
	 *                                value
	 */
	public MicroProfileJavaDiagnosticsSettings(List<String> patterns, DiagnosticSeverity validationValueSeverity,
			ExecutionMode mode) {
		this.patterns = patterns;
		this.validationValueSeverity = validationValueSeverity;
		this.mode = mode;
	}

	/**
	 * Returns the list of patterns used to disable validation for certain
	 * configuration property names.
	 * <p>
	 * If the list is {@code null}, an empty list is returned.
	 * </p>
	 *
	 * @return an unmodifiable list of patterns determining which property names
	 *         should be excluded from validation
	 */
	public List<String> getPatterns() {
		return patterns == null ? Collections.emptyList() : this.patterns;
	}

	/**
	 * Returns the severity level to use when reporting diagnostics for invalid
	 * configuration property values.
	 *
	 * @return the severity used for reporting value validation errors
	 */
	public DiagnosticSeverity getValidationValueSeverity() {
		return validationValueSeverity;
	}

	public ExecutionMode getMode() {
		return mode;
	}
}
