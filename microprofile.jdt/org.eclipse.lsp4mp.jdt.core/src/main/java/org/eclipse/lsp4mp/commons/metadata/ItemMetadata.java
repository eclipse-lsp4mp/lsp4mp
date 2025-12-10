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
package org.eclipse.lsp4mp.commons.metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration item metadata.
 *
 * @author Angelo ZERR
 *
 * @see https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-configuration-metadata.html
 */
public class ItemMetadata extends ItemBase {

	private static final String JAVA_UTIL_OPTIONAL_PREFIX = "java.util.Optional<";

	/**
	 * Values are read and available for usage at build time.
	 */
	public static final int CONFIG_PHASE_BUILD_TIME = 1;
	/**
	 * Values are read and available for usage at build time, and available on a
	 * read-only basis at run time.
	 */
	public static final int CONFIG_PHASE_BUILD_AND_RUN_TIME_FIXED = 2;
	/**
	 * Values are read and available for usage at run time and are re-read on each
	 * program execution. These values are used to configure ConfigSourceProvider
	 * implementations
	 */
	public static final int CONFIG_PHASE_BOOTSTRAP = 3;
	/**
	 * Values are read and available for usage at run time and are re-read on each
	 * program execution.
	 */
	public static final int CONFIG_PHASE_RUN_TIME = 4;

	private String type;

	// Cache for getSimpleType() to avoid recomputation
	private transient String simpleType;

	private String sourceField;

	private String sourceMethod;

	private String defaultValue;

	private String extensionName;
	private boolean required;
	private int phase;

	private transient List<String> wildcardExpansions;

