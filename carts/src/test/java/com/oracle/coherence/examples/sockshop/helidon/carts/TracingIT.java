/*
 * Copyright (c) 2020,2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.sockshop.helidon.carts;

import com.oracle.coherence.common.base.Blocking;
import com.tangosol.net.CacheFactory;
import com.tangosol.util.Base;
import io.helidon.microprofile.server.Server;
import io.jaegertracing.internal.JaegerSpan;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TracingIT {
	protected static Server SERVER;

	@BeforeAll
	static void startServer() {
		SERVER = Server.builder().port(0).build().start();
	}

	@AfterAll
	static void stopServer() {
		SERVER.stop();
	}

	@BeforeEach
	void setup() {
		RestAssured.reset();
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = SERVER.port();
	}

	@Test
	void testTracing() {
		TestBody testBody = () -> {
			given().
					contentType(ContentType.JSON).
					body(new Item("X1", 0, 10f)).
				when().
					post("/carts/{cartId}/items", "C1").
				then().
					statusCode(HttpStatus.SC_CREATED).
					body("itemId", Is.is("X1"),
							"quantity", Is.is(1),
							"unitPrice", Is.is(10f));

			Blocking.sleep(250);
		};
		runTest(testBody);


		JaegerSpan[] localSpans = validateOpsPresent(
				new String[]{"HTTP Request", "Invoke.process"},
				AdaptiveTracerFactory.getReporter().getSpans()
		);
		Arrays.stream(localSpans).forEach(TracingIT::validateTagsForSpan);
	}

	private void runTest(TestBody testBody) {
		try {
			testBody.run();
		} catch (Exception e) {
			throw Base.ensureRuntimeException(e);
		}
	}

	protected static JaegerSpan[] validateOpsPresent(String[] sOpNames, List<JaegerSpan> spans) {
		assertThat(sOpNames, is(notNullValue()));
		assertThat("Spans not recorded.", spans.isEmpty(), is(false));

		JaegerSpan[] spansFound = new JaegerSpan[sOpNames.length];
		for (int i = 0, len = sOpNames.length; i < len; i++) {
			if (spansFound[i] == null) {
				final String searchFor = sOpNames[i];

				spansFound[i] = spans.stream().filter(
						span -> Base.equals(searchFor, span.getOperationName())).findFirst().orElse(null);
			}
		}

		for (int i = 0, len = sOpNames.length; i < len; i++) {
			assertThat("Unable to find operation " + sOpNames[i] + " in spans on the member.",
					spansFound[i],
					is(notNullValue()));
		}
		return spansFound;
	}

	@SuppressWarnings("SameParameterValue")
	protected static void validateTagsForSpan(JaegerSpan span) {
		assertThat(span, is(notNullValue()));
		Map<String, Object> metadata = span.getTags();
		String sOpName = span.getOperationName();

		CacheFactory.log("Validating span: " + span + ", tags: " + span.getTags());

		if (sOpName.startsWith("HTTP Request")) {
			assertThat(sOpName + ": incorrect http path",
					metadata.get("http.url"),
					is("/carts/C1/items"));
		}

		if (sOpName.startsWith("Invoke")) {
			assertThat(sOpName + ": incorrect cache name",
					metadata.get("cache"),
					is("carts"));
		}
	}

	@FunctionalInterface
	protected interface TestBody {
		void run() throws Exception;
	}
}