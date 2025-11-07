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
package org.eclipse.lsp4mp.ls.properties;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesChangeEvent;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.extensions.ExtendedMicroProfileProjectInfo;
import org.eclipse.lsp4mp.ls.api.MicroProfileProjectInfoProvider;

/**
 * MicroProfile project information cache.
 * 
 * @author Angelo ZERR
 *
 */
class MicroProfileProjectInfoCache {

	private static final Logger LOGGER = Logger.getLogger(MicroProfileProjectInfoCache.class.getName());

	private final Map<String /* application.properties URI */, CompletableFuture<MicroProfileProjectInfo>> cache;
	private final Map<String /* project URI */, MicroProfileProjectRuntime> projectRuntimes;

	private final MicroProfileProjectInfoProvider provider;

	public MicroProfileProjectInfoCache(MicroProfileProjectInfoProvider provider) {
		this.provider = provider;
		this.cache = new ConcurrentHashMap<>();
		this.projectRuntimes = new ConcurrentHashMap<>();
	}

	/**
	 * Returns as promise the MicroProfile project information for the given
	 * application.properties URI.
	 * 
	 * @param params the URI of the application.properties.
	 * @return as promise the MicroProfile project information for the given
	 *         application.properties URI.
	 */
	public CompletableFuture<MicroProfileProjectInfo> getProjectInfo(MicroProfileProjectInfoParams params) {
		return getProjectInfoFromCache(params). //
				exceptionally(ex -> {
					LOGGER.log(Level.WARNING, String.format(
							"Error while getting MicroProfileProjectInfo (classpath) for '%s'", params.getUri()), ex);
					return MicroProfileProjectInfo.EMPTY_PROJECT_INFO;
				});
	}

	CompletableFuture<MicroProfileProjectInfo> getProjectInfoFromCache(MicroProfileProjectInfoParams params) {
		// Search future which load project info in cache
		CompletableFuture<MicroProfileProjectInfo> projectInfo = cache.get(params.getUri());
		if (projectInfo == null || projectInfo.isCancelled() || projectInfo.isCompletedExceptionally()) {
			// not found in the cache, load the project info from the JDT LS Extension
			params.setScopes(MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES);
			CompletableFuture<MicroProfileProjectInfo> future = provider.getProjectInfo(params). //
					thenApply(info -> new ExtendedMicroProfileProjectInfo(info, getOrCreateProjectRuntime(info)));
			// cache the future.
			cache.put(params.getUri(), future);
			return future;
		}
		if (!projectInfo.isDone()) {
			return projectInfo;
		}

		ExtendedMicroProfileProjectInfo wrapper = getProjectInfoWrapper(projectInfo);
		if (wrapper.isReloadFromSource()) {
			// There are some java sources changed, get the MicroProfile properties from
			// java
			// sources.
			params.setScopes(MicroProfilePropertiesScope.ONLY_SOURCES);
			return provider.getProjectInfo(params). //
					exceptionally(ex -> {
						LOGGER.log(Level.WARNING, String.format(
								"Error while getting MicroProfileProjectInfo (sources) for '%s'", params.getUri()), ex);
						return MicroProfileProjectInfo.EMPTY_PROJECT_INFO;
					}) //
					.thenApply(info ->
					// then update the cache with the new properties
					{
						wrapper.updateSourcesProperties(info.getProperties(), info.getHints());
						return wrapper;
					});
		}

		// Returns the cached project info
		return projectInfo;
	}

	private MicroProfileProjectRuntime getOrCreateProjectRuntime(MicroProfileProjectInfo info) {
		return projectRuntimes.computeIfAbsent(info.getProjectURI(), f -> new MicroProfileProjectRuntime(
				info.getClasspath() != null ? info.getClasspath() : Collections.emptySet()));
	}

	private static ExtendedMicroProfileProjectInfo getProjectInfoWrapper(
			CompletableFuture<MicroProfileProjectInfo> future) {
		return future != null ? (ExtendedMicroProfileProjectInfo) future.getNow(null) : null;
	}

	public Collection<String> propertiesChanged(MicroProfilePropertiesChangeEvent event) {
		List<MicroProfilePropertiesScope> scopes = event.getType();
		if (MicroProfilePropertiesScope.isOnlyConfigFiles(scopes)) {
			// Some properties config files (ex : microprofile-config.properties) has been
			// saved, ignore this event.
			return Collections.emptyList();
		}
		boolean changedOnlyInSources = MicroProfilePropertiesScope.isOnlySources(scopes);
		if (changedOnlyInSources) {
			// Some Java sources files has been saved, evict the cache for item metadata
			// (properties) computed from Java source files only.
			return javaSourceChanged(event.getProjectURIs());
		}
		// Classpath changed (ex : add, remove maven/gradle dependencies) evict the full
		// cache.
		return classpathChanged(event.getProjectURIs());
	}

	private Collection<String> classpathChanged(Set<String> projectURIs) {
		List<String> applicationPropertiesURIs = getApplicationPropertiesURIs(projectURIs);
		applicationPropertiesURIs.forEach(cache::remove);
		updateProjectRuntimes(projectURIs);
		return applicationPropertiesURIs;
	}

	private Collection<String> javaSourceChanged(Set<String> projectURIs) {
		List<String> applicationPropertiesURIs = getApplicationPropertiesURIs(projectURIs);
		for (String uri : applicationPropertiesURIs) {
			ExtendedMicroProfileProjectInfo info = getProjectInfoWrapper(cache.get(uri));
			if (info != null) {
				info.clearPropertiesFromSource();
			}
		}
		updateProjectRuntimes(projectURIs);
		return applicationPropertiesURIs;
	}

	private void updateProjectRuntimes(Set<String> projectURIs) {
		projectURIs.forEach(projectUri -> {
			MicroProfileProjectRuntime projectRuntime = projectRuntimes.get(projectUri);
			if( projectRuntime  != null) {
				projectRuntime.clearProjectClassCache();
			}
		});
	}

	/**
	 * Returns the application.propeties URIs which belongs to the given project
	 * URIs.
	 * 
	 * @param projectURIs project URIs
	 * 
	 * @return the application.propeties URIs which belongs to the given project
	 *         URIs.
	 */
	private List<String> getApplicationPropertiesURIs(Set<String> projectURIs) {
		return cache.entrySet().stream().filter(entry -> {
			MicroProfileProjectInfo projectInfo = getProjectInfoWrapper(entry.getValue());
			if (projectInfo != null) {
				return projectURIs.contains(projectInfo.getProjectURI());
			}
			return false;
		}).map(Map.Entry::getKey).collect(Collectors.toList());
	}

}
