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
package org.eclipse.lsp4mp.jdt.internal.config.java;

import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.CONFIG_PROPERTIES_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.CONFIG_PROPERTIES_ANNOTATION_PREFIX;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_NAME;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.DIAGNOSTIC_DATA_NAME;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE;
import static org.eclipse.lsp4mp.jdt.core.utils.AnnotationUtils.getAnnotationMemberValueExpression;

import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.commons.utils.AntPathMatcher;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4mp.jdt.core.java.validators.JavaASTValidator;
import org.eclipse.lsp4mp.jdt.core.project.JDTMicroProfileProject;
import org.eclipse.lsp4mp.jdt.core.project.JDTMicroProfileProjectManager;
import org.eclipse.lsp4mp.jdt.core.utils.AnnotationUtils;
import org.eclipse.lsp4mp.jdt.core.utils.JDTTypeUtils;
import org.eclipse.lsp4mp.jdt.internal.config.properties.MicroProfileConfigPropertyProvider;

import com.google.gson.JsonObject;

/**
 * Collects diagnostics related to the <code>@ConfigProperty</code> annotation
 * in a Java file.
 *
 * Produces diagnostics when:
 * <ul>
 * <li>The <code>defaultValue</code> attribute value cannot be represented by
 * the type of the field being annotated</li>
 * <li>The config property defined by the annotation doesn't have a default
 * value and doesn't have a value assigned to it in any properties file</li>
 * </ul>
 *
 */
public class MicroProfileConfigASTValidator extends JavaASTValidator {

	private static final Logger LOGGER = Logger.getLogger(MicroProfileConfigASTValidator.class.getName());

	private static final AntPathMatcher pathMatcher = new AntPathMatcher();

	private static final String EMPTY_LIST_LIKE_WARNING_MESSAGE = "''defaultValue=\"\"'' will behave as if no default value is set, and will not be treated as an empty ''{0}''.";

	private static final String NO_VALUE_ERROR_MESSAGE = "The property ''{0}'' is not assigned a value in any config file, and must be assigned at runtime.";

	private static final String EMPTY_KEY_ERROR_MESSAGE = "The member ''{0}'' can'''t be empty.";

	private List<String> patterns;
	// prefix from @ConfigProperties(prefix="")
	private String currentPrefix;

