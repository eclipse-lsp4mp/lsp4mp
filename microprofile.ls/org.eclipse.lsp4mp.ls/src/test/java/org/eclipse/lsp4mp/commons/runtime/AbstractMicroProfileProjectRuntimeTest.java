package org.eclipse.lsp4mp.commons.runtime;

import org.junit.Test;

/**
 * Test MicroProfile converter validation with Quarkus project runtime
 * (Converters are registered with SmallRye).
 */
public abstract class AbstractMicroProfileProjectRuntimeTest extends AbstractProjectRuntimeTest {

	public AbstractMicroProfileProjectRuntimeTest(MicroProfileProjectRuntime projectRuntime,
			ExecutionMode executionMode) {
		super(projectRuntime, executionMode);
	}

	@Test
	public void testInt() {
		assertValiateWithConverter("1", "int");
		assertValiateWithConverter("1X", "int", "SRCFG00029: Expected an integer value, got \"1X\"");
	}

	@Test
	public void testInteger() {
		assertValiateWithConverter("1", "java.lang.Integer");
		assertValiateWithConverter("1X", "java.lang.Integer", "SRCFG00029: Expected an integer value, got \"1X\"");
	}

	@Test
	public void testIntArray() {
		assertValiateWithConverter("1,2,3,4", "int[]");
		assertValiateWithConverter("1,2X,3x,4", "int[]", //
				"SRCFG00029: Expected an integer value, got \"2X\"", //
				"SRCFG00029: Expected an integer value, got \"3x\"");
	}

	@Test
	public void testIntegerArray() {
		assertValiateWithConverter("1,2,3,4", "java.lang.Integer[]");
		assertValiateWithConverter("1,2X,3x,4", "java.lang.Integer[]", //
				"SRCFG00029: Expected an integer value, got \"2X\"", //
				"SRCFG00029: Expected an integer value, got \"3x\"");
	}

	@Test
	public void testIntegerList() {
		assertValiateWithConverter("1,2,3,4", "java.util.List<java.lang.Integer>");
		assertValiateWithConverter("1,2X,3x,4", "java.lang.Integer[]", //
				"SRCFG00029: Expected an integer value, got \"2X\"", //
				"SRCFG00029: Expected an integer value, got \"3x\"");
	}

	@Test
	public void testClassObject() {
		assertValiateWithConverter("java.lang.String", "java.lang.Class<java.lang.Object>");
	}

	protected void testEnumFromJAR(boolean supportsError) {
		// Enum value valid
		assertValiateWithConverter("FOO", "org.acme.MyEnum");
		if (supportsError) {
			// Enum can be validated in full mode
			assertValiateWithConverter("BLACK", "org.jboss.logmanager.handlers.AsyncHandler$OverflowAction",
					"SRCFG00049: Cannot convert BLACK to enum class org.jboss.logmanager.handlers.AsyncHandler$OverflowAction, allowed values: discard,block");
		} else {
			// Custom enum cannot be validated in safe mode, because it doesn't know the
			// project classpath
			assertValiateWithConverter("BLACK", "org.jboss.logmanager.handlers.AsyncHandler$OverflowAction");
		}
	}

	@Test
	public void testEnumFromClasses() {
		// Enum value valid
		assertValiateWithConverter("FOO", "org.acme.MyEnum");
		if (getExecutionMode() == ExecutionMode.FULL) {
			// Custom enum can be validated in full mode
			assertValiateWithConverter("FOOX", "org.acme.MyEnum",
					"SRCFG00049: Cannot convert FOOX to enum class org.acme.MyEnum, allowed values: bar,foo");
		} else {
			// Custom enum cannot be validated in safe mode, because it doesn't know the
			// project classpath
			assertValiateWithConverter("FOOX", "org.acme.MyEnum");
		}
	}
}
