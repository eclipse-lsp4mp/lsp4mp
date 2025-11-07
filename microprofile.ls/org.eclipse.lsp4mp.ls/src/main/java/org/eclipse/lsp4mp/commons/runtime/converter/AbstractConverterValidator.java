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
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link ConverterValidator} that uses reflection to access a
 * MicroProfile Config {@code Converter} for a specific type.
 * 
 * <p>
 * This class dynamically obtains the converter via reflection from the provided
 * Config instance, and executes it to validate string values. Conversion errors
 * are reported through a {@link DiagnosticsCollector}.
 * </p>
 * 
 * <p>
 * It supports converters returned either as
 * {@code java.util.Optional<Converter<T>>} or directly as {@code Converter<T>}.
 * </p>
 */
public abstract class AbstractConverterValidator<T> implements ConverterValidator {

	private static final Logger LOGGER = Logger.getLogger(AbstractConverterValidator.class.getName());

	private final T config;
	private final Type forType;
	private boolean prepared;

	/**
	 * Creates a new validator for the given type using the provided {@code Config}
	 * instance.
	 * 
	 * @param config  the MicroProfile Config instance to obtain converters from
	 * @param forType the type to validate values against
	 */
	public AbstractConverterValidator(T config, Type forType) {
		this.config = config;
		this.forType = forType;
		this.prepared = prepare();
	}

	/**
	 * Validates the given string value against the target type. If conversion
	 * fails, the error is reported through the provided collector.
	 * 
	 * @param value     the string value to validate
	 * @param start     the start offset for diagnostics
	 * @param collector the collector to report conversion errors
	 */
	@Override
	public void validate(String value, int start, DiagnosticsCollector collector) {
		if (!prepared)
			return;

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
	 * Prepares the validator by obtaining the converter for the target type via
	 * reflection.
	 *
	 * @return {@code true} if a converter is available, {@code false} otherwise
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
	 * Extracts the meaningful error message from a reflection/invocation exception.
	 *
	 * @param e the exception thrown during conversion
	 * @return the extracted error message or {@code null} if none
	 */
	private static String getErrorMessage(Throwable e) {
		Throwable t = e;
		while (t instanceof InvocationTargetException
				|| (t != null && t.getCause() instanceof InvocationTargetException)) {
			t = t.getCause();
		}
		if (t != null) {
			return t.getMessage();
		}
		return e.getMessage();
	}

	/**
	 * Indicates whether this validator can perform validation for the target type.
	 * 
	 * @return {@code true} if a converter is available, {@code false} otherwise
	 */
	@Override
	public boolean canValidate() {
		return prepared;
	}

	public T getConfig() {
		return config;
	}

	public Type getForType() {
		return forType;
	}

	protected abstract boolean initialize() throws Exception;

	protected abstract void convert(String value) throws Exception;

}
