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
package org.eclipse.lsp4mp.jdt.core;

import static org.eclipse.lsp4mp.jdt.core.utils.JDTTypeUtils.getPropertyType;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.lsp4mp.commons.metadata.ItemHint;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.metadata.ValueHint;

/**
 * Abstract class for properties provider.
 *
 * @author Angelo ZERR
 *
 */
public abstract class AbstractPropertiesProvider implements IPropertiesProvider {

	/**
	 * Returns the Java search pattern.
	 *
	 * @return the Java search pattern.
	 */
	protected abstract String[] getPatterns();

	/**
	 * Return an instance of search pattern.
	 */
	public SearchPattern createSearchPattern() {
		SearchPattern leftPattern = null;
		String[] patterns = getPatterns();

		if (patterns == null) {
			return null;
		}

		for (String pattern : patterns) {
			if (leftPattern == null) {
				leftPattern = createSearchPattern(pattern);
			} else {
				SearchPattern rightPattern = createSearchPattern(pattern);
				if (rightPattern != null) {
					leftPattern = SearchPattern.createOrPattern(leftPattern, rightPattern);
				}
			}
		}
		return leftPattern;
	}

	/**
	 * Create an instance of search pattern with the given <code>pattern</code>.
	 *
	 * @param pattern the search pattern
	 * @return an instance of search pattern with the given <code>pattern</code>.
	 */
	protected abstract SearchPattern createSearchPattern(String pattern);

	/**
	 * Create a search pattern for the given <code>annotationName</code> annotation
	 * name.
	 *
	 * @param annotationName the annotation name to search.
	 * @return a search pattern for the given <code>annotationName</code> annotation
	 *         name.
	 */
	protected static SearchPattern createAnnotationTypeReferenceSearchPattern(String annotationName) {
		return SearchPattern.createPattern(annotationName, IJavaSearchConstants.ANNOTATION_TYPE,
				IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE, SearchPattern.R_EXACT_MATCH);
	}

	/**
	 * Create a search pattern for the given <code>className</code> class name.
	 *
	 * @param annotationName the class name to search.
	 * @return a search pattern for the given <code>className</code> class name.
	 */
	protected static SearchPattern createAnnotationTypeDeclarationSearchPattern(String annotationName) {
		return SearchPattern.createPattern(annotationName, IJavaSearchConstants.ANNOTATION_TYPE,
				IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH);
	}

	/**
	 * Add item metadata.
	 *
	 * @param collector     the properties collector.
	 * @param name          the property name.
	 * @param type          the type of the property.
	 * @param description   the description of the property.
	 * @param sourceType    the source type (class or interface) of the property.
	 * @param sourceField   the source field (field name) and null otherwise.
	 * @param sourceMethod  the source method (signature method) and null otherwise.
	 * @param defaultValue  the default value and null otherwise.
	 * @param extensionName the extension name and null otherwise.
	 * @param binary        true if the property comes from a JAR and false
	 *                      otherwise.
	 * @param phase         the MicroProfile config phase.
	 * @return the item metadata.
	 */
	protected ItemMetadata addItemMetadata(IPropertiesCollector collector, String name, String type, String description,
			String sourceType, String sourceField, String sourceMethod, String defaultValue, String extensionName,
			boolean binary, int phase) {
		return collector.addItemMetadata(name, type, description, sourceType, sourceField, sourceMethod, defaultValue,
				extensionName, binary, phase);
	}

	/**
	 * Add item metadata.
	 *
	 * @param collector     the properties collector.
	 * @param name          the property name.
	 * @param type          the type of the property.
	 * @param description   the description of the property.
	 * @param sourceType    the source type (class or interface) of the property.
	 * @param sourceField   the source field (field name) and null otherwise.
	 * @param sourceMethod  the source method (signature method) and null otherwise.
	 * @param defaultValue  the default value and null otherwise.
	 * @param extensionName the extension name and null otherwise.
	 * @param binary        true if the property comes from a JAR and false
	 *                      otherwise.
	 * @return the item metadata.
	 */
	protected ItemMetadata addItemMetadata(IPropertiesCollector collector, String name, String type, String description,
			String sourceType, String sourceField, String sourceMethod, String defaultValue, String extensionName,
			boolean binary) {
		return addItemMetadata(collector, name, type, description, sourceType, sourceField, sourceMethod, defaultValue,
				extensionName, binary, 0);
	}

	/**
	 * Get or create the update hint from the given type.
	 *
	 * @param collector
	 * @param type        the JDT type and null otherwise.
	 * @param typeName    the type name which is the string of the JDT type.
	 * @param javaProject the java project where the JDT type belong to.
	 * @return the hint name.
	 * @throws JavaModelException
	 */
	protected String updateHint(IPropertiesCollector collector, IType type) throws JavaModelException {
		if (type == null) {
			return null;
		}
		if (type.isEnum()) {
			// Register Enumeration in "hints" section
			String hint = getPropertyType(type, null);
			if (!collector.hasItemHint(hint)) {
				ItemHint itemHint = collector.getItemHint(hint);
				itemHint.setSourceType(hint);
				if (!type.isBinary()) {
					itemHint.setSource(Boolean.TRUE);
				}
				IJavaElement[] children = type.getChildren();
				for (IJavaElement c : children) {
					if (c.getElementType() == IJavaElement.FIELD && ((IField) c).isEnumConstant()) {
						String enumName = ((IField) c).getElementName();
						// TODO: extract Javadoc
						ValueHint value = new ValueHint();
						value.setValue(enumName);
						itemHint.getValues().add(value);
					}
				}
			}
			return hint;
		}
		return null;
	}
	
	/**
	 * Returns true if the given Java element has already generated MicroProfile
	 * properties for this provider and false otherwise.
	 * 
	 * This check allows to prevent from duplicate properties.
	 * 
	 * @param element the Java element collected by the search engine to generate
	 *                some MicroProfile properties.
	 * @param context the search context.
	 * @return true if the given Java element has already generated MicroProfile
	 *         properties for this provider and false otherwise.
	 */
	protected boolean isAlreadyProcessed(Object element, SearchContext context) {
		if (element == null) {
			return true;
		}
		String key = this.getClass().getName();
		Set<Object> elements = (Set<Object>) context.get(key);
		if (elements == null) {
			elements = new HashSet<>();
			context.put(key, elements);
		}
		return !elements.add(element);
	}

}
