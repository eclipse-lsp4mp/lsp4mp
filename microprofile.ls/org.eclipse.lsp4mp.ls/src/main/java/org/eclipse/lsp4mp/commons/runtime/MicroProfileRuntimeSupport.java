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
package org.eclipse.lsp4mp.commons.runtime;

/**
 * Base class for all runtime support components within a MicroProfile project.
 *
 * <p>
 * Each runtime support has a reference to the project it belongs to, which
 * provides access to the project’s classloader, class cache, and other
 * supports.
 * </p>
 * 
 * @author Angelo ZERR
 */
public interface MicroProfileRuntimeSupport {

	/**
	 * Returns the owning project runtime.
	 *
	 * @return the MicroProfileProjectRuntime instance
	 */
	MicroProfileProjectRuntime getProject();

	ExecutionMode getExecutionMode();

	Class<? extends MicroProfileRuntimeSupport> getClassApi();

	/**
	 * Reset method to clear any cached state in the runtime support.
	 * 
	 * <p>
	 * Concrete implementations must define this method to clear their internal
	 * caches or any temporary state when the project runtime is refreshed or reset.
	 * </p>
	 */
	void reset();

}
