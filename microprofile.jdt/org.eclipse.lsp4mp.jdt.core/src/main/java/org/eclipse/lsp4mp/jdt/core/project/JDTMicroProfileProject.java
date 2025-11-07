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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.utils.ConfigSourcePropertiesProviderUtils;
import org.eclipse.lsp4mp.commons.utils.IConfigSourcePropertiesProvider;
import org.eclipse.lsp4mp.commons.utils.PropertyValueExpander;
import org.eclipse.lsp4mp.jdt.internal.core.ConfigSourceProviderRegistry;
import org.eclipse.lsp4mp.jdt.internal.core.project.ConfigSourcePropertiesProvider;

/**
 * JDT MicroProfile project wraps a Java project {@link IJavaProject} to store
 * properties coming from META-INF/microprofile-config.properties,
 * META-INF/microprofile-config-dev.properties, application.properties (for
 * Quarkus), etc.
 *
 * @author Angelo ZERR
 *
 */
public class JDTMicroProfileProject {

	private static final Logger LOGGER = Logger.getLogger(JDTMicroProfileProject.class.getName());

	private final IJavaProject javaProject;
	private MicroProfileProjectRuntime projectRuntime;

	private List<IConfigSource> configSources;

	private transient IConfigSourcePropertiesProvider aggregatedPropertiesProvider = null;
	private transient PropertyValueExpander propertyValueExpander = null;

	public JDTMicroProfileProject(IJavaProject javaProject) {
		this.javaProject = javaProject;

	}

	/**
	 * Returns the value of this property or <code>defaultValue</code> if it is not
	 * defined in this project.
	 * 
	 * Expands property expressions when there are no cyclical references between
	 * property values.
	 *
	 * @param propertyKey  the property to get with the profile included, in the
	 *                     format used by microprofile-config.properties
	 * @param defaultValue the value to return if the value for the property is not
	 *                     defined in this project
	 * @return the value of this property or <code>defaultValue</code> if it is not
	 *         defined in this project
	 */
	public String getProperty(String propertyKey, String defaultValue) {

		if (aggregatedPropertiesProvider == null) {
			aggregatedPropertiesProvider = getAggregatedPropertiesProvider();
		}

		String unresolved = aggregatedPropertiesProvider.getValue(propertyKey);
		if (unresolved == null) {
			return defaultValue;
		} else if (unresolved.contains("${")) {
			if (propertyValueExpander == null) {
				propertyValueExpander = new PropertyValueExpander(aggregatedPropertiesProvider);
			}
			String expandedValue = propertyValueExpander.getValue(propertyKey);
			if (expandedValue == null) {
				return defaultValue;
			}
			return expandedValue;
		} else {
			return unresolved;
		}
	}

	/**
	 * Returns the value of this property or null if it is not defined in this
	 * project.
	 * 
	 * Expands property expressions when there are no cyclical references between
	 * property values.
	 *
	 * @param propertyKey the property to get with the profile included, in the
	 *                    format used by microprofile-config.properties
	 * @return the value of this property or null if it is not defined in this
	 *         project
	 */
	public String getProperty(String propertyKey) {
		return getProperty(propertyKey, null);
	}

	/**
	 * Returns the value of this property as an int, or <code>defaultValue</code>
	 * when there is no value or the value cannot be parsed as a String.
	 * 
	 * Expands property expressions when there are no cyclical references between
	 * property values.
	 * 
	 * @param key          the property to get with the profile included, in the
	 *                     format used by microprofile-config.properties
	 * @param defaultValue the value to return if the value for the property is not
	 *                     defined in this project
	 * @return the value of this property as an int, or <code>defaultValue</code>
	 *         when there is no value or the value cannot be parsed as a String
	 */
	public Integer getPropertyAsInteger(String key, Integer defaultValue) {
		String value = getProperty(key, null);
		if (value == null) {
			return defaultValue;
		}
		try {
			int intValue = Integer.parseInt(value);
			return intValue;
		} catch (NumberFormatException nfe) {
			return defaultValue;
		}
	}

