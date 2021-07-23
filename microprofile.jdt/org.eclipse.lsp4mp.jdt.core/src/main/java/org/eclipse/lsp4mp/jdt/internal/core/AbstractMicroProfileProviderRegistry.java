/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4mp.jdt.core.IPropertiesProvider;
import org.eclipse.lsp4mp.jdt.core.MicroProfileCorePlugin;
import org.eclipse.lsp4mp.jdt.core.project.IConfigSourceProvider;

/**
 * Registry to hold providers for an extension point
 *
 * @param T the interface that the providers implement
 * @author datho7561
 */
public abstract class AbstractMicroProfileProviderRegistry<T> implements IRegistryChangeListener {

	private static final String CLASS_ATTR = "class";

	private static final Logger LOGGER = Logger.getLogger(AbstractMicroProfileProviderRegistry.class.getName());

	private boolean extensionProvidersLoaded;
	private boolean registryListenerIntialized;
	private final List<T> providers;

	public AbstractMicroProfileProviderRegistry() {
		super();
		this.extensionProvidersLoaded = false;
		this.registryListenerIntialized = false;
		this.providers = new ArrayList<>();
	}

	/**
	 * Returns the extension id of the provider extension point
	 *
	 * @return the extension id of the provider extension point
	 */
	public abstract String getProviderExtensionId();

	/**
	 * Returns all the providers.
	 *
	 * @return all the providers.
	 */
	public List<T> getProviders() {
		loadExtensionProviders();
		return providers;
	}

	private synchronized void loadExtensionProviders() {
		if (extensionProvidersLoaded)
			return;

		// Immediately set the flag, as to ensure that this method is never
		// called twice
		extensionProvidersLoaded = true;

		LOGGER.log(Level.INFO, "->- Loading ." + getProviderExtensionId() + " extension point ->-");

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] cf = registry.getConfigurationElementsFor(MicroProfileCorePlugin.PLUGIN_ID,
				getProviderExtensionId());
		addExtensionProviders(cf);
		addRegistryListenerIfNeeded();

		LOGGER.log(Level.INFO, "-<- Done loading ." + getProviderExtensionId() + " extension point -<-");
	}

	@Override
	public void registryChanged(final IRegistryChangeEvent event) {
		IExtensionDelta[] deltas = event.getExtensionDeltas(MicroProfileCorePlugin.PLUGIN_ID,
				getProviderExtensionId());
		if (deltas != null) {
			synchronized (this) {
				for (IExtensionDelta delta : deltas) {
					IConfigurationElement[] cf = delta.getExtension().getConfigurationElements();
					if (delta.getKind() == IExtensionDelta.ADDED) {
						addExtensionProviders(cf);
					} else {
						removeExtensionProviders(cf);
					}
				}
			}
		}
	}

	private void addExtensionProviders(IConfigurationElement[] cf) {
		for (IConfigurationElement ce : cf) {
			try {
				T provider = (T) ce.createExecutableExtension(CLASS_ATTR);
				synchronized (providers) {
					providers.add(provider);
				}
				LOGGER.log(Level.INFO, "  Loaded " + getProviderExtensionId() + ": " + provider.getClass().getName());
			} catch (Throwable t) {
				LOGGER.log(Level.SEVERE, "  Loaded while loading " + getProviderExtensionId(), t);
			}
		}
	}

	private void removeExtensionProviders(IConfigurationElement[] cf) {
		for (IConfigurationElement ce : cf) {
			try {
				IPropertiesProvider provider = (IPropertiesProvider) ce.createExecutableExtension(CLASS_ATTR);
				synchronized (providers) {
					providers.remove(provider);
				}
				LOGGER.log(Level.INFO, "  Unloaded propertiesProviders: " + provider.getClass().getName());
			} catch (Throwable t) {
				LOGGER.log(Level.SEVERE, "  Unloaded while loading  propertiesProviders", t);
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

}