/*******************************************************************************
* Copyright (c) 2020 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.jdt.internal.metrics.java;

import static org.eclipse.lsp4mp.jdt.internal.metrics.MicroProfileMetricsConstants.DEPENDENT_JAKARTA_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.metrics.MicroProfileMetricsConstants.DEPENDENT_JAVAX_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.metrics.MicroProfileMetricsConstants.DIAGNOSTIC_SOURCE;
import static org.eclipse.lsp4mp.jdt.internal.metrics.MicroProfileMetricsConstants.GAUGE_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.metrics.MicroProfileMetricsConstants.METRIC_ID;
import static org.eclipse.lsp4mp.jdt.internal.metrics.MicroProfileMetricsConstants.REQUEST_SCOPED_JAKARTA_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.metrics.MicroProfileMetricsConstants.REQUEST_SCOPED_JAVAX_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.metrics.MicroProfileMetricsConstants.SESSION_SCOPED_JAKARTA_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.metrics.MicroProfileMetricsConstants.SESSION_SCOPED_JAVAX_ANNOTATION;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.IJavaDiagnosticsParticipant;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4mp.jdt.core.utils.AnnotationUtils;
import org.eclipse.lsp4mp.jdt.core.utils.JDTTypeUtils;
import org.eclipse.lsp4mp.jdt.core.utils.PositionUtils;

/**
 * 
 * MicroProfile Metrics Diagnostics
 * <ul>
 * <li>Diagnostic 1: display @Gauge annotation diagnostic message if the
 * underlying bean is annotated with @RequestScoped, @SessionScoped
 * or @Dependent. Suggest that @AnnotationScoped is used instead.</li>
 * </ul>
 * 
 * 
 * @author Kathryn Kodama
 * 
 * @See https://github.com/eclipse/microprofile-metrics
 */
public class MicroProfileMetricsDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context, IProgressMonitor monitor)
			throws CoreException {
		// Collection of diagnostics for MicroProfile Metrics is done only if
		// microprofile-metrics is on the classpath
		IJavaProject javaProject = context.getJavaProject();
		return JDTTypeUtils.findType(javaProject, METRIC_ID) != null;
	}

	@Override
	public void collectDiagnostics(JavaDiagnosticsContext context, IProgressMonitor monitor) throws CoreException {
		ITypeRoot typeRoot = context.getTypeRoot();
		IJavaElement[] elements = typeRoot.getChildren();
		collectDiagnostics(elements, context, monitor);
	}

	private static void collectDiagnostics(IJavaElement[] elements, JavaDiagnosticsContext context,
			IProgressMonitor monitor) throws CoreException {
		for (IJavaElement element : elements) {
			if (monitor.isCanceled()) {
				return;
			}
			if (element.getElementType() == IJavaElement.TYPE) {
				IType type = (IType) element;
				if (!type.isInterface()) {
					validateClassType(type, context, monitor);
				}
				continue;
			}
		}
	}

	private static void validateClassType(IType classType, JavaDiagnosticsContext context, IProgressMonitor monitor)
			throws CoreException {
		DocumentFormat documentFormat = context.getDocumentFormat();
		boolean hasInvalidScopeAnnotation = AnnotationUtils.hasAnnotation(classType, REQUEST_SCOPED_JAVAX_ANNOTATION)
				|| AnnotationUtils.hasAnnotation(classType, SESSION_SCOPED_JAVAX_ANNOTATION)
				|| AnnotationUtils.hasAnnotation(classType, DEPENDENT_JAVAX_ANNOTATION)
				|| AnnotationUtils.hasAnnotation(classType, REQUEST_SCOPED_JAKARTA_ANNOTATION)
				|| AnnotationUtils.hasAnnotation(classType, SESSION_SCOPED_JAKARTA_ANNOTATION)
				|| AnnotationUtils.hasAnnotation(classType, DEPENDENT_JAKARTA_ANNOTATION);
		// check for Gauge annotation for Diagnostic 1 only if the class has an invalid
		// scope annotation
		if (hasInvalidScopeAnnotation) {
			for (IJavaElement element : classType.getChildren()) {
				if (monitor.isCanceled()) {
					return;
				}
				if (element.getElementType() == IJavaElement.METHOD) {
					IMethod method = (IMethod) element;
					validateMethod(classType, method, context);
				}
			}
		}
	}

	private static void validateMethod(IType classType, IMethod method, JavaDiagnosticsContext context)
			throws CoreException {
		DocumentFormat documentFormat = context.getDocumentFormat();
		boolean hasGaugeAnnotation = AnnotationUtils.hasAnnotation(method, GAUGE_ANNOTATION);

		// Diagnostic 1: display @Gauge annotation diagnostic message if
		// the underlying bean is annotated with @RequestScoped, @SessionScoped or
		// @Dependent.
		// Suggest that @AnnotationScoped is used instead.</li>
		if (hasGaugeAnnotation) {
			Range cdiBeanRange = PositionUtils.toNameRange(classType, context.getUtils());
			context.addDiagnostic(createDiagnostic1Message(classType, documentFormat), cdiBeanRange, DIAGNOSTIC_SOURCE,
					MicroProfileMetricsErrorCode.ApplicationScopedAnnotationMissing);
		}
	}

	private static String createDiagnostic1Message(IType classType, DocumentFormat documentFormat) {
		StringBuilder message = new StringBuilder("The class ");
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(classType.getFullyQualifiedName());
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(
				" using the @Gauge annotation should use the @ApplicationScoped annotation. The @Gauge annotation does not"
						+ " support multiple instances of the underlying bean to be created.");
		return message.toString();
	}

}