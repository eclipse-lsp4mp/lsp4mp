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
package org.eclipse.lsp4mp.commons;

import java.util.Collections;
import java.util.Set;

import org.eclipse.lsp4mp.commons.metadata.ConfigurationMetadata;

/**
 * MicroProfile Project Information
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileProjectInfo extends ConfigurationMetadata {

	public static final MicroProfileProjectInfo EMPTY_PROJECT_INFO;

	static {
		EMPTY_PROJECT_INFO = new MicroProfileProjectInfo();
		EMPTY_PROJECT_INFO.setProperties(Collections.emptyList());
		EMPTY_PROJECT_INFO.setHints(Collections.emptyList());
		EMPTY_PROJECT_INFO.setProjectURI("");
	}

	private String projectURI;

	private ClasspathKind classpathKind;
	
	private Set<String> classpath;

	/**
	 * Returns the project URI.
	 *
	 * @return the project URI.
	 */
	public String getProjectURI() {
		return projectURI;
	}

	/**
	 * Set the project URI.
	 *
	 * @param projectURI the project URI.
	 */
	public void setProjectURI(String projectURI) {
		this.projectURI = projectURI;
	}

	/**
	 * Returns the class path kind.
	 *
	 * @return
	 */
	public ClasspathKind getClasspathKind() {
		return classpathKind;
	}

	/**
	 * Set the class path kind.
	 *
	 * @param classpathKind
	 */
	public void setClasspathKind(ClasspathKind classpathKind) {
		this.classpathKind = classpathKind;
	}

	public Set<String> getClasspath() {
		return classpath;
	}
	
	public void setClasspath(Set<String> classpath) {
		this.classpath = classpath;
	}
}
