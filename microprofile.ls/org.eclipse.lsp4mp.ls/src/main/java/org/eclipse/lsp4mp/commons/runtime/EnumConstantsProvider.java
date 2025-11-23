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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides the list of constant names for a given enum type.
 *
 * <p>
 * This functional interface is used by the MicroProfile runtime support to 
 * resolve the allowed values of an enum when validating property values.
 * </p>
 *
 * <p>
 * The behavior depends on the execution mode:
 * </p>
 * <ul>
 *   <li><b>FULL mode</b>: Implementations may attempt to load the enum class 
 *       from the project classpath and return its declared constants.</li>
 *   <li><b>SAFE mode</b>: Implementations cannot access project classes. In 
 *       this case, constants may be resolved from fallback mechanisms 
 *       (e.g., SmallRye Config hosted in the Language Server) or may return 
 *       an empty list if the enum type is unknown.</li>
 * </ul>
 *
 * <p>
 * Returning an empty list never causes a failure by itself: it simply means 
 * the validator cannot offer enum-aware validation for that type.
 * </p>
 */
@FunctionalInterface
public interface EnumConstantsProvider {

	public static class SimpleEnumConstantsProvider implements EnumConstantsProvider {

		private Map<String, List<String>> enums;
		
		@Override
		public List<String> getConstants(String enumType) {
			return enums != null ? enums.get(enumType) : null;
		}
		
		public void addEnumConstants(String enumType, List<String> enumConstNames) {
			if (enums == null) {
				enums = new HashMap<>();
			}
			enums.put(enumType, enumConstNames);
		}		
	}
	
    /**
     * Returns the list of constant names for the given enum type.
     *
     * @param enumType the fully qualified name of the enum class
     * @return the list of enum constant names, or an empty list if the enum cannot be resolved
     */
    List<String> getConstants(String enumType);
}
