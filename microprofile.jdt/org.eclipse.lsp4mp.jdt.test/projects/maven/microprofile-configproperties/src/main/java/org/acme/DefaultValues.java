package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Optional;

public class DefaultValuesValidation {
	@ConfigProperty(defaultValue = "a string")
	public String string;

	@ConfigProperty(defaultValue = "yes")
	public boolean bool;

	@ConfigProperty(defaultValue = "4")
	public int integer;

	@ConfigProperty(defaultValue = "x")
	public int invalidInteger;

	@ConfigProperty(defaultValue = "a string")
	public Custom custom;

	@ConfigProperty(defaultValue = "/foo/bar")
	public java.nio.file.Path path;

	@ConfigProperty(defaultValue = "http://localhost/bar")
	public java.net.URI uri;

	@ConfigProperty(defaultValue = "2000-12-31T01:23:45Z")
	public java.time.Instant instant;

	@ConfigProperty(defaultValue = "PT2S")
	public java.time.Duration duration;

	@ConfigProperty(defaultValue = "boo")
	public boolean boolUnexpectedFalse;

	/**
	 * This type has a public Constructor with a single String parameter and as such
	 * represents a valid implicit converter as per Microprofile-Config
	 * specification (See {@link org.eclipse.microprofile.config.spi.Converter}).
	 */
	public static class Custom {
		private final String value;

		public Custom(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}
}