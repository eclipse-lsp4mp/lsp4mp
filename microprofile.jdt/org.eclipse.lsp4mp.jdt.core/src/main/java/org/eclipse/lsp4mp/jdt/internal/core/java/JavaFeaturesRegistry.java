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
package org.eclipse.lsp4mp.jdt.internal.core.java;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4mp.jdt.core.MicroProfileCorePlugin;
import org.eclipse.lsp4mp.jdt.core.jaxrs.IJaxRsInfoProvider;
import org.eclipse.lsp4mp.jdt.internal.core.java.codeaction.JavaCodeActionDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.codelens.JavaCodeLensDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.completion.JavaCompletionDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.definition.JavaDefinitionDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.diagnostics.JavaDiagnosticsDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.hover.JavaHoverDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.inlayhint.JavaInlayHintDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.symbols.JavaWorkspaceSymbolsDefinition;
import org.eclipse.lsp4mp.jdt.internal.jaxrs.java.DefaultJaxRsInfoProvider;

/**
 * Registry to hold the extension point
 * "org.eclipse.lsp4mp.jdt.core.javaFeaturesParticipants".
 *
 */
public class JavaFeaturesRegistry {

	private static final String EXTENSION_JAVA_FEATURE_PARTICIPANTS = "javaFeatureParticipants";
	private static final String CODEACTION_ELT = "codeAction";
	private static final String CODELENS_ELT = "codeLens";
	private static final String INLAY_HINT_ELT = "inlayHint";
	private static final String COMPLETION_ELT = "completion";
	private static final String DEFINITION_ELT = "definition";
	private static final String DIAGNOSTICS_ELT = "diagnostics";
	private static final String HOVER_ELT = "hover";
	private static final String WORKSPACE_SYMBOLS_ELT = "workspaceSymbols";
	private static final String JAXRS_ELT = "jaxrs";

	private static final Logger LOGGER = Logger.getLogger(JavaFeaturesRegistry.class.getName());

	private static final JavaFeaturesRegistry INSTANCE = new JavaFeaturesRegistry();

	private final List<JavaCodeActionDefinition> javaCodeActionDefinitions;

	private final List<JavaCodeLensDefinition> javaCodeLensDefinitions;

	private final List<JavaInlayHintDefinition> javaInlayHintDefinitions;

	private final List<JavaCompletionDefinition> javaCompletionDefinitions;

	private final List<JavaDefinitionDefinition> javaDefinitionDefinitions;

	private final List<JavaDiagnosticsDefinition> javaDiagnosticsDefinitions;

	private final List<JavaHoverDefinition> javaHoverDefinitions;

	private final List<JavaWorkspaceSymbolsDefinition> javaWorkspaceSymbolsDefinitions;

	private final List<IJaxRsInfoProvider> jaxRsInfoProviders;

	private boolean javaFeatureDefinitionsLoaded;

	public static JavaFeaturesRegistry getInstance() {
		return INSTANCE;
	}

	public JavaFeaturesRegistry() {
		javaFeatureDefinitionsLoaded = false;
		javaCodeActionDefinitions = new ArrayList<>();
		javaCodeLensDefinitions = new ArrayList<>();
		javaInlayHintDefinitions = new ArrayList<>();
		javaCompletionDefinitions = new ArrayList<>();
		javaDefinitionDefinitions = new ArrayList<>();
		javaDiagnosticsDefinitions = new ArrayList<>();
		javaHoverDefinitions = new ArrayList<>();
		javaWorkspaceSymbolsDefinitions = new ArrayList<>();
		jaxRsInfoProviders = new ArrayList<>();
	}

	/**
	 * Returns a list of code action definition.
	 *
	 * @return a list of code action definition.
	 */
	public List<JavaCodeActionDefinition> getJavaCodeActionDefinitions(String codeActionKind) {
		loadJavaFeatureDefinitions();
		return javaCodeActionDefinitions.stream().filter(definition -> codeActionKind.startsWith(definition.getKind()))
				.collect(Collectors.toList());
	}

	/**
	 * Returns a list of codeLens definition.
	 *
	 * @return a list of codeLens definition.
	 */
	public List<JavaCodeLensDefinition> getJavaCodeLensDefinitions() {
		loadJavaFeatureDefinitions();
		return javaCodeLensDefinitions;
	}

	/**
	 * Returns a list of inlayHint definition.
	 *
	 * @return a list of inlayHint definition.
	 */
	public List<JavaInlayHintDefinition> getJavaInlayHintDefinitions() {
		loadJavaFeatureDefinitions();
		return javaInlayHintDefinitions;
	}

	/**
	 * Returns a list of completion definition
	 *
	 * @return a list of completion definition
	 */
	public List<JavaCompletionDefinition> getJavaCompletionDefinitions() {
		loadJavaFeatureDefinitions();
		return javaCompletionDefinitions;
	}

