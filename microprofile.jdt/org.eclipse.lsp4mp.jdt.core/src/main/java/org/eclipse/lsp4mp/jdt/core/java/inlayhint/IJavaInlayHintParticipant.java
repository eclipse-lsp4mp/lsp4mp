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
package org.eclipse.lsp4mp.jdt.core.java.inlayhint;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.lsp4j.InlayHint;

/**
 * Java inlay hint participants API.
 *
 * @author Angelo ZERR
 *
 */
public interface IJavaInlayHintParticipant {

	/**
	 * Returns true if inlayHint must be collected for the given context and false
	 * otherwise.
	 *
	 * <p>
	 * Collection is done by default. Participants can override this to check if
	 * some classes are on the classpath before deciding to process the collection.
	 * </p>
	 *
	 * @param the     java inlayHint context
	 * @param monitor the progress monitor
	 * @return true if inlayHint must be collected for the given context and false
	 *         otherwise.
	 *
	 */
	default boolean isAdaptedForInlayHint(JavaInlayHintsContext context, IProgressMonitor monitor) throws CoreException {
		return true;
	}

	/**
	 * Begin inlayHint collection.
	 *
	 * @param context the java inlayHint context
	 * @param monitor the progress monitor
	 *
	 * @throws CoreException
	 */
	default void beginInlayHint(JavaInlayHintsContext context, IProgressMonitor monitor) throws CoreException {

	}

	/**
	 * Collect inlayHint according to the context.
	 *
	 * @param context the java inlayHint context
	 * @param monitor the progress monitor
	 *
	 * @return the inlayHint list and null otherwise.
	 *
	 * @throws CoreException
	 */
	void collectInlayHints(JavaInlayHintsContext context, IProgressMonitor monitor) throws CoreException;

	/**
	 * End inlayHint collection.
	 *
	 * @param context the java inlayHint context
	 * @param monitor the progress monitor
	 *
	 * @throws CoreException
	 */
	default void endInlayHint(JavaInlayHintsContext context, IProgressMonitor monitor) throws CoreException {

	}
}
