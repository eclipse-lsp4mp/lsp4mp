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
package org.eclipse.lsp4mp.jdt.internal.core.java.inlayhint;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.lsp4mp.jdt.core.java.inlayhint.IJavaInlayHintParticipant;
import org.eclipse.lsp4mp.jdt.core.java.inlayhint.JavaInlayHintsContext;
import org.eclipse.lsp4mp.jdt.internal.core.java.AbstractJavaFeatureDefinition;

/**
 * Wrapper class around java participants {@link IJavaInlayHintParticipant}.
 */
public class JavaInlayHintDefinition extends AbstractJavaFeatureDefinition<IJavaInlayHintParticipant>
		implements IJavaInlayHintParticipant {
	private static final Logger LOGGER = Logger.getLogger(JavaInlayHintDefinition.class.getName());

	public JavaInlayHintDefinition(IConfigurationElement element) {
		super(element);
	}

	// -------------- InlayHint

	@Override
	public boolean isAdaptedForInlayHint(JavaInlayHintsContext context, IProgressMonitor monitor) {
		try {
			return getParticipant().isAdaptedForInlayHint(context, monitor);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while calling isAdaptedForInlayHint", e);
			return false;
		}
	}

	@Override
	public void beginInlayHint(JavaInlayHintsContext context, IProgressMonitor monitor) {
		try {
			getParticipant().beginInlayHint(context, monitor);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while calling beginInlayHint", e);
		}
	}

	@Override
	public void collectInlayHints(JavaInlayHintsContext context, IProgressMonitor monitor) {
		try {
			getParticipant().collectInlayHints(context, monitor);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while collecting inlayHint", e);
		}
	}

	@Override
	public void endInlayHint(JavaInlayHintsContext context, IProgressMonitor monitor) {
		try {
			getParticipant().endInlayHint(context, monitor);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while calling endInlayHint", e);
		}
	}

}
