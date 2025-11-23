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
package org.eclipse.lsp4mp.commons.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for parsing Java type signatures into reflective {@link Type}
 * instances.
 *
 * <p>
 * Supports both simple types and generic types, e.g.:
 * </p>
 *
 * <pre>
 * java.lang.String
 * java.util.List&lt;java.lang.String&gt;
 * java.util.Map&lt;java.lang.String, java.lang.Integer&gt;
 * </pre>
 *
 * <p>
 * The class uses a provided {@link ClassLoader} to load types. When loading
 * classes, it calls {@link Class#forName(String, boolean, ClassLoader)} with
 * {@code initialize=false}, which is important for security:
 * </p>
 *
 * <ul>
 * <li>Static initializers in the loaded classes will <b>not</b> be
 * executed.</li>
 * <li>This prevents executing potentially malicious code at parse time.</li>
 * <li>You can safely load classes only to inspect their type for converters
 * (e.g., SmallRye Config converters) without risk.</li>
 * </ul>
 *
 * <h2>Examples</h2>
 *
 * <pre>
 * Type type1 = TypeSignatureParser.parse("java.lang.String");
 * Type type2 = TypeSignatureParser.parse("java.util.List&lt;java.lang.String&gt;");
 * Type type3 = TypeSignatureParser.parse("java.util.Map&lt;java.lang.String, java.lang.Integer&gt;");
 * </pre>
 */
public class TypeSignatureParser {

	public static Type parse(String signature) {
		return parse(signature, null);
	}

	/**
	 * Parses a type signature using the current thread's context class loader.
	 *
	 * @param signature the type signature to parse
	 * @return a {@link Type} representation of the signature
	 * @throws IllegalArgumentException if a class cannot be found
	 */
	public static Type parse(String signature, EnumConstantsProvider enumConstNamesProvider) {
		return parse(signature, enumConstNamesProvider, Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Parses a type signature using the given class loader.
	 *
	 * @param signature   the type signature to parse
	 * @param classLoader the {@link ClassLoader} to resolve classes
	 * @return a {@link Type} representing the parsed signature
	 * @throws IllegalArgumentException if a class cannot be found
	 */
	public static Type parse(String signature, EnumConstantsProvider enumConstNamesProvider, ClassLoader classLoader) {
		return new Parser(signature, enumConstNamesProvider, classLoader).parseType();
	}

	public static class EmulateType implements Type {

		private final String typeName;

		public EmulateType(String typeName) {
			this.typeName = typeName;
		}

		@Override
		public String getTypeName() {
			return typeName;
		}

	}

	public static class EnumType extends EmulateType {
		private List<String> enumConstNames;

		public EnumType(String typeName, List<String> enumConstNames) {
			super(typeName);
			this.enumConstNames = enumConstNames;
		}

		public List<String> getEnumConstants() {
			return enumConstNames;
		}

		public void setEnumConstNames(List<String> enumConstNames) {
			this.enumConstNames = enumConstNames;
		}

	}

	/**
	 * Internal parser for recursive parsing of type signatures.
	 */
	private static class Parser {
		private final String s;
		private int pos = 0;
		private final ClassLoader classLoader;
		private final EnumConstantsProvider enumConstNamesProvider;

		Parser(String s, EnumConstantsProvider enumConstNamesProvider, ClassLoader classLoader) {
			this.s = s;
			this.enumConstNamesProvider = enumConstNamesProvider;
			this.classLoader = classLoader;
		}

		/**
		 * Parses a single type, possibly parameterized.
		 *
		 * @return a {@link Type} for the parsed signature
		 */
		Type parseType() {
			String raw = readIdentifier();
			skipSpaces();

			if (peek() != '<') {
				// Simple type, load the class safely
				return loadClass(raw);
			}

			next(); // skip '<'
			List<Type> args = new ArrayList<>();

			while (true) {
				skipSpaces();
				args.add(parseType()); // recursive parsing for generics
				skipSpaces();

				if (peek() == ',') {
					next();
					continue;
				}
				break;
			}

			expect('>');
			return parameterized(raw, args.toArray(Type[]::new));
		}

		/**
		 * Reads a fully qualified class name or identifier until a boundary character
		 * (whitespace, comma, angle bracket).
		 */
		String readIdentifier() {
			int start = pos;
			while (pos < s.length() && !isBoundary(s.charAt(pos)))
				pos++;
			return s.substring(start, pos).trim();
		}

		/**
		 * Checks whether a character marks the end of an identifier.
		 */
		boolean isBoundary(char c) {
			return c == '<' || c == '>' || c == ',' || Character.isWhitespace(c);
		}

		/**
		 * Returns the next character without advancing the cursor.
		 */
		char peek() {
			return pos < s.length() ? s.charAt(pos) : '\0';
		}

		/**
		 * Advances the cursor and returns the next character.
		 */
		int next() {
			return pos < s.length() ? s.charAt(pos++) : -1;
		}

		/**
		 * Skips whitespace characters.
		 */
		void skipSpaces() {
			while (pos < s.length() && Character.isWhitespace(s.charAt(pos)))
				pos++;
		}

		/**
		 * Expects a specific character at the current position, throws if not found.
		 */
		void expect(char c) {
			if (peek() != c) {
				throw new IllegalArgumentException("Expected '" + c + "' at position " + pos + " in: " + s);
			}
			pos++;
		}

		/**
		 * Loads a class safely using
		 * {@link Class#forName(String, boolean, ClassLoader)}.
		 *
		 * <p>
		 * Using {@code initialize=false} ensures that static initializers are NOT
		 * executed, making this safe for parsing and inspecting types from potentially
		 * untrusted code.
		 * </p>
		 *
		 * @param name the fully qualified class name
		 * @return the loaded {@link Class} object
		 * @throws IllegalArgumentException if the class cannot be found
		 */
		Type loadClass(String name) {
			return MicroProfileProjectRuntime.findType(name, enumConstNamesProvider, classLoader);
		}

		/**
		 * Creates a synthetic {@link ParameterizedType} for the raw type and type
		 * arguments.
		 */
		ParameterizedType parameterized(String raw, Type... args) {
			return new ParameterizedType() {
				@Override
				public Type[] getActualTypeArguments() {
					return args;
				}

				@Override
				public Type getRawType() {
					return loadClass(raw);
				}

				@Override
				public Type getOwnerType() {
					return null;
				}

				@Override
				public String toString() {
					String[] parts = new String[args.length];
					for (int i = 0; i < args.length; i++)
						parts[i] = args[i].toString();
					return raw + "<" + String.join(", ", parts) + ">";
				}
			};
		}
	}
}
