/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.jdt.core.jaxrs;

import org.eclipse.lsp4mp.jdt.core.jaxrs.JaxRsUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * JAX-RS utilities tests.
 *
 * @author Angelo ZERR
 *
 */
public class JaxRsUtilsTest {

	@Test
	public void buildURL() {

		String actual = JaxRsUtils.buildURL("http://locatlhost:8080/", "/v2", "rest");
		Assert.assertEquals("http://locatlhost:8080/v2/rest", actual);

		actual = JaxRsUtils.buildURL("http://locatlhost:8080", "/v2", "rest");
		Assert.assertEquals("http://locatlhost:8080/v2/rest", actual);

		actual = JaxRsUtils.buildURL("http://locatlhost:8080/", "v2", "rest");
		Assert.assertEquals("http://locatlhost:8080/v2/rest", actual);

		actual = JaxRsUtils.buildURL("http://locatlhost:8080/", "/v2/", "rest");
		Assert.assertEquals("http://locatlhost:8080/v2/rest", actual);

		actual = JaxRsUtils.buildURL("http://locatlhost:8080/", "/v2/", "/rest");
		Assert.assertEquals("http://locatlhost:8080/v2/rest", actual);

	}
}
