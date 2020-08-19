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
package org.eclipse.lsp4mp.jdt.internal.core;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.lsp4mp.jdt.core.ArtifactResolver;
import org.eclipse.lsp4mp.jdt.internal.core.utils.DependencyUtil;

/**
 * Maven artifact resolver used to download JAR and JAR sources with maven.
 *
 * @author Angelo ZERR
 *
 */
public class MavenArtifactResolver implements ArtifactResolver {

	private static final Logger LOGGER = Logger.getLogger(MavenArtifactResolver.class.getName());

	@Override
	public String getArtifact(Artifact artifact, IProgressMonitor monitor) {
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		String version = artifact.getVersion();
		String classifier = artifact.getClassifier();
		File jarFile = null;
		try {
			jarFile = DependencyUtil.getArtifact(groupId, artifactId, version, classifier, monitor);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Maven artifact JAR (groupId=" + groupId + ", artifactId=" + artifactId
					+ ", version=" + version + ") download failed.", e);
			return null;
		}
		return jarFile != null ? jarFile.toString() : null;
	}

	@Override
	public Set<Artifact> getDependencies(Artifact artifact, IProgressMonitor monitor) {
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		String version = artifact.getVersion();
		try {
			return DependencyUtil.getDependencies(groupId, artifactId, version, monitor);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Maven artifact JAR dependencies (groupId=" + groupId + ", artifactId="
					+ artifactId + ", version=" + version + ") failed.", e);
			return Collections.emptySet();
		}
	}
}