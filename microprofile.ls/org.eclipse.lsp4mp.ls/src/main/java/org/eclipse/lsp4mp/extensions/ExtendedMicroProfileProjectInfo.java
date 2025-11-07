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
package org.eclipse.lsp4mp.extensions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ItemBase;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.metadata.ValueHint;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.model.PropertiesModel;

/**
 * Extended MicroProfile Project Information.
 *
 */
public class ExtendedMicroProfileProjectInfo extends MicroProfileProjectInfo {

	/**
	 * Computed metadata build from dynamic properties and a given hint value.
	 *
	 */
	private static class ComputedItemMetadata extends ItemMetadata {

		/**
		 * Computed metadata constructor
		 *
		 * @param metadata dynamic metadata name (ex : name =
		 *                 '${mp.register.rest.client.class}/mp-rest/url)').
		 * @param itemHint item hint which matches the dynamic metadata (ex : name =
		 *                 '${mp.register.rest.client.class}').
		 * @param value    the item value (ex : value =
		 *                 'org.acme.restclient.CountriesService').
		 */
		public ComputedItemMetadata(ItemMetadata metadata, ItemHint itemHint, ValueHint value) {
			// replace dynamic part from metadata name (ex:
			// '${mp.register.rest.client.class}/mp-rest/url'))
			// with hint value (ex: 'org.acme.restclient.CountriesService') to obtain
			// the new name 'org.acme.restclient.CountriesService/mp-rest/url'
			String name = metadata.getName().replace(itemHint.getName(), value.getValue());
			super.setName(name);
			super.setSource(Boolean.TRUE);
			super.setType(metadata.getType());
			super.setDescription(metadata.getDescription());
			super.setSourceType(value.getSourceType());
		}
	}

	private final transient MicroProfileProjectRuntime projectRuntime;

	private transient boolean reloadFromSource;

	private transient List<ItemMetadata> dynamicProperties;

	private transient final Function<String, ItemHint> getHint = hint -> getHint(hint);

	private transient final List<ItemMetadataProvider> providers;

	private transient boolean updating;

	public ExtendedMicroProfileProjectInfo(MicroProfileProjectInfo delegate) {
		this(delegate, null);
	}

	public ExtendedMicroProfileProjectInfo(MicroProfileProjectInfo delegate,
			MicroProfileProjectRuntime projectRuntime) {
		this.projectRuntime = projectRuntime;
		super.setProjectURI(delegate.getProjectURI());
		// Update hints
		super.setHints(
				new CopyOnWriteArrayList<>(delegate.getHints() != null ? delegate.getHints() : new ArrayList<>()));
		// Get dynamic and static properties from delegate project info
		List<ItemMetadata> staticProperties = delegate.getProperties() != null ? delegate.getProperties()
				: new ArrayList<>();
		List<ItemMetadata> dynamicProperties = computeDynamicProperties(staticProperties);
		staticProperties.removeAll(dynamicProperties);
		expandProperties(staticProperties, dynamicProperties, getHint);

		// Update dynamic and static properties
		this.setDynamicProperties(new CopyOnWriteArrayList<ItemMetadata>(dynamicProperties));
		super.setProperties(new CopyOnWriteArrayList<>(staticProperties));
		this.reloadFromSource = false;

		// Initialize custom item metadata provider
		providers = new ArrayList<>();
		ServiceLoader<ItemMetadataProviderFactory> factories = ServiceLoader.load(ItemMetadataProviderFactory.class);
		for (ItemMetadataProviderFactory factory : factories) {
			providers.add(factory.create(this));
		}
	}

	/**
	 * Clear the cache only for MicroProfile properties coming from java sources.
	 */
	public void clearPropertiesFromSource() {
		setReloadFromSource(true);
	}

	private static List<ItemMetadata> computeDynamicProperties(List<ItemMetadata> properties) {
		return properties.stream().filter(p -> p != null && p.getName().contains("${")).collect(Collectors.toList());
	}

