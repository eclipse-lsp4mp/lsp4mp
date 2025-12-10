/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.services.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintKind;
import org.eclipse.lsp4j.InlayHintLabelPart;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4mp.commons.MicroProfileInlayHintSettings;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.metadata.ItemMetadata;
import org.eclipse.lsp4mp.commons.runtime.MicroProfileProjectRuntime;
import org.eclipse.lsp4mp.commons.runtime.converter.ConverterValidator;
import org.eclipse.lsp4mp.commons.utils.ConfigSourcePropertiesProviderUtils;
import org.eclipse.lsp4mp.commons.utils.IConfigSourcePropertiesProvider;
import org.eclipse.lsp4mp.commons.utils.PropertyValueExpander;
import org.eclipse.lsp4mp.commons.utils.StringUtils;
import org.eclipse.lsp4mp.extensions.ExtendedMicroProfileProjectInfo;
import org.eclipse.lsp4mp.ls.commons.BadLocationException;
import org.eclipse.lsp4mp.model.Node;
import org.eclipse.lsp4mp.model.Node.NodeType;
import org.eclipse.lsp4mp.model.PropertiesModel;
import org.eclipse.lsp4mp.model.Property;
import org.eclipse.lsp4mp.model.PropertyKey;
import org.eclipse.lsp4mp.model.PropertyValue;
import org.eclipse.lsp4mp.settings.MicroProfileExecutionSettings;
import org.eclipse.lsp4mp.utils.PropertiesFileUtils;

/**
 * The properties file inlay hint support.
 * 
 * Given this properties file:
 * 
 * <code>
    server.url=https://${host}:${port:8080}/${endpoint}
	host=microprofile.io
	app=project
	service=eclipse/microprofile-config
	endpoint=${app}/${service}
 * </code>
 * 
 * Inlay hint will be displayed for the 2 properties which have expression:
 * 
 * <code>
    server.url=https://${host}:${port:8080}/${endpoint} [https://microprofile.io:8080/project/eclipse/microprofile-config]
	host=microprofile.io
	app=project
	service=eclipse/microprofile-config
	endpoint=${app}/${service} [eclipse/microprofile-config]
 * </code>
 */

class PropertiesFileInlayHint {

	private static final Logger LOGGER = Logger.getLogger(PropertiesFileInlayHint.class.getName());

	public List<InlayHint> getInlayHint(PropertiesModel document, MicroProfileProjectInfo projectInfo, Range range,
			MicroProfileInlayHintSettings inlayHintSettings, MicroProfileExecutionSettings executionSettings,
			CancelChecker cancelChecker) {
		List<ItemMetadata> metadatas = projectInfo != null && projectInfo.getProperties() != null
				? projectInfo.getProperties()
				: Collections.emptyList();
		MicroProfileProjectRuntime projectRuntime = null;
		if (projectInfo instanceof ExtendedMicroProfileProjectInfo) {
			projectRuntime = ((ExtendedMicroProfileProjectInfo) projectInfo).getProjectRuntime();
		}
		boolean showResolveExpressions = inlayHintSettings.getResolveExpressions().isEnabled();
		boolean showConverters = inlayHintSettings.getConverters().isEnabled();
		boolean showTypes = inlayHintSettings.getTypes().isEnabled();

		List<InlayHint> hints = new ArrayList<>();
		List<Node> children = document.getChildren();
		for (Node child : children) {
			cancelChecker.checkCanceled();
			if (child.getNodeType() == NodeType.PROPERTY) {
				Property property = (Property) child;
				PropertyKey propertyKey = property.getKey();
				if (propertyKey != null) {

					if (showConverters || showTypes) {
						ItemMetadata metadata = PropertiesFileUtils.getProperty(property.getPropertyName(),
								projectInfo);
						if (metadata != null) {
							String type = metadata.getType();
							if (!StringUtils.isEmpty(type)) {
								ConverterValidator converterValidator = null;
								if (showConverters && projectRuntime != null) {
									converterValidator = projectRuntime.findConverterValidator(type, projectInfo,
											executionSettings.getExecutionMode());
								}

								try {
									InlayHint hint = new InlayHint();

									List<InlayHintLabelPart> label = new ArrayList<>();
									if (showTypes) {
										// Show Java types
										label.add(new InlayHintLabelPart(" :"));
										InlayHintLabelPart typeLabel = new InlayHintLabelPart(metadata.getSimpleType());
										label.add(typeLabel);
									}
									if (converterValidator != null
											&& converterValidator.getConverterSimpleClassName() != null) {
										// Show converter
										if (label.isEmpty()) {
											label.add(new InlayHintLabelPart(" :"));
										} else {
											label.add(new InlayHintLabelPart(" - "));
										}
										InlayHintLabelPart converterLabel = new InlayHintLabelPart(
												converterValidator.getConverterSimpleClassName());
										label.add(converterLabel);
									}

									hint.setLabel(label);
									hint.setKind(InlayHintKind.Type);
									Position pos = document.positionAt(propertyKey.getEnd());
									hint.setPosition(pos);
									hints.add(hint);
								} catch (BadLocationException e) {
									LOGGER.log(Level.SEVERE, "PropertiesFileInlayHint, position error", e);
								}

							}
						}
					}

					PropertyValue valueNode = property.getValue();
					if (valueNode != null) {

						if (showResolveExpressions && valueNode.hasExpression()) {
							// The current property has a value with expression:
							// ex : server.url=https://${host}:${port:8080}/${endpoint}
							IConfigSourcePropertiesProvider propertiesProvider = ConfigSourcePropertiesProviderUtils
									.layer(document, new PropertiesInfoPropertiesProvider(metadatas));
							PropertyValueExpander expander = new PropertyValueExpander(propertiesProvider);
							String resolved = expander.getValue(property.getKey().getPropertyNameWithProfile());
							if (resolved != null) {
								try {
									// The expression 'https://${host}:${port:8080}/${endpoint}' can be resolved
									// ex : https://microprofile.io:8080/project/eclipse/microprofile-config
									// Display this resolved with inlay hint:
									// server.url=https://${host}:${port:8080}/${endpoint}
									// [https://microprofile.io:8080/project/eclipse/microprofile-config]
									InlayHint hint = new InlayHint();
									hint.setLabel(" " + resolved);
									hint.setKind(InlayHintKind.Parameter);
									Position pos = document.positionAt(valueNode.getEnd());
									hint.setPosition(pos);
									hints.add(hint);
								} catch (BadLocationException e) {
									LOGGER.log(Level.SEVERE, "PropertiesFileInlayHint, position error", e);
								}
							}
						}
					}
				}
			}
		}
		return hints;
	}
}