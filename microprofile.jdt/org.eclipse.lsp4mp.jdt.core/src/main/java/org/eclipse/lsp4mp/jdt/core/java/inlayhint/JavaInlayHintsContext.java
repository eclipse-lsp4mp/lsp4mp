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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintKind;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintSettings;
import org.eclipse.lsp4mp.commons.runtime.EnumConstantsProvider;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;
import org.eclipse.lsp4mp.jdt.core.java.AbtractJavaContext;
import org.eclipse.lsp4mp.jdt.core.utils.IJDTUtils;

/**
 * Java inlayHint context for a given compilation unit.
 *
 * @author Angelo ZERR
 *
 */
public class JavaInlayHintsContext extends AbtractJavaContext {

	private static final Logger LOGGER = Logger.getLogger(JavaInlayHintsContext.class.getName());

	private final MicroProfileJavaInlayHintParams params;

	private final List<InlayHint> inlayHints;

	private MicroProfileJavaInlayHintSettings settings;

	public JavaInlayHintsContext(String uri, ITypeRoot typeRoot, IJDTUtils utils,
			MicroProfileJavaInlayHintParams params, List<InlayHint> inlayHints) {
		super(uri, typeRoot, utils);
		this.params = params;
		this.inlayHints = inlayHints;
		if (params.getSettings() == null) {
			this.settings = new MicroProfileJavaInlayHintSettings(ExecutionMode.SAFE);
		} else {
			this.settings = params.getSettings();
		}
	}

	public MicroProfileJavaInlayHintSettings getSettings() {
		return settings;
	}

	public MicroProfileJavaInlayHintParams getParams() {
		return params;
	}

	public InlayHint addInlayHint(String label, int offset, int length) {
		try {
			IOpenable openable = getTypeRoot();
			Range range = getUtils().toRange(openable, offset, length);
			InlayHint inlayHint = new InlayHint();
			inlayHint.setLabel(label);
			inlayHint.setKind(InlayHintKind.Type);
			inlayHint.setPosition(range.getStart());
			return addInlayHint(inlayHint);
		} catch (JavaModelException e) {
			LOGGER.log(Level.SEVERE, "Error while creating inlay hint '" + label + "'.", e);
			return null;
		}
	}

	public InlayHint addInlayHint(InlayHint inlayHint) {
		inlayHints.add(inlayHint);
		return inlayHint;
	}

	public void addConverterInlayHint(ITypeBinding fieldBinding, ASTNode node) {
		MicroProfileProjectRuntime projectRuntime = super.getProjectRuntime();
		if (projectRuntime == null) {
			return;
		}
		ExecutionMode preferredMode = getSettings().getMode();
		EnumConstantsProvider.SimpleEnumConstantsProvider provider = new EnumConstantsProvider.SimpleEnumConstantsProvider();
		String fqn = toQualifiedTypeString(fieldBinding, provider);
		ConverterValidator converterValidator = projectRuntime.findConverterValidator(fqn, provider, preferredMode);
		String converter = converterValidator.getConverterSimpleClassName() != null
				? converterValidator.getConverterSimpleClassName()
				: null;
		if (converter != null) {
			addInlayHint(converter + " ", node.getStartPosition(), node.getLength());
		}
	}
}