	private List<ConverterKind> converterKinds;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
		// Invalidate the cache whenever the type changes
		this.simpleType = null;
	}

	public String getSourceField() {
		return sourceField;
	}

	public void setSourceField(String sourceField) {
		this.sourceField = sourceField;
	}

	public String getSourceMethod() {
		return sourceMethod;
	}

	public void setSourceMethod(String sourceMethod) {
		this.sourceMethod = sourceMethod;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getExtensionName() {
		return extensionName;
	}

	public void setExtensionName(String extensionName) {
		this.extensionName = extensionName;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public int getPhase() {
		return phase;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public List<ConverterKind> getConverterKinds() {
		return converterKinds;
	}

	public void setConverterKinds(List<ConverterKind> converterKinds) {
		this.converterKinds = converterKinds;
	}

	/**
	 * Returns all possible expansions of the property name pattern contained in
	 * this metadata.
	 * <p>
	 * Each block delimited by curly braces "{...}" or square brackets "[...]" is
	 * treated as a wildcard. For example, a pattern like
	 * "quarkus.log.category.{*}.level" could produce variants such as:
	 * <ul>
	 * <li>"quarkus.log.category.{*}.level"</li>
	 * <li>"quarkus.log.category.level"</li>
	 * </ul>
	 * This allows matching property names with or without specific segments,
	 * handling multiple wildcards and combinations efficiently.
	 *
	 * @return a list of all expanded property name variants, or {@code null} if not
	 *         computed yet
	 */
	public List<String> getWildcardExpansions() {
		return wildcardExpansions;
	}

	/**
	 * Sets the cached expansions for the property name pattern contained in this
	 * metadata.
	 * <p>
	 * This should be used to store the results of {@link #expandPatterns(String)}
	 * or similar logic that generates all combinations of wildcards in the pattern.
	 * Caching the expansions avoids recomputing them multiple times for the same
	 * metadata.
	 *
	 * @param expansions the list of expanded property name variants to cache
	 */
	public void setWildcardExpansions(List<String> expansions) {
		this.wildcardExpansions = expansions;
	}

	public boolean isStringType() {
		return "java.lang.String".equals(getType()) || //
				"java.util.Optional<java.lang.String>".equals(getType());
	}

	public boolean isBooleanType() {
		return "boolean".equals(getType()) || //
				"java.lang.Boolean".equals(getType()) || //
				"java.util.Optional<java.lang.Boolean>".equals(getType());
	}

	public boolean isIntegerType() {
		return "int".equals(getType()) || //
				"java.lang.Integer".equals(getType()) || //
				"java.util.OptionalInt".equals(getType()) || //
				"java.util.Optional<java.lang.Integer>".equals(getType());
	}

	public boolean isFloatType() {
		return "float".equals(getType()) || //
				"java.lang.Float".equals(getType()) || //
				"java.util.Optional<java.lang.Float>".equals(getType());
	}

	public boolean isLongType() {
		return "long".equals(getType()) || //
				"java.lang.Long".equals(getType()) || //
				"java.util.OptionalLong".equals(getType()) || //
				"java.util.Optional<java.lang.Long>".equals(getType());
	}

	public boolean isDoubleType() {
		return "double".equals(getType()) || //
				"java.lang.Double".equals(getType()) || //
				"java.util.OptionalDouble".equals(getType()) || //
				"java.util.Optional<java.lang.Double>".equals(getType());
	}

	public boolean isShortType() {
		return "short".equals(getType()) || //
				"java.lang.Short".equals(getType()) || //
				"java.util.Optional<java.lang.Short>".equals(getType());
	}

	public boolean isBigDecimalType() {
		return "java.math.BigDecimal".equals(getType()) || //
				"java.util.Optional<java.math.BigDecimal>".equals(getType());
	}

	public boolean isBigIntegerType() {
		return "java.math.BigInteger".equals(getType()) || //
				"java.util.Optional<java.math.BigInteger>".equals(getType());
	}

	public boolean isRegexType() {
		return "java.util.regex.Pattern".equals(getType()) || //
				"java.util.Optional<java.util.regex.Pattern>".equals(getType());
	}

	/**
	 * Returns the paths of the metadata.
	 *
	 * @return the paths of the metadata.
	 */
	public String[] getPaths() {
		String name = getName();
		if (name != null) {
			return name.split("\\.");
		}
		return null;
	}

	/**
	 * Returns a readable representation of the type, including generics. Examples:
	 * - "java.lang.String" -> "String" - "java.util.List<java.lang.String>" ->
	 * "List<String>" - "java.util.Map<java.lang.String,
	 * java.util.List<java.lang.Integer>>" -> "Map<String, List<Integer>>"
	 * 
	 * Uses a recursive parser and caches the result in simpleTypeCache.
	 */
	public String getSimpleType() {
		if (simpleType != null) {
			return simpleType;
		}
		if (type == null) {
			return null;
		}
		simpleType = parseSimpleType(type, new IndexWrapper());
		return simpleType;
	}

	// Helper class to pass the current parsing index by reference
	private static class IndexWrapper {
		int index = 0;
	}

	// Recursive parser for generic types
	private static String parseSimpleType(String input, IndexWrapper idx) {
		StringBuilder raw = new StringBuilder();
		while (idx.index < input.length()) {
			char c = input.charAt(idx.index);
			if (c == '<' || c == ',' || c == '>')
				break;
			raw.append(c);
			idx.index++;
		}

		// Extract simple class name without package
		String typeName = simpleName(raw.toString().trim());

		// If there are generic parameters
		if (idx.index < input.length() && input.charAt(idx.index) == '<') {
			idx.index++; // skip '<'
			List<String> params = new ArrayList<>();
			while (true) {
				params.add(parseSimpleType(input, idx));
				if (idx.index >= input.length())
					break;
				char sep = input.charAt(idx.index);
				idx.index++; // skip ',' or '>'
				if (sep == '>')
					break;
			}

			// Reconstruct the full generic type
			StringBuilder sb = new StringBuilder(typeName + "<");
			for (int i = 0; i < params.size(); i++) {
				if (i > 0)
					sb.append(", ");
				sb.append(params.get(i));
			}
			sb.append(">");
			return sb.toString();
		}

		return typeName;
	}

	// Utility to get the class name without the package
	private static String simpleName(String fullName) {
		int lastDot = fullName.lastIndexOf('.');
		return lastDot != -1 ? fullName.substring(lastDot + 1) : fullName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
		result = prime * result + ((extensionName == null) ? 0 : extensionName.hashCode());
		result = prime * result + phase;
		result = prime * result + (required ? 1231 : 1237);
		result = prime * result + ((sourceField == null) ? 0 : sourceField.hashCode());
		result = prime * result + ((sourceMethod == null) ? 0 : sourceMethod.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemMetadata other = (ItemMetadata) obj;
		if (defaultValue == null) {
			if (other.defaultValue != null)
				return false;
		} else if (!defaultValue.equals(other.defaultValue))
			return false;
		if (extensionName == null) {
			if (other.extensionName != null)
				return false;
		} else if (!extensionName.equals(other.extensionName))
			return false;
		if (phase != other.phase)
			return false;
		if (required != other.required)
			return false;
		if (sourceField == null) {
			if (other.sourceField != null)
				return false;
		} else if (!sourceField.equals(other.sourceField))
			return false;
		if (sourceMethod == null) {
			if (other.sourceMethod != null)
				return false;
		} else if (!sourceMethod.equals(other.sourceMethod))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	/**
	 * Returns the type to use for hint.
	 *
	 * @param type the source type.
	 * @return the type to use for hint.
	 */
	public String getHintType() {
		if (type == null) {
			return null;
		}
		if (type.startsWith(JAVA_UTIL_OPTIONAL_PREFIX)) {
			return type.substring(JAVA_UTIL_OPTIONAL_PREFIX.length(), type.length() - 1);
		}
		return type;
	}

}
