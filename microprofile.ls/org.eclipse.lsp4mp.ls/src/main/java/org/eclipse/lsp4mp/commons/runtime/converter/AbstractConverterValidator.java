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

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4mp.commons.runtime.DiagnosticsCollector;

/**
 * Base class for implementing a MicroProfile Config value validator with
 * conversion support.
 *
 * <p>
 * This validator works in two execution modes:
 * </p>
 *
 * <ul>
 * <li><strong>SAFE</strong> – The project classpath and classes are never
 * accessed. Only the embedded SmallRye Config runtime is used. No reflection on
 * user classes is performed. This ensures stable and sandboxed validation.</li>
 *
 * <li><strong>FULL</strong> – Uses the project classpath. Project classes may
 * be loaded, reflection is allowed, and custom converters from the project can
 * be used.</li>
 * </ul>
 *
 * <p>
 * Conversion failures are reported through a {@link DiagnosticsCollector}.
 * </p>
 *
 * @param <T> the configuration source type (e.g., MicroProfile Config instance)
 */
public abstract class AbstractConverterValidator<T> implements ConverterValidator {

	private static final Logger LOGGER = Logger.getLogger(AbstractConverterValidator.class.getName());

	/** The configuration source (e.g., MicroProfile Config) */
	private final T config;

	/** The target type for conversion */
	private Class<?> forType;

	/** Indicates whether the validator has been prepared */
	private boolean prepared;

	/**
	 * Creates a new validator for the given target type using the provided
	 * configuration.
	 *
	 * @param config  the MicroProfile Config instance to obtain converters from
	 * @param forType the type to validate values against
	 */
	public AbstractConverterValidator(T config, Class<?> forType) {
		this.config = config;
		this.forType = forType;
		this.prepared = prepare();
	}

	/**
	 * Validates a string value against the target type.
	 *
	 * <p>
	 * Behavior by execution mode:
	 * </p>
	 * <ul>
	 * <li><strong>SAFE mode</strong> – Uses only the embedded SmallRye Config. No
	 * project classes are loaded. Custom converters from the project are
	 * ignored.</li>
	 * <li><strong>FULL mode</strong> – Uses the project's config implementation.
	 * Project classes and custom converters can be loaded via reflection.</li>
	 * </ul>
	 *
	 * @param value     the string value to validate
	 * @param start     the start offset for reporting diagnostics
	 * @param collector the diagnostics collector to report conversion errors
	 */
	@Override
	public void validate(String value, int start, DiagnosticsCollector collector) {
		if (!prepared) {
			return;
		}

		try {
			convert(value);
		} catch (Throwable e) {
			String errorMessage = getErrorMessage(e);
			if (errorMessage != null) {
				collector.collect(errorMessage, "microprofile-config", "value", start, value.length());
			}
		}
	}

	/**
	 * Prepares the validator by initializing the converter for the target type.
	 *
	 * @return {@code true} if the validator is ready to perform conversion,
	 *         {@code false} otherwise
	 */
	private boolean prepare() {
		try {
			return initialize();
		} catch (Throwable e) {
			LOGGER.log(Level.INFO, "Error preparing converter for type " + forType.getTypeName(), e);
			return false;
		}
	}

	/**
	 * Extracts a meaningful error message from a reflection or invocation
	 * exception.
	 *
	 * @param e the exception thrown during conversion
	 * @return the extracted message, or {@code null} if none
	 */
	private static String getErrorMessage(Throwable e) {
		Throwable t = e;
		while (t instanceof InvocationTargetException
				|| (t != null && t.getCause() instanceof InvocationTargetException)) {
			t = t.getCause();
		}
		return (t != null) ? t.getMessage() : e.getMessage();
	}

	/**
	 * Indicates whether this validator is prepared and can perform validation.
	 *
	 * @return {@code true} if the validator is ready, {@code false} otherwise
	 */
	@Override
	public boolean canValidate() {
		return prepared;
	}

	/**
	 * Returns the configuration source used by this validator.
	 *
	 * @return the config instance
	 */
	public T getConfig() {
		return config;
	}

	/**
	 * Returns the target type for which values are validated.
	 *
	 * @return the target type class
	 */
	public Class<?> getForType() {
		return forType;
	}

	/**
	 * Initializes the validator by setting up the converter for the target type.
	 * This method is called during construction to prepare the validator.
	 *
	 * <p>
	 * In SAFE mode, only embedded converters are used. In FULL mode, project
	 * classes and converters may be loaded via reflection.
	 * </p>
	 *
	 * @return {@code true} if initialization succeeded, {@code false} otherwise
	 * @throws Exception if initialization fails
	 */
	protected abstract boolean initialize() throws Exception;

	/**
	 * Converts the given string value to the target type.
	 *
	 * <p>
	 * In SAFE mode, conversion uses only the embedded runtime. In FULL mode,
	 * project classes and converters may be used.
	 * </p>
	 *
	 * @param value the string value to convert
	 * @throws Exception if conversion fails
	 */
	protected abstract void convert(String value) throws Exception;
}
