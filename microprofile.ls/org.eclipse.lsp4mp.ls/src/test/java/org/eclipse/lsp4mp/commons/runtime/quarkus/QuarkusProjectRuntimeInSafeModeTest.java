package org.eclipse.lsp4mp.commons.runtime.quarkus;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.QUARKUS_PROJECT_RUNTIME;

import org.eclipse.lsp4mp.commons.runtime.AbstractMicroProfileProjectRuntimeTest;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.junit.Test;

public class QuarkusProjectRuntimeInSafeModeTest extends AbstractMicroProfileProjectRuntimeTest {

	public QuarkusProjectRuntimeInSafeModeTest() {
		super(QUARKUS_PROJECT_RUNTIME, ExecutionMode.SAFE);
	}
	
	@Test
	public void testEnumFromJAR() {
		super.testEnumFromJAR(false);
	}

}
