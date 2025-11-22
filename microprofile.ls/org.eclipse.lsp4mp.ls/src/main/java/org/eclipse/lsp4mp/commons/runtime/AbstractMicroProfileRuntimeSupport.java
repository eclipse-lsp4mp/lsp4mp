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
 * Abstract base implementation for all {@link MicroProfileRuntimeSupport}
 * components associated with a MicroProfile project.
 *
 * <p>
 * A runtime support provides project-scoped facilities such as access to shared
 * class loaders, class cache, metadata services, and other runtime capabilities.
 * This base class stores the reference to the owning
 * {@link MicroProfileProjectRuntime} and exposes behaviour common to all
 * MicroProfile runtime integrations.
 * </p>
 *
 * <p>
 * Each runtime support instance operates under a specific {@link ExecutionMode},
 * which controls how much of the project's environment may be used:
 * </p>
 *
 * <ul>
 *   <li><b>{@code SAFE}</b> — the project classpath is <em>not</em> used.
 *       No project JARs or compiled classes are ever loaded. Only built-in,
 *       framework-level analysis may occur (e.g. static metadata inspection,
 *       parsing, configuration model building). This mode guarantees that no
 *       user code is executed.</li>
 *
 *   <li><b>{@code FULL}</b> — the runtime may load and execute classes from the
 *       project’s classpath, including project JARs and compiled sources. This
 *       enables features that require executing or instantiating user code
 *       (e.g. CDI extensions, configuration providers, reflection-driven
 *       metadata discovery). This mode provides full capabilities but must only
 *       be used in trusted environments.</li>
 * </ul>
 *
 * <p>
 * Subclasses should extend this class to implement runtime features specific to
 * a given MicroProfile implementation or environment.
 * </p>
 *
 * @author Angelo ZERR
 */
public abstract class AbstractMicroProfileRuntimeSupport implements MicroProfileRuntimeSupport {

	/** The MicroProfile project runtime that owns this support. */
	private final MicroProfileProjectRuntime project;

	/** The execution mode under which this support operates. */
	private ExecutionMode executionMode;

	/**
	 * Creates a new runtime support instance.
	 *
	 * @param project        the {@link MicroProfileProjectRuntime} to which this
	 *                       support belongs; must not be {@code null}
	 * @param executionMode  the {@link ExecutionMode} ({@code SAFE} or {@code FULL})
	 *                       that determines whether the project classpath may be used;
	 *                       must not be {@code null}
	 */
	public AbstractMicroProfileRuntimeSupport(MicroProfileProjectRuntime project, ExecutionMode executionMode) {
		this.project = project;
		this.executionMode = executionMode;
	}

	/**
	 * Returns the {@link MicroProfileProjectRuntime} that owns this runtime support.
	 *
	 * @return the owning project runtime (never {@code null})
	 */
	public MicroProfileProjectRuntime getProject() {
		return project;
	}

	/**
	 * Returns the execution mode that determines how this runtime support may
	 * interact with the project's environment.
	 *
	 * @return the execution mode (never {@code null})
	 */
	@Override
	public ExecutionMode getExecutionMode() {
		return executionMode;
	}
}
