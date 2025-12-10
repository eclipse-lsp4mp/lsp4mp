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

import java.util.Objects;

/**
 * MicroProfile inlay hints settings.
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileInlayHintSettings {

	private static final MicroProfileInlayHintTypeSettings DEFAULT_CONVERTERS;
	private static final MicroProfileInlayHintTypeSettings DEFAULT_DEFAULT_VALUES;
	private static final MicroProfileInlayHintTypeSettings DEFAULT_RESOLVE_EXPRESSIONS;
	private static final MicroProfileInlayHintTypeSettings DEFAULT_TYPES;

	static {
		DEFAULT_CONVERTERS = new MicroProfileInlayHintTypeSettings();
		DEFAULT_CONVERTERS.setEnabled(false);
		DEFAULT_DEFAULT_VALUES = new MicroProfileInlayHintTypeSettings();
		DEFAULT_DEFAULT_VALUES.setEnabled(true);
		DEFAULT_RESOLVE_EXPRESSIONS = new MicroProfileInlayHintTypeSettings();
		DEFAULT_RESOLVE_EXPRESSIONS.setEnabled(true);
		DEFAULT_TYPES = new MicroProfileInlayHintTypeSettings();
		DEFAULT_TYPES.setEnabled(false);
	}

	private boolean enabled;
	private MicroProfileInlayHintTypeSettings converters;
	private MicroProfileInlayHintTypeSettings defaultValues;
	private MicroProfileInlayHintTypeSettings resolveExpressions;
	private MicroProfileInlayHintTypeSettings types;
	private transient boolean updated;

	public MicroProfileInlayHintSettings() {
		setEnabled(true);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public MicroProfileInlayHintTypeSettings getConverters() {
		updateDefault();
		return converters;
	}

	public void setConverters(MicroProfileInlayHintTypeSettings converters) {
		this.converters = converters;
		this.updated = false;
	}

	public MicroProfileInlayHintTypeSettings getDefaultValues() {
		updateDefault();
		return defaultValues;
	}

	public void setDefaultValues(MicroProfileInlayHintTypeSettings defaultValues) {
		this.defaultValues = defaultValues;
		this.updated = false;
	}

	public MicroProfileInlayHintTypeSettings getResolveExpressions() {
		updateDefault();
		return resolveExpressions;
	}

	public void setResolveExpressions(MicroProfileInlayHintTypeSettings resolveExpressions) {
		this.resolveExpressions = resolveExpressions;
		this.updated = false;
	}

	public MicroProfileInlayHintTypeSettings getTypes() {
		updateDefault();
		return types;
	}

	public void setTypes(MicroProfileInlayHintTypeSettings types) {
		this.types = types;
		this.updated = false;
	}

	/**
	 * Update each kind of validation settings with default value if not defined.
	 */
	private void updateDefault() {
		if (updated) {
			return;
		}
		setConverters(converters != null ? converters : DEFAULT_CONVERTERS);
		setDefaultValues(defaultValues != null ? defaultValues : DEFAULT_DEFAULT_VALUES);
		setResolveExpressions(resolveExpressions != null ? resolveExpressions : DEFAULT_RESOLVE_EXPRESSIONS);
		setTypes(types != null ? types : DEFAULT_TYPES);
		updated = true;
	}

	/**
	 * Update the the inlay hint settings with the given new inlay hint settings.
	 *
	 * @param newInlayHint the new inlay hint settings.
	 */
	public boolean update(MicroProfileInlayHintSettings newInlayHint) {
		if (newInlayHint == null || Objects.equals(this, newInlayHint)) {
			return false;
		}
		this.setEnabled(newInlayHint.isEnabled());
		this.setConverters(newInlayHint.getConverters());
		this.setDefaultValues(newInlayHint.getDefaultValues());
		this.setResolveExpressions(newInlayHint.getResolveExpressions());
		this.setTypes(newInlayHint.getTypes());
		return true;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(converters, defaultValues, enabled, resolveExpressions, types);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MicroProfileInlayHintSettings other = (MicroProfileInlayHintSettings) obj;
		return java.util.Objects.equals(converters, other.converters)
				&& java.util.Objects.equals(defaultValues, other.defaultValues) && enabled == other.enabled
				&& java.util.Objects.equals(resolveExpressions, other.resolveExpressions)
				&& java.util.Objects.equals(types, other.types);
	}

}
