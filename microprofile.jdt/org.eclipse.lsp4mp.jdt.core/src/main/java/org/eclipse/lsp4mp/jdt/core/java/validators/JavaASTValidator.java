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
package org.eclipse.lsp4mp.jdt.core.java.validators;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.IJavaErrorCode;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.JavaDiagnosticsContext;

/**
 * 
 * JDT visitor to process validation and report LSP diagnostics by visiting AST.
 * 
 * To manage validation by visiting AST, you need:
 * 
 * <ul>
 * <li>create class which extends {@link JavaASTValidator}</li>
 * <li>register this class with the
 * "org.eclipse.lsp4mp.jdt.core.javaASTValidators" extension point:
 * 
 * <code>
 *    <extension point="org.eclipse.lsp4mp.jdt.core.javaASTValidators">
      <!-- Java validation for the MicroProfile @Fallback / @Asynchronous annotations -->
      <validator class=
"org.eclipse.lsp4mp.jdt.internal.faulttolerance.java.MicroProfileFaultToleranceASTValidator" />
   </extension>

 * </code></li>
 * </ul>
 * 
 * 
 * @author Angelo ZERR
 *
 */
public class JavaASTValidator extends ASTVisitor {

	private JavaDiagnosticsContext context;

	/**
	 * Initialize the visitor with a given context and diagnostics to update.
	 * 
	 * @param context     the context.
	 * @param diagnostics the diagnostics to update.
	 */
	public void initialize(JavaDiagnosticsContext context) {
		this.context = context;
	}

	/**
	 * Returns true if diagnostics must be collected for the given context and false
	 * otherwise.
	 *
	 * <p>
	 * Collection is done by default. Participants can override this to check if
	 * some classes are on the classpath before deciding to process the collection.
	 * </p>
	 *
	 * @param the     java diagnostics context
	 * @param monitor the progress monitor
	 * @return true if diagnostics must be collected for the given context and false
	 *         otherwise.
	 *
	 */
	public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context, IProgressMonitor monitor)
			throws CoreException {
		return true;
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
		return context.addDiagnostic(message, source, offset, length, code, severity);
	}

	public Diagnostic addDiagnostic(String message, String source, ASTNode node, String code,
			DiagnosticSeverity severity, int start, int end) {
		return addDiagnostic(message, source, node.getStartPosition() + start, end, code, severity);
	}

	public JavaDiagnosticsContext getContext() {
		return context;
	}

}