	/**
	 * Returns a list of definition definition.
	 *
	 * @return a list of definition definition.
	 */
	public List<JavaDefinitionDefinition> getJavaDefinitionDefinitions() {
		loadJavaFeatureDefinitions();
		return javaDefinitionDefinitions;
	}

	/**
	 * Returns a list of diagnostics definition.
	 *
	 * @return a list of diagnostics definition.
	 */
	public List<JavaDiagnosticsDefinition> getJavaDiagnosticsDefinitions() {
		loadJavaFeatureDefinitions();
		return javaDiagnosticsDefinitions;
	}

	/**
	 * Returns a list of hover definition.
	 *
	 * @return a list of hover definition.
	 */
	public List<JavaHoverDefinition> getJavaHoverDefinitions() {
		loadJavaFeatureDefinitions();
		return javaHoverDefinitions;
	}

	/**
	 * Returns a list of workspace symbols definition.
	 *
	 * @return a list of workspace symbols definition
	 */
	public List<JavaWorkspaceSymbolsDefinition> getJavaWorkspaceSymbolsDefinitions() {
		loadJavaFeatureDefinitions();
		return javaWorkspaceSymbolsDefinitions;
	}

	/**
	 * Returns a list of JAX-RS info providers.
	 *
	 * @return a list of JAX-RS info providers
	 */
	public List<IJaxRsInfoProvider> getJaxRsInfoProviders() {
		loadJavaFeatureDefinitions();
		return jaxRsInfoProviders;
	}

	private synchronized void loadJavaFeatureDefinitions() {
		if (javaFeatureDefinitionsLoaded)
			return;

		// Immediately set the flag, as to ensure that this method is never
		// called twice
		javaFeatureDefinitionsLoaded = true;

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] cf = registry.getConfigurationElementsFor(MicroProfileCorePlugin.PLUGIN_ID,
				EXTENSION_JAVA_FEATURE_PARTICIPANTS);
		addJavaFeatureDefinition(cf);
		jaxRsInfoProviders.add(new DefaultJaxRsInfoProvider());
	}

	private void addJavaFeatureDefinition(IConfigurationElement[] cf) {
		for (IConfigurationElement ce : cf) {
			try {
				createAndAddDefinition(ce);
			} catch (Throwable t) {
				LOGGER.log(Level.SEVERE, "Error while collecting java features extension contributions", t);
			}
		}
	}

	private void createAndAddDefinition(IConfigurationElement ce) throws CoreException {
		switch (ce.getName()) {
		case CODEACTION_ELT: {
			JavaCodeActionDefinition definition = new JavaCodeActionDefinition(ce);
			synchronized (javaCodeActionDefinitions) {
				javaCodeActionDefinitions.add(definition);
			}
			break;
		}
		case CODELENS_ELT: {
			JavaCodeLensDefinition definition = new JavaCodeLensDefinition(ce);
			synchronized (javaCodeLensDefinitions) {
				javaCodeLensDefinitions.add(definition);
			}
			break;
		}
		case INLAY_HINT_ELT: {
			JavaInlayHintDefinition definition = new JavaInlayHintDefinition(ce);
			synchronized (javaInlayHintDefinitions) {
				javaInlayHintDefinitions.add(definition);
			}
			break;
		}
		case COMPLETION_ELT: {
			JavaCompletionDefinition definition = new JavaCompletionDefinition(ce);
			synchronized (javaCompletionDefinitions) {
				javaCompletionDefinitions.add(definition);
			}
			break;
		}
		case DIAGNOSTICS_ELT: {
			JavaDiagnosticsDefinition definition = new JavaDiagnosticsDefinition(ce);
			synchronized (javaDiagnosticsDefinitions) {
				javaDiagnosticsDefinitions.add(definition);
			}
			break;
		}
		case DEFINITION_ELT: {
			JavaDefinitionDefinition definition = new JavaDefinitionDefinition(ce);
			synchronized (javaDefinitionDefinitions) {
				javaDefinitionDefinitions.add(definition);
			}
			break;
		}
		case HOVER_ELT: {
			JavaHoverDefinition definition = new JavaHoverDefinition(ce);
			synchronized (javaHoverDefinitions) {
				javaHoverDefinitions.add(definition);
			}
			break;
		}
		case WORKSPACE_SYMBOLS_ELT: {
			JavaWorkspaceSymbolsDefinition definition = new JavaWorkspaceSymbolsDefinition(ce);
			synchronized (javaWorkspaceSymbolsDefinitions) {
				javaWorkspaceSymbolsDefinitions.add(definition);
			}
			break;
		}
		case JAXRS_ELT: {
			JaxRsInfoDefinition definition = new JaxRsInfoDefinition(ce);
			synchronized (jaxRsInfoProviders) {
				jaxRsInfoProviders.add(definition);
			}
			break;
		}
		default:
		}
	}

}
