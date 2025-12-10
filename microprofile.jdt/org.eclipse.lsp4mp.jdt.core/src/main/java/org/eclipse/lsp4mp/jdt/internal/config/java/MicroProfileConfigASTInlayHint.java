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
package org.eclipse.lsp4mp.jdt.internal.config.java;

import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.CONFIG_PROPERTY_ANNOTATION_NAME;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintSettings;
import org.eclipse.lsp4mp.jdt.core.java.inlayhint.JavaASTInlayHint;
import org.eclipse.lsp4mp.jdt.core.java.inlayhint.JavaInlayHintsContext;
import org.eclipse.lsp4mp.jdt.core.utils.AnnotationUtils;
import org.eclipse.lsp4mp.jdt.core.utils.JDTTypeUtils;

/**
 * Show converters and default value as inlay hint in Java file for fields which
 * are annotated with ConfigProperty annotation.
 */
public class MicroProfileConfigASTInlayHint extends JavaASTInlayHint {

	private static final Logger LOGGER = Logger.getLogger(MicroProfileConfigASTInlayHint.class.getName());

	@Override
	public boolean visit(FieldDeclaration fieldDeclaration) {
		@SuppressWarnings("rawtypes")
		List modifiers = fieldDeclaration.modifiers();
		for (Object modifier : modifiers) {
			if (modifier instanceof NormalAnnotation
					&& (AnnotationUtils.isMatchAnnotation((NormalAnnotation) modifier, CONFIG_PROPERTY_ANNOTATION))) {
				ITypeBinding fieldBinding = fieldDeclaration.getType().resolveBinding();
				getContext().addConverterInlayHint(fieldBinding, fieldDeclaration);
			}
		}
		return true;
	}

	@Override
	public boolean visit(NormalAnnotation annotation) {
		if (AnnotationUtils.isMatchAnnotation(annotation, CONFIG_PROPERTY_ANNOTATION)
				&& annotation.getParent() instanceof FieldDeclaration) {
			JavaInlayHintsContext context = getContext();
			MicroProfileJavaInlayHintSettings settings = context.getSettings();
			if (settings.getDefaultValues().isEnabled()) {
				generateDefaultValueInlayHint(annotation, context);
			}
			if (settings.getConverters().isEnabled()) {
				generateConverterInlayHint(annotation, context);
			}
		}
		return true;
	}

	private static void generateConverterInlayHint(NormalAnnotation annotation, JavaInlayHintsContext context) {
		FieldDeclaration fieldDeclaration = (FieldDeclaration) annotation.getParent();
		List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
		if (!fragments.isEmpty()) {
			ITypeBinding fieldBinding = fieldDeclaration.getType().resolveBinding();
			context.addConverterInlayHint(fieldBinding, fragments.get(0));
		}
	}

	private static void generateDefaultValueInlayHint(NormalAnnotation annotation, JavaInlayHintsContext context) {
		try {
			Expression nameExpr = AnnotationUtils.getAnnotationMemberValueExpression(annotation,
					CONFIG_PROPERTY_ANNOTATION_NAME);
			if (nameExpr != null) {
				Expression defaultValueExpr = AnnotationUtils.getAnnotationMemberValueExpression(annotation,
						CONFIG_PROPERTY_ANNOTATION_DEFAULT_VALUE);
				if (defaultValueExpr == null) {
					String propertyKey = JDTTypeUtils.extractStringValue(nameExpr);
					String propertyValue = context.getMicroProfileProject().getProperty(propertyKey);
					if (StringUtils.isNotBlank(propertyValue)) {
						context.addInlayHint(", defaultValue=\"" + propertyValue + "\"",
								nameExpr.getStartPosition() + nameExpr.getLength(), 0);
					}
				}
			}
		} catch (JavaModelException e) {
			LOGGER.log(Level.WARNING,
					"Exception when trying to get defaultValue of a @ConfigProperty annotation while calculating diagnostics for it",
					e);
		}
	}

}
