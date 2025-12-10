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
package org.eclipse.lsp4mp.commons;

import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;

/**
 * Inlay hints settings for Java file.
 */
public class MicroProfileJavaInlayHintSettings extends MicroProfileInlayHintSettings {

	private ExecutionMode mode;

	/**
	 * Creates new inlay hints settings.
	 *
	 * @param the execution mode to use to validate property value
	 */
	public MicroProfileJavaInlayHintSettings(ExecutionMode mode) {
		this.mode = mode;
	}

	public ExecutionMode getMode() {
		return mode;
	}
}