	/**
	 * Returns a list of all values for properties and different profiles that are
	 * defined in this project.
	 * 
	 * <p>
	 * This list contains information for the property (ex : greeting.message) and
	 * profile property (ex : %dev.greeting.message).
	 * </p>
	 * 
	 * <p>
	 * When several properties file (ex : microprofile-config.properties,
	 * application.properties, etc) define the same property, it's the file which
	 * have the bigger ordinal (see {@link IConfigSource#getOrdinal()} which is
	 * returned.
	 * </p>
	 * 
	 * <p>
	 * Expands property expressions for each of the values of the key when there are
	 * no cyclical references between property values.
	 * </p>
	 *
	 * @param propertyKey the name of the property to collect the values for
	 * @return a list of all values for properties and different profiles that are
	 *         defined in this project.
	 */
	public List<MicroProfileConfigPropertyInformation> getPropertyInformations(String propertyKey) {
		// Use a map to override property values
		// eg. if application.yaml defines a value for a property it should override the
		// value defined in application.properties
		Map<String, MicroProfileConfigPropertyInformation> propertyToInfoMap = new HashMap<>();
		// Go backwards so that application.properties replaces
		// microprofile-config.properties, etc.
		List<IConfigSource> configSources = getConfigSources();
		for (int i = configSources.size() - 1; i >= 0; i--) {
			IConfigSource configSource = configSources.get(i);
			List<MicroProfileConfigPropertyInformation> propertyInformations = configSource
					.getPropertyInformations(propertyKey);
			if (propertyInformations != null) {
				for (MicroProfileConfigPropertyInformation propertyInformation : propertyInformations) {
					propertyToInfoMap.put(propertyInformation.getPropertyNameWithProfile(), propertyInformation);
				}
			}
		}
		return propertyToInfoMap.values().stream() //
				.sorted((a, b) -> {
					return a.getPropertyNameWithProfile().compareTo(b.getPropertyNameWithProfile());
				}) //
				.map(info -> {
					String resolved = this.getProperty(info.getPropertyNameWithProfile());
					return new MicroProfileConfigPropertyInformation(info.getPropertyNameWithProfile(), resolved,
							info.getSourceConfigFileURI(), info.getConfigFileName());
				}).collect(Collectors.toList());
	}

	/**
	 * Returns the list of config sources.
	 * 
	 * @return the list of config sources.
	 */
	public List<IConfigSource> getConfigSources() {
		if (configSources == null) {
			configSources = loadConfigSources(javaProject);
		}
		return configSources;
	}

	/**
	 * Evict the config sources cache and related cached information as soon as one
	 * of properties, yaml file is saved.
	 */
	public void evictConfigSourcesCache() {
		configSources = null;
		propertyValueExpander = null;
		aggregatedPropertiesProvider = null;
	}

	/**
	 * Load config sources from the given project and sort it by using
	 * {@link IConfigSource#getOrdinal()}
	 * 
	 * @param javaProject the Java project
	 * @return the loaded config sources.
	 */
	private synchronized List<IConfigSource> loadConfigSources(IJavaProject javaProject) {
		if (configSources != null) {
			// Case when there are several Threads which load config sources, the second
			// Thread should not reload the config sources again.
			return configSources;
		}
		List<IConfigSource> configSources = new ArrayList<>();
		try {
			Set<IPath> outputLocationAlreadyProcessed = new HashSet<>();
			for (IClasspathEntry sourceEntry : javaProject.getResolvedClasspath(true)) {
				if (!sourceEntry.isTest() && sourceEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE
						&& sourceEntry.getOutputLocation() != null) {
					IPath output = sourceEntry.getOutputLocation();
					IPath outputLocation = javaProject.getProject().getLocation().append(output.removeFirstSegments(1));
					if (!outputLocationAlreadyProcessed.contains(outputLocation)) {
						outputLocationAlreadyProcessed.add(outputLocation);
						File outputFile = outputLocation.toFile();
						if (outputFile.exists() && outputFile.isDirectory()) {
							for (IConfigSourceProvider provider : ConfigSourceProviderRegistry.getInstance()
									.getProviders()) {
								configSources.addAll(provider.getConfigSources(javaProject, outputFile));
							}
						}
					}
				}
			}
		} catch (JavaModelException e) {
			LOGGER.log(Level.WARNING, "Error while loading config sources", e);
		}
		Collections.sort(configSources, (a, b) -> b.getOrdinal() - a.getOrdinal());
		return configSources;
	}

