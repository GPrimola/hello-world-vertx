package com.softowers;

import io.vertx.core.json.Json;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.io.IOException;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class MyFirstVerticleTest {
	
	private Vertx vertx;
	private Integer randomHttpPort;
	
	@Before
	public void setUp(TestContext context) throws IOException {
		vertx = Vertx.vertx();

		// Generate a random port to Verticle server
		ServerSocket socket = new ServerSocket(0);
		randomHttpPort = socket.getLocalPort();
		socket.close();

		// Build options to Verticle.config()
		DeploymentOptions options = new DeploymentOptions();
		options.setConfig(new JsonObject()
			.put("http.port", randomHttpPort)
			.put("url", "jdbc:hsqldb:mem:test?shutdown=true")
			.put("driver_class", "org.hsqldb.jdbcDriver")
		);

		vertx.deployVerticle(MyFirstVerticle.class.getName(), options, context.asyncAssertSuccess());
	}
	
	@After
	public void tearDown(TestContext context) {
		vertx.close(context.asyncAssertSuccess());
	}
	
	@Test
	public void testMyFirstVerticleShouldStart(TestContext context) {
		final Async async = context.async();
		
		vertx.createHttpClient()
				.getNow(randomHttpPort, "localhost", "/",
						response -> {
							response.handler(body -> {
								context.assertTrue(body.toString().contains("<h1>Hello World Vert.x 3 application!</h1>"));
								async.complete();
							});
						});
	}

	@Test
	public void checkThatIndexPageIsServed(TestContext context) {
		Async async = context.async();

		vertx.createHttpClient()
				.getNow(randomHttpPort, "localhost", "/assets/index.html",
						response -> {
							context.assertEquals(response.statusCode(), 200);
							context.assertEquals(response.headers().get("content-type"), "text/html");
							response.bodyHandler(body -> {
								context.assertTrue(body.toString().contains("<title>My Whisky Collection</title>"));
								async.complete();
							});
						});
	}

	@Test
	public void checkThatWeCanAddWhisky(TestContext context) {
		Async async = context.async();

		final String whiskyName = "James";
		final String whiskyOrigin = "Ireland";
		final String whiskyJson = Json.encode(new Whisky(whiskyName, whiskyOrigin));
		final String length = Integer.toString(whiskyJson.length());

		vertx.createHttpClient()
				.post(randomHttpPort, "localhost", "/api/whiskies")
				.putHeader("content-type", "application/json; charset=utf-8")
				.putHeader("content-length", length)
				.handler(response -> {
					context.assertEquals(response.statusCode(), 201);
					context.assertTrue(response.headers().get("content-type").contains("application/json"));

					response.bodyHandler(body -> {
						final Whisky whisky = Json.decodeValue(body.toString(), Whisky.class);
						context.assertEquals(whisky.getName(), whiskyName);
						context.assertEquals(whisky.getOrigin(), whiskyOrigin);
						context.assertNotNull(whisky.getId());
						async.complete();
					});
				})
				.write(whiskyJson)
				.end();
	}
}
