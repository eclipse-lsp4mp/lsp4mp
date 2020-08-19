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
package org.eclipse.lsp4mp.jdt.internal.core.project;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;
import org.yaml.snakeyaml.Yaml;

/**
 * Yaml config source implementation
 *
 * @author dakwon
 *
 */
public class YamlConfigSource extends AbstractConfigSource<Map<String, Object>> {

	public YamlConfigSource(String configFileName, IJavaProject javaProject) {
		super(configFileName, javaProject);
	}

	@Override
	protected Map<String, Object> loadConfig(InputStream input) throws IOException {
		Yaml yaml = new Yaml();
		return (Map<String, Object>) yaml.load(input);
	}

	@Override
	protected String getProperty(String key, Map<String, Object> config) {
		String[] keyArray = key.split("\\.");
		Map<String, Object> curr = config;

		Object value;
		for (int i = 0; i < keyArray.length - 1; i++) {
			value = curr.get(keyArray[i]);
			if (value == null || value instanceof String) {
				return null;
			}

			curr = (Map<String, Object>) value;
		}

		value = curr.get(keyArray[keyArray.length - 1]);
		if (value instanceof Map) {
			// In this case:
			//
			// cors:
			// ~: true
			//
			// map.get(null) returns the value of ~
			value = ((Map<String, Object>) value).get(null);
		}

		if (value == null) {
			return null;
		}

		return String.valueOf(value);
	}

}
