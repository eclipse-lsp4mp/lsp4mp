/*******************************************************************************
* Copyright (c) 2019-2026 Red Hat Inc. and others.
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesChangeEvent;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.jdt.core.IMicroProfilePropertiesChangedListener;
import org.eclipse.lsp4mp.jdt.core.project.JDTMicroProfileProject;
import org.eclipse.lsp4mp.jdt.core.project.JDTMicroProfileProjectManager;
import org.eclipse.lsp4mp.jdt.core.utils.JDTMicroProfileUtils;

/**
 * This class tracks :
 *
 * <ul>
 * <li>the classpath changed of all Java project</li>
 * <li>java sources changed on save</li>
 * </ul>
 *
 * In this case it executes the "microprofile/propertiesChanged" command on
 * client side with array of project URIs which have classpath/sources changed.
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfilePropertiesListenerManager {

	private static final Logger LOGGER = Logger.getLogger(MicroProfilePropertiesListenerManager.class.getName());

	private static final MicroProfilePropertiesListenerManager INSTANCE = new MicroProfilePropertiesListenerManager();

	// Debounce delay to group multiple file changes into a single notification
	private static final long DEBOUNCE_DELAY_MS = 2000;

	public static MicroProfilePropertiesListenerManager getInstance() {
		return INSTANCE;
	}

	private class MicroProfileListener
			implements IElementChangedListener, IResourceChangeListener, IResourceDeltaVisitor {

		private static final String JAVA_FILE_EXTENSION = "java";

		// Lock for synchronizing access to pending event state
		private final Object eventLock = new Object();
		// Event waiting to be fired after debounce delay
		private MicroProfilePropertiesChangeEvent pendingEvent = null;
		// Scheduled task for firing the pending event
		private ScheduledFuture<?> scheduledNotification = null;

		@Override
		public void elementChanged(ElementChangedEvent event) {
			if (listeners.isEmpty()) {
				return;
			}
			// Collect project names which have classpath changed.
			MicroProfilePropertiesChangeEvent mpEvent = processDelta(event.getDelta(), null);
			if (mpEvent != null) {
				fireAsyncEvent(mpEvent);
			}
		}

		private MicroProfilePropertiesChangeEvent processDeltaChildren(IJavaElementDelta delta,
				MicroProfilePropertiesChangeEvent event) {
			for (IJavaElementDelta c : delta.getAffectedChildren()) {
				event = processDelta(c, event);
			}
			return event;
		}

		private MicroProfilePropertiesChangeEvent processDelta(IJavaElementDelta delta,
				MicroProfilePropertiesChangeEvent event) {
			IJavaElement element = delta.getElement();
			switch (element.getElementType()) {
			case IJavaElement.JAVA_MODEL:
				event = processDeltaChildren(delta, event);
				break;
			case IJavaElement.JAVA_PROJECT:
				if (isCreatedOrDeleted(delta) || isClasspathChanged(delta.getFlags())) {
					if (event == null) {
						event = new MicroProfilePropertiesChangeEvent();
						event.setType(MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES);
						event.setProjectURIs(new HashSet<String>());
					}
					IJavaProject project = (IJavaProject) element;
					event.getProjectURIs().add(JDTMicroProfileUtils.getProjectURI(project));
					try {
						JDTMicroProfileProject mpProject = JDTMicroProfileProjectManager.getInstance()
								.getJDTMicroProfileProject(project);
						if (mpProject != null && mpProject.getProjectRuntime() != null) {
							mpProject.getProjectRuntime().clearProjectClassCache();
						}
					} catch (JavaModelException e) {

					}
				}
				break;
			default:
				break;
			}
			return event;
		}

		private boolean isCreatedOrDeleted(IJavaElementDelta delta) {
			int kind = delta.getKind();
			return kind == IJavaElementDelta.ADDED || kind == IJavaElementDelta.REMOVED;
		}

		private boolean isClasspathChanged(int flags) {
			return 0 != (flags & (IJavaElementDelta.F_CLASSPATH_CHANGED | IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED
					| IJavaElementDelta.F_CLOSED | IJavaElementDelta.F_OPENED));
		}

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			switch (event.getType()) {
			case IResourceChangeEvent.POST_CHANGE:
				IResourceDelta resourceDelta = event.getDelta();
				if (resourceDelta != null) {
					try {
						resourceDelta.accept(this);
					} catch (CoreException e) {
						if (LOGGER.isLoggable(Level.SEVERE)) {
							LOGGER.log(Level.SEVERE, "Error while tracking save of Java file", e);
						}
					}
				}
				break;
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
				if (isJavaFile(file) && isFileContentChanged(delta)) {
					// A Java file has been saved
					MicroProfilePropertiesChangeEvent event = new MicroProfilePropertiesChangeEvent();
					event.setType(MicroProfilePropertiesScope.ONLY_SOURCES);
					event.setProjectURIs(new HashSet<String>());
					event.getProjectURIs().add(JDTMicroProfileUtils.getProjectURI(file.getProject()));
					JDTMicroProfileProject mpProject = JDTMicroProfileProjectManager.getInstance()
							.getJDTMicroProfileProject(file);
					if (mpProject != null && mpProject.getProjectRuntime() != null) {
						mpProject.getProjectRuntime().clearProjectClassCache();
					}
					fireAsyncEvent(event);
				} else if (isConfigSource(file) && isFileContentChanged(delta)) {
					MicroProfilePropertiesChangeEvent event = new MicroProfilePropertiesChangeEvent();
					event.setType(MicroProfilePropertiesScope.ONLY_CONFIG_FILES);
					event.setProjectURIs(new HashSet<String>());
					event.getProjectURIs().add(JDTMicroProfileUtils.getProjectURI(file.getProject()));
					fireAsyncEvent(event);
				}
			}
			return false;
		}

		private void fireAsyncEvent(MicroProfilePropertiesChangeEvent event) {
			synchronized (eventLock) {
				// Merge with pending event if one exists
				if (pendingEvent == null) {
					pendingEvent = event;
				} else {
					mergeEvents(pendingEvent, event);
				}

				// Cancel previous timer if it exists
				if (scheduledNotification != null && !scheduledNotification.isDone()) {
					scheduledNotification.cancel(false);
				}

				// Schedule notification after debounce delay
				scheduledNotification = scheduler.schedule(() -> {
					MicroProfilePropertiesChangeEvent eventToFire;
					synchronized (eventLock) {
						eventToFire = pendingEvent;
						pendingEvent = null;
						scheduledNotification = null;
					}

					if (eventToFire != null) {
						notifyListeners(eventToFire);
					}
				}, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
			}
		}

		/**
		 * Merges two events by combining their project URIs and taking the widest
		 * scope. Scope hierarchy: SOURCES_AND_DEPENDENCIES > ONLY_SOURCES >
		 * ONLY_CONFIG_FILES
		 */
		private void mergeEvents(MicroProfilePropertiesChangeEvent target, MicroProfilePropertiesChangeEvent source) {
			// Merge project URIs
			if (source.getProjectURIs() != null) {
				if (target.getProjectURIs() == null) {
					target.setProjectURIs(new HashSet<>());
				}
				target.getProjectURIs().addAll(source.getProjectURIs());
			}

			// Handle event type - take the widest scope
			if (source.getType() == MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES) {
				target.setType(MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES);
			} else if (source.getType() == MicroProfilePropertiesScope.ONLY_SOURCES
					&& target.getType() != MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES) {
				target.setType(MicroProfilePropertiesScope.ONLY_SOURCES);
			}
		}

		/**
		 * Notifies all registered listeners about the properties change event.
		 */
		private void notifyListeners(MicroProfilePropertiesChangeEvent event) {
			for (IMicroProfilePropertiesChangedListener listener : listeners) {
				try {
					listener.propertiesChanged(event);
				} catch (Exception e) {
					if (LOGGER.isLoggable(Level.SEVERE)) {
						LOGGER.log(Level.SEVERE,
								"Error while sending LSP 'microprofile/propertiesChanged' notification", e);
					}
				}
			}
		}

		private boolean isJavaFile(IFile file) {
			return JAVA_FILE_EXTENSION.equals(file.getFileExtension());
		}

		private boolean isConfigSource(IFile file) {
			return JDTMicroProfileProjectManager.getInstance().isConfigSource(file);
		}

		private boolean isFileContentChanged(IResourceDelta delta) {
			return (delta.getKind() == IResourceDelta.CHANGED && (delta.getFlags() & IResourceDelta.CONTENT) != 0);
		}

	}

	private MicroProfileListener microprofileListener;

	private final Set<IMicroProfilePropertiesChangedListener> listeners;

	private ScheduledExecutorService scheduler;

	private MicroProfilePropertiesListenerManager() {
		listeners = new HashSet<>();
	}

	/**
	 * Add the given MicroProfile properties changed listener.
	 *
	 * @param listener the listener to add
	 */
	public void addMicroProfilePropertiesChangedListener(IMicroProfilePropertiesChangedListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * Remove the given MicroProfile properties changed listener.
	 *
	 * @param listener the listener to remove
	 */
	public void removeMicroProfilePropertiesChangedListener(IMicroProfilePropertiesChangedListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * Initialize the classpath listener manager.
	 */
	public synchronized void initialize() {
		if (microprofileListener != null) {
			return;
		}
		this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "MicroProfile-Properties-Debouncer");
			t.setDaemon(true);
			return t;
		});
		this.microprofileListener = new MicroProfileListener();
		JavaCore.addElementChangedListener(microprofileListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(microprofileListener,
				IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * Destroy the classpath listener manager.
	 */
	public synchronized void destroy() {
		if (microprofileListener != null) {
			JavaCore.removeElementChangedListener(microprofileListener);
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(microprofileListener);
			this.microprofileListener = null;
		}
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				scheduler.shutdownNow();
				Thread.currentThread().interrupt();
			}
			scheduler = null;
		}
	}

}