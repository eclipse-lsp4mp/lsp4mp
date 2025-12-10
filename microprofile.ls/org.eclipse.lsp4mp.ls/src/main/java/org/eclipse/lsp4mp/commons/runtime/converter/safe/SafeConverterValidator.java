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
package org.eclipse.lsp4mp.commons.runtime.converter.safe;

import java.util.Optional;

import org.eclipse.lsp4mp.commons.runtime.converter.AbstractConverterValidator;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * SAFE mode implementation of {@link ConverterValidator} for a single type.
 *
 * <p>
 * In SAFE mode:
 * </p>
 * <ul>
 * <li>The project classpath and project classes are <strong>not</strong>
 * accessed.</li>
 * <li>Only the embedded SmallRye Config runtime is used.</li>
 * <li>No reflection on user classes is performed.</li>
 * <li>Validation uses converters provided by SmallRye Config.</li>
 * </ul>
 *
 * <p>
 * This validator delegates the actual conversion to the {@link Converter}
 * obtained from the SmallRye {@link Config} instance.
 * </p>
 * 
 * @author Angelo
 */
class SafeConverterValidator extends AbstractConverterValidator<Config> {

	/** The converter used to convert string values to the target type */
	private Converter<?> converter;

	/**
	 * Creates a new SAFE validator for the given type using the provided
	 * {@code Config}.
	 *
	 * @param config  the MicroProfile Config instance to obtain converters from
	 * @param forType the type to validate values against
	 */
	SafeConverterValidator(Config config, Class<?> forType) {
		super(config, forType);
	}

	/**
	 * Converts the given string value using the configured SmallRye converter.
	 *
	 * <p>
	 * Does nothing if no converter is available.
	 * </p>
	 *
	 * @param value the string value to convert
	 * @throws Exception if conversion fails
	 */
	@Override
	protected void convert(String value) throws Exception {
		if (converter == null) {
			return;
		}
		converter.convert(value);
	}

	/**
	 * Initializes the validator by obtaining a converter for the target type from
	 * the SmallRye Config instance.
	 *
	 * <p>
	 * In SAFE mode, only converters from the embedded runtime are used; project
	 * classes are never accessed.
	 * </p>
	 *
	 * @return {@code true} if a converter is available, {@code false} otherwise
	 * @throws Exception if initialization fails
	 */
	@Override
	protected boolean initialize() throws Exception {
		Config config = getConfig();
		Class forType = getForType();
		Optional<Converter<?>> result = config.getConverter(forType);
		if (result.isEmpty()) {
			return false;
		}
		converter = result.get();
		return true;
	}

	@Override
	public String getConverterClassName() {
		return converter != null ? converter.getClass().getName() : null;
	}

	@Override
	public String getConverterSimpleClassName() {
		return converter != null ? converter.getClass().getSimpleName() : null;
	}
}
