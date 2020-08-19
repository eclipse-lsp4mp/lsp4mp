/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.jdt.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;

/**
 * Class for base context.
 *
 * @author Angelo ZERR
 *
 */
public class BaseContext {

	private final IJavaProject javaProject;
	private final List<MicroProfilePropertiesScope> scopes;
	private final Map<String, Object> cache;

	public BaseContext(IJavaProject javaProject, List<MicroProfilePropertiesScope> scopes) {
		this.javaProject = javaProject;
		this.scopes = scopes;
		cache = new HashMap<>();
	}

	/**
	 * Associates the specified value with the specified key in the cache.
	 *
	 * @param key   the key.
	 * @param value the value.
	 */
	public void put(String key, Object value) {
		cache.put(key, value);
	}

	/**
	 * Returns the value to which the specified key is mapped, or {@code null} if
	 * this map contains no mapping for the key.
	 *
	 * @param key the key.
	 * @return the value to which the specified key is mapped, or {@code null} if
	 *         this map contains no mapping for the key.
	 */
	public Object get(String key) {
		return cache.get(key);
	}

	/**
	 * Returns the java project.
	 *
	 * @return the java project.
	 */
	public IJavaProject getJavaProject() {
		return javaProject;
	}

	/**
	 * Returns the scope of the search.
	 *
	 * @return the scope of the search.
	 */
	public List<MicroProfilePropertiesScope> getScopes() {
		return scopes;
	}
}
