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
package org.eclipse.lsp4mp.commons.runtime.converter.full;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.eclipse.lsp4mp.commons.runtime.converter.AbstractConverterValidator;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;
import org.eclipse.lsp4mp.commons.runtime.converter.DiagnosticsCollector;

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
class FullConverterValidator extends AbstractConverterValidator<Object> {

	private Method getConverterMethod;
	private Method convertMethod;
	private boolean hasOptional;

	/**
	 * Creates a new validator for the given type using the provided {@code Config}
	 * instance.
	 * 
	 * @param config  the MicroProfile Config instance to obtain converters from
	 * @param forType the type to validate values against
	 */
	FullConverterValidator(Object config, Type forType) {
		super(config, forType);
	}

	@Override
	protected void convert(String value) throws Exception {
		Object config = getConfig();
		Type forType = getForType();
		Object converterInstance;
		if (hasOptional) {
			Object optional = getConverterMethod.invoke(config, forType);
			Method get = optional.getClass().getMethod("get");
			converterInstance = get.invoke(optional);
		} else {
			converterInstance = getConverterMethod.invoke(config, forType);
		}

		convertMethod.invoke(converterInstance, value);
	}

	/**
	 * Prepares the validator by obtaining the converter for the target type via
	 * reflection.
	 *
	 * @return {@code true} if a converter is available, {@code false} otherwise
	 */
	@Override
	protected boolean initialize() throws Exception {
		Object config = getConfig();
		Type forType = getForType();

		// get <T> Optional<Converter<T>> Config#getConverter(Class<T> forType) method.
		Method getConverter = config.getClass().getMethod("getConverter", Class.class);
		getConverter.setAccessible(true);
		Object optional = getConverter.invoke(config, forType);

		Object converterInstance;
		hasOptional = true;
		try {
			// get Optional<Converter<T>>.isPresent()
			Method isPresent = optional.getClass().getMethod("isPresent");
			if (!(boolean) isPresent.invoke(optional)) {
				return false;
			}
			// get Optional<Converter<T>>.get()
			Method get = optional.getClass().getMethod("get");
			converterInstance = get.invoke(optional);
		} catch (Exception e) {
			// for some reason, getConverter(Class<T> forType) returns
			// sometimes Converter<T> ?
			hasOptional = false;
			converterInstance = optional;
		}

		Method convert = converterInstance.getClass().getMethod("convert", String.class);
		convert.setAccessible(true);

		this.getConverterMethod = getConverter;
		this.convertMethod = convert;
		return true;
	}
}
