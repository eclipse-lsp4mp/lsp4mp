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
import org.eclipse.lsp4mp.commons.runtime.DiagnosticsCollector;
import org.eclipse.lsp4mp.commons.runtime.EnumConstantsProvider;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileRuntimeSupport;
import org.eclipse.lsp4mp.commons.runtime.TypeSignatureParser.EmulateType;
import org.eclipse.lsp4mp.commons.runtime.TypeSignatureParser.EnumType;
import org.eclipse.lsp4mp.commons.runtime.converter.safe.EnumTypeConverterValidator;

/**
 * Base class providing runtime support for validating and converting string
 * values against Java types using MicroProfile Config converters.
 *
 * <p>
 * This class caches converters per type to improve performance and handles
 * collections, maps, optionals, suppliers, and arrays.
 * </p>
 *
 * <p>
 * Behavior differs depending on the execution mode:
 * </p>
 * <ul>
 * <li><strong>SAFE</strong> – No access to the project classpath or classes.
 * Only the embedded SmallRye Config runtime is used. No reflection on project
 * classes is performed. This ensures sandboxed validation.</li>
 * <li><strong>FULL</strong> – Uses the project classpath. Project classes and
 * custom converters may be loaded via reflection.</li>
 * </ul>
 *
 * @param <T> the configuration source type (e.g., MicroProfile Config instance)
 * @author Angelo
 */
