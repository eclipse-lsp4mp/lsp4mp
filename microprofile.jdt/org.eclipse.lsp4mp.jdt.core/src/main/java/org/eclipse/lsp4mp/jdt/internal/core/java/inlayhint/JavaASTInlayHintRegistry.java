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
package org.eclipse.lsp4mp.jdt.internal.core.java.inlayhint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.lsp4mp.jdt.core.MicroProfileCorePlugin;
import org.eclipse.lsp4mp.jdt.core.java.inlayhint.JavaASTInlayHint;
import org.eclipse.lsp4mp.jdt.core.java.inlayhint.JavaInlayHintsContext;

/**
 * Registry to hold the Extension point
 * "org.eclipse.lsp4mp.jdt.core.javaASTInlayHints".
 *
 * @author Angelo ZERR
 *
 */
public class JavaASTInlayHintRegistry implements IRegistryChangeListener {

	private static final Logger LOGGER = Logger.getLogger(JavaASTInlayHintRegistry.class.getName());

	private static final JavaASTInlayHintRegistry INSTANCE = new JavaASTInlayHintRegistry();

	private static final String EXTENSION_ID = "javaASTInlayHints";

	private static final String INLAY_HINT_ELT = "inlayHint";

	private static final String CLASS_ATTR = "class";

	public static JavaASTInlayHintRegistry getInstance() {
		return INSTANCE;
	}

	private boolean extensionProvidersLoaded;
	private boolean registryListenerIntialized;

	private final List<IConfigurationElement> inlayHintsFromClass;

	private JavaASTInlayHintRegistry() {
		super();
		this.extensionProvidersLoaded = false;
		this.registryListenerIntialized = false;
		this.inlayHintsFromClass = new ArrayList<>();
	}

	public String getExtensionId() {
		return EXTENSION_ID;
	}

	private synchronized void loadExtensionJavaASTInlayHints() {
		if (extensionProvidersLoaded)
			return;

		// Immediately set the flag, as to ensure that this method is never
		// called twice
		extensionProvidersLoaded = true;

		LOGGER.log(Level.INFO, "->- Loading ." + getExtensionId() + " extension point ->-");

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] cf = registry.getConfigurationElementsFor(MicroProfileCorePlugin.PLUGIN_ID,
				getExtensionId());
		addExtensionJavaASTInlayHints(cf);
		addRegistryListenerIfNeeded();

		LOGGER.log(Level.INFO, "-<- Done loading ." + getExtensionId() + " extension point -<-");
	}

	@Override
	public void registryChanged(final IRegistryChangeEvent event) {
		IExtensionDelta[] deltas = event.getExtensionDeltas(MicroProfileCorePlugin.PLUGIN_ID, getExtensionId());
		if (deltas != null) {
			synchronized (this) {
				for (IExtensionDelta delta : deltas) {
					IConfigurationElement[] cf = delta.getExtension().getConfigurationElements();
					if (delta.getKind() == IExtensionDelta.ADDED) {
						addExtensionJavaASTInlayHints(cf);
					}
				}
			}
		}
	}

	private void addExtensionJavaASTInlayHints(IConfigurationElement[] cf) {
		for (IConfigurationElement ce : cf) {
			try {
				if (INLAY_HINT_ELT.equals(ce.getName())) {
					// <inlayHint class="" />
					inlayHintsFromClass.add(ce);
				}
				String pluginId = ce.getNamespaceIdentifier();
				LOGGER.log(Level.INFO, "  Loaded " + getExtensionId() + ": " + pluginId);
			} catch (Throwable t) {
				LOGGER.log(Level.SEVERE, "  Loaded while loading " + getExtensionId(), t);
			}
		}
	}

	private void addRegistryListenerIfNeeded() {
		if (registryListenerIntialized)
			return;

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		registry.addRegistryChangeListener(this, MicroProfileCorePlugin.PLUGIN_ID);
		registryListenerIntialized = true;
	}

	public void destroy() {
		Platform.getExtensionRegistry().removeRegistryChangeListener(this);
	}

	public void initialize() {

	}

	public Collection<ASTVisitor> getInlayHints(JavaInlayHintsContext context, IProgressMonitor monitor) {
		loadExtensionJavaASTInlayHints();
		List<ASTVisitor> inlayHints = new ArrayList<>();
		for (IConfigurationElement ce : inlayHintsFromClass) {
			try {
				JavaASTInlayHint inlayHint = (JavaASTInlayHint) ce.createExecutableExtension(CLASS_ATTR);
				addInlayHint(inlayHint, context, monitor, inlayHints);
			} catch (CoreException e) {
				LOGGER.log(Level.SEVERE, "  Error while creating JavaASTInlayHint " + ce.getAttribute(CLASS_ATTR), e);
			}
		}
		return inlayHints;
	}

	private void addInlayHint(JavaASTInlayHint inlayHint, JavaInlayHintsContext context, IProgressMonitor monitor,
			List<ASTVisitor> inlayHints) {
		inlayHint.initialize(context);
		try {
			if (inlayHint.isAdaptedForInlayHints(context, monitor)) {
				inlayHints.add(inlayHint);
			}
		} catch (CoreException e) {
			LOGGER.log(Level.SEVERE,
					"  Error while adding inlayHint JavaASTInlayHint " + inlayHint.getClass().getName(), e);
		}
	}

}