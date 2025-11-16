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
package org.eclipse.lsp4mp.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ConfigurationMetadata;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.metadata.ValueHint;
import org.eclipse.lsp4mp.commons.metadata.ValueProvider;
import org.eclipse.lsp4mp.commons.metadata.ValueProvider.ValueProviderDefaultName;
import org.eclipse.lsp4mp.commons.metadata.ValueProviderParameter;
import org.eclipse.lsp4mp.commons.utils.StringUtils;
import org.eclipse.lsp4mp.ls.commons.SnippetsBuilder;
import org.eclipse.lsp4mp.model.PropertiesModel;
import org.eclipse.lsp4mp.services.properties.QuarkusModel;

/**
 * MicroProfile project information utilities.
 *
 * @author Angelo ZERR
 *
 */
public class PropertiesFileUtils {

	private static final Logger LOGGER = Logger.getLogger(PropertiesFileUtils.class.getName());

	private static final BiConsumer<Integer, StringBuilder> KEY_MAP_MARKDOWN_REPLACE = (i, newName) -> newName
			.append("\\{\\*\\}");

	private static final BiConsumer<Integer, StringBuilder> INDEX_ARRAY_MARKDOWN_REPLACE = (i, newName) -> newName
			.append("[\\*\\]");

	private static final BiConsumer<Integer, StringBuilder> KEY_MAP_COMPLETION_PLACEHOLDER_REPLACE = (i,
			newName) -> SnippetsBuilder.placeholders(i++, "key", newName);

	private static final BiConsumer<Integer, StringBuilder> INDEX_ARRAY_COMPLETION_PLACEHOLDER_REPLACE = (i,
			newName) -> {
		newName.append('[');
		SnippetsBuilder.placeholders(i++, "0", newName);
		newName.append(']');
	};

	/**
	 * Result of formatted property name
	 *
	 */
	public static class FormattedPropertyResult {

		private final String propertyName;

		private final int parameterCount;

		public FormattedPropertyResult(String propertyName, int parameterCount) {
			this.propertyName = propertyName;
			this.parameterCount = parameterCount;
		}

		/**
		 * Returns the formatted property name
		 *
		 * @return the formatted property name
		 */
		public String getPropertyName() {
			return propertyName;
		}

		/**
		 * Returns the mapped and index array parameter count.
		 *
		 * @return the mapped and index array parameter count.
		 */
		public int getParameterCount() {
			return parameterCount;
		}
	}

	/**
	 * Returns the enums values according the property type.
	 *
	 * @param property      the property.
	 * @param configuration the configuration.
	 * @return the enums values according the property type
	 */
	public static Collection<ValueHint> getEnums(ItemMetadata property, ConfigurationMetadata configuration) {
		ItemHint hint = configuration.getHint(property);
		if (hint != null) {
			ItemHint handleAsHint = getHandleAsHint(configuration, hint);
			if (handleAsHint != null) {
				return handleAsHint.getValues();
			}
			return hint.getValues();
		}
		if (property.isBooleanType()) {
			return QuarkusModel.BOOLEAN_ENUMS.getValues();
		}
		return null;
	}

	/**
	 * Returns true if the given <code>value</code> is a valid enumeration for the
	 * given <code>metadata</code> and false otherwise.
	 *
	 * @param property      the property.
	 * @param configuration the configuration.
	 * @param value         the value to check.
	 * @return true if the given <code>value</code> is a valid enumeration for the
	 *         given <code>metadata</code> and false otherwise.
	 */
	public static boolean isValidEnum(ItemMetadata property, ConfigurationMetadata configuration, String value) {
		ItemHint itemHint = configuration.getHint(property);
		if (itemHint == null) {
			return true;
		}
		if (itemHint.getValue(value, property.getConverterKinds()) != null) {
			return true;
		}
		ItemHint handleAsHint = getHandleAsHint(configuration, itemHint);
		if (handleAsHint != null) {
			return handleAsHint.getValue(value, property.getConverterKinds()) != null;
		}
		return false;
	}

