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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProjectClassLoader loads classes from the project output directories and
 * project jars using a parent-last strategy. Classes from the parent
 * ClassLoader are used only if the class cannot be found in the project
 * directories or jars.
 */
class ProjectClassLoader extends ClassLoader {

	private final URLClassLoader jarClassLoader; // classloader for project jars
	private final List<Path> classesDirectories; // output folders (target/classes, build/classes)
	private final Map<String, Class<?>> projectClassCache = new ConcurrentHashMap<>();

	/**
	 * Create a ProjectClassLoader for a set of paths (jars + dirs).
	 *
	 * @param classpath Paths to jars and/or directories
	 * @param parent    Parent ClassLoader
	 */
	public ProjectClassLoader(Set<String> classpath, ClassLoader parent) {
		super(parent);

		List<URL> jarUrls = new ArrayList<>();
		List<Path> dirPaths = new ArrayList<>();

		for (String entry : classpath) {
			try {
				Path path = Paths.get(entry);
				if (Files.isDirectory(path)) {
					dirPaths.add(path);
				} else {
					jarUrls.add(path.toUri().toURL());
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		// jarClassLoader has null parent: we handle delegation ourselves
		this.jarClassLoader = new URLClassLoader(jarUrls.toArray(new URL[0]), null);
		this.classesDirectories = dirPaths;
	}

	/**
	 * Parent-last loadClass: tries project dirs -> project jars -> parent
	 */
	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		// 1. Check project cache
		Class<?> cached = projectClassCache.get(name);
		if (cached != null) {
			if (resolve)
				resolveClass(cached);
			return cached;
		}

		// 2. Try project output directories
		try {
			Class<?> cls = findClass(name);
			if (resolve)
				resolveClass(cls);
			return cls;
		} catch (ClassNotFoundException ignored) {
		}

		// 3. Try project jars
		try {
			Class<?> cls = jarClassLoader.loadClass(name);
			if (resolve)
				resolveClass(cls);
			projectClassCache.put(name, cls); // cache for future
			return cls;
		} catch (ClassNotFoundException ignored) {
		}

		// 4. Fallback to parent classloader
		return super.loadClass(name, resolve);
	}

	/**
	 * Finds a class in the project output directories.
	 */
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		// already cached?
		Class<?> cached = projectClassCache.get(name);
		if (cached != null)
			return cached;

		String relPath = name.replace('.', '/') + ".class";

		for (Path dir : classesDirectories) {
			Path classFile = dir.resolve(relPath);
			if (Files.exists(classFile)) {
				try {
					byte[] bytes = Files.readAllBytes(classFile);
					Class<?> cls = defineClass(name, bytes, 0, bytes.length);
					projectClassCache.put(name, cls);
					return cls;
				} catch (IOException e) {
					throw new RuntimeException("Failed to read class file: " + classFile, e);
				}
			}
		}

		throw new ClassNotFoundException(name);
	}

	@Override
	protected URL findResource(String name) {
		// 1. Project output directories
		for (Path dir : classesDirectories) {
			Path resourceFile = dir.resolve(name);
			if (Files.exists(resourceFile)) {
				try {
					return resourceFile.toUri().toURL();
				} catch (MalformedURLException e) {
					// should not happen
				}
			}
		}

		// 2. Project jars
		URL resource = jarClassLoader.findResource(name);
		if (resource != null) {
			return resource;
		}

		// 3. Not found in this classloader
		return null;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		List<URL> result = new ArrayList<>();

		// 1. Project output directories
		for (Path dir : classesDirectories) {
			Path resourceFile = dir.resolve(name);
			if (Files.exists(resourceFile)) {
				result.add(resourceFile.toUri().toURL());
			}
		}

		// 2. Project jars
		Enumeration<URL> jarResources = jarClassLoader.findResources(name);
		while (jarResources.hasMoreElements()) {
			result.add(jarResources.nextElement());
		}

		// 3. Parent
		Enumeration<URL> parentResources = getParent().getResources(name);
		while (parentResources.hasMoreElements()) {
			result.add(parentResources.nextElement());
		}

		return Collections.enumeration(result);
	}

	/**
	 * Clears cached classes (to call after project rebuild)
	 */
	public void clearProjectClassCache() {
		projectClassCache.clear();
	}

	/**
	 * Returns the URLs of jars used by this loader (useful for reflection or
	 * converters)
	 */
	public URL[] getJarURLs() {
		if (jarClassLoader instanceof URLClassLoader) {
			return ((URLClassLoader) jarClassLoader).getURLs();
		}
		return new URL[0];
	}
}
