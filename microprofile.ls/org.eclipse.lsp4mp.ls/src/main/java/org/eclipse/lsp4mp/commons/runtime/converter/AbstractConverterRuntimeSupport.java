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

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4mp.commons.runtime.AbstractMicroProfileRuntimeSupport;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileRuntimeSupport;

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
public abstract class AbstractConverterRuntimeSupport<T> extends AbstractMicroProfileRuntimeSupport
		implements ConverterRuntimeSupportApi {

	private static final Logger LOGGER = Logger.getLogger(AbstractConverterRuntimeSupport.class.getName());

	private T config;
	private boolean initialized;

	/** Cache of ConverterInvoker per type */
	private final Map<String, ConverterValidator> converterCache = new ConcurrentHashMap<>();

	public AbstractConverterRuntimeSupport(MicroProfileProjectRuntime project, ExecutionMode executionMode) {
		super(project, executionMode);
	}

	@Override
	public void validate(String value, String type, DiagnosticsCollector collector) {
		try {
			T cfg = getConfig();
			if (cfg == null) {
				return;
			}
			// Get or prepare the converter invoker
			ConverterValidator validator = converterCache.computeIfAbsent(type,
					t -> resolveConverter(getProject().findClassType(t), cfg));
			if (validator.canValidate()) {
				validator.validate(value, collector);
			}
		} catch (Throwable e) {
			LOGGER.log(Level.WARNING, "Error while validating '" + value + "' value with type '" + type + "", e);
		}
	}

	protected T getConfig() {
		if (config != null || initialized) {
			return config;
		}
		config = loadConfig();
		initialized = true;
		return config;
	}

	@Override
	public Class<? extends MicroProfileRuntimeSupport> getClassApi() {
		return ConverterRuntimeSupportApi.class;
	}

	/**
	 * Returns true if classpath hosts an implementation of MicroProfile
	 * ConfigProviderResolver and false otherwise.
	 * 
	 * @return true if classpath hosts an implementation of MicroProfile
	 *         ConfigProviderResolver and false otherwise.
	 */
	public boolean hasConfigProviderResolver() {
		getConfig();
		return initialized && config != null;
	}

	/**
	 * Reset cached config and converters.
	 */
	@Override
	public void reset() {
		config = null;
		initialized = false;
		converterCache.clear();
	}

	/**
	 * Resolves a converter for the specified type using the provided Config
	 * instance.
	 * 
	 * <p>
	 * Handles collections, maps, optionals, suppliers, and arrays by returning
	 * appropriate {@link ConverterValidator} instances.
	 * </p>
	 * 
	 * @param type   the Java type to resolve a converter for
	 * @param config the MicroProfile Config instance
	 * @return a {@link ConverterValidator} capable of validating values of the
	 *         given type
	 */
	protected ConverterValidator resolveConverter(Type type, T config) {
		Class rawType = rawTypeOf(type);
		if (type instanceof ParameterizedType) {
			Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
			if (rawType == List.class) {
				return newCollectionConverter(resolveConverter(typeArgs[0], config));
			}

			if (rawType == Set.class) {
				return newCollectionConverter(resolveConverter(typeArgs[0], config));
			}

			if (rawType == Map.class) {
				return newMapConverter(resolveConverter(typeArgs[0], config), resolveConverter(typeArgs[1], config));
			}

			if (rawType == Optional.class) {
				return newOptionalConverter(resolveConverter(typeArgs[0], config));
			}

			if (rawType == Supplier.class || "jakarta.inject.Provider".equals(rawType.getName())) {
				return resolveConverter(typeArgs[0], config);
			}
		} else if (rawType != null && rawType.isArray()) {
			return newCollectionConverter(resolveConverter(rawType.getComponentType(), config));
		}

		return newConverter(config, type);
	}

	private static ConverterValidator newOptionalConverter(ConverterValidator converter) {
		return converter;
	}

	private static ConverterValidator newMapConverter(ConverterValidator converter, ConverterValidator converter2) {
		return converter;
	}

	private static ConverterValidator newCollectionConverter(ConverterValidator converter) {
		return new CollectionConverter(converter);
	}

	/**
	 * Returns the raw {@link Class} corresponding to a given {@link Type}.
	 * 
	 * @param <T>  the type of the raw class
	 * @param type the Java type
	 * @return the raw {@link Class} of the type, or {@code null} if it cannot be
	 *         determined
	 */
	static <T> Class<T> rawTypeOf(Type type) {
		if (type instanceof Class) {
			return (Class) type;
		} else if (type instanceof ParameterizedType) {
			return rawTypeOf(((ParameterizedType) type).getRawType());
		} else if (type instanceof GenericArrayType) {
			return (Class<T>) Array.newInstance(rawTypeOf(((GenericArrayType) type).getGenericComponentType()), 0)
					.getClass();
		} else {
			return null;
		}
	}

	/**
	 * Converter implementation for collections (List, Set, array) that delegates
	 * validation to an underlying element converter.
	 */
	static class CollectionConverter implements ConverterValidator {

		private ConverterValidator delegate;

		/**
		 * Creates a new collection converter delegating to the given element converter.
		 * 
		 * @param delegate the element converter
		 */
		CollectionConverter(ConverterValidator delegate) {
			this.delegate = delegate;
		}

		/**
		 * Returns {@code true} if the underlying element converter can validate values.
		 */
		@Override
		public boolean canValidate() {
			return delegate.canValidate();
		}

		/**
		 * Validates a comma-separated list of values by delegating each element to the
		 * underlying converter.
		 * 
		 * @param value     the string containing one or more comma-separated elements
		 * @param start     the start offset for diagnostics
		 * @param collector the collector to report validation errors
		 */
		@Override
		public void validate(String value, int start, DiagnosticsCollector collector) {
			int end = 0;

			StringBuilder currentValue = new StringBuilder();
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == ',') {
					delegate.validate(currentValue.toString(), start, collector);
					currentValue.setLength(0);
					start = i + 1;
				} else {
					currentValue.append(c);
				}
			}
			if (!currentValue.isEmpty()) {
				delegate.validate(currentValue.toString(), start, collector);
			}
		}
	}

	protected abstract ConverterValidator newConverter(T config, Type type);

	protected abstract T loadConfig();

}
