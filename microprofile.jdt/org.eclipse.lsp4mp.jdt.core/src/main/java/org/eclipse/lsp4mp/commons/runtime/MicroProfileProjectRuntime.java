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
package org.eclipse.lsp4mp.commons.runtime;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4mp.commons.runtime.converter.ConverterRuntimeSupportApi;
import org.eclipse.lsp4mp.commons.runtime.converter.DiagnosticsCollector;
import org.eclipse.lsp4mp.commons.runtime.converter.full.FullConverterRuntimeSupport;
import org.eclipse.lsp4mp.commons.runtime.converter.safe.SafeConverterRuntimeSupport;

/**
 * MicroProfileProjectRuntime represents a running MicroProfile project.
 *
 * <p>
 * It encapsulates:
 * <ul>
 * <li>The project classpath (and a dedicated ClassLoader)</li>
 * <li>Instances of runtime supports (e.g., ConverterRuntimeSupport)</li>
 * <li>A cache of loaded classes</li>
 * </ul>
 * 
 * @author Angelo ZERR
 * 
 */
public class MicroProfileProjectRuntime {

	private static final Logger LOGGER = Logger.getLogger(MicroProfileProjectRuntime.class.getName());

	/** Map of registered runtime supports for this project */
	private final Map<Class<? extends MicroProfileRuntimeSupport>, MicroProfileRuntimeSupport> safeRuntimesSupport;
	private final Map<Class<? extends MicroProfileRuntimeSupport>, MicroProfileRuntimeSupport> fullRuntimesSupport;

	/**
	 * Parent ClassLoader of the current thread used as parent for the
	 * runtimeClassLoader
	 */
	private final ClassLoader parentClassLoader;

	/** Cache of loaded classes (String → Class) to avoid reloading */
	private final Map<String, Type> classTypes;

	/** Dedicated ClassLoader for the project runtime, created from the classpath */
	private ProjectClassLoader runtimeClassLoader;

	private Set<String> classpath;

	/**
	 * Main constructor.
	 *
	 * @param classpath a set of project paths (jars or directories)
	 */
	public MicroProfileProjectRuntime(Set<String> classpath) {
		// Initialize maps
		safeRuntimesSupport = new HashMap<>();
		fullRuntimesSupport = new HashMap<>();
		classTypes = new HashMap<>();

		parentClassLoader = Thread.currentThread().getContextClassLoader();

		// Update the classpath and create runtimeClassLoader
		updateClassPath(classpath);

		// 1) Automatically load runtime supports via ServiceLoader
		loadRuntimeSupports();

		// 2) Manually register ConverterRuntimeSupport
		registerRuntimeSupport(new SafeConverterRuntimeSupport(this));
		registerRuntimeSupport(new FullConverterRuntimeSupport(this));
	}

	public void validateValue(String value, String type, ExecutionMode preferredMode, DiagnosticsCollector collector) {
		ConverterRuntimeSupportApi converterRuntimeSupport = getRuntimeSupport(ConverterRuntimeSupportApi.class,
				preferredMode);
		if (!converterRuntimeSupport.hasConfigProviderResolver()) {
			converterRuntimeSupport = getRuntimeSupport(ConverterRuntimeSupportApi.class, ExecutionMode.SAFE);
		}
		converterRuntimeSupport.validate(value, type, collector);
	}

	/**
	 * Loads all available MicroProfileRuntimeSupport implementations via
	 * ServiceLoader.
	 */
	private void loadRuntimeSupports() {
		try {
			ServiceLoader<MicroProfileRuntimeSupport> loader = ServiceLoader.load(MicroProfileRuntimeSupport.class,
					getRuntimeClassLoader());

			for (MicroProfileRuntimeSupport runtimeSupport : loader) {
				registerRuntimeSupport(runtimeSupport);
			}
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to load runtime supports via ServiceLoader", t);
		}
	}

	/**
	 * Retrieves a registered runtime support for this project.
	 *
	 * @param type the class of the runtime support
	 * @return the registered instance or null if not present
	 */
	public <T extends MicroProfileRuntimeSupport> T getRuntimeSupport(Class<T> type, ExecutionMode executionMode) {
		if (executionMode == ExecutionMode.FULL) {
			return type.cast(fullRuntimesSupport.get(type));
		}
		return type.cast(safeRuntimesSupport.get(type));
	}

	/**
	 * Registers a runtime support for this project.
	 *
	 * @param runtimeSupport the instance to register
	 */
	public <T extends MicroProfileRuntimeSupport> void registerRuntimeSupport(T runtimeSupport) {
		if (runtimeSupport.getExecutionMode() == ExecutionMode.FULL) {
			fullRuntimesSupport.put(runtimeSupport.getClassApi(), runtimeSupport);
		} else {
			safeRuntimesSupport.put(runtimeSupport.getClassApi(), runtimeSupport);
		}
		;
	}

	/**
	 * Returns the dedicated ClassLoader for this project's runtime.
	 */
	public ClassLoader getRuntimeClassLoader() {
		return runtimeClassLoader;
	}

	/**
	 * Updates the project classpath and creates a new ClassLoader.
	 *
	 * @param classpath set of project paths (jars or directories)
	 */
	public void updateClassPath(Set<String> classpath) {
		this.classpath = classpath;
		// Create a ParentLast URLClassLoader specific for this project
		this.runtimeClassLoader = new ProjectClassLoader(classpath, parentClassLoader);
		fullRuntimesSupport.values().forEach(MicroProfileRuntimeSupport::reset);
		safeRuntimesSupport.values().forEach(MicroProfileRuntimeSupport::reset);
	}

	/**
	 * Retrieves the Class corresponding to the given fully qualified name. Uses an
	 * internal cache to avoid reloading the same class multiple times.
	 *
	 * @param type fully qualified class name (FQCN)
	 * @return the corresponding Class, or null if not found
	 */
	public Type findClassType(String type) {
		Type cl = classTypes.get(type);
		if (cl == null) {
			try {
				cl = forNameSmart(type);
				// Load the class via the project's dedicated ClassLoader
				classTypes.put(type, cl);
			} catch (Exception e) {
				// ignore if class does not exist
			}
		}
		return cl;
	}

	private Type forNameSmart(String typeName) throws ClassNotFoundException {

		// Primitives
		switch (typeName) {
		case "int":
			return int.class;
		case "boolean":
			return boolean.class;
		case "byte":
			return byte.class;
		case "char":
			return char.class;
		case "short":
			return short.class;
		case "long":
			return long.class;
		case "float":
			return float.class;
		case "double":
			return double.class;
		case "void":
			return void.class;
		}

		if (typeName.endsWith("[]")) {
			String element = typeName.substring(0, typeName.length() - 2);
			Class<?> elementClass = (Class<?>) forNameSmart(element);
			return java.lang.reflect.Array.newInstance(elementClass, 0).getClass();
		} else if (typeName.indexOf('<') == -1) {
			return Class.forName(typeName, false, getRuntimeClassLoader());
		} else {
			return TypeSignatureParser.parse(typeName, getRuntimeClassLoader());
		}
	}

	/**
	 * Clears cached classes (to call after project rebuild)
	 */
	public void clearProjectClassCache() {
		classpath.clear();
		runtimeClassLoader.clearProjectClassCache();
	}

	/**
	 * Returns the classpath.
	 * 
	 * @return the classpath
	 */
	public Set<String> getClasspath() {
		return classpath;
	}

}
