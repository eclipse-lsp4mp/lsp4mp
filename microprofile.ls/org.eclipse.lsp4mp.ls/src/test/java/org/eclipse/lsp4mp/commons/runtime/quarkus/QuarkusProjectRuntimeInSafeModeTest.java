package org.eclipse.lsp4mp.commons.runtime.quarkus;

import org.eclipse.lsp4mp.commons.runtime.AbstractMicroProfileProjectRuntimeTest;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.QUARKUS_PROJECT_RUNTIME;

public class QuarkusProjectRuntimeInSafeModeTest extends AbstractMicroProfileProjectRuntimeTest {

	public QuarkusProjectRuntimeInSafeModeTest() {
		super(QUARKUS_PROJECT_RUNTIME, ExecutionMode.SAFE);
	}

}
