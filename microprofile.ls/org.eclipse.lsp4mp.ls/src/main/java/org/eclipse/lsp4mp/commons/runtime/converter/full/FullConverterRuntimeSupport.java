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
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.runtime.converter.AbstractConverterRuntimeSupport;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;

/**
 * ConverterRuntimeSupport allows dynamic validation of string values against
 * Java types using MicroProfile Config converters from the project classpath.
 *
 * <p>
 * This class uses reflection to avoid classloader issues and caches converters
 * per type for performance. Only converters discovered via
 * Config.getConverter(Class) are used.
 * </p>
 * 
 * @author Angelo ZERR
 */
public class FullConverterRuntimeSupport extends AbstractConverterRuntimeSupport<Object> {

	private static final Logger LOGGER = Logger.getLogger(FullConverterRuntimeSupport.class.getName());

	private static final String[] DEFAULT_RESOLVERS = { "io.smallrye.config.SmallRyeConfigProviderResolver" };

	public FullConverterRuntimeSupport(MicroProfileProjectRuntime project) {
		super(project, ExecutionMode.FULL);
	}

	@Override
	protected Object loadConfig() {
		Object resolver = loadConfigProviderResolverReflect();
		if (resolver != null) {
			try {
				Method getBuilder = resolver.getClass().getMethod("getBuilder");
				Object builder = getBuilder.invoke(resolver);

				builder.getClass().getMethod("forClassLoader", ClassLoader.class).invoke(builder,
						getProject().getRuntimeClassLoader());

				builder.getClass().getMethod("addDiscoveredConverters").invoke(builder);

				return builder.getClass().getMethod("build").invoke(builder);

			} catch (Throwable e) {
				LOGGER.log(Level.INFO,
						"Error creating MicroProfile Config via reflection from " + resolver.getClass().getName(), e);
			}
		}
		return null;
	}

	@Override
	protected ConverterValidator newConverter(Object config, Class<?> type) {
		return new FullConverterValidator(config, type);
	}

	/**
	 * Loads the first available ConfigProviderResolver.
	 */
	private Object loadConfigProviderResolverReflect() {
		ClassLoader runtimeCL = getProject().getRuntimeClassLoader();
		if (runtimeCL == null)
			return null;

		List<Object> resolvers = new ArrayList<>();
		for (String className : DEFAULT_RESOLVERS) {
			try {
				Class<?> cls = Class.forName(className, false, runtimeCL);
				resolvers.add(cls.getConstructor().newInstance());
				LOGGER.info("Loaded ConfigProviderResolver: " + className);
			} catch (ClassNotFoundException e) {
				// ignore
			} catch (Throwable t) {
				LOGGER.log(Level.INFO, "Cannot instantiate ConfigProviderResolver: " + className, t);
			}
		}

		if (resolvers.isEmpty()) {
			try {
				Class<?> resolverClass = Class.forName("org.eclipse.microprofile.config.spi.ConfigProviderResolver",
						true, runtimeCL);
				ServiceLoader<?> loader = ServiceLoader.load(resolverClass, runtimeCL);
				for (Object provider : loader) {
					resolvers.add(provider);
					LOGGER.info("Loaded ConfigProviderResolver via ServiceLoader: " + provider.getClass().getName());
				}
			} catch (Throwable t) {
				LOGGER.log(Level.INFO, "ServiceLoader scan failed for ConfigProviderResolver", t);
			}
		}

		if (resolvers.isEmpty()) {
			LOGGER.warning("No ConfigProviderResolver found in project classpath");
			return null;
		}

		return resolvers.get(0);
	}

}
