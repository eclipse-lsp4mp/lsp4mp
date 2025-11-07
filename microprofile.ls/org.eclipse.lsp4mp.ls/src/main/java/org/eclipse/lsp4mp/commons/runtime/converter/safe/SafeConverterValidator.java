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

import java.lang.reflect.Type;
import java.util.Optional;

import org.eclipse.lsp4mp.commons.runtime.converter.AbstractConverterValidator;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;
import org.eclipse.lsp4mp.commons.runtime.converter.DiagnosticsCollector;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;

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
class SafeConverterValidator extends AbstractConverterValidator<Config> {

	private Converter<?> converter;

	/**
	 * Creates a new validator for the given type using the provided {@code Config}
	 * instance.
	 * 
	 * @param config  the MicroProfile Config instance to obtain converters from
	 * @param forType the type to validate values against
	 */
	SafeConverterValidator(Config config, Type forType) {
		super(config, forType);
	}

	@Override
	protected void convert(String value) throws Exception {
		if (converter == null) {
			return;
		}
		converter.convert(value);
	}

	/**
	 * Prepares the validator by obtaining the converter for the target type via
	 * reflection.
	 *
	 * @return {@code true} if a converter is available, {@code false} otherwise
	 */
	@Override
	protected boolean initialize() throws Exception {
		Config config = getConfig();
		Type forType = getForType();

		Optional<Converter<?>> result = config.getConverter((Class) forType);
		if (result.isEmpty()) {
			return false;
		}
		converter = result.get();
		return true;
	}
}
