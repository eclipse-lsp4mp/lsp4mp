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
package org.eclipse.lsp4mp.jdt.core.java.inlayhint;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.ASTVisitor;

/**
 * 
 * JDT visitor to process validation and report LSP inlay hints by visiting AST.
 * 
 * To collect inlay hint by visiting AST, you need:
 * 
 * <ul>
 * <li>create class which extends {@link JavaASTInlayHint}</li>
 * <li>register this class with the
 * "org.eclipse.lsp4mp.jdt.core.javaASTInlayHints" extension point:
 * 
 * <code>
 *    <extension point="org.eclipse.lsp4mp.jdt.core.javaASTInlayHints">
      <inlayHint class="..." />
   </extension>

 * </code></li>
 * </ul>
 * 
 * 
 * @author Angelo ZERR
 *
 */
public class JavaASTInlayHint extends ASTVisitor {

	private JavaInlayHintsContext context;

	/**
	 * Initialize the visitor with a given context and inlay hints to update.
	 * 
	 * @param context the context.
	 * @param inlay   hints the inlay hints to update.
	 */
	public void initialize(JavaInlayHintsContext context) {
		this.context = context;
	}

	/**
	 * Returns true if inlay hints must be collected for the given context and false
	 * otherwise.
	 *
	 * <p>
	 * Collection is done by default. Participants can override this to check if
	 * some classes are on the classpath before deciding to process the collection.
	 * </p>
	 *
	 * @param the     java inlay hints context
	 * @param monitor the progress monitor
	 * @return true if inlay hints must be collected for the given context and false
	 *         otherwise.
	 *
	 */
	public boolean isAdaptedForInlayHints(JavaInlayHintsContext context, IProgressMonitor monitor)
			throws CoreException {
		return true;
	}

	public JavaInlayHintsContext getContext() {
		return context;
	}

}