	/**
	 * Returns the "handle-as" item hint from the given hint and false otherwise.
	 * 
	 * @param configuration the configuration.
	 * @param hint          the item hint.
	 * @return the "handle-as" item hint from the given hint and false otherwise.
	 */
	public static ItemHint getHandleAsHint(ConfigurationMetadata configuration, ItemHint hint) {
		if (hint == null) {
			return null;
		}
		List<ValueProvider> providers = hint.getProviders();
		if (providers != null && !providers.isEmpty()) {
			ValueProvider provider = providers.get(0);
			if (ValueProviderDefaultName.HANDLE_AS.getName().equals(provider.getName())) {
				ValueProviderParameter parameters = provider.getParameters();
				if (parameters != null) {
					String target = parameters.getTarget();
					if (target != null) {
						ItemHint targetHint = configuration.getHint(target);
						if (targetHint != null) {
							return targetHint;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns the MicroProfile property from the given property name and null
	 * otherwise.
	 *
	 * @param propertyName the property name
	 * @param info         the MicroProfile project information which hosts the
	 *                     MicroProfile properties.
	 * @return the MicroProfile property from the given property name and null
	 *         otherwise.
	 */
	public static ItemMetadata getProperty(String propertyName, MicroProfileProjectInfo info) {
		if (StringUtils.isEmpty(propertyName)) {
			return null;
		}
		Collection<ItemMetadata> properties = info.getProperties();
		for (ItemMetadata property : properties) {
			if (property != null && match(propertyName, property)) {
				return property;
			}
		}
		if (EnvUtils.isWindows && System.getenv(propertyName) != null) {
			// Here we are on Windows OS and the property name is an Environment variable
			// As environment variable on Windows OS doesn't take care of case (ex : PATH,
			// Path, path is the same for Windows OS)
			// we need to search property by ignore the case.
			for (ItemMetadata property : properties) {
				if (EnvUtils.ENVIRONMENT_VARIABLES_ORIGIN.equals(property.getOrigin())) {
					if (propertyName.equalsIgnoreCase(property.getName())) {
						return property;
					}
				}
			}
		}
		return null;
	}

	private static boolean match(String propertyName, ItemMetadata metadata) {
		String rawPattern = metadata.getName();
		if (rawPattern.indexOf('{') != -1 || rawPattern.indexOf('[') != -1) {
			// Example of expansion:
			// Pattern: quarkus.log.category.{*}.level
			// expandPatterns(pattern) -> [
			// "quarkus.log.category.{*}.level",
			// "quarkus.log.category.level"
			// ]
			List<String> patterns = metadata.getWildcardExpansions();
			if (patterns == null) {
				patterns = expandPatterns(rawPattern);
				metadata.setWildcardExpansions(patterns);
			}
			for (String expanded : patterns) {
				if (match(propertyName, expanded)) {
					return true;
				}
			}
		}
		return match(propertyName, rawPattern);
	}

	/**
	 * Returns true if the given property name matches the given pattern and false
	 * otherwise.
	 *
	 * The pattern can be:
	 *
	 * <ul>
	 * <li>a simple pattern: it means that pattern is equals to the property
	 * name</li>
	 * <li>a map pattern: pattern which contains {*}.
	 * </ul>
	 *
	 * @param propertyName the property name
	 * @param pattern      the pattern
	 * @return true if the given property name matches the given pattern and false
	 *         otherwise.
	 */
	private static boolean match(String propertyName, String pattern) {
		int i2 = 0;
		int len = Math.max(propertyName.length(), pattern.length());
		for (int i1 = 0; i1 < len; i1++) {
			char c1 = getCharAt(pattern, i1);
			boolean keyMapOrArrayIndex = false;
			if ('{' == c1 && '*' == getCharAt(pattern, i1 + 1) && '}' == getCharAt(pattern, i1 + 2)) {
				// It's a key map {*}.
				i1 = i1 + 2;
				keyMapOrArrayIndex = true;
			} else if ('[' == c1 && '*' == getCharAt(pattern, i1 + 1) && ']' == getCharAt(pattern, i1 + 2)) {
				// It's an array index [*].
				i1 = i1 + 2;
				keyMapOrArrayIndex = true;
			}

			char c2 = getCharAt(propertyName, i2);
			if (keyMapOrArrayIndex) {
				if (c2 == '\u0000') {
					return false;
				}
				boolean endsWithQuote = (c2 == '"');
				while (c2 != '\u0000') {
					c2 = getCharAt(propertyName, ++i2);
					if (endsWithQuote) {
						if (c2 == '"') {
							i2++;
							break;
						}
					} else if ('.' == c2 && propertyName.charAt(i2 - 1) != '\\'
							&& propertyName.charAt(i2 - 2) != '\\') {
						break;
					}
				}
				keyMapOrArrayIndex = false;
			} else {
				if (c2 != c1) {
					return false;
				}
				i2++;
			}
		}
		return true;
	}

	private static char getCharAt(String text, int index) {
		if (index >= text.length()) {
			return '\u0000';
		}
		return text.charAt(index);
	}

	/**
	 * Generates all possible variants of a pattern containing the wildcards
	 * {@code "{*}"} or {@code "[*]"}.
	 * Each wildcard can be either kept or removed, producing 2^n combinations
	 * for n wildcards. When removing a wildcard, any dot immediately to the left
	 * of it is also removed.
	 *
	 * <p><b>Example:</b></p>
	 * <pre>
	 * Input : {@code foo.{*}.bar.[*].baz}
	 * Output: [
	 *   {@code foo.{*}.bar.[*].baz},
	 *   {@code foo.bar.[*].baz},
	 *   {@code foo.{*}.bar.baz},
	 *   {@code foo.bar.baz}
	 * ]
	 * </pre>
	 *
	 * <p><b>Quarkus example:</b></p>
	 * <pre>
	 * Input : {@code quarkus.log.category.{*}.level}
	 * Output: [
	 *   {@code quarkus.log.category.{*}.level},
	 *   {@code quarkus.log.category.level}
	 * ]
	 * </pre>
	 *
	 * <p>Notes:</p>
	 * <ul>
	 *   <li>Only {@code "{*}"} and {@code "[*]"} are recognized as wildcards.</li>
	 * </ul>
	 */
	private static List<String> expandPatterns(String pattern) {
		List<int[]> wildcards = new ArrayList<>();
		int len = pattern.length();

		// detect all {...} and [...] blocks
		for (int i = 0; i < len; i++) {
			char c = pattern.charAt(i);
			if (c == '{' || c == '[') {
				char close = (c == '{') ? '}' : ']';
				int start = i;
				int end = i + 1;
				// find the closing bracket
				while (end < len && pattern.charAt(end) != close) {
					end++;
				}
				if (end < len && pattern.charAt(end) == close) {
					wildcards.add(new int[] { start, end });
					i = end; // skip processed block
				}
			}
		}

		if (wildcards.isEmpty()) {
			return Collections.singletonList(pattern);
		}

		List<String> result = new ArrayList<>();
		int combos = 1 << wildcards.size(); // 2^n combinations

		// generate all combinations
		for (int mask = 0; mask < combos; mask++) {
			StringBuilder sb = new StringBuilder(len);
			int lastPos = 0;

			for (int w = 0; w < wildcards.size(); w++) {
				int[] wc = wildcards.get(w);
				int start = wc[0];
				int end = wc[1] + 1; // inclusive

				// append before wildcard
				if (lastPos < start) {
					sb.append(pattern, lastPos, start);
				}

				if (((mask >> w) & 1) == 0) {
					// keep wildcard
					sb.append(pattern, start, end);
				} else {
					// remove wildcard and optional dot before
					if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '.') {
						sb.setLength(sb.length() - 1);
					}
				}

				lastPos = end;
			}

			// append remaining part
			if (lastPos < len) {
				sb.append(pattern, lastPos, len);
			}

			result.add(sb.toString());
		}

		return result;
	}

	public static String formatPropertyForMarkdown(String propertyName) {
		return formatProperty(propertyName, KEY_MAP_MARKDOWN_REPLACE, INDEX_ARRAY_MARKDOWN_REPLACE).getPropertyName();
	}

	public static FormattedPropertyResult formatPropertyForCompletion(String propertyName) {
		return formatProperty(propertyName, KEY_MAP_COMPLETION_PLACEHOLDER_REPLACE,
				INDEX_ARRAY_COMPLETION_PLACEHOLDER_REPLACE);
	}

	private static FormattedPropertyResult formatProperty(String propertyName,
			BiConsumer<Integer, StringBuilder> keyMapReplace, BiConsumer<Integer, StringBuilder> indexArrayReplace) {
		if (!isMappedProperty(propertyName) && !isIndexArrayProperty(propertyName)) {
			return new FormattedPropertyResult(propertyName, 0);
		}
		StringBuilder newName = new StringBuilder();
		int parameterCount = 0;
		for (int i = 0; i < propertyName.length(); i++) {
			char c = propertyName.charAt(i);
			if (c == '{') {
				i = i + 2;
				parameterCount++;
				keyMapReplace.accept(parameterCount, newName);
			} else if (c == '[') {
				i = i + 2;
				parameterCount++;
				indexArrayReplace.accept(parameterCount, newName);
			} else {
				newName.append(c);
			}
		}
		return new FormattedPropertyResult(newName.toString(), parameterCount);
	}

	/**
	 * Returns true if the given property name is a mapped property and false
	 * otherwise.
	 *
	 * @param propertyName the property name
	 * @return true if the given property name is a mapped property and false
	 *         otherwise.
	 */
	public static boolean isMappedProperty(String propertyName) {
		return propertyName.indexOf("{*}") != -1;
	}

	/**
	 * Returns true if the given property name is a index array property and false
	 * otherwise.
	 *
	 * @param propertyName the property name
	 * @return true if the given property name is a index array property and false
	 *         otherwise.
	 */
	public static boolean isIndexArrayProperty(String propertyName) {
		return propertyName.indexOf("[*]") != -1;
	}

	/**
	 * 
	 * @param documentURI
	 * @return
	 */
	public static PropertiesModel loadProperties(String documentURI) {
		try {
			return PropertiesModel.parse(IOUtils.convertStreamToString(new URL(documentURI).openStream()), documentURI,
					() -> {
					});
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while loading properties file '" + documentURI + "'.", e);
			return null;
		}
	}
}
