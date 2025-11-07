/*******************************************************************************
* Copyright (c) 2025 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.commons.runtime;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterRuntimeSupportApi;
import org.junit.Assert;

/**
 * Abstract class to test project runtime.
 */
public abstract class AbstractProjectRuntimeTest {

	private final MicroProfileProjectRuntime projectRuntime;
	private ExecutionMode executionMode;

	public AbstractProjectRuntimeTest(MicroProfileProjectRuntime projectRuntime, ExecutionMode executionMode) {
		this.projectRuntime = projectRuntime;
		this.executionMode = executionMode;
	}

	public void assertValiateWithConverter(String value, String type, String... expectedMessages) {
		ConverterRuntimeSupportApi converterRuntimeSupport = projectRuntime
				.getRuntimeSupport(ConverterRuntimeSupportApi.class, executionMode);
		final List<String> actualMessages = new ArrayList<>();
		converterRuntimeSupport.validate(value, type, (errorMessage, source, errorCode, start, end) -> {
			actualMessages.add(errorMessage);
		});
		Assert.assertArrayEquals("", expectedMessages, actualMessages.toArray());
	}
}
