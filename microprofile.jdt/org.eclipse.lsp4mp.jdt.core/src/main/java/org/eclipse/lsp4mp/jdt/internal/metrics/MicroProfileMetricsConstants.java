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
package org.eclipse.lsp4mp.jdt.internal.metrics;

/**
 * MicroProfile Metrics constants
 *
 * @author David Kwon
 *
 */
public class MicroProfileMetricsConstants {

	public static final String METRIC_ID = "org.eclipse.microprofile.metrics.MetricID";

	public static final String GAUGE_ANNOTATION = "org.eclipse.microprofile.metrics.Gauge";

	public static final String DIAGNOSTIC_SOURCE = "microprofile-metrics";

	// CDI Scope Annotations
	public static final String APPLICATION_SCOPED_ANNOTATION = "javax.enterprise.context.ApplicationScoped";
	public static final String REQUEST_SCOPED_ANNOTATION = "javax.enterprise.context.RequestScoped";
	public static final String SESSION_SCOPED_ANNOTATION = "javax.enterprise.context.SessionScoped";
	public static final String DEPENDENT_ANNOTATION = "javax.enterprise.context.Dependent";

	public static final String REQUEST_SCOPED_ANNOTATION_NAME = "RequestScoped";
	public static final String SESSION_SCOPED_ANNOTATION_NAME = "SessionScoped";
	public static final String DEPENDENT_ANNOTATION_NAME = "Dependent";
}