	/**
	 * Returns true if the given property has a value declared for any profile, and
	 * false otherwise.
	 *
	 * @param property the property to check if there is a value for
	 * @return true if the given property has a value declared for any profile, and
	 *         false otherwise
	 */
	public boolean hasProperty(String property) {
		List<IConfigSource> configSources = getConfigSources();
		for (IConfigSource configSource : configSources) {
			if (configSource.getPropertyInformations(property) != null) {
				return true;
			}
		}
		return false;
	}

	private IConfigSourcePropertiesProvider getAggregatedPropertiesProvider() {
		List<IConfigSource> configSources = getConfigSources();
		if (configSources.size() == 0) {
			// Return an empty IConfigSourcePropertiesProvider
			return new IConfigSourcePropertiesProvider() {

				@Override
				public Set<String> keys() {
					return Collections.emptySet();
				}

				@Override
				public boolean hasKey(String key) {
					return false;
				}

				@Override
				public String getValue(String key) {
					return null;
				}

			};
		}
		IConfigSourcePropertiesProvider provider = new ConfigSourcePropertiesProvider(
				configSources.get(configSources.size() - 1));
		for (int i = configSources.size() - 2; i >= 0; i--) {
			provider = ConfigSourcePropertiesProviderUtils
					.layer(new ConfigSourcePropertiesProvider(configSources.get(i)), provider);
		}
		return provider;
	}

	public MicroProfileProjectRuntime getProjectRuntime() {
		if (projectRuntime == null) {
			try {
				Set<String> classpath = resolveClasspathJars(javaProject);
				projectRuntime = new MicroProfileProjectRuntime(classpath);
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error while loading project runtime", e);
			}
		}
		return projectRuntime;
	}

	private static Set<String> resolveClasspathJars(IJavaProject javaProject) throws JavaModelException {
		Set<String> result = new HashSet<>();

		// We ask JDT for the *resolved* classpath (includes Maven/Gradle dependencies)
		IClasspathEntry[] entries = ((JavaProject) javaProject).getResolvedClasspath();

		for (IClasspathEntry entry : entries) {
			switch (entry.getEntryKind()) {

			case IClasspathEntry.CPE_LIBRARY:
				// Regular JAR dependency or JRE libs
				addPath(entry.getPath(), result);
				break;

			case IClasspathEntry.CPE_VARIABLE:
				// Linked variable (ex: M2_REPO)
				// IPath resolved = JavaCore.getResolvedVariablePath(entry.getPath());
				// addPath(resolved, result);
				break;

			case IClasspathEntry.CPE_SOURCE:
				// We include compiled output folder (target/classes or bin/)
				IPath output = entry.getOutputLocation();
				if (output == null) {
					output = javaProject.getOutputLocation();
				}
				if (output != null) {
					IProject project = javaProject.getProject();
					IPath absoluteOutput = project.getWorkspace().getRoot().getFolder(output).getLocation();
					addPath(absoluteOutput, result);
				}
				break;

			default:
				// ignore project references for now
				break;
			}
		}

		return result;
	}

	private static void addPath(IPath path, Set<String> result) {
		if (path != null) {
			result.add(path.toFile().getAbsolutePath());
		}
	}

}