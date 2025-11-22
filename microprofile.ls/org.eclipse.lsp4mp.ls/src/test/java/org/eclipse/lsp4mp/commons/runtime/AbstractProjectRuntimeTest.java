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

import java.util.ArrayList;
import java.util.List;

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
	
	public ExecutionMode getExecutionMode() {
		return executionMode;
	}
}
