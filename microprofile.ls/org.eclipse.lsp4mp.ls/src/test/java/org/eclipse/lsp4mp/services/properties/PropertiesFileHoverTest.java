/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/

package org.eclipse.lsp4mp.services.properties;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.assertHoverMarkdown;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.assertHoverPlaintext;

import java.util.Arrays;

import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.junit.Test;

/**
 * Test with hover in 'microprofile-config.properties' file.
 *
 */
public class PropertiesFileHoverTest {

	@Test
	public void unkwownProperty() throws Exception {
		String value = "unkwo|wn";
		String hoverLabel = null;
		assertHoverMarkdown(value, hoverLabel, 0);
	};

	@Test
	public void testKeyHoverMarkdown() throws Exception {
		String value = "quarkus.applica|tion.name = name";
		String hoverLabel = "**quarkus.application.name**" + System.lineSeparator() + System.lineSeparator() + //
				"The name of the application.\nIf not set, defaults to the name of the project (except for tests where it is not set at all)."
				+ System.lineSeparator() + System.lineSeparator() + //
				" * Type: `java.util.Optional<java.lang.String>`" + System.lineSeparator() + //
				" * Value: `name`" + System.lineSeparator() + //
				" * Phase: `buildtime & runtime`" + System.lineSeparator() + //
				" * Extension: `quarkus-core`";
		assertHoverMarkdown(value, hoverLabel, 0);
	};

	@Test
	public void testKeyHoverPlaintext() throws Exception {
		String value = "quarkus.applica|tion.name = name";
		String hoverLabel = "quarkus.application.name" + System.lineSeparator() + System.lineSeparator() + //
				"The name of the application.\nIf not set, defaults to the name of the project (except for tests where it is not set at all)."
				+ System.lineSeparator() + System.lineSeparator() + //
				"Type: java.util.Optional<java.lang.String>" + System.lineSeparator() + //
				"Value: name" + System.lineSeparator() + //
				"Phase: buildtime & runtime" + System.lineSeparator() + //
				"Extension: quarkus-core";
		assertHoverPlaintext(value, hoverLabel, 0);
	};

	@Test
	public void testKeyHoverNoSpaces() throws Exception {
		String value = "quarkus.applica|tion.name=name";
		String hoverLabel = "**quarkus.application.name**" + System.lineSeparator() + System.lineSeparator() + //
				"The name of the application.\nIf not set, defaults to the name of the project (except for tests where it is not set at all)."
				+ System.lineSeparator() + System.lineSeparator() + //
				" * Type: `java.util.Optional<java.lang.String>`" + System.lineSeparator() + //
				" * Value: `name`" + System.lineSeparator() + //
				" * Phase: `buildtime & runtime`" + System.lineSeparator() + //
				" * Extension: `quarkus-core`";
		assertHoverMarkdown(value, hoverLabel, 0);
	};

	@Test
	public void testKeyHoverOnEqualsSign() throws Exception {
		assertHoverMarkdown("quarkus.application.name |= name", null, 0);
		String hoverLabel = "**quarkus.application.name**" + System.lineSeparator() + System.lineSeparator() + //
				"The name of the application.\nIf not set, defaults to the name of the project (except for tests where it is not set at all)."
				+ System.lineSeparator() + System.lineSeparator() + //
				" * Type: `java.util.Optional<java.lang.String>`" + System.lineSeparator() + //
				" * Value: `name`" + System.lineSeparator() + //
				" * Phase: `buildtime & runtime`" + System.lineSeparator() + //
				" * Extension: `quarkus-core`";
		assertHoverMarkdown("quarkus.application.name|=name", hoverLabel, 0);
		hoverLabel = "**quarkus.log.syslog.async.overflow**" + System.lineSeparator() + System.lineSeparator() + //
				"Determine whether to block the publisher (rather than drop the message) when the queue is full"
				+ System.lineSeparator() + System.lineSeparator() + //
				" * Type: `org.jboss.logmanager.handlers.AsyncHandler$OverflowAction`" + System.lineSeparator() + //
				" * Default: `block`" + System.lineSeparator() + //
				" * Value: `DISCARD`" + System.lineSeparator() + //
				" * Phase: `runtime`" + System.lineSeparator() + //
				" * Extension: `quarkus-core`";
		assertHoverMarkdown("quarkus.log.syslog.async.overflow|=DISCARD", hoverLabel, 0);
	};

