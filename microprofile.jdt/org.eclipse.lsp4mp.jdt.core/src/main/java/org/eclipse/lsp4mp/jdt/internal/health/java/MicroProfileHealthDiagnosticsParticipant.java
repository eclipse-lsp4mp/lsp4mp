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
package org.eclipse.lsp4mp.jdt.internal.health.java;

import static org.eclipse.lsp4mp.jdt.internal.health.MicroProfileHealthConstants.HEALTH_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.health.MicroProfileHealthConstants.HEALTH_CHECK_INTERFACE;
import static org.eclipse.lsp4mp.jdt.internal.health.MicroProfileHealthConstants.HEALTH_CHECK_INTERFACE_NAME;
import static org.eclipse.lsp4mp.jdt.internal.health.MicroProfileHealthConstants.LIVENESS_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.health.MicroProfileHealthConstants.READINESS_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.health.MicroProfileHealthConstants.STARTUP_ANNOTATION;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.IJavaDiagnosticsParticipant;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4mp.jdt.core.utils.AnnotationUtils;
import org.eclipse.lsp4mp.jdt.core.utils.IJDTUtils;
import org.eclipse.lsp4mp.jdt.core.utils.JDTTypeUtils;
import org.eclipse.lsp4mp.jdt.core.utils.PositionUtils;
import org.eclipse.lsp4mp.jdt.internal.health.MicroProfileHealthConstants;

/**
 *
 * MicroProfile Health Diagnostics:
 *
 * <ul>
 * <li>Diagnostic 1:display Health annotation diagnostic message if
 * Health/Liveness/Readiness/Startup annotation exists but HealthCheck interface
 * is not implemented</li>
 * <li>Diagnostic 2: display HealthCheck diagnostic message if HealthCheck
 * interface is implemented but Health/Liveness/Readiness/Startup annotation
 * does not exist</li>
 *
 * </ul>
 *
 * <p>
 * Those rules comes from
 * https://github.com/MicroShed/microprofile-language-server/blob/8f3401852d2b82310f49cd41ec043f5b541944a9/src/main/java/com/microprofile/lsp/internal/diagnostic/MicroProfileDiagnostic.java#L250
 * </p>
 *
 * @author Angelo ZERR
 *
 * @See https://github.com/eclipse/microprofile-health
 *
 */
public class MicroProfileHealthDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context, IProgressMonitor monitor)
			throws CoreException {
		// Collection of diagnostics for MicroProfile Health is done only if
		// microprofile-health is on the classpath
		IJavaProject javaProject = context.getJavaProject();
		return JDTTypeUtils.findType(javaProject, HEALTH_CHECK_INTERFACE) != null;
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
		IJDTUtils utils = context.getUtils();
		DocumentFormat documentFormat = context.getDocumentFormat();
		IType[] interfaces = findImplementedInterfaces(classType, monitor);
		boolean implementsHealthCheck = Stream.of(interfaces)
				.anyMatch(interfaceType -> HEALTH_CHECK_INTERFACE_NAME.equals(interfaceType.getElementName()));
		boolean hasOneOfHealthAnnotation = AnnotationUtils.hasAnyAnnotation(classType, LIVENESS_ANNOTATION,
				READINESS_ANNOTATION, STARTUP_ANNOTATION, HEALTH_ANNOTATION);
		// Diagnostic 1:display Health annotation diagnostic message if
		// Health/Liveness/Readiness/Startup annotation exists but HealthCheck interface
		// is not
		// implemented
		if (hasOneOfHealthAnnotation && !implementsHealthCheck) {
			Range healthCheckInterfaceRange = PositionUtils.toNameRange(classType, utils);
			context.addDiagnostic(createDiagnostic1Message(classType, documentFormat), healthCheckInterfaceRange,
					MicroProfileHealthConstants.DIAGNOSTIC_SOURCE, MicroProfileHealthErrorCode.ImplementHealthCheck);
		}

		// Diagnostic 2: display HealthCheck diagnostic message if HealthCheck interface
		// is implemented but Health/Liveness/Readiness annotation does not exist
		if (implementsHealthCheck && !hasOneOfHealthAnnotation) {
			Range healthCheckInterfaceRange = PositionUtils.toNameRange(classType, utils);
			context.addDiagnostic(createDiagnostic2Message(classType, documentFormat), healthCheckInterfaceRange,
					MicroProfileHealthConstants.DIAGNOSTIC_SOURCE, MicroProfileHealthErrorCode.HealthAnnotationMissing);
		}
	}

	private static String createDiagnostic1Message(IType classType, DocumentFormat documentFormat) {
		StringBuilder message = getMessage(classType, documentFormat);
		message.append(" using the ");
		listAvailableAnnotations(classType, message);
		message.append(" annotation should implement the HealthCheck interface.");
		return message.toString();
	}

	private static String createDiagnostic2Message(IType classType, DocumentFormat documentFormat) {
		StringBuilder message = getMessage(classType, documentFormat);
		message.append(" implementing the HealthCheck interface should use the ");
		listAvailableAnnotations(classType, message);
		message.append(" annotation.");
		return message.toString();
	}

	private static StringBuilder getMessage(IType classType, DocumentFormat documentFormat) {
		StringBuilder message = new StringBuilder("The class ");
		String backtick = (documentFormat == DocumentFormat.Markdown) ? "`" : "";
		message.append(backtick).append(classType.getFullyQualifiedName()).append(backtick);
		return message;
	}

	private static void listAvailableAnnotations(IType classType, StringBuilder message) {
		List<String> annotations = new ArrayList<>(4);
		annotations.add("@Liveness");
		annotations.add("@Readiness");
		if (JDTTypeUtils.findType(classType.getJavaProject(), STARTUP_ANNOTATION) != null) {
			annotations.add("@Startup");
		}
		if (JDTTypeUtils.findType(classType.getJavaProject(), HEALTH_ANNOTATION) != null) {
			annotations.add("@Health");
		}
		int size = annotations.size();
		int secondLast = size - 2;
		for (int i = 0; i < size; i++) {
			message.append(annotations.get(i));
			if (i == secondLast) {
				message.append(" or ");
			} else if (i < secondLast) {
				message.append(", ");
			}
		}
	}

	private static IType[] findImplementedInterfaces(IType type, IProgressMonitor progressMonitor)
			throws CoreException {
		ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(progressMonitor);
		return typeHierarchy.getRootInterfaces();
	}
}
