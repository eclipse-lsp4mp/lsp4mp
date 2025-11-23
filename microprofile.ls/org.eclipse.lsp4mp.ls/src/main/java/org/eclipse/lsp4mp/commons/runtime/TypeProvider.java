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

import java.lang.reflect.Type;

/**
 * Resolves a type from its fully qualified name, optionally using an
 * {@link EnumConstantsProvider} when the target type is an enum.
 *
 * <p>
 * This interface abstracts the mechanism used by the MicroProfile runtime
 * support and configuration validator to resolve Java types referenced in
 * configuration metadata. The resolution strategy depends on the
 * {@link ExecutionMode}:
 * </p>
 *
 * <ul>
 *   <li><b>FULL mode</b>: The implementation may load classes from the
 *       project classpath (for example using the project's class loader).
 *       Custom enums, library enums and other project types can be resolved.</li>
 *
 *   <li><b>SAFE mode</b>: The project classpath is not used. No reflection
 *       is allowed. Implementations must avoid loading project classes.
 *       Types may be resolved only through known built-in types or metadata
 *       provided by the Language Server (e.g. SmallRye Config hosted by the LS).
 *       Custom project types generally cannot be resolved.</li>
 * </ul>
 *
 * <p>
 * When the given type represents an enum and cannot be loaded (e.g. in SAFE
 * mode or if the type is unknown), the provided
 * {@link EnumConstantsProvider} may be consulted as a fallback to retrieve
 * the list of allowed enum constants, enabling partial validation even
 * without runtime class loading.
 * </p>
 *
 * <p>
 * If the type cannot be resolved, implementations may return {@code null}.
 * Validation logic should treat a {@code null} type as "unknown type".
 * </p>
 */
public interface TypeProvider {

    /**
     * Resolves the Java {@link Type} corresponding to the given fully
     * qualified type name.
     *
     * @param type the fully qualified name of the target type
     * @param enumConstNamesProvider provider used to retrieve enum constants
     *        when the type represents an enum but cannot be loaded
     * @param executionMode determines whether project classpath and reflection
     *        are allowed ({@code FULL}) or forbidden ({@code SAFE})
     * @return the resolved {@link Type}, or {@code null} if it cannot be resolved
     */
    Type findType(String type,
                       EnumConstantsProvider enumConstNamesProvider,
                       ExecutionMode executionMode);
}
