/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.jdt.internal.core.providers;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.lsp4mp.jdt.core.IProjectLabelProvider;
import org.eclipse.lsp4mp.jdt.core.utils.JDTMicroProfileUtils;;

/**
 * Provides a MicroProfile-specific label to a project if the project is a
 * MicroProfile project
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileProjectLabelProvider implements IProjectLabelProvider {

	public static String MICROPROFILE_LABEL = "microprofile";

	@Override
	public List<String> getProjectLabels(IJavaProject project) throws JavaModelException {
		if (JDTMicroProfileUtils.isMicroProfileProject(project)) {
			return Collections.singletonList(MICROPROFILE_LABEL);
		};
		return Collections.emptyList();
	}
}
