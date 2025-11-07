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
 * Represents a validator that can check a string value against a specific type
 * using the project's MicroProfile Config converters.
 *
 * <p>
 * Implementations may use reflection to invoke converters dynamically. This
 * interface focuses solely on validation: converting the value and collecting
 * any errors as diagnostics.
 * </p>
 */
public interface ConverterValidator {

    /**
     * Validates the given value and collects diagnostics if the value is invalid.
     *
     * @param value     the string value to validate
     * @param start     the start offset in the source (for diagnostics)
     * @param collector collector used to report validation errors
     */
    void validate(String value, int start, DiagnosticsCollector collector);

    /**
     * Validates the given value assuming start offset 0.
     *
     * @param value     the string value to validate
     * @param collector collector used to report validation errors
     */
    default void validate(String value, DiagnosticsCollector collector) {
        validate(value, 0, collector);
    }

    /**
     * Indicates whether this validator is ready to perform validation.
     *
     * @return true if the validator can validate values for its type, false otherwise
     */
    boolean canValidate();
}
