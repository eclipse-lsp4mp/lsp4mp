package org.eclipse.lsp4mp.commons.runtime.converter;

import org.eclipse.lsp4mp.commons.runtime.MicroProfileRuntimeSupport;

public interface ConverterRuntimeSupportApi extends MicroProfileRuntimeSupport {

	/**
	 * Returns true if classpath hosts an implementation of MicroProfile
	 * ConfigProviderResolver and false otherwise.
	 * 
	 * @return true if classpath hosts an implementation of MicroProfile
	 *         ConfigProviderResolver and false otherwise.
	 */
	boolean hasConfigProviderResolver();
	
	void validate(String value, String type, DiagnosticsCollector collector);


}