	@Test
	public void testValueHoverOnEqualsSign() throws Exception {
		assertHoverMarkdown("quarkus.log.syslog.async.overflow |= DISCARD", null, 0);
		String hoverLabel = "**quarkus.log.syslog.async.overflow**" + System.lineSeparator() + System.lineSeparator() + //
				"Determine whether to block the publisher (rather than drop the message) when the queue is full"
				+ System.lineSeparator() + System.lineSeparator() + //
				" * Type: `org.jboss.logmanager.handlers.AsyncHandler$OverflowAction`" + System.lineSeparator() + //
				" * Default: `block`" + System.lineSeparator() + //
				" * Value: `DISCARD`" + System.lineSeparator() + //
				" * Phase: `runtime`" + System.lineSeparator() + //
				" * Extension: `quarkus-core`";
		assertHoverMarkdown("quarkus.log.syslog.async.overflow|=DISCARD", hoverLabel, 0);
	};

	@Test
	public void testNoHoverOnEqualsWhenNoValue() throws Exception {
		String value = "a=1\n" + //
				"b=|";
		assertHoverMarkdown(value, null, 0);
	};

	@Test
	public void testDefaultProfileHover() throws Exception {
		String value = "%d|ev.quarkus.log.syslog.async.overflow=DISCARD";
		String hoverLabelMarkdown = "**dev**" + System.lineSeparator() + System.lineSeparator()
				+ "Profile activated when in development mode (quarkus:dev)." + System.lineSeparator();
		assertHoverMarkdown(value, hoverLabelMarkdown, 0);
	}

	@Test
	public void testDefaultProfileHoverSpacesInFront() throws Exception {
		String value = "        %d|ev.quarkus.log.syslog.async.overflow=DISCARD";
		String hoverLabelMarkdown = "**dev**" + System.lineSeparator() + System.lineSeparator()
				+ "Profile activated when in development mode (quarkus:dev)." + System.lineSeparator();
		assertHoverMarkdown(value, hoverLabelMarkdown, 8);
	}

	@Test
	public void testOnlyDefaultProfile() throws Exception {
		String value = "%de|v";
		String hoverLabelMarkdown = "**dev**" + System.lineSeparator() + System.lineSeparator()
				+ "Profile activated when in development mode (quarkus:dev)." + System.lineSeparator();
		assertHoverMarkdown(value, hoverLabelMarkdown, 0);

		value = "|%prod";
		hoverLabelMarkdown = "**prod**" + System.lineSeparator() + System.lineSeparator()
				+ "The default profile when not running in development or test mode." + System.lineSeparator();
		assertHoverMarkdown(value, hoverLabelMarkdown, 0);

		value = "%test|";
		hoverLabelMarkdown = "**test**" + System.lineSeparator() + System.lineSeparator()
				+ "Profile activated when running tests." + System.lineSeparator();
		assertHoverMarkdown(value, hoverLabelMarkdown, 0);
	};

	@Test
	public void testOnlyNonDefaultProfile() throws Exception {
		String value = "%hel|lo";
		String hoverLabel = null;
		assertHoverMarkdown(value, hoverLabel, 0);

		value = "%hello|";
		assertHoverMarkdown(value, hoverLabel, 0);

		value = "|%hello";
		assertHoverMarkdown(value, hoverLabel, 0);
	};

	@Test
	public void testKeyWithProfileHoverMarkdown() throws Exception {
		String value = "%dev.quarkus.applica|tion.name = name";
		String hoverLabel = "**quarkus.application.name**" + System.lineSeparator() + System.lineSeparator() + //
				"The name of the application.\nIf not set, defaults to the name of the project (except for tests where it is not set at all)."
				+ System.lineSeparator() + System.lineSeparator() + //
				" * Profile: `dev`" + System.lineSeparator() + //
				" * Type: `java.util.Optional<java.lang.String>`" + System.lineSeparator() + //
				" * Value: `name`" + System.lineSeparator() + //
				" * Phase: `buildtime & runtime`" + System.lineSeparator() + //
				" * Extension: `quarkus-core`";
		assertHoverMarkdown(value, hoverLabel, 0);
	};

