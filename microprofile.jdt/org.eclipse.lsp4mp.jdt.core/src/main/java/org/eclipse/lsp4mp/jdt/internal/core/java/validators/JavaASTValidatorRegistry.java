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
package org.eclipse.lsp4mp.jdt.internal.core.java.validators;

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
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4mp.jdt.core.java.validators.JavaASTValidator;
import org.eclipse.lsp4mp.jdt.internal.core.java.validators.annotations.AnnotationAttributeRule;
import org.eclipse.lsp4mp.jdt.internal.core.java.validators.annotations.AnnotationRule;
import org.eclipse.lsp4mp.jdt.internal.core.java.validators.annotations.AnnotationRulesJavaASTValidator;
import org.eclipse.lsp4mp.jdt.internal.core.java.validators.annotations.AnnotationValidator;

/**
 * Registry to hold the Extension point
 * "org.eclipse.lsp4mp.jdt.core.javaASTValidators".
 *
 * @author Angelo ZERR
 *
 */
public class JavaASTValidatorRegistry extends AnnotationValidator implements IRegistryChangeListener {

	private static final Logger LOGGER = Logger.getLogger(JavaASTValidatorRegistry.class.getName());

	private static final JavaASTValidatorRegistry INSTANCE = new JavaASTValidatorRegistry();

	private static final String EXTENSION_ID = "javaASTValidators";

	private static final String VALIDATOR_ELT = "validator";

	private static final String CLASS_ATTR = "class";

	private static final String ANNOTATION_VALIDATOR_ELT = "annotationValidator";

	private static final String ANNOTATION_ATTR = "annotation";

	private static final String SOURCE_ATTR = "source";

	private static final String ATTRIBUTE_ELT = "attribute";

	private static final String NAME_ATTR = "name";

	private static final String RANGE_ATTR = "range";

	public static JavaASTValidatorRegistry getInstance() {
		return INSTANCE;
	}

	private boolean extensionProvidersLoaded;
	private boolean registryListenerIntialized;

	private final List<IConfigurationElement> validatorsFromClass;

	private JavaASTValidatorRegistry() {
		super();
		this.extensionProvidersLoaded = false;
		this.registryListenerIntialized = false;
		this.validatorsFromClass = new ArrayList<>();
	}

	public String getExtensionId() {
		return EXTENSION_ID;
	}

	@Override
	public String validate(String value, AnnotationAttributeRule rule) {
		loadExtensionJavaASTValidators();
		return super.validate(value, rule);
	}

	@Override
	public Collection<AnnotationRule> getRules() {
		loadExtensionJavaASTValidators();
		return super.getRules();
	}

	private synchronized void loadExtensionJavaASTValidators() {
		if (extensionProvidersLoaded)
			return;

		// Immediately set the flag, as to ensure that this method is never
		// called twice
		extensionProvidersLoaded = true;

		LOGGER.log(Level.INFO, "->- Loading ." + getExtensionId() + " extension point ->-");

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] cf = registry.getConfigurationElementsFor(MicroProfileCorePlugin.PLUGIN_ID,
				getExtensionId());
		addExtensionJavaASTValidators(cf);
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
						addExtensionJavaASTValidators(cf);
					}
				}
			}
		}
	}

	private void addExtensionJavaASTValidators(IConfigurationElement[] cf) {
		for (IConfigurationElement ce : cf) {
			try {
				if (VALIDATOR_ELT.equals(ce.getName())) {
					// <validator class="" />
					validatorsFromClass.add(ce);
				} else if (ANNOTATION_VALIDATOR_ELT.equals(ce.getName())) {
//					   <extension point="org.eclipse.lsp4mp.jdt.core.javaASTValidators">
//					      <!-- Java validation for the MicroProfile Fault Tolerance annotations -->
//					      <annotationValidator annotation="org.eclipse.microprofile.faulttolerance.CircuitBreaker"
//					                           source="microprofile-faulttolerance" >
//					         <attribute name="delay" range="0" /> <!-- x >=0 -->
//					         <attribute name="requestVolumeThreshold" range="1" /> <!-- x >=1 -->
//					         <attribute name="failureRatio" range="[0,1]" /> <!-- 0 <= x <= 1 -->
//					         <attribute name="successThreshold" range="1" /> <!-- x >=1 -->         
//					      </annotationValidator>
//					   </extension>

					String annotation = ce.getAttribute(ANNOTATION_ATTR);
					String source = ce.getAttribute(SOURCE_ATTR);
					AnnotationRule annotationRule = new AnnotationRule(annotation, source);

					// collect attributes
					for (IConfigurationElement attributeElement : ce.getChildren(ATTRIBUTE_ELT)) {
						String name = attributeElement.getAttribute(NAME_ATTR);
						String range = attributeElement.getAttribute(RANGE_ATTR);

						AnnotationAttributeRule attributeRule = new AnnotationAttributeRule(name);
						attributeRule.setRange(range);
						annotationRule.addRule(attributeRule);
					}

					super.registerRule(annotationRule);

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

	public Collection<ASTVisitor> getValidators(JavaDiagnosticsContext context, IProgressMonitor monitor) {
		List<ASTVisitor> validators = new ArrayList<>();
		addValidator(new AnnotationRulesJavaASTValidator(getRules()), context, monitor, validators);
		for (IConfigurationElement ce : validatorsFromClass) {
			try {
				JavaASTValidator validator = (JavaASTValidator) ce.createExecutableExtension(CLASS_ATTR);
				addValidator(validator, context, monitor, validators);
			} catch (CoreException e) {
				LOGGER.log(Level.SEVERE, "  Error while creating JavaASTValidator " + ce.getAttribute(CLASS_ATTR), e);
			}
		}
		return validators;
	}

	private void addValidator(JavaASTValidator validator, JavaDiagnosticsContext context, IProgressMonitor monitor,
			List<ASTVisitor> validators) {
		validator.initialize(context);
		try {
			if (validator.isAdaptedForDiagnostics(context, monitor)) {
				validators.add(validator);
			}
		} catch (CoreException e) {
			LOGGER.log(Level.SEVERE,
					"  Error while adding validator JavaASTValidator " + validator.getClass().getName(), e);
		}
	}

}