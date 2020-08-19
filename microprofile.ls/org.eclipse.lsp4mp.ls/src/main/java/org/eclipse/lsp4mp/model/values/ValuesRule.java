/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.model.values;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lsp4mp.commons.metadata.ItemHint.ValueHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.model.PropertiesModel;

/**
 * Values rule to manage values for some property. A values rule is composed by
 *
 * <ul>
 * <li>a matcher to know if a given property match the values rule</li>
 * <li>the list of values {@link ValueHint}.</li>
 * </ul>
 *
 * @author Angelo ZERR
 *
 */
public class ValuesRule {

	private PropertyMatcher matcher;

	private List<ValueHint> values;

	private List<String> valuesRef;

	private transient boolean valuesCleaned;

	public PropertyMatcher getMatcher() {
		return matcher;
	}

	public void setMatcher(PropertyMatcher matcher) {
		this.matcher = matcher;
	}

	public void setValues(List<ValueHint> values) {
		this.values = values;
		this.valuesCleaned = false;
	}

	public List<ValueHint> getValues() {
		cleanValues();
		return values;
	}

	private void cleanValues() {
		if (valuesCleaned || values == null || values.isEmpty()) {
			return;
		}
		try {
			values = values.stream().filter(e -> e.getValue() != null && !e.getValue().isEmpty())
					.collect(Collectors.toList());
		} finally {
			valuesCleaned = true;
		}
	}

	public List<String> getValuesRef() {
		return valuesRef;
	}

	/**
	 * Returns true if the given metadata property match the property matcher and
	 * false otherwise.
	 *
	 * @param metadata the metadata property to match
	 * @param model    the properties model
	 * @return true if the given metadata property match the property matcher and
	 *         false otherwise.
	 */
	public boolean match(ItemMetadata metadata, PropertiesModel model) {
		return getMatcher().match(metadata, model);
	}

}
