/******************************************************************************
 *  EnumTypeConverterValidator
 *  ---------------------------------------------------------------------------
 *  This file is part of the MicroProfile / SmallRye configuration validation
 *  system (safe runtime conversion).
 *
 *  It validates that a given string value corresponds to an existing enum
 *  constant, using the hyphenated convention (e.g. "MY_VALUE" → "my-value").
 *
 *  Original code preserved; only documentation and file header added.
 ******************************************************************************/

package org.eclipse.lsp4mp.commons.runtime.converter.safe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.lsp4mp.commons.runtime.DiagnosticsCollector;
import org.eclipse.lsp4mp.commons.runtime.EnumConstantsProvider;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.TypeProvider;
import org.eclipse.lsp4mp.commons.runtime.TypeSignatureParser.EnumType;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;

import io.smallrye.config.common.utils.StringUtil;

/**
 * A {@link ConverterValidator} implementation dedicated to validating that a
 * user-provided string corresponds to a valid enumeration constant.
 * <p>
 * This validator:
 * <ul>
 * <li>Normalizes input values using the hyphenation convention (via
 * {@code StringUtil.skewer()}).</li>
 * <li>Supports dynamic refresh of enum constants through
 * {@link EnumConstantsProvider}.</li>
 * <li>Reports diagnostics when the provided value does not match any known enum
 * constant.</li>
 * </ul>
 *
 * <p>
 * The validation logic is "safe": it does not attempt to load enum classes
 * directly, which allows it to operate in restricted environments (e.g. LSP,
 * sandboxed, or remote execution).
 * </p>
 */
public class EnumTypeConverterValidator implements ConverterValidator {

	private static final String ENUM_ERROR_MESSAGE = "SRCFG00049: Cannot convert %s to enum class %s, allowed values: %s";

	/** The enum type metadata coming from the BNF/TypeSignatureParser. */
	private final EnumType enumType;

	/**
	 * A map of hyphenated enum constants → original enum constant name. Used to
	 * support case-insensitive and style-normalized lookups.
	 */
	private final Map<String, String> values = new HashMap<>();

	/**
	 * Creates a new validator for the given enum type.
	 *
	 * @param enumType the enum type metadata that contains the original enum
	 *                 constant list
	 */
	public EnumTypeConverterValidator(EnumType enumType) {
		this.enumType = enumType;
		updateValues();
	}

	/**
	 * Refreshes the internal enum constant list based on the result of the
	 * {@link EnumConstantsProvider}. This allows environments where the enum
	 * constants are discovered dynamically without loading the actual enum class
	 * (safe execution mode).
	 *
	 * @param enumConstNamesProvider provider that retrieves the list of enum
	 *                               constants
	 * @param typeProvider           unused in this validator, but part of the
	 *                               interface
	 * @param executionMode          execution mode (safe, runtime, etc.)
	 */
	@Override
	public void refreshEnumType(EnumConstantsProvider enumConstNamesProvider, TypeProvider typeProvider,
			ExecutionMode executionMode) {

		List<String> enumConstants = enumConstNamesProvider.getConstants(enumType.getTypeName());

		if (!Objects.equals(enumType.getEnumConstants(), enumConstants)) {
			enumType.setEnumConstNames(enumConstants);
			updateValues();
		}
	}

	/**
	 * Validates that the given string corresponds to a known enum constant.
	 * <p>
	 * Validation is performed on the {@code hyphenated} representation of the
	 * value. For example:
	 * 
	 * <pre>
	 *   "MyValue" → "my-value"
	 * </pre>
	 *
	 * @param value     the string to validate
	 * @param start     the starting offset of the value in the document
	 * @param collector collects validation errors for reporting in the client
	 */
	@Override
	public void validate(String value, int start, DiagnosticsCollector collector) {
		final String trimmedValue = value.trim();
		if (trimmedValue.isEmpty()) {
			return;
		}

		final String hyphenatedValue = hyphenate(trimmedValue);
		final String enumValue = values.get(hyphenatedValue);

		if (enumValue != null) {
			return; // Valid enum
		}

		String errorMessage = String.format(ENUM_ERROR_MESSAGE, value, enumType.getTypeName(),
				String.join(",", values.keySet()));

		collector.collect(errorMessage, "microprofile-config", "value", start, value.length());
	}

	/**
	 * Updates the internal map of hyphenated values to enum names. This is used
	 * after initial creation and after each refresh.
	 */
	private void updateValues() {
		values.clear();
		for (String enumValue : this.enumType.getEnumConstants()) {
			values.put(hyphenate(enumValue), enumValue);
		}
	}

	/**
	 * Hyphenates a value using SmallRye’s {@link StringUtil#skewer(String)}
	 * (original logic preserved exactly as in the source).
	 *
	 * @param value the original enum constant or user value
	 * @return the hyphenated / normalized form
	 */
	private static String hyphenate(String value) {
		return StringUtil.skewer(value);
	}

	/**
	 * This validator always supports validation of enum types.
	 *
	 * @return true
	 */
	@Override
	public boolean canValidate() {
		return true;
	}

	@Override
	public String getConverterClassName() {
		return null;
	}

	@Override
	public String getConverterSimpleClassName() {
		return "EnumConverter";
	}
}
