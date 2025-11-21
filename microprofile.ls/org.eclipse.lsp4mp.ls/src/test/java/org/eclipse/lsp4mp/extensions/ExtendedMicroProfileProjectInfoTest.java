/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.extensions;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.metadata.ValueHint;
import org.eclipse.lsp4mp.services.properties.PropertiesFileAssert;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test with {@link ExtendedMicroProfileProjectInfo}.
 * 
 * @author Angelo ZERR
 *
 */
public class ExtendedMicroProfileProjectInfoTest {

	@Test
	public void expand() {
		MicroProfileProjectInfo info = new MicroProfileProjectInfo();
		info.setProperties(new ArrayList<>());
		info.setHints(new ArrayList<>());

		// fill properties
		ItemMetadata p = new ItemMetadata();
		p.setName("${mp.register.rest.client.class}/mp-rest/connectTimeout");
		p.setDescription("Timeout specified in milliseconds to wait to connect to the remote endpoint.");
		p.setType("long");
		info.getProperties().add(p);

		// fill hints
		ItemHint hint = new ItemHint();
		hint.setName("${mp.register.rest.client.class}");
		hint.setValues(new ArrayList<>());
		info.getHints().add(hint);

		ValueHint value = new ValueHint();
		value.setValue("org.acme.restclient.CountriesService");
		value.setSourceType("org.acme.restclient.CountriesService");
		hint.getValues().add(value);

		value = new ValueHint();
		value.setValue("org.acme.restclient.StreetsService");
		value.setSourceType("org.acme.restclient.StreetsService");
		hint.getValues().add(value);

		ExtendedMicroProfileProjectInfo wrapper = new ExtendedMicroProfileProjectInfo(info);
		Assert.assertEquals(2, wrapper.getProperties().size());

		ItemMetadata first = wrapper.getProperties().get(0);
		Assert.assertEquals("org.acme.restclient.CountriesService/mp-rest/connectTimeout", first.getName());
		Assert.assertEquals("org.acme.restclient.CountriesService", first.getSourceType());
		Assert.assertEquals("long", first.getType());
		Assert.assertEquals("Timeout specified in milliseconds to wait to connect to the remote endpoint.",
				first.getDescription());
	}

	@Test
	public void expandWithSourcesUpdate() {
		MicroProfileProjectInfo info = new MicroProfileProjectInfo();
		info.setProperties(new ArrayList<>());
		info.setHints(new ArrayList<>());

		// fill properties

		// Create a binary dynamic properties
		ItemMetadata p = new ItemMetadata();
		p.setName("quarkus.cache.caffeine.${quarkus.cache.name}.initial-capacity");
		p.setSource(Boolean.FALSE);
		info.getProperties().add(p);

		// Create a source hint
		ItemHint hint = new ItemHint();
		hint.setName("${quarkus.cache.name}");
		hint.setValues(new ArrayList<>());
		hint.setSource(Boolean.TRUE);
		info.getHints().add(hint);

		ValueHint value = new ValueHint();
		value.setValue("A");
		hint.getValues().add(value);

		value = new ValueHint();
		value.setValue("B");
		hint.getValues().add(value);

		ExtendedMicroProfileProjectInfo wrapper = new ExtendedMicroProfileProjectInfo(info);

		Assert.assertEquals(2, wrapper.getProperties().size());
		assertPropertyExist("quarkus.cache.caffeine.A.initial-capacity", wrapper);
		assertPropertyExist("quarkus.cache.caffeine.B.initial-capacity", wrapper);

		// Update with empty hints
		wrapper.updateSourcesProperties(new ArrayList<>(), new ArrayList<>());
		int sysEnvPropertiesNumber = System.getProperties().size() + System.getenv().size();
		Assert.assertEquals(0 + sysEnvPropertiesNumber, wrapper.getProperties().size());
		wrapper.updateSourcesProperties(new ArrayList<>(), new ArrayList<>());
		sysEnvPropertiesNumber = System.getProperties().size() + System.getenv().size();
		Assert.assertEquals(0 + sysEnvPropertiesNumber, wrapper.getProperties().size());
		
		// Update with 3 hints
		value = new ValueHint();
		value.setValue("C");
		hint.getValues().add(value);

		wrapper.updateSourcesProperties(new ArrayList<>(), Arrays.asList(hint));
		sysEnvPropertiesNumber = System.getProperties().size() + System.getenv().size();
		Assert.assertEquals(3 + sysEnvPropertiesNumber, wrapper.getProperties().size());
		assertPropertyExist("quarkus.cache.caffeine.A.initial-capacity", wrapper);
		assertPropertyExist("quarkus.cache.caffeine.B.initial-capacity", wrapper);
		assertPropertyExist("quarkus.cache.caffeine.C.initial-capacity", wrapper);

	}

	private void assertPropertyExist(String propertyName, ExtendedMicroProfileProjectInfo wrapper) {
		Assert.assertTrue(wrapper.getProperties().stream().anyMatch(p -> propertyName.equals(p.getName())));
	}

}
