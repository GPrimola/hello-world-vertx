package edu.giorgio;

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
		options.setConfig(new JsonObject().put("http.port", randomHttpPort));

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
}
