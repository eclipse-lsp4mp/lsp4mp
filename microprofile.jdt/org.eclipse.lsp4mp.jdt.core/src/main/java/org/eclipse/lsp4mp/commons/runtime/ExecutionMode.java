package org.eclipse.lsp4mp.commons.runtime;

public enum ExecutionMode {

	SAFE(1, "safe"), //
	FULL(2, "full");

	private final int value;
	private final String name;

	ExecutionMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	/**
	 * Returns the enum corresponding to the integer value. Throws
	 * IllegalArgumentException if value is invalid.
	 */
	public static ExecutionMode forValue(int value) {
		ExecutionMode[] allValues = ExecutionMode.values();
		if (value < 1 || value > allValues.length) {
			throw new IllegalArgumentException("Illegal enum value: " + value);
		}
		return allValues[value - 1];
	}

	/**
	 * Returns the enum corresponding to the string value (name). Defaults to SAFE
	 * if null or unrecognized.
	 */
	public static ExecutionMode forValue(String value) {
		if (value == null)
			return SAFE;
		for (ExecutionMode mode : values()) {
			if (mode.name.equalsIgnoreCase(value)) {
				return mode;
			}
		}
		return SAFE; // default
	}
}
