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
package org.eclipse.lsp4mp.jdt.internal.restclient.java;

import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.INJECT_JAKARTA_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileConfigConstants.INJECT_JAVAX_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.restclient.MicroProfileRestClientConstants.REGISTER_REST_CLIENT_ANNOTATION;
import static org.eclipse.lsp4mp.jdt.internal.restclient.MicroProfileRestClientConstants.REST_CLIENT_ANNOTATION;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.IJavaDiagnosticsParticipant;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4mp.jdt.core.utils.AnnotationUtils;
import org.eclipse.lsp4mp.jdt.core.utils.JDTTypeUtils;
import org.eclipse.lsp4mp.jdt.core.utils.PositionUtils;
import org.eclipse.lsp4mp.jdt.internal.restclient.MicroProfileRestClientConstants;
import org.eclipse.lsp4mp.jdt.internal.restclient.MicroProfileRestClientErrorCode;

/**
 *
 * MicroProfile RestClient Diagnostics:
 *
 * <ul>
 * <li>Diagnostic 1: Field on current type has Inject and RestClient annotations
 * but corresponding interface does not have RegisterRestClient annotation</li>
 * <li>Diagnostic 2: Current type is an interface, does not have
 * RegisterRestClient annotation but corresponding fields have Inject and
 * RestClient annotation</li>
 * <li>Diagnostic 3: Field on current type has Inject and not RestClient
 * annotations but corresponding interface has RegisterRestClient annotation
 * </li>
 * <li>Diagnostic 4: Field on current type has RestClient and not Inject
 * annotations but corresponding interface has RegisterRestClient annotation
 * </li>
 * <li>Diagnostic 5: Field on current type has not RestClient and not Inject
 * annotations but corresponding interface has RegisterRestClient
 * annotation</li>
 * </ul>
 *
 * <p>
 * Those rules comes from
 * https://github.com/MicroShed/microprofile-language-server/blob/8f3401852d2b82310f49cd41ec043f5b541944a9/src/main/java/com/microprofile/lsp/internal/diagnostic/MicroProfileDiagnostic.java#L75
 * </p>
 *
 * @author Angelo ZERR
 *
 * @See https://github.com/eclipse/microprofile-rest-client
 *
 */
