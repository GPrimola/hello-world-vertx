package com.softowers;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.Map;

public class MyFirstVerticle extends AbstractVerticle {
	
	private Map<Integer, Whisky> products = new LinkedHashMap<>();
	
	private void createData() {
		Whisky bowmore = new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay");
		products.put(bowmore.getId(), bowmore);
		Whisky talisker = new Whisky("Talisker 57° North", "Scotland, Island");
		products.put(talisker.getId(), talisker);
	}
	
	@Override
	public void start(Future<Void> future) {
		createData();
		
		vertx
		.createHttpServer()
		.requestHandler(routesHandler())
		.listen(httpPort(), result -> {
			if(result.succeeded()) {
				future.complete();
			} else {
				future.fail(result.cause());
			}
		});
	}
	
	private Handler<HttpServerRequest> routesHandler() {
		Router router = Router.router(vertx);
		
		router.route().handler(BodyHandler.create());
		router.route("/assets/*").handler(StaticHandler.create("assets"));
		
		router.get("/api/whiskies").handler(this::index);
		router.post("/api/whiskies").handler(this::create);
		router.get("/api/whiskies/:id").handler(this::find);
		router.put("/api/whiskies/:id").handler(this::update);
		router.delete("/api/whiskies/:id").handler(this::destroy);

		router.route("/").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			
			response
				.putHeader("content-type", "text/html")
				.end("<h1>Hello World Vert.x 3 application!</h1>");
		});
		
		return router::accept;
	}
	
	private void index(RoutingContext context) {
		context
			.response()
			.putHeader("content-type", "application/json; charset=utf-8")
			.end(Json.encodePrettily(products.values()));
	}
	
	private void create(RoutingContext context) {
		Whisky newWhisky = Json.decodeValue(context.getBodyAsString(), Whisky.class);
		products.put(newWhisky.getId(), newWhisky);
		context.response()
			.setStatusCode(201)
			.putHeader("content-type", "application/json; charset=utf-8")
			.end(Json.encodePrettily(newWhisky));
	}

	private void find(RoutingContext context) {
		String idParam = context.request().getParam("id");
		if(idParam == null) {
			context.response().setStatusCode(400).end();
		} else {
			Integer id = Integer.parseInt(idParam);
			if(products.get(id) == null) {
				context.response().setStatusCode(404).end();
			} else {
				context.response()
						.putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encodePrettily(products.get(id)));
			}
		}
	}

	private void update(RoutingContext context) {
		String idParam = context.request().getParam("id");
		if(idParam == null) {
			context.response().setStatusCode(400).end();
		} else {
			Integer id = Integer.parseInt(idParam);
			Whisky whisky = products.get(id);
			if(whisky == null) {
				context.response().setStatusCode(404).end();
			} else {
				JsonObject formData = context.getBodyAsJson();
				whisky.setName(formData.getString("name"));
				whisky.setOrigin(formData.getString("origin"));
				context.response()
						.putHeader("content-type", "application/json; charset=utf-8")
						.end(Json.encodePrettily(whisky));
			}
		}
	}
	
	private void destroy(RoutingContext context) {
		String idParam = context.request().getParam("id");
		if(idParam == null) {
			context.response().setStatusCode(400).end();
		} else {
			Integer id = Integer.parseInt(idParam);
			if(products.get(id) == null) {
				context.response().setStatusCode(404);
			} else {
				products.remove(id);
			}
		}
		
		context.response().setStatusCode(204).end();
	}
	
	private Integer httpPort() {
		return config().getInteger("http.port", 8080);
	}

}
