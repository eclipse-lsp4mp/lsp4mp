package org.eclipse.lsp4mp.settings;

import java.util.Objects;

import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;

public class MicroProfileExecutionSettings {

	private String mode;

	public MicroProfileExecutionSettings() {
		setMode(ExecutionMode.SAFE.name().toLowerCase());
	}

	public ExecutionMode getExecutionMode() {
		return ExecutionMode.forValue(mode);
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * Update the the execution settings with the given new execution settings.
	 *
	 * @param newExecution the new execution settings.
	 */
	public boolean update(MicroProfileExecutionSettings newExecution) {
		if (newExecution == null || Objects.equals(this, newExecution)) {
			return false;
		}
		this.setMode(newExecution.getMode());
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mode);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MicroProfileExecutionSettings other = (MicroProfileExecutionSettings) obj;
		return Objects.equals(mode, other.mode);
	}
	
	

}