	@Override
	public void initialize(JavaDiagnosticsContext context) {
		super.initialize(context);
		this.currentPrefix = null;
		this.patterns = getPatternsFromContext(context);
	}

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context, IProgressMonitor monitor)
			throws CoreException {
		IJavaProject javaProject = context.getJavaProject();
		return JDTTypeUtils.findType(javaProject, CONFIG_PROPERTY_ANNOTATION) != null;
	}

	private static List<String> getPatternsFromContext(JavaDiagnosticsContext context) {
		return context.getSettings().getPatterns();
	}

	@Override
	public boolean visit(TypeDeclaration typeDeclaration) {
		// Get prefix from @ConfigProperties(prefix="")
		@SuppressWarnings("rawtypes")
		List modifiers = typeDeclaration.modifiers();
		for (Object modifier : modifiers) {
			if (modifier instanceof NormalAnnotation
					&& (AnnotationUtils.isMatchAnnotation((NormalAnnotation) modifier, CONFIG_PROPERTIES_ANNOTATION)
							// FIXME: this should be handled in quarkus-ls
							|| AnnotationUtils.isMatchAnnotation((NormalAnnotation) modifier,
									"io.quarkus.arc.config.ConfigProperties"))) {
				try {
					Expression prefixExpr = AnnotationUtils.getAnnotationMemberValueExpression(
							(NormalAnnotation) modifier, CONFIG_PROPERTIES_ANNOTATION_PREFIX);
					if (prefixExpr != null) {
						currentPrefix = JDTTypeUtils.extractStringValue(prefixExpr);
					}
				} catch (JavaModelException e) {
					LOGGER.log(Level.WARNING, "Exception when trying to get prefix of a @ConfigProperties annotation",
							e);
				}
			}
		}
		return true;
	}

	@Override
	public void endVisit(TypeDeclaration node) {
		this.currentPrefix = null;
		super.endVisit(node);
	}

	@Override
	public boolean visit(NormalAnnotation annotation) {
		if (AnnotationUtils.isMatchAnnotation(annotation, CONFIG_PROPERTY_ANNOTATION)
				&& annotation.getParent() instanceof FieldDeclaration) {
			try {
				Expression defaultValueExpr = AnnotationUtils.getAnnotationMemberValueExpression(annotation,
						CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
				validatePropertyDefaultValue(annotation, defaultValueExpr);
				validatePropertyHasValue(annotation, defaultValueExpr);
			} catch (JavaModelException e) {
				LOGGER.log(Level.WARNING,
						"Exception when trying to get defaultValue of a @ConfigProperty annotation while calculating diagnostics for it",
						e);
			}
		}
		return false;
	}

	/**
	 * Validate "defaultValue" attribute of <code>@ConfigProperty</code> and
	 * generate diagnostics if "defaultValue" cannot be represented by the given
	 * field type.
	 *
	 * See
	 * https://github.com/eclipse/microprofile-config/blob/master/spec/src/main/asciidoc/converters.asciidoc
	 * for more details on default converters.
	 *
	 * @param annotation       the annotation to validate the defaultValue of
	 * @param defaultValueExpr the default value expression, or null if no default
	 *                         value is defined
	 */
	private void validatePropertyDefaultValue(Annotation annotation, Expression defaultValueExpr) {
		FieldDeclaration fieldDeclaration = (FieldDeclaration) annotation.getParent();
		if (defaultValueExpr != null) {
			String defValue = JDTTypeUtils.extractStringValue(defaultValueExpr);
			ITypeBinding fieldBinding = fieldDeclaration.getType().resolveBinding();
			if (fieldBinding != null && defValue != null) {
				if (isListLike(fieldBinding) && defValue.isEmpty()) {
					String message = MessageFormat.format(EMPTY_LIST_LIKE_WARNING_MESSAGE, fieldBinding.getName());
					super.addDiagnostic(message, MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE, defaultValueExpr,
							MicroProfileConfigErrorCode.EMPTY_LIST_NOT_SUPPORTED, DiagnosticSeverity.Warning);
				}
				getContext().validateWithConverter(defValue, fieldBinding, defaultValueExpr);
			}
		}
	}

	/**
	 * Generates diagnostics if the property that this <code>@ConfigProperty</code>
	 * defines does not have a value assigned to it.
	 *
	 * @param annotation       the ConfigProperty annotation
	 * @param defaultValueExpr the default value expression, or null if no default
	 *                         value is defined
	 */
	private void validatePropertyHasValue(Annotation annotation, Expression defaultValueExpr) {
		try {
			String name = null;
			Expression nameExpression = getAnnotationMemberValueExpression(annotation, CONFIG_PROPERTY_ANNOTATION_NAME);
			boolean hasDefaultValue = defaultValueExpr != null;

			if (nameExpression != null) {
				name = JDTTypeUtils.extractStringValue(nameExpression);
				name = MicroProfileConfigPropertyProvider.getPropertyName(name, currentPrefix);
			}

			if (name != null) {
				if (name.isEmpty()) {
					String message = MessageFormat.format(EMPTY_KEY_ERROR_MESSAGE, CONFIG_PROPERTY_ANNOTATION_NAME);
					Diagnostic d = super.addDiagnostic(message, MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE, nameExpression,
							MicroProfileConfigErrorCode.EMPTY_KEY, DiagnosticSeverity.Error);
				} else if (!hasDefaultValue && !doesPropertyHaveValue(name, getContext()) && !isPropertyIgnored(name)) {
					String message = MessageFormat.format(NO_VALUE_ERROR_MESSAGE, name);
					Diagnostic d = super.addDiagnostic(message, MICRO_PROFILE_CONFIG_DIAGNOSTIC_SOURCE, nameExpression,
							MicroProfileConfigErrorCode.NO_VALUE_ASSIGNED_TO_PROPERTY, DiagnosticSeverity.Warning);
					setDataForUnassigned(name, d);
				}
			}
		} catch (JavaModelException e) {
			LOGGER.log(Level.WARNING,
					"Exception while calculating diagnostics for @ConfigProperty (property has value)", e);
		}
	}

	private boolean isPropertyIgnored(String propertyName) {
		for (String pattern : patterns) {
			if (pathMatcher.match(pattern, propertyName)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isListLike(ITypeBinding type) {
		if (type.isArray()) {
			return true;
		}
		String fqn = Signature.getTypeErasure(type.getQualifiedName());
		return "java.util.List".equals(fqn) || "java.util.Set".equals(fqn);
	}

	private static boolean doesPropertyHaveValue(String property, JavaDiagnosticsContext context) {
		IJavaProject javaProject = context.getJavaProject();
		try {
			JDTMicroProfileProject mpProject = JDTMicroProfileProjectManager.getInstance()
					.getJDTMicroProfileProject(javaProject);
			return mpProject.hasProperty(property);
		} catch (JavaModelException e) {
			LOGGER.log(Level.SEVERE, "@ConfigProperty validation attempted in a non MicroProfile project", e);
			return false;
		}
	}

	public static void setDataForUnassigned(String name, Diagnostic diagnostic) {
		JsonObject data = new JsonObject();
		data.addProperty(DIAGNOSTIC_DATA_NAME, name);
		diagnostic.setData(data);
	}
}
