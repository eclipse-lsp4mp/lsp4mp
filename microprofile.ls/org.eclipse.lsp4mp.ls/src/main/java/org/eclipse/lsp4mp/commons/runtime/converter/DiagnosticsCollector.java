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

/**
 * Functional interface used to collect diagnostics produced during the
 * validation of configuration values.
 * 
 * <p>
 * Implementations of this interface are responsible for recording or reporting
 * errors, warnings, or informational messages associated with a value being
 * validated, including its position and severity.
 * </p>
 * 
 * <p>
 * Typical usage is in conjunction with {@link ConverterValidator}, where a
 * validator checks a string value against a specific type and calls this
 * collector if the value is invalid or cannot be converted.
 * </p>
 * 
 * <pre>
 * collector.collect("Invalid integer format", "microprofile-config", "SRCFG00029", DiagnosticSeverity.Error, 0, 5);
 * </pre>
 * 
 * @author Angelo ZERR
 */
@FunctionalInterface
public interface DiagnosticsCollector {

	/**
	 * Collects a diagnostic message for a configuration value.
	 *
	 * @param errorMessage the human-readable error message describing the problem
	 * @param source       the source of the diagnostic (e.g.,
	 *                     "microprofile-config")
	 * @param errorCode    a machine-readable code identifying the error
	 * @param severity     the severity of the diagnostic (error, warning, info)
	 * @param start        the start offset in the source value for highlighting
	 * @param end          the end offset in the source value for highlighting
	 */
	void collect(String errorMessage, String source, String errorCode, int start, int end);
}