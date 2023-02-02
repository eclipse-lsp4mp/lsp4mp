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
package org.eclipse.lsp4mp.jdt.core.config.properties;

import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.assertProperties;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.assertPropertiesDuplicate;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.p;

import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest;
import org.junit.Test;


/**
 * Tests if <code>boolean</code> and <code>int</code> @ConfigItem properties without the
 * <code>defaultValue</code> annotation, has a default value of <code>false</code>
 * and <code>0</code> respectively
 */
public class ConfigItemIntBoolDefaultValueTest extends BasePropertiesManagerTest {


	@Test
	public void configItemIntBoolDefaultValueTest() throws Exception {

		MicroProfileProjectInfo infoFromClasspath = getMicroProfileProjectInfoFromMavenProject(
				MicroProfileMavenProjectName.config_quickstart, MicroProfilePropertiesScope.SOURCES_AND_DEPENDENCIES);

		assertProperties(infoFromClasspath,
				31 /* properties from Java sources with ConfigProperty */ + //
				7 /* static properties from microprofile-context-propagation-api */ + //
				1 /* static property from microprofile config_ordinal */,

				// GreetingConstructorResource(
				// 		@ConfigProperty(name = "greeting.constructor.message") String message,
				//		@ConfigProperty(name = "greeting.constructor.suffix" , defaultValue="!") String suffix,
				//		@ConfigProperty(name = "greeting.constructor.name") Optional<String> name)
				p(null, "greeting.constructor.message", "java.lang.String", null, false, "org.acme.config.GreetingConstructorResource",
						null, "GreetingConstructorResource(QString;QString;QOptional<QString;>;)V", 0, null),

				p(null, "greeting.constructor.suffix", "java.lang.String", null, false, "org.acme.config.GreetingConstructorResource",
						null, "GreetingConstructorResource(QString;QString;QOptional<QString;>;)V", 0, "!"),

				p(null, "greeting.constructor.name", "java.util.Optional", null, false, "org.acme.config.GreetingConstructorResource",
						null, "GreetingConstructorResource(QString;QString;QOptional<QString;>;)V", 0, null),

				// setMessage(@ConfigProperty(name = "greeting.method.message") String message)
				p(null, "greeting.method.message", "java.lang.String", null, false, "org.acme.config.GreetingMethodResource",
						null, "setMessage(QString;)V", 0, null),

				// setSuffix(@ConfigProperty(name = "greeting.method.suffix" , defaultValue="!") String suffix)
				p(null, "greeting.method.suffix", "java.lang.String", null, false, "org.acme.config.GreetingMethodResource",
						null, "setSuffix(QString;)V", 0, "!"),

				// setName(@ConfigProperty(name = "greeting.method.name") Optional<String> name)
				p(null, "greeting.method.name", "java.util.Optional", null, false, "org.acme.config.GreetingMethodResource",
						null, "setName(QOptional<QString;>;)V", 0, null)
				);

		assertPropertiesDuplicate(infoFromClasspath);
	}
}