public abstract class AbstractConverterRuntimeSupport<T> extends AbstractMicroProfileRuntimeSupport
		implements ConverterRuntimeSupportApi {

	private static final Logger LOGGER = Logger.getLogger(AbstractConverterRuntimeSupport.class.getName());

	/** Null converter that does nothing */
	private static final ConverterValidator NULL_CONVERTER = new ConverterValidator() {
		@Override
		public void validate(String value, int start, DiagnosticsCollector collector) {
			// No-op
		}

		@Override
		public boolean canValidate() {
			return false;
		}
	};

	/** Configuration instance used for conversions */
	private T config;

	/** Initialization flag */
	private boolean initialized;

	/** Cache of ConverterValidator per type */
	private final Map<String, ConverterValidator> converterCache = new ConcurrentHashMap<>();

	/**
	 * Constructs a new runtime support instance.
	 *
	 * @param project       the owning MicroProfile project runtime
	 * @param executionMode the execution mode (SAFE or FULL)
	 */
	public AbstractConverterRuntimeSupport(MicroProfileProjectRuntime project, ExecutionMode executionMode) {
		super(project, executionMode);
	}

	/**
	 * Validates a string value against the specified type.
	 *
	 * <p>
	 * Delegates to a cached {@link ConverterValidator} for the given type.
	 * </p>
	 *
	 * @param value     the string value to validate
	 * @param type      the fully-qualified type name
	 * @param collector diagnostics collector to report validation errors
	 */
	@Override
	public void validate(String value, String type, EnumConstantsProvider enumConstNamesProvider,
			DiagnosticsCollector collector) {
		try {
			T cfg = getConfig();
			if (cfg == null) {
				return;
			}
			ConverterValidator validator = converterCache.computeIfAbsent(type,
					t -> resolveConverter(getProject().findType(t, enumConstNamesProvider, getExecutionMode()), cfg));
			if (validator.canValidate()) {
				// Refresh if needed Enum type if validator manages enum type
				validator.refreshEnumType(enumConstNamesProvider, getProject(), getExecutionMode());
				// Validate value
				validator.validate(value, collector);
			}
		} catch (Throwable e) {
			LOGGER.log(Level.WARNING, "Error while validating '" + value + "' value with type '" + type + "'", e);
		}
	}

	/**
	 * Returns the configuration instance, initializing it lazily if necessary.
	 *
	 * @return the configuration instance, or null if unavailable
	 */
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
	 * Returns {@code true} if a MicroProfile ConfigProviderResolver is present in
	 * the current classpath.
	 *
	 * <p>
	 * SAFE mode always returns {@code false} since the project classpath is never
	 * consulted.
	 * </p>
	 *
	 * @return true if a ConfigProviderResolver implementation is present, false
	 *         otherwise
	 */
	public boolean hasConfigProviderResolver() {
		getConfig();
		return initialized && config != null;
	}

	/**
	 * Resets the cached configuration and converters.
	 */
	@Override
	public void reset() {
		config = null;
		initialized = false;
		converterCache.clear();
	}

	/**
	 * Resolves a {@link ConverterValidator} for the specified Java type.
	 *
	 * <p>
	 * Handles collections, maps, optionals, suppliers, and arrays by returning
	 * appropriate validators.
	 * </p>
	 *
	 * @param type   the Java type to resolve
	 * @param config the configuration instance
	 * @return a ConverterValidator for the given type, or a no-op validator if none
	 */
	private ConverterValidator resolveConverter(Type type, T config) {
		if (type == null) {
			return NULL_CONVERTER;
		}
		if (type instanceof EnumType) {
			return new EnumTypeConverterValidator((EnumType) type);
		}
		Type rawType = rawTypeOf(type);
		if (type instanceof ParameterizedType) {
			Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
			if (rawType == List.class || rawType == Set.class) {
				return newCollectionConverter(resolveConverter(typeArgs[0], config));
			}
			if (rawType == Map.class) {
				return newMapConverter(resolveConverter(typeArgs[0], config), resolveConverter(typeArgs[1], config));
			}
			if (rawType == Optional.class) {
				return newOptionalConverter(resolveConverter(typeArgs[0], config));
			}
			if (rawType == Supplier.class
					|| (rawType != null && "jakarta.inject.Provider".equals(rawType.getTypeName()))) {
				return resolveConverter(typeArgs[0], config);
			}
		} else if (rawType instanceof Class) {
			Class rawTypeClass = (Class) rawType;
			if (rawTypeClass.isArray()) {
				return newCollectionConverter(resolveConverter(rawTypeClass.getComponentType(), config));
			}
		}
		if (!(type instanceof Class)) {
			return NULL_CONVERTER;
		}
		return newConverter(config, (Class) type);
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
	 * @param <T>  the type of the class
	 * @param type the type to resolve
	 * @return the raw class, or null if it cannot be determined
	 */
	static Type rawTypeOf(Type type) {
		if (type instanceof Class || type instanceof EmulateType) {
			return type;
		} else if (type instanceof ParameterizedType) {
			return rawTypeOf(((ParameterizedType) type).getRawType());
		} else if (type instanceof GenericArrayType) {
			return Array.newInstance((Class) rawTypeOf(((GenericArrayType) type).getGenericComponentType()), 0)
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

		private final ConverterValidator delegate;

		/**
		 * Creates a new collection converter delegating to the given element converter.
		 *
		 * @param delegate the element converter
		 */
		CollectionConverter(ConverterValidator delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean canValidate() {
			return delegate.canValidate();
		}

		@Override
		public void validate(String value, int start, DiagnosticsCollector collector) {
			int startOffset = start;
			StringBuilder currentValue = new StringBuilder();
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == ',') {
					delegate.validate(currentValue.toString(), startOffset, collector);
					currentValue.setLength(0);
					startOffset = i + 1;
				} else {
					currentValue.append(c);
				}
			}
			if (!currentValue.isEmpty()) {
				delegate.validate(currentValue.toString(), startOffset, collector);
			}
		}
	}

	/**
	 * Creates a new converter for the given type using the provided configuration.
	 *
	 * @param config the configuration instance
	 * @param type   the Java type to convert
	 * @return a ConverterValidator for the type
	 */
	protected abstract ConverterValidator newConverter(T config, Class<?> type);

	/**
	 * Loads the configuration instance used for conversions.
	 *
	 * @return the configuration instance
	 */
	protected abstract T loadConfig();
}
