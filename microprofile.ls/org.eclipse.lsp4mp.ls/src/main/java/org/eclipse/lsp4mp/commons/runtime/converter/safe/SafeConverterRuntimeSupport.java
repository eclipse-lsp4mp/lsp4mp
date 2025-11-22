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

import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.runtime.converter.AbstractConverterRuntimeSupport;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;
import org.eclipse.microprofile.config.Config;

import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * SAFE execution mode implementation of
 * {@link AbstractConverterRuntimeSupport}.
 *
 * <p>
 * In SAFE mode:
 * </p>
 * <ul>
 * <li>No access to the project classpath or classes.</li>
 * <li>No project JARs or user-defined classes are loaded.</li>
 * <li>Only the embedded SmallRye Config runtime is used for validation and
 * conversion.</li>
 * <li>No reflection on project classes is performed.</li>
 * <li>Provides deterministic and sandboxed conversion behavior.</li>
 * </ul>
 *
 * <p>
 * This class overrides the loading of configuration and the creation of
 * converters to use SmallRye defaults only.
 * </p>
 * 
 * @author Angelo ZERR
 */
public class SafeConverterRuntimeSupport extends AbstractConverterRuntimeSupport<Config> {

	/**
	 * Constructs a new SAFE runtime support instance for the given project.
	 *
	 * @param project the owning MicroProfile project runtime
	 */
	public SafeConverterRuntimeSupport(MicroProfileProjectRuntime project) {
		super(project, ExecutionMode.SAFE);
	}

	/**
	 * Loads the embedded SmallRye configuration for SAFE execution.
	 *
	 * @return a {@link Config} instance backed by SmallRye Config
	 */
	@Override
	protected Config loadConfig() {
		return new SmallRyeConfigBuilder().build();
	}

	/**
	 * Creates a new {@link ConverterValidator} for the specified type using
	 * SmallRye Config.
	 *
	 * @param config the SmallRye {@link Config} instance
	 * @param type   the target Java type to validate/convert
	 * @return a {@link ConverterValidator} instance for SAFE conversion
	 */
	@Override
	protected ConverterValidator newConverter(Config config, Class<?> type) {
		return new SafeConverterValidator(config, type);
	}
}
