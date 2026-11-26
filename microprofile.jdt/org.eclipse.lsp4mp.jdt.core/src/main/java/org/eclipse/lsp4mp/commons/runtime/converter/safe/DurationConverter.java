/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.commons.runtime.converter.safe;

import java.time.Duration;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * Converter for {@link Duration} used in SAFE execution mode.
 *
 * <p>
 * In addition to the ISO-8601 representation (e.g. {@code PT60S}), it accepts the
 * relaxed format used by Quarkus/SmallRye configuration:
 * </p>
 * <ul>
 * <li>a bare number is interpreted as a number of seconds ({@code 60});</li>
 * <li>a value starting with a digit is treated as an ISO-8601 duration without the
 * leading {@code PT} ({@code 60S}, {@code 10m}, {@code 3h});</li>
 * <li>any other value is parsed as ISO-8601 ({@code PT1H30M}).</li>
 * </ul>
 *
 * <p>
 * SmallRye Config does not register an explicit converter for {@link Duration}, so
 * SAFE mode would otherwise fall back to {@link Duration#parse(CharSequence)}, which
 * only accepts the strict ISO-8601 format. This mirrors the behavior of the Quarkus
 * {@code DurationConverter} that is used in FULL mode.
 */
public class DurationConverter implements Converter<Duration> {

	private static final long serialVersionUID = 1L;

	private static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");

	private static final Pattern START_WITH_DIGITS = Pattern.compile("^[-+]?\\d.*$");

	@Override
	public Duration convert(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		if (value.isEmpty()) {
			return null;
		}
		if (DIGITS.matcher(value).matches()) {
			return Duration.ofSeconds(Long.parseLong(value));
		}
		if (START_WITH_DIGITS.matcher(value).matches()) {
			return Duration.parse("PT" + value);
		}
		return Duration.parse(value);
	}
}
