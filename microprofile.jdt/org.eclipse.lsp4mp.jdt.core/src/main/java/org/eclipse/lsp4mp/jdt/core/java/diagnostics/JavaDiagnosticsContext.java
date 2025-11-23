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
package org.eclipse.lsp4mp.jdt.core.java.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;
import org.eclipse.lsp4mp.commons.runtime.EnumConstantsProvider;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.jdt.core.java.AbtractJavaContext;
import org.eclipse.lsp4mp.jdt.core.project.JDTMicroProfileProject;
import org.eclipse.lsp4mp.jdt.core.project.JDTMicroProfileProjectManager;
import org.eclipse.lsp4mp.jdt.core.utils.IJDTUtils;
import org.eclipse.lsp4mp.jdt.internal.config.java.MicroProfileConfigErrorCode;

/**
 * Java diagnostics context for a given compilation unit.
 *
 * @author Angelo ZERR
 *
 */
public class JavaDiagnosticsContext extends AbtractJavaContext {

	private static final Logger LOGGER = Logger.getLogger(JavaDiagnosticsContext.class.getName());

	private final List<Diagnostic> diagnostics;

	private final DocumentFormat documentFormat;

	private final MicroProfileJavaDiagnosticsSettings settings;

	private final MicroProfileProjectRuntime projectRuntime;

	public JavaDiagnosticsContext(String uri, ITypeRoot typeRoot, IJDTUtils utils, DocumentFormat documentFormat,
			MicroProfileJavaDiagnosticsSettings settings) {
		super(uri, typeRoot, utils);
		this.diagnostics = new ArrayList<>();
		this.documentFormat = documentFormat;
		if (settings == null) {
			this.settings = new MicroProfileJavaDiagnosticsSettings(Collections.emptyList(), DiagnosticSeverity.Error,
					ExecutionMode.SAFE);
		} else {
			this.settings = settings;
		}
		this.projectRuntime = getProjectRuntime();
	}

	private MicroProfileProjectRuntime getProjectRuntime() {
		try {
			JDTMicroProfileProject mpProject = JDTMicroProfileProjectManager.getInstance()
					.getJDTMicroProfileProject(getJavaProject());
			return mpProject.getProjectRuntime();
		} catch (Exception e) {
			// Do nothing
			return null;
		}
	}

	public DocumentFormat getDocumentFormat() {
		return documentFormat;
	}

	/**
	 * Returns the MicroProfileJavaDiagnosticsSettings.
	 *
	 * Should not be null.
	 *
	 * @return the MicroProfileJavaDiagnosticsSettings
	 */
	public MicroProfileJavaDiagnosticsSettings getSettings() {
		return this.settings;
	}

	public Diagnostic createDiagnostic(String uri, String message, Range range, String source, IJavaErrorCode code) {
		return createDiagnostic(uri, message, range, source, code, DiagnosticSeverity.Warning);
	}

	public Diagnostic createDiagnostic(String uri, String message, Range range, String source, IJavaErrorCode code,
			DiagnosticSeverity severity) {
		return createDiagnostic(uri, message, range, source, code != null ? code.getCode() : null, severity);
	}

	public Diagnostic createDiagnostic(String uri, String message, Range range, String source, String code,
			DiagnosticSeverity severity) {
		Diagnostic diagnostic = new Diagnostic();
		diagnostic.setSource(source);
		diagnostic.setMessage(message);
		diagnostic.setSeverity(severity);
		diagnostic.setRange(range);
		if (code != null) {
			diagnostic.setCode(code);
		}
		return diagnostic;
	}

	public Diagnostic addDiagnostic(String message, String source, ASTNode node, IJavaErrorCode code,
			DiagnosticSeverity severity) {
		return addDiagnostic(message, source, node.getStartPosition(), node.getLength(), code, severity);
	}

	public Diagnostic addDiagnostic(String message, String source, int offset, int length, IJavaErrorCode code,
			DiagnosticSeverity severity) {
		return addDiagnostic(message, source, offset, length, code != null ? code.getCode() : null, severity);
	}

	public Diagnostic addDiagnostic(String message, String source, int offset, int length, String code,
			DiagnosticSeverity severity) {
		try {
			String fileUri = getUri();
			IOpenable openable = getTypeRoot();
			Range range = getUtils().toRange(openable, offset, length);
			Diagnostic d = createDiagnostic(fileUri, message, range, source, code, severity);
			diagnostics.add(d);
			return d;
		} catch (JavaModelException e) {
			LOGGER.log(Level.SEVERE, "Error while creating diagnostic '" + message + "'.", e);
			return null;
		}
	}

	public Diagnostic addDiagnostic(String message, String source, ASTNode node, String code,
			DiagnosticSeverity severity, int start, int end) {
		return addDiagnostic(message, source, node.getStartPosition() + start, end, code, severity);
	}

	public void validateWithConverter(String defValue, ITypeBinding fieldBinding, Expression defaultValueExpr) {
		DiagnosticSeverity valueSeverity = getSettings().getValidationValueSeverity();
		if (projectRuntime == null || valueSeverity == null) {
			return;
		}
		ExecutionMode preferredMode = getSettings().getMode();

		EnumConstantsProvider.SimpleEnumConstantsProvider provider = new EnumConstantsProvider.SimpleEnumConstantsProvider();
		String fqn = toQualifiedTypeString(fieldBinding, provider);
		projectRuntime.validateValue(defValue, fqn, provider, preferredMode,
				(errorMessage, source, code, start, end) -> {
					addDiagnostic(errorMessage, source, defaultValueExpr,
							MicroProfileConfigErrorCode.DEFAULT_VALUE_IS_WRONG_TYPE.getCode(), valueSeverity, start + 1,
							end);
				});
	}

	private static String toQualifiedTypeString(ITypeBinding binding,
			EnumConstantsProvider.SimpleEnumConstantsProvider provider) {
		if (binding == null) {
			return "";
		}

		// Primitive types
		if (binding.isPrimitive() || (binding.isArray() && binding.getComponentType() != null
				&& binding.getComponentType().isPrimitive())) {
			// ex:
			// - int, char, etc
			// - int[], char[], etc
			return binding.getName();
		}

		// Base qualified name (e.g., java.util.List)
		String baseQualifieldName = binding.getErasure().getBinaryName();
		if (binding.isEnum()) {
			List<String> enumConstNames = new ArrayList<>();
			for (IVariableBinding field : binding.getDeclaredFields()) {
				if (field.isEnumConstant()) {
					enumConstNames.add(field.getName());
				}
			}
			provider.addEnumConstants(baseQualifieldName, enumConstNames);
		}

		StringBuilder sb = new StringBuilder(baseQualifieldName);

		// Generic type arguments
		ITypeBinding[] typeArguments = binding.getTypeArguments();
		if (typeArguments != null && typeArguments.length > 0) {
			sb.append("<");
			for (int i = 0; i < typeArguments.length; i++) {
				if (i > 0)
					sb.append(", ");
				sb.append(toQualifiedTypeString(typeArguments[i], provider));
			}
			sb.append(">");
		}

		return sb.toString();
	}

	public List<Diagnostic> getDiagnostics() {
		return diagnostics;
	}

}
