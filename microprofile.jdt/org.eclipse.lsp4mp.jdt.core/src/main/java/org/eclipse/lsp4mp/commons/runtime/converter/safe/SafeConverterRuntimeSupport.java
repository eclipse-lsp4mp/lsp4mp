package org.eclipse.lsp4mp.commons.runtime.converter.safe;

import java.lang.reflect.Type;

import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.runtime.converter.AbstractConverterRuntimeSupport;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;
import org.eclipse.microprofile.config.Config;

import io.smallrye.config.SmallRyeConfigBuilder;

public class SafeConverterRuntimeSupport extends AbstractConverterRuntimeSupport<Config> {

	public SafeConverterRuntimeSupport(MicroProfileProjectRuntime project) {
		super(project, ExecutionMode.SAFE);
	}

	@Override
	protected Config loadConfig() {
		// Load SmallRye Config
		return new SmallRyeConfigBuilder() //
				.build();
	}

	@Override
	protected ConverterValidator newConverter(Config config, Type type) {
		return new SafeConverterValidator(config, type);
	}

}
