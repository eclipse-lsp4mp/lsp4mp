package org.eclipse.lsp4mp.commons;

import java.util.Objects;

public class MicroProfileInlayHintTypeSettings {

	private boolean enabled;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public int hashCode() {
		return Objects.hash(enabled);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MicroProfileInlayHintTypeSettings other = (MicroProfileInlayHintTypeSettings) obj;
		return enabled == other.enabled;
	}

}
