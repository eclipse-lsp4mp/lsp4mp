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
package org.eclipse.lsp4mp.jdt.internal.core.java.inlayhint;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4mp.jdt.core.java.inlayhint.IJavaInlayHintParticipant;
import org.eclipse.lsp4mp.jdt.core.java.inlayhint.JavaInlayHintsContext;
import org.eclipse.lsp4mp.jdt.core.java.validators.JavaASTValidator;
import org.eclipse.lsp4mp.jdt.internal.core.java.validators.MultiASTVisitor;

/**
 * The java diagnostic participant which visit one time a given AST compilation
 * unit and loops for each {@link JavaASTValidator} registered with
 * "org.eclipse.lsp4mp.jdt.core.javaASTValidators" extension point to report LSP
 * {@link Diagnostic}.
 * 
 * @author Angelo ZERR
 *
 */
public class JavaASTInlayHintsParticipant implements IJavaInlayHintParticipant {

	@Override
	public void collectInlayHints(JavaInlayHintsContext context, IProgressMonitor monitor) throws CoreException {
		// Collect the list of JavaASTValidator which are adapted for the current AST
		// compilation unit to validate.
		Collection<ASTVisitor> validators = JavaASTInlayHintRegistry.getInstance().getInlayHints(context, monitor);
		if (!validators.isEmpty()) {
			// Visit the AST compilation unit and process each validator.
			CompilationUnit ast = context.getASTRoot();
			ast.accept(new MultiASTVisitor(validators));
		}
	}

}