	/**
	 * Update the new MicroProfile properties in the cache coming java sources.
	 *
	 * @param propertiesFromJavaSource properties to add in the cache.
	 * @param hintsFromJavaSource      hints to add in the cache.
	 */
	public synchronized void updateSourcesProperties(List<ItemMetadata> propertiesFromJavaSource,
			List<ItemHint> hintsFromJavaSource) {
		// remove old hints from Java sources
		if (hintsFromJavaSource != null) {
			updateListFromPropertiesSources(getHints(), hintsFromJavaSource);
		}
		// remove old properties from Java sources
		if (propertiesFromJavaSource != null) {
			List<ItemMetadata> staticProperties = propertiesFromJavaSource;
			List<ItemMetadata> dynamicProperties = computeDynamicProperties(staticProperties);
			staticProperties.removeAll(dynamicProperties);

			// expand properties by using new dynamic properties
			expandProperties(staticProperties, dynamicProperties, getHint);
			// expand properties by using old dynamic properties (coming from binary
			// properties)
			expandProperties(staticProperties, getDynamicProperties(), getHint);
			updateListFromPropertiesSources(getProperties(), staticProperties);
			updateListFromPropertiesSources(getDynamicProperties(), dynamicProperties);
		}
		// Update custom properties
		updateCustomProperties(null);
		setReloadFromSource(false);
	}

	private static <T extends ItemBase> void updateListFromPropertiesSources(List<T> allProperties,
			List<T> propertiesFromJavaSources) {
		List<? extends ItemBase> oldPropertiesFromJavaSources = allProperties.stream().filter(h -> {
			return h == null || !h.isBinary();
		}).collect(Collectors.toList());
		allProperties.removeAll(oldPropertiesFromJavaSources);
		// add new properties from Java sources
		allProperties.addAll(propertiesFromJavaSources);
	}

	private static void expandProperties(List<ItemMetadata> allProperties, List<ItemMetadata> dynamicProperties,
			Function<String, ItemHint> getHint) {
		for (ItemMetadata metadata : dynamicProperties) {
			int start = metadata.getName().indexOf("${");
			int end = metadata.getName().indexOf("}", start);
			String hint = metadata.getName().substring(start, end + 1);
			ItemHint itemHint = getHint.apply(hint);
			if (itemHint != null) {
				for (ValueHint value : itemHint.getValues()) {
					allProperties.add(new ComputedItemMetadata(metadata, itemHint, value));
				}
			}
		}
	}

	public boolean isReloadFromSource() {
		return reloadFromSource;
	}

	void setReloadFromSource(boolean reloadFromSource) {
		this.reloadFromSource = reloadFromSource;
	}

	public List<ItemMetadata> getDynamicProperties() {
		return dynamicProperties;
	}

	void setDynamicProperties(List<ItemMetadata> dynamicProperties) {
		this.dynamicProperties = dynamicProperties;
	}

	/**
	 * Update custom properties.
	 * 
	 * @param document the properties file document and null if update comes from
	 *                 changes of Java sources.
	 */
	public void updateCustomProperties(PropertiesModel document) {
		if (updating) {
			return;
		}
		synchUpdateCustomProperties(document);
	}

	public MicroProfileProjectRuntime getProjectRuntime() {
		return projectRuntime;
	}

	private synchronized void synchUpdateCustomProperties(PropertiesModel document) {
		try {
			this.updating = true;
			for (ItemMetadataProvider provider : providers) {
				// update the provider if update comes from:
				// - a Java sources changes (document = null)
				// - a microprofile-config.properties changes (document != null)
				if (document != null || provider.isAvailable()) {
					List<ItemMetadata> oldProperties = provider.getProperties();
					if (oldProperties != null) {
						oldProperties = new ArrayList<>(oldProperties);
					}
					// Check that document is not out of date
					CancelChecker cancelChecker = document != null ? document.getCancelChecker() : null;
					if (cancelChecker != null) {
						cancelChecker.checkCanceled();
					}
					provider.update(document);
					List<ItemMetadata> newProperties = provider.getProperties();
					if (!Objects.deepEquals(oldProperties, newProperties)) {
						if (oldProperties != null) {
							getProperties().removeAll(oldProperties);
						}
						if (newProperties != null) {
							getProperties().addAll(newProperties);
						}
					}
				}
			}
		} finally {
			this.updating = false;
		}
	}

}