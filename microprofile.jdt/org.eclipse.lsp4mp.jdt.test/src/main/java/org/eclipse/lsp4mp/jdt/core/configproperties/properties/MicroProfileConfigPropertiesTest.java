/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.jdt.core.configproperties.properties;

import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.assertProperties;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.assertPropertiesDuplicate;
import static org.eclipse.lsp4mp.jdt.core.MicroProfileAssert.p;

import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.jdt.core.BasePropertiesManagerTest;
import org.junit.Test;

/**
 * Test collection of MicroProfile properties from @ConfigProperties
 *
 * @author Angelo ZERR
 *
 * @see https://github.com/quarkusio/quarkus/blob/main/extensions/arc/deployment/src/test/java/io/quarkus/arc/test/config/ConfigPropertiesTest.java
 * 
 */
public class MicroProfileConfigPropertiesTest extends BasePropertiesManagerTest {

	@Test
	public void configPropertiesFromJavaSources() throws Exception {

		MicroProfileProjectInfo infoFromJavaSources = getMicroProfileProjectInfoFromMavenProject(
				MicroProfileMavenProjectName.microprofile_configproperties, MicroProfilePropertiesScope.ONLY_SOURCES);

		assertProperties(infoFromJavaSources, 17 /* properties from Java sources with ConfigProperties */,

				p(null, "server.host", "java.lang.String", null, false, "org.acme.Details", "host", null, 0, null),
				p(null, "server.port", "int", null, false, "org.acme.Details", "port", null, 0, null),
				p(null, "server.endpoint", "java.lang.String", null, false, "org.acme.Details", "endpoint", null, 0,
						null),
				p(null, "server.old.location", "java.lang.String", null, false, "org.acme.Details", "location", null, 0,
						null),

				p(null, "host2", "java.lang.String", null, false, "org.acme.DetailsWithoutPrefix", "host2", null, 0,
						null),
				p(null, "port2", "int", null, false, "org.acme.DetailsWithoutPrefix", "port2", null, 0, null),
				p(null, "endpoint2", "java.lang.String", null, false, "org.acme.DetailsWithoutPrefix", "endpoint2",
						null, 0, null),
				p(null, "old.location2", "java.lang.String", null, false, "org.acme.DetailsWithoutPrefix", "location2",
						null, 0, null),

				p(null, "host3", "java.lang.String", null, false, "org.acme.Server$ServerConfigProperties", "host3",
						null, 0, null),
				p(null, "port3", "int", null, false, "org.acme.Server$ServerConfigProperties", "port3", null, 0, null),
				p(null, "reasons3", "java.util.Map", null, false,
						"org.acme.Server$ServerConfigProperties", "reasons3", null, 0, null),
				p(null, "server3.host3", "java.lang.String", null, false, "org.acme.Server$ServerConfigProperties",
						"host3", null, 0, null),
				p(null, "server3.port3", "int", null, false, "org.acme.Server$ServerConfigProperties", "port3", null, 0,
						null),
				p(null, "server3.reasons3", "java.util.Map", null, false,
						"org.acme.Server$ServerConfigProperties", "reasons3", null, 0, null),
				p(null, "cloud.host3", "java.lang.String", null, false, "org.acme.Server$ServerConfigProperties",
						"host3", null, 0, null),
				p(null, "cloud.port3", "int", null, false, "org.acme.Server$ServerConfigProperties", "port3", null, 0,
						null),
				p(null, "cloud.reasons3", "java.util.Map", null, false,
						"org.acme.Server$ServerConfigProperties", "reasons3", null, 0, null)

		);

		assertPropertiesDuplicate(infoFromJavaSources);
	}
}
