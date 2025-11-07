/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.jdt.core.project;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.lsp4mp.jdt.internal.core.ConfigSourceProviderRegistry;
import org.eclipse.lsp4mp.jdt.internal.core.FakeJavaProject;

/**
 * {@link JDTMicroProfileProject} manager.
 *
 * @author Angelo ZERR
 *
 */
public class JDTMicroProfileProjectManager {

	private static final Logger LOGGER = Logger.getLogger(JDTMicroProfileProjectManager.class.getName());

	private static final JDTMicroProfileProjectManager INSTANCE = new JDTMicroProfileProjectManager();

	public static JDTMicroProfileProjectManager getInstance() {
		return INSTANCE;
	}

	private final Map<IJavaProject, JDTMicroProfileProject> projects;
	private MicroProfileProjectListener microprofileProjectListener;

	private class MicroProfileProjectListener implements IResourceChangeListener, IResourceDeltaVisitor {

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			switch (event.getType()) {
			case IResourceChangeEvent.PRE_DELETE: {
				IResource resource = event.getResource();
				if (resource != null) {
					switch (resource.getType()) {
					case IResource.PROJECT:
						// called when project is deleted.
						evict((IProject) resource);
						break;
					}
				}
				break;
			}
			case IResourceChangeEvent.PRE_CLOSE: {
				IResource resource = event.getResource();
				if (resource != null) {
					switch (resource.getType()) {
					case IResource.PROJECT:
						// called when project is closed.
						evict((IProject) resource);
						break;
					}
				}
				break;
			}
			case IResourceChangeEvent.POST_CHANGE:
				IResourceDelta resourceDelta = event.getDelta();
				if (resourceDelta != null) {
					try {
						resourceDelta.accept(this);
					} catch (CoreException e) {
						if (LOGGER.isLoggable(Level.SEVERE)) {
							LOGGER.log(Level.SEVERE, "Error while tracking MicroProfile properties file", e);
						}
					}
				}
				break;
			}
		}

		private void evict(IProject project) {
			IJavaProject javaProject = JavaCore.create(project);
			if (javaProject != null) {
				// Remove the JDTMicroProfile project instance from the cache.
				projects.remove(javaProject);
			}
		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			if (resource == null) {
				return false;
			}
			switch (resource.getType()) {
			case IResource.ROOT:
			case IResource.PROJECT:
			case IResource.FOLDER:
				return resource.isAccessible();
			case IResource.FILE:
				IFile file = (IFile) resource;
				if ((isFileDeleted(delta) || isFileContentChanged(delta) || isFileAdded(delta))
						&& isConfigSource(file)) {
					// it's a config source file (ex : microprofile-config.properties)
					JDTMicroProfileProject mpProject = getJDTMicroProfileProject(file);
					if (mpProject != null) {
						// Evict the properties cache
						mpProject.evictConfigSourcesCache();
					}
				}
			}
			return false;
		}

		private boolean isFileDeleted(IResourceDelta delta) {
			return delta.getKind() == IResourceDelta.REMOVED;
		}

		private boolean isFileAdded(IResourceDelta delta) {
			return delta.getKind() == IResourceDelta.ADDED;
		}

		private boolean isFileContentChanged(IResourceDelta delta) {
			return (delta.getKind() == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.CONTENT) != 0);
		}

	}

	private JDTMicroProfileProjectManager() {
		this.projects = new HashMap<>();

	}

	public JDTMicroProfileProject getJDTMicroProfileProject(IJavaProject project) throws JavaModelException {
		return getJDTMicroProfileProject(project, true);
	}

	private JDTMicroProfileProject getJDTMicroProfileProject(IJavaProject project, boolean create)
			throws JavaModelException {
		IJavaProject javaProject = FakeJavaProject.getRealJavaProject(project);
		JDTMicroProfileProject info = projects.get(javaProject);
		if (info == null) {
			if (!create) {
				return null;
			}
			info = new JDTMicroProfileProject(javaProject);
			projects.put(javaProject, info);
		}
		return info;
	}

	public boolean isConfigSource(IFile file) {
		String fileName = file.getName();
		for (IConfigSourceProvider provider : ConfigSourceProviderRegistry.getInstance().getProviders()) {
			if (provider.isConfigSource(fileName)) {
				return true;
			}
		}
		return false;
	}

	public JDTMicroProfileProject getJDTMicroProfileProject(IFile file) {
		IJavaProject project = JavaCore.create(file.getProject());
		if (project == null) {
			return null;
		}
		try {
			return getJDTMicroProfileProject(project, false);
		} catch (JavaModelException e) {
			LOGGER.log(Level.SEVERE, "Error while getting MicroProfile project", e);
		}
		return null;
	}

	public void initialize() {
		if (microprofileProjectListener != null) {
			return;
		}
		microprofileProjectListener = new MicroProfileProjectListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(microprofileProjectListener);
	}

	public void destroy() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(microprofileProjectListener);
	}
}
