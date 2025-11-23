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

import org.eclipse.lsp4mp.commons.runtime.EnumConstantsProvider;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.TypeProvider;
import org.eclipse.lsp4mp.commons.runtime.converter.AbstractConverterValidator;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;

/**
 * FULL mode implementation of {@link ConverterValidator} for a single type.
 *
 * <p>
 * In FULL mode:
 * </p>
 * <ul>
 * <li>The project classpath and project classes <strong>are</strong>
 * accessible.</li>
 * <li>Converters may be obtained from the project using reflection.</li>
 * <li>Both standard and user-defined MicroProfile Config converters are
 * used.</li>
 * <li>Reflection handles both {@code Optional<Converter<T>>} and direct
 * {@code Converter<T>} returns.</li>
 * </ul>
 *
 * <p>
 * This validator uses reflection to obtain and invoke the {@code convert}
 * method on the target converter dynamically, ensuring that project classes are
 * correctly handled.
 * </p>
 * 
 * @author Angelo
 */
class FullConverterValidator extends AbstractConverterValidator<Object> {

	/** Method to obtain the converter from the Config instance */
	private Method getConverterMethod;

	/** Method to convert a string value using the converter */
	private Method convertMethod;

	/** Indicates whether getConverter returns an Optional */
	private boolean hasOptional;

	/**
	 * Creates a new FULL validator for the given type using the provided
	 * {@code Config}.
	 *
	 * @param config  the MicroProfile Config instance to obtain converters from
	 * @param forType the type to validate values against
	 */
	FullConverterValidator(Object config, Class<?> forType) {
		super(config, forType);
	}

	/**
	 * Converts the given string value using the project converter obtained via
	 * reflection.
	 *
	 * @param value the string value to convert
	 * @throws Exception if conversion fails
	 */
	@Override
	protected void convert(String value) throws Exception {
		Object config = getConfig();
		Class<?> forType = getForType();
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
	 * Initializes the validator by obtaining the converter for the target type via
	 * reflection.
	 *
	 * <p>
	 * Handles both {@code Optional<Converter<T>>} and direct {@code Converter<T>}
	 * returned by {@code Config#getConverter(Class<T>)}.
	 * </p>
	 *
	 * @return {@code true} if a converter is available, {@code false} otherwise
	 * @throws Exception if initialization fails
	 */
	@Override
	protected boolean initialize() throws Exception {
		Object config = getConfig();
		Type forType = getForType();

		// Obtain the method getConverter(Class<T> forType)
		Method getConverter = config.getClass().getMethod("getConverter", Class.class);
		getConverter.setAccessible(true);
		Object optional = getConverter.invoke(config, forType);

		Object converterInstance;
		hasOptional = true;

		try {
			Method isPresent = optional.getClass().getMethod("isPresent");
			if (!(boolean) isPresent.invoke(optional)) {
				return false;
			}
			Method get = optional.getClass().getMethod("get");
			converterInstance = get.invoke(optional);
		} catch (Exception e) {
			// Fallback if getConverter returns Converter<T> directly
			hasOptional = false;
			converterInstance = optional;
		}

		Method convert = converterInstance.getClass().getMethod("convert", String.class);
		convert.setAccessible(true);

		this.getConverterMethod = getConverter;
		this.convertMethod = convert;
		return true;
	}

	@Override
	public void refreshEnumType(EnumConstantsProvider enumConstNamesProvider, TypeProvider typeProvider,
			ExecutionMode executionMode) {
		Class<?> forType = getForType();
		if (forType != null && forType.isEnum()) {
			forType = (Class<?>) typeProvider.findType(forType.getTypeName(), enumConstNamesProvider,
					executionMode);
			try {
				initialize();
			} catch (Exception e) {
				// Do nothing
			}
		}
	}
}
