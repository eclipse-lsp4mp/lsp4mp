/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.settings;

import java.util.Objects;

/**
 * MicroProfile code lens settings.
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileCodeLensSettings {

	private boolean urlCodeLensEnabled;

	public boolean isUrlCodeLensEnabled() {
		return urlCodeLensEnabled;
	}

	public void setUrlCodeLensEnabled(boolean urlCodeLensEnabled) {
		this.urlCodeLensEnabled = urlCodeLensEnabled;
	}

	public boolean update(MicroProfileCodeLensSettings newCodeLens) {
		if (newCodeLens == null || Objects.equals(this, newCodeLens)) {
			return false;
		}
		this.setUrlCodeLensEnabled(newCodeLens.isUrlCodeLensEnabled());
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(urlCodeLensEnabled);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MicroProfileCodeLensSettings other = (MicroProfileCodeLensSettings) obj;
		return urlCodeLensEnabled == other.urlCodeLensEnabled;
	}

}
