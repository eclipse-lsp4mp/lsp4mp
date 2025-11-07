package org.eclipse.lsp4mp.settings;

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
	public void update(MicroProfileExecutionSettings newExecution) {
		this.setMode(newExecution.getMode());
	}

}
