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
package org.eclipse.lsp4mp.extensions.reactivemessaging;

import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.c;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.load;
import static org.eclipse.lsp4mp.services.properties.PropertiesFileAssert.r;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.ls.commons.BadLocationException;
import org.eclipse.lsp4mp.services.properties.PropertiesFileAssert;
import org.junit.Test;

/**
 * Test Microprofile Reactive Messaging completion in 'application.properties'
 * file.
 * 
 * @author Angelo ZERR
 *
 */
public class MicroProfileReactiveMessagingCompletionTest {

	private static MicroProfileProjectInfo DEFAULT_PROJECT;

	@Test
	public void dynamic() throws BadLocationException {
		String value = "|";
		testCompletionFor(value, true, null,
				c("mp.messaging.incoming.prices.connector",
						"mp.messaging.incoming.prices.connector=${1|smallrye-kafka,smallrye-amqp|}", r(0, 0, 0)), //
				c("mp.messaging.outgoing.my-data-stream.connector",
						"mp.messaging.outgoing.my-data-stream.connector=${1|smallrye-kafka,smallrye-amqp|}",
						r(0, 0, 0)), //
				c("mp.messaging.outgoing.generated-price.connector",
						"mp.messaging.outgoing.generated-price.connector=${1|smallrye-kafka,smallrye-amqp|}",
						r(0, 0, 0)), //
				c("mp.messaging.emitter.default-buffer-size", "mp.messaging.emitter.default-buffer-size=${0:128}",
						r(0, 0, 0)) //
		);

		value = "mp.messaging.incoming.prices.connector=smallrye-kafka\n" + //
				"|";
		testCompletionFor(value, true, null,
				c("mp.messaging.outgoing.my-data-stream.connector",
						"mp.messaging.outgoing.my-data-stream.connector=${1|smallrye-kafka,smallrye-amqp|}",
						r(1, 0, 0)), //
				c("mp.messaging.outgoing.generated-price.connector",
						"mp.messaging.outgoing.generated-price.connector=${1|smallrye-kafka,smallrye-amqp|}",
						r(1, 0, 0)), //
				c("mp.messaging.emitter.default-buffer-size", "mp.messaging.emitter.default-buffer-size=${0:128}",
						r(1, 0, 0)), //
				c("mp.messaging.incoming.prices.topic", "mp.messaging.incoming.prices.topic=$0", r(1, 0, 0)), //
				c("mp.messaging.incoming.prices.bootstrap.servers",
						"mp.messaging.incoming.prices.bootstrap.servers=${0:localhost:9092}", r(1, 0, 0)));
	}

	@Test
	public void noExistChannel() throws BadLocationException {
		String value = "mp.messaging.incoming.XXXXXX.connector=smallrye-kafka\n" + //
				"|";
		testCompletionFor(value, true, null, //
				c("mp.messaging.incoming.prices.connector",
						"mp.messaging.incoming.prices.connector=${1|smallrye-kafka,smallrye-amqp|}", r(1, 0, 0)), //
				c("mp.messaging.outgoing.my-data-stream.connector",
						"mp.messaging.outgoing.my-data-stream.connector=${1|smallrye-kafka,smallrye-amqp|}",
						r(1, 0, 0)), //
				c("mp.messaging.outgoing.generated-price.connector",
						"mp.messaging.outgoing.generated-price.connector=${1|smallrye-kafka,smallrye-amqp|}",
						r(1, 0, 0)), //
				c("mp.messaging.emitter.default-buffer-size", "mp.messaging.emitter.default-buffer-size=${0:128}",
						r(1, 0, 0)));
	}

	private static void testCompletionFor(String value, boolean snippetSupport, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		PropertiesFileAssert.testCompletionFor(value, snippetSupport, null, expectedCount,
				getMicroProfileReactiveMessagingProjectInfo(), expectedItems);
	}

	private static MicroProfileProjectInfo getMicroProfileReactiveMessagingProjectInfo() {
		if (DEFAULT_PROJECT == null) {
			DEFAULT_PROJECT = load(MicroProfileReactiveMessagingCompletionTest.class
					.getResourceAsStream("mp-reactive-messaging-properties.json"));
		}
		return DEFAULT_PROJECT;
	}

}
