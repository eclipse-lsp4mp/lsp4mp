package org.eclipse.lsp4mp.commons.runtime.quarkus;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.QUARKUS_PROJECT_RUNTIME;

import org.eclipse.lsp4mp.commons.runtime.AbstractMicroProfileProjectRuntimeTest;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;

public class QuarkusProjectRuntimeInFullModeTest extends AbstractMicroProfileProjectRuntimeTest {

	public QuarkusProjectRuntimeInFullModeTest() {
		super(QUARKUS_PROJECT_RUNTIME, ExecutionMode.FULL);
	}

}
