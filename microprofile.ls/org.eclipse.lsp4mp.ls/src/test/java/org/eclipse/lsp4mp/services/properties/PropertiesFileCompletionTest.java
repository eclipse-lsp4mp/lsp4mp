/*******************************************************************************
0* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.services.properties;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.c;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.r;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.testCompletionFor;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.testCompletionItemResolveFor;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.testCompletionItemUnresolvedFor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.ls.commons.BadLocationException;
import org.junit.Test;

/**
 * Test with completion in 'microprofile-config.properties' file.
 *
 * @author Angelo ZERR
 *
 */
public class PropertiesFileCompletionTest {

	@Test
	public void completionOnComments() throws BadLocationException {
		String value = "#|";
		testCompletionFor(value, true, 0);

		value = " #|";
		testCompletionFor(value, true, 0);
	}

	@Test
	public void completionOnKey() throws BadLocationException {
		String value = "|";
		testCompletionFor(value, false, //
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 0)));
		testCompletionFor(value, true, //
				c("quarkus.http.cors = false", "quarkus.http.cors=${1|false,true|}", r(0, 0, 0)));

		value = " |";
		testCompletionFor(value, false, //
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 1)));
		testCompletionFor(value, true, //
				c("quarkus.http.cors = false", "quarkus.http.cors=${1|false,true|}", r(0, 0, 1)));

		value = " quarkus.http.co|rs = ";
		testCompletionFor(value, false, //
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 21)));
		testCompletionFor(value, true, //
				c("quarkus.http.cors = false", "quarkus.http.cors=${1|false,true|}", r(0, 0, 21)));

		value = " quarkus.application.name =| ";
		testCompletionFor(value, true, 0);
	}

	@Test
	public void completionOnKeyItemDefaults() throws BadLocationException {
		String value = "|";
		testCompletionFor(value, false, false, true,
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 0)));
		testCompletionFor(value, true, false, true,
				c("quarkus.http.cors = false", "quarkus.http.cors=${1|false,true|}", r(0, 0, 0)));

		value = " |";
		testCompletionFor(value, false, false, true,
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 1)));
		testCompletionFor(value, true, false, true,
				c("quarkus.http.cors = false", "quarkus.http.cors=${1|false,true|}", r(0, 0, 1)));

		value = " quarkus.http.co|rs = ";
		testCompletionFor(value, false, false, true,
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 21)));
		testCompletionFor(value, true, false, true,
				c("quarkus.http.cors = false", "quarkus.http.cors=${1|false,true|}", r(0, 0, 21)));

		value = " quarkus.application.name =| ";
		testCompletionFor(value, true, false, true, 0);
	}

	@Test
	public void completionOnKeyMap() throws BadLocationException {
		String value = "quarkus.log.category|";
		testCompletionFor(value, false, //
				c("quarkus.log.category.{*}.level = inherit", "quarkus.log.category.{*}.level=inherit", r(0, 0, 20)));
		testCompletionFor(value, true, //
				c("quarkus.log.category.{*}.level = inherit",
						"quarkus.log.category.${1:key}.level=${2|OFF,SEVERE,WARNING,CONFIG,FINE,FINER,FINEST,ALL,FATAL,ERROR,WARN,INFO,DEBUG,TRACE|}",
						r(0, 0, 20)));
	}

	@Test
	public void completionOnKeyMapItemDefaults() throws BadLocationException {
		String value = "quarkus.log.category|";
		testCompletionFor(value, false, false, true, //
				c("quarkus.log.category.{*}.level = inherit", "quarkus.log.category.{*}.level=inherit", r(0, 0, 20)));
		testCompletionFor(value, true, false, true, //
				c("quarkus.log.category.{*}.level = inherit",
						"quarkus.log.category.${1:key}.level=${2|OFF,SEVERE,WARNING,CONFIG,FINE,FINER,FINEST,ALL,FATAL,ERROR,WARN,INFO,DEBUG,TRACE|}",
						r(0, 0, 20)));
	}

	@Test
	public void completionOnEmptyLine() throws BadLocationException {
		String value = "quarkus.application.name= \r\n" + //
				"|\r\n" + //
				"quarkus.application.version= ";
		testCompletionFor(value, false,
				c("quarkus.log.category.{*}.level = inherit", "quarkus.log.category.{*}.level=inherit", r(1, 0, 0)));
		testCompletionFor(value, true, c("quarkus.log.category.{*}.level = inherit",
				"quarkus.log.category.${1:key}.level=${2|OFF,SEVERE,WARNING,CONFIG,FINE,FINER,FINEST,ALL,FATAL,ERROR,WARN,INFO,DEBUG,TRACE|}",
				r(1, 0, 0)));
	}

	@Test
	public void completionOnValueNoCompletionItems() throws BadLocationException {
		String value = "quarkus.application.name = | ";
		testCompletionFor(value, true, 0);
	}

	@Test
	public void completionOnValueOnAssign() throws BadLocationException {
		String value = "quarkus.log.console.async.overflow=| ";
		testCompletionFor(value, true, c("block", "block", r(0, 35, 36)), c("discard", "discard", r(0, 35, 36)));
	}

	@Test
	public void completionOnValueEndOfLine() throws BadLocationException {
		String value = "quarkus.log.console.async.overflow=|\n";
		testCompletionFor(value, true, c("block", "block", r(0, 35, 35)), c("discard", "discard", r(0, 35, 35)));
	}

	@Test
	public void completionOnValueOnPropertyValue() throws BadLocationException {
		String value = "quarkus.log.console.async.overflow=BLO| ";
		testCompletionFor(value, true, c("block", "block", r(0, 35, 39)));
	}

	@Test
	public void completionOnValueOnPropertyValueItemDefaults() throws BadLocationException {
		String value = "quarkus.log.console.async.overflow=BLO| ";
		testCompletionFor(value, true, false, true, c("block", "block", r(0, 35, 39)));
	}

	@Test
	public void completionOnValueBetweenPropertyValue() throws BadLocationException {
		String value = "quarkus.log.console.async.overflow=B|L";
		testCompletionFor(value, true, c("block", "block", r(0, 35, 37)));
	}

	@Test
	public void completionOnKeyWithEnums() throws BadLocationException {
		String value = "|";
		// OverflowAction enum type
		testCompletionFor(value, false, //
				c("quarkus.log.console.async.overflow = block", "quarkus.log.console.async.overflow=block",
						r(0, 0, 0)));
		testCompletionFor(value, true, //
				c("quarkus.log.console.async.overflow = block",
						"quarkus.log.console.async.overflow=${1|block,discard|}", r(0, 0, 0)));

		// Boolean type
		testCompletionFor(value, false, //
				c("quarkus.datasource.enable-metrics", "quarkus.datasource.enable-metrics=false", r(0, 0, 0)));
		testCompletionFor(value, true, //
				c("quarkus.datasource.enable-metrics", "quarkus.datasource.enable-metrics=${1|false,true|}",
						r(0, 0, 0)));
	}

	@Test
	public void completionOnValueWithEnumsKebabCase() throws BadLocationException {
		String value = "quarkus.datasource.transaction-isolation-level=|";
		testCompletionFor(value, true, //
				c("undefined", "undefined", r(0, 47, 47)), //
				c("none", "none", r(0, 47, 47)), //
				c("read-uncommitted", "read-uncommitted", r(0, 47, 47)), //
				c("read-committed", "read-committed", r(0, 47, 47)), //
				c("repeatable-read", "repeatable-read", r(0, 47, 47)), //
				c("serializable", "serializable", r(0, 47, 47)) //
		);
	}

	@Test
	public void completionOnProfile() throws BadLocationException {
		String value = "%|";
		testCompletionFor(value, true, 3,
				c("dev", "%dev", r(0, 0, 1),
						"dev" + System.lineSeparator() + System.lineSeparator()
								+ "Profile activated when in development mode (quarkus:dev)." + System.lineSeparator()), //
				c("prod", "%prod", r(0, 0, 1), "prod" + System.lineSeparator() + System.lineSeparator()
						+ "The default profile when not running in development or test mode." + System.lineSeparator()), //
				c("test", "%test", r(0, 0, 1), "test" + System.lineSeparator() + System.lineSeparator()
						+ "Profile activated when running tests." + System.lineSeparator()));

		value = "%st|\n" + //
				"%staging.property=123";
		testCompletionFor(value, true, 4, c("staging", "%staging", r(0, 0, 3)), //
				c("dev", "%dev", r(0, 0, 3),
						"dev" + System.lineSeparator() + System.lineSeparator()
								+ "Profile activated when in development mode (quarkus:dev)." + System.lineSeparator()), //
				c("prod", "%prod", r(0, 0, 3), "prod" + System.lineSeparator() + System.lineSeparator()
						+ "The default profile when not running in development or test mode." + System.lineSeparator()), //
				c("test", "%test", r(0, 0, 3), "test" + System.lineSeparator() + System.lineSeparator()
						+ "Profile activated when running tests." + System.lineSeparator()));

		value = "%staging|.";
		testCompletionFor(value, true, 3,
				c("dev", "%dev", r(0, 0, 8),
						"dev" + System.lineSeparator() + System.lineSeparator()
								+ "Profile activated when in development mode (quarkus:dev)." + System.lineSeparator()), //
				c("prod", "%prod", r(0, 0, 8), "prod" + System.lineSeparator() + System.lineSeparator()
						+ "The default profile when not running in development or test mode." + System.lineSeparator()), //
				c("test", "%test", r(0, 0, 8), "test" + System.lineSeparator() + System.lineSeparator()
						+ "Profile activated when running tests." + System.lineSeparator()));
	}

	@Test
	public void completionOnProfileWithPropertyName() throws BadLocationException {
		String value = "%pr|quarkus.application.name";
		testCompletionFor(value, true, 3,
				c("dev", "%dev.", r(0, 0, 3),
						"dev" + System.lineSeparator() + System.lineSeparator()
								+ "Profile activated when in development mode (quarkus:dev)." + System.lineSeparator()), //
				c("prod", "%prod.", r(0, 0, 3), "prod" + System.lineSeparator() + System.lineSeparator()
						+ "The default profile when not running in development or test mode." + System.lineSeparator()), //
				c("test", "%test.", r(0, 0, 3), "test" + System.lineSeparator() + System.lineSeparator()
						+ "Profile activated when running tests." + System.lineSeparator()));

		value = "%d|.quarkus.application.name";
		testCompletionFor(value, true, 3,
				c("dev", "%dev", r(0, 0, 2),
						"dev" + System.lineSeparator() + System.lineSeparator()
								+ "Profile activated when in development mode (quarkus:dev)." + System.lineSeparator()), //
				c("prod", "%prod", r(0, 0, 2), "prod" + System.lineSeparator() + System.lineSeparator()
						+ "The default profile when not running in development or test mode." + System.lineSeparator()), //
				c("test", "%test", r(0, 0, 2), "test" + System.lineSeparator() + System.lineSeparator()
						+ "Profile activated when running tests." + System.lineSeparator()));
	}

	@Test
	public void completionAfterProfile() throws BadLocationException {
		String value = "%dev.|";
		testCompletionFor(value, false, //
				c("quarkus.http.cors = false", "%dev.quarkus.http.cors=false", r(0, 0, 5)));
		testCompletionFor(value, true, //
				c("quarkus.http.cors = false", "%dev.quarkus.http.cors=${1|false,true|}", r(0, 0, 5)));
	}

	@Test
	public void completionAfterProfileItemDefaults() throws BadLocationException {
		String value = "%dev.|";
		testCompletionFor(value, false, false, true,
				c("quarkus.http.cors = false", "%dev.quarkus.http.cors=false", r(0, 0, 5)));
		testCompletionFor(value, true, false, true,
				c("quarkus.http.cors = false", "%dev.quarkus.http.cors=${1|false,true|}", r(0, 0, 5)));
	}

	@Test
	public void noCompletionForExistingProperties() throws BadLocationException {

		String value = "|";

		MicroProfileProjectInfo projectInfo = new MicroProfileProjectInfo();
		List<ItemMetadata> properties = new ArrayList<ItemMetadata>();
		ItemMetadata p1 = new ItemMetadata();
		p1.setName("quarkus.http.cors");
		properties.add(p1);
		ItemMetadata p2 = new ItemMetadata();
		p2.setName("quarkus.application.name");
		properties.add(p2);

		projectInfo.setProperties(properties);

		testCompletionFor(value, false, 2, projectInfo, c("quarkus.http.cors", "quarkus.http.cors=", r(0, 0, 0)),
				c("quarkus.application.name", "quarkus.application.name=", r(0, 0, 0)));

		value = "quarkus.http.cors=false\r\n" + //
				"|";

		testCompletionFor(value, false, 1, projectInfo,
				c("quarkus.application.name", "quarkus.application.name=", r(1, 0, 0)));

	}

	@Test
	public void noCompletionForExistingPropertiesWithProfile() throws BadLocationException {

		String value = "%prod.quarkus.application.name=name\n" + "%prod.|";

		MicroProfileProjectInfo projectInfo = new MicroProfileProjectInfo();
		List<ItemMetadata> properties = new ArrayList<ItemMetadata>();

		ItemMetadata p1 = new ItemMetadata();
		p1.setName("quarkus.http.cors");
		properties.add(p1);
		ItemMetadata p2 = new ItemMetadata();
		p2.setName("quarkus.application.name");
		properties.add(p2);

		projectInfo.setProperties(properties);

		testCompletionFor(value, false, 1, projectInfo, c("quarkus.http.cors", "%prod.quarkus.http.cors=", r(1, 0, 6)));
	}

	@Test
	public void completionForExistingPropertiesDifferentProfile() throws BadLocationException {
		testCompletionForExistingPropertiesDifferentProfile(false);
	}

	@Test
	public void completionForExistingPropertiesDifferentProfileItemsDefaults() throws BadLocationException {
		testCompletionForExistingPropertiesDifferentProfile(true);
	}

	private void testCompletionForExistingPropertiesDifferentProfile(boolean isItemDefaultsSupport)
			throws BadLocationException {

		String value = "|";

		MicroProfileProjectInfo projectInfo = new MicroProfileProjectInfo();
		List<ItemMetadata> properties = new ArrayList<ItemMetadata>();
		ItemMetadata p1 = new ItemMetadata();
		p1.setName("quarkus.http.cors");
		properties.add(p1);
		ItemMetadata p2 = new ItemMetadata();
		p2.setName("quarkus.application.name");
		properties.add(p2);

		projectInfo.setProperties(properties);

		testCompletionFor(value, false, false, isItemDefaultsSupport, null, 2, projectInfo,
				c("quarkus.http.cors", "quarkus.http.cors=", r(0, 0, 0)),
				c("quarkus.application.name", "quarkus.application.name=", r(0, 0, 0)));

		value = "quarkus.http.cors=false\r\n" + //
				"%dev.|";

		testCompletionFor(value, false, false, isItemDefaultsSupport, null, 2, projectInfo,
				c("quarkus.http.cors", "%dev.quarkus.http.cors=", r(1, 0, 5)),
				c("quarkus.application.name", "%dev.quarkus.application.name=", r(1, 0, 5)));

		value = "quarkus.http.cors=false\r\n" + //
				"%dev.quarkus.application.name\r\n" + //
				"%prod.|";

		testCompletionFor(value, false, false, isItemDefaultsSupport, null, 2, projectInfo,
				c("quarkus.http.cors", "%prod.quarkus.http.cors=", r(2, 0, 6)),
				c("quarkus.application.name", "%prod.quarkus.application.name=", r(2, 0, 6)));
	}

	@Test
	public void completionOnValueForLevelBasedOnRule() throws BadLocationException {
		// quarkus.log.file.level has 'java.util.logging.Level' which has no
		// enumeration
		// to fix it, quarkus-values-rules.json defines the Level enumerations
		String value = "quarkus.log.file.level=| ";
		testCompletionFor(value, true, c("OFF", "OFF", r(0, 23, 24)), c("SEVERE", "SEVERE", r(0, 23, 24)));
	}

	@Test
	public void completionOnValueForLevelBasedOnRuleWithNewline() throws BadLocationException {
		// quarkus.log.file.level has 'java.util.logging.Level' which has no
		// enumeration
		// to fix it, quarkus-values-rules.json defines the Level enumerations
		String value = "quarkus.log.file.level=|\n";
		testCompletionFor(value, true, c("OFF", "OFF", r(0, 23, 23)), c("SEVERE", "SEVERE", r(0, 23, 23)));
	}

	@Test
	public void completionSpacingSurroundingEquals() throws BadLocationException {
		String value = "|";
		testCompletionFor(value, false, true, //
				c("quarkus.http.cors = false", "quarkus.http.cors = false", r(0, 0, 0)));
		testCompletionFor(value, true, true,
				c("quarkus.http.cors = false", "quarkus.http.cors = ${1|false,true|}", r(0, 0, 0)));
	}

	@Test
	public void completionDefaultValueContainsDollarSign() throws BadLocationException {
		MicroProfileProjectInfo projectInfo = new MicroProfileProjectInfo();
		ItemMetadata metadata = new ItemMetadata();
		metadata.setName("price.string");
		metadata.setDefaultValue("Price: $10");
		projectInfo.setProperties(Collections.singletonList(metadata));

		String value = "|";
		testCompletionFor(value, true, 1, projectInfo, //
				c("price.string = Price: $10", "price.string=${0:Price: \\$10}", r(0, 0, 0)));
		testCompletionFor(value, false, 1, projectInfo, //
				c("price.string = Price: $10", "price.string=Price: $10", r(0, 0, 0)));
	}

	@Test
	public void completionDefaultValueContainsBraces() throws BadLocationException {
		MicroProfileProjectInfo projectInfo = new MicroProfileProjectInfo();
		ItemMetadata metadata = new ItemMetadata();
		metadata.setName("price.string");
		metadata.setDefaultValue("Price: {10}");
		projectInfo.setProperties(Collections.singletonList(metadata));

		String value = "|";
		testCompletionFor(value, true, 1, projectInfo, //
				c("price.string = Price: {10}", "price.string=${0:Price: {10\\}}", r(0, 0, 0)));
		testCompletionFor(value, false, 1, projectInfo, //
				c("price.string = Price: {10}", "price.string=Price: {10}", r(0, 0, 0)));
	}

	@Test
	public void completionDefaultValueContainsDollarSignAndBraces() throws BadLocationException {
		MicroProfileProjectInfo projectInfo = new MicroProfileProjectInfo();
		ItemMetadata metadata = new ItemMetadata();
		metadata.setName("price.string");
		metadata.setDefaultValue("Price: ${price}");
		projectInfo.setProperties(Collections.singletonList(metadata));

		String value = "|";
		testCompletionFor(value, true, 1, projectInfo, //
				c("price.string = Price: ${price}", "price.string=${0:Price: \\${price\\}}", r(0, 0, 0)));
		testCompletionFor(value, false, 1, projectInfo, //
				c("price.string = Price: ${price}", "price.string=Price: ${price}", r(0, 0, 0)));
	}

	@Test
	public void completionBetweenPropertyNameAndEquals() throws BadLocationException {
		String value = "quarkus.http.cors|=";
		testCompletionFor(value, false, c("quarkus.http.cors.headers", "quarkus.http.cors.headers=", r(0, 0, 18)));
	}

	@Test
	public void completionBetweenIncompletePropertyNameAndEquals() throws BadLocationException {
		String value = "quarkus.|=";
		testCompletionFor(value, false, c("quarkus.application.name", "quarkus.application.name=", r(0, 0, 9)));
	}

	@Test
	public void completionBetweenPropertyNameAndEqualsWithValue() throws BadLocationException {
		String value = "quarkus.http.cors|=existing";
		testCompletionFor(value, false,
				c("quarkus.http.cors.headers = existing", "quarkus.http.cors.headers=existing", r(0, 0, 26)));
	}

	@Test
	public void completionBetweenPropertyNameAndSpaceWithValue() throws BadLocationException {
		String value = "quarkus.http.cors| =existing";
		testCompletionFor(value, false,
				c("quarkus.http.cors.headers = existing", "quarkus.http.cors.headers=existing", r(0, 0, 27)));
	}

	@Test
	public void completionAfterJustEquals() throws BadLocationException {
		String value = "quarkus.http.cors = |";
		testCompletionFor(value, true, 2);
	}

	@Test
	public void completionBetweenPropertyNameAndSpaceWithBooleanValue() throws BadLocationException {
		String value = "quarkus.banner.en|=existing";
		testCompletionFor(value, false,
				c("quarkus.banner.enabled = existing", "quarkus.banner.enabled=existing", r(0, 0, 26)));
		testCompletionFor(value, true,
				c("quarkus.banner.enabled = existing", "quarkus.banner.enabled=${1|false,true|}", r(0, 0, 26)));
	}

	@Test
	public void completionOnKeyResolve() throws BadLocationException {
		String expectedDocumentation = "quarkus.http.cors" + System.lineSeparator() + System.lineSeparator() + //
				"Enable the CORS filter." + System.lineSeparator() + System.lineSeparator() + //
				"Type: boolean" + System.lineSeparator() + //
				"Default: false" + System.lineSeparator() + //
				"Phase: runtime" + System.lineSeparator() + //
				"Extension: quarkus-vertx-http";
		String value = "|";
		testCompletionItemResolveFor(value, //
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 0), expectedDocumentation));
		testCompletionItemUnresolvedFor(value, //
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 0)));

		value = " |";
		testCompletionItemResolveFor(value, //
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 1), expectedDocumentation));
		testCompletionItemUnresolvedFor(value, //
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 1)));

		value = " quarkus.http.co|rs = ";
		testCompletionItemResolveFor(value, //
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 21), expectedDocumentation));
		testCompletionItemUnresolvedFor(value, //
				c("quarkus.http.cors = false", "quarkus.http.cors=false", r(0, 0, 21)));
	}

}