public class MicroProfileRestClientDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

	@Override
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context, IProgressMonitor monitor)
			throws CoreException {
		// Collection of diagnostics for MicroProfile RestClient is done only if
		// microprofile-rest-client is on the classpath
		IJavaProject javaProject = context.getJavaProject();
		return JDTTypeUtils.findType(javaProject, REST_CLIENT_ANNOTATION) != null;
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
				if (type.isInterface()) {
					validateInterfaceType(type, context, monitor);
				} else {
					validateClassType(type, context, monitor);
				}
				continue;
			}
		}
	}

	private static void validateClassType(IType classType, JavaDiagnosticsContext context, IProgressMonitor monitor)
			throws CoreException {
		for (IJavaElement element : classType.getChildren()) {
			if (monitor.isCanceled()) {
				return;
			}
			if (element.getElementType() == IJavaElement.FIELD) {
				IField field = (IField) element;
				validateField(field, context);
			}
		}
	}

	private static void validateField(IField field, JavaDiagnosticsContext context) throws CoreException {
		DocumentFormat documentFormat = context.getDocumentFormat();
		boolean hasInjectAnnotation = AnnotationUtils.hasAnnotation(field, INJECT_JAVAX_ANNOTATION)
				|| AnnotationUtils.hasAnnotation(field, INJECT_JAKARTA_ANNOTATION);
		boolean hasRestClientAnnotation = AnnotationUtils.hasAnnotation(field, REST_CLIENT_ANNOTATION);
		String fieldTypeName = JDTTypeUtils.getResolvedTypeName(field);
		IType fieldType = JDTTypeUtils.findType(field.getJavaProject(), fieldTypeName);
		boolean hasRegisterRestClient = AnnotationUtils.hasAnnotation(fieldType, REGISTER_REST_CLIENT_ANNOTATION)
				&& fieldType.isInterface();

		if (!hasRegisterRestClient) {
			if (hasInjectAnnotation && hasRestClientAnnotation) {
				// Diagnostic 1: Field on current type has Inject and RestClient annotations but
				// corresponding interface does not have RegisterRestClient annotation
				Range restClientRange = PositionUtils.toNameRange(field, context.getUtils());
				context.addDiagnostic(createDiagnostic1Message(field, fieldTypeName, documentFormat), restClientRange,
						MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE, null);
			}
		} else {
			if (hasInjectAnnotation && !hasRestClientAnnotation) {
				// Diagnostic 3: Field on current type has Inject and not RestClient annotations
				// but corresponding interface has RegisterRestClient annotation
				Range restClientRange = PositionUtils.toNameRange(field, context.getUtils());
				context.addDiagnostic(
						"The Rest Client object should have the @RestClient annotation to be injected as a CDI bean.",
						restClientRange, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
						MicroProfileRestClientErrorCode.RestClientAnnotationMissing);
			} else if (!hasInjectAnnotation && hasRestClientAnnotation) {
				// Diagnostic 4: Field on current type has RestClient and not Inject
				// annotations but corresponding interface has RegisterRestClient annotation
				Range restClientRange = PositionUtils.toNameRange(field, context.getUtils());
				context.addDiagnostic(
						"The Rest Client object should have the @Inject annotation to be injected as a CDI bean.",
						restClientRange, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
						MicroProfileRestClientErrorCode.InjectAnnotationMissing);
			} else if (!hasInjectAnnotation && !hasRestClientAnnotation) {
				// Diagnostic 5: Field on current type has not RestClient and not Inject
				// annotations
				// but corresponding interface has RegisterRestClient annotation
				Range restClientRange = PositionUtils.toNameRange(field, context.getUtils());
				context.addDiagnostic(
						"The Rest Client object should have the @Inject and @RestClient annotations to be injected as a CDI bean.",
						restClientRange, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
						MicroProfileRestClientErrorCode.InjectAndRestClientAnnotationMissing);
			}
		}
	}

	private static String createDiagnostic1Message(IField field, String fieldTypeName, DocumentFormat documentFormat) {
		StringBuilder message = new StringBuilder("The corresponding ");
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(fieldTypeName);
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(" interface does not have the @RegisterRestClient annotation. The field ");
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(field.getElementName());
		if (DocumentFormat.Markdown.equals(documentFormat)) {
			message.append("`");
		}
		message.append(" will not be injected as a CDI bean.");
		return message.toString();
	}

	private static void validateInterfaceType(IType interfaceType, JavaDiagnosticsContext context,
			IProgressMonitor monitor) throws CoreException {
		boolean hasRegisterRestClient = AnnotationUtils.hasAnnotation(interfaceType, REGISTER_REST_CLIENT_ANNOTATION);
		if (hasRegisterRestClient) {
			return;
		}

		final AtomicInteger nbReferences = new AtomicInteger(0);
		SearchPattern pattern = SearchPattern.createPattern(interfaceType, IJavaSearchConstants.REFERENCES);
		SearchEngine engine = new SearchEngine();
		engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
				createSearchScope(interfaceType.getJavaProject()), new SearchRequestor() {

					@Override
					public void acceptSearchMatch(SearchMatch match) throws CoreException {
						Object o = match.getElement();
						if (o instanceof IField) {
							validateReferenceField((IField) o);
						}
					}

					private void validateReferenceField(IField field) throws CoreException {
						boolean hasInjectAnnotation = AnnotationUtils.hasAnnotation(field, INJECT_JAVAX_ANNOTATION)
								|| AnnotationUtils.hasAnnotation(field, INJECT_JAKARTA_ANNOTATION);
						boolean hasRestClientAnnotation = AnnotationUtils.hasAnnotation(field, REST_CLIENT_ANNOTATION);
						if (hasInjectAnnotation && hasRestClientAnnotation) {
							nbReferences.incrementAndGet();
						}
					}
				}, monitor);

		if (nbReferences.get() > 0) {
			Range restInterfaceRange = PositionUtils.toNameRange(interfaceType, context.getUtils());
			context.addDiagnostic(
					"The interface `" + interfaceType.getElementName()
							+ "` does not have the @RegisterRestClient annotation. The " + nbReferences.get()
							+ " fields references will not be injected as CDI beans.",
					restInterfaceRange, MicroProfileRestClientConstants.DIAGNOSTIC_SOURCE,
					MicroProfileRestClientErrorCode.RegisterRestClientAnnotationMissing);
		}
	}

	private static IJavaSearchScope createSearchScope(IJavaProject javaProject) throws CoreException {
		return SearchEngine.createJavaSearchScope(new IJavaProject[] { javaProject }, IJavaSearchScope.SOURCES);
	}
}