	@Test
	public void testKeyMap() throws Exception {
		String value = "quar|kus.log.category.\"com.lordofthejars\".level=DEBUG";
		String hoverLabel = "**quarkus.log.category.\\{\\*\\}.level**" + System.lineSeparator() + System.lineSeparator()
				+ //
				"The log level level for this category" + System.lineSeparator() + System.lineSeparator() + //
				" * Type: `io.quarkus.runtime.logging.InheritableLevel`" + System.lineSeparator() + //
				" * Default: `inherit`" + System.lineSeparator() + //
				" * Value: `DEBUG`" + System.lineSeparator() + //
				" * Phase: `runtime`" + System.lineSeparator() + //
				" * Extension: `quarkus-core`";
		assertHoverMarkdown(value, hoverLabel, 0);
	};

	@Test
	public void hoverWithEnums() throws Exception {
		String value = "quarkus.log.console.async.overflow=BLO|CK";
		// OverflowAction enum type
		String hoverLabel = "**BLOCK**" + System.lineSeparator();
		assertHoverMarkdown(value, hoverLabel, 35);
	}

	@Test
	public void hoverOnValueForLevelBasedOnRule() throws Exception {
		// quarkus.log.file.level has 'java.util.logging.Level'
		String value = "quarkus.log.file.level=OF|F ";
		String hoverLabel = "**OFF**" + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"`OFF` is a special level that can be used to turn off logging.\nThis level is initialized to `Integer.MAX_VALUE`."
				+ //
				System.lineSeparator();
		assertHoverMarkdown(value, hoverLabel, 23);
	}

	@Test
	public void hoverWithEnumsKebabCase() throws Exception {
		String value = "quarkus.datasource.transaction-isolation-level = read-unc|ommitted";
		// io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation
		// enum type
		String hoverLabel = "**READ_UNCOMMITTED**" + System.lineSeparator();
		assertHoverMarkdown(value, hoverLabel, 49);
	}

	@Test
	public void hoverWithPropertyWithNullValue() throws Exception {
		ItemMetadata appNameProperty = new ItemMetadata();
		appNameProperty.setType("java.util.Optional\u003cjava.lang.String\u003e");
		appNameProperty.setSourceField("name");
		appNameProperty.setExtensionName("quarkus-core");
		appNameProperty.setRequired(false);
		appNameProperty.setPhase(2);
		appNameProperty.setName("quarkus.application.name");
		appNameProperty.setDescription("The name of the application.\nIf not set, defaults to the name of the project (except for tests where it is not set at all).");
		appNameProperty.setSourceType("io.quarkus.runtime.ApplicationConfig");

		ItemMetadata propertyWithNullValue = new ItemMetadata();
		propertyWithNullValue.setName("my.property");
		propertyWithNullValue.setDefaultValue(null);

		ItemMetadata propertyWithNotNullValue = new ItemMetadata();
		propertyWithNotNullValue.setName("my.property");
		propertyWithNotNullValue.setDefaultValue("asdf");

		MicroProfileProjectInfo projectInfo = new MicroProfileProjectInfo();
		projectInfo.setProperties(Arrays.asList(propertyWithNullValue, propertyWithNotNullValue, appNameProperty));

		String value = "quarkus.applica|tion.name = name";
		String hoverLabel = "**quarkus.application.name**" + System.lineSeparator() + System.lineSeparator() + //
				"The name of the application.\nIf not set, defaults to the name of the project (except for tests where it is not set at all)."
				+ System.lineSeparator() + System.lineSeparator() + //
				" * Type: `java.util.Optional<java.lang.String>`" + System.lineSeparator() + //
				" * Value: `name`" + System.lineSeparator() + //
				" * Phase: `buildtime & runtime`" + System.lineSeparator() + //
				" * Extension: `quarkus-core`";
		assertHoverMarkdown(value, hoverLabel, 0, projectInfo);
	}

}
