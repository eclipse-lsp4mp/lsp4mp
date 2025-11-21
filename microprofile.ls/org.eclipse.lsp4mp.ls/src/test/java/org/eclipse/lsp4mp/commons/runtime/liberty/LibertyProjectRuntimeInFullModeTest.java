package org.eclipse.lsp4mp.commons.runtime.liberty;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.LIBERTY_PROJECT_RUNTIME;

import org.eclipse.lsp4mp.commons.runtime.AbstractMicroProfileProjectRuntimeTest;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.junit.Test;

/**
 * Liberty project doesn't host MicroProfile Config implementation in their
 * classpath. This test is used to validate property values in full mode which
 * is not available for liberty and in this case swith to safe mode by using
 * SmallRye Config hosted in the MicroProfile LS.
 */
public class LibertyProjectRuntimeInFullModeTest extends AbstractMicroProfileProjectRuntimeTest {

	public LibertyProjectRuntimeInFullModeTest() {
		super(LIBERTY_PROJECT_RUNTIME, ExecutionMode.FULL);
	}
	
	@Test
	public void testEnumFromJAR() {
		super.testEnumFromJAR(false);
	}

}
