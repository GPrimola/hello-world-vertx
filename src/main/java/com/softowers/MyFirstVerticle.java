package com.softowers;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MyFirstVerticle extends AbstractVerticle {
	
	private Map<Integer, Whisky> products = new LinkedHashMap<>();
	private JDBCClient jdbc;
	private static final Logger logger = LoggerFactory.getLogger(MyFirstVerticle.class);

	private Integer httpPort() {
		return config().getInteger("http.port", 8080);
	}

	private void createData(AsyncResult<SQLConnection> asyncResult,
							Handler<AsyncResult<Void>> next,
							Future<Void> future) {
		if(asyncResult.failed()) {
			future.fail(asyncResult.cause());
		} else {
			SQLConnection connection = asyncResult.result();
			connection.execute(
					"CREATE TABLE IF NOT EXISTS " +
							"whiskies (" +
								"id INTEGER IDENTITY, name varchar(100), origin varchar(100)" +
							")",
					connectionResult -> {
						if(connectionResult.failed()) {
							future.fail(connectionResult.cause());
							connection.close();
							return;
						}

						connection.query("SELECT * FROM whiskies", select -> {
							if(select.failed()) {
								future.fail(select.cause());
								connection.close();
								return;
							}

							if(select.result().getNumRows() == 0) {
								Whisky whisky1 = new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay");
								Whisky whisky2 = new Whisky("Talisker 57° North", "Scotland, Island");
								insert(whisky1, connection,
										v -> insert(whisky2, connection,
												insertResult -> {
													next.handle(Future.succeededFuture());
													connection.close();
												}));
							} else {
								next.handle(Future.succeededFuture());
								connection.close();
							}
						});
					});
		}
	}

	private void insert(Whisky whisky, SQLConnection connection, Handler<AsyncResult<Whisky>> next) {
		String insertSQL = "INSERT INTO whiskies (name, origin) VALUES (?, ?)";

		connection.updateWithParams(insertSQL,
				new JsonArray().add(whisky.getName()).add(whisky.getOrigin()),
				sqlResult -> {
					if(sqlResult.failed()) {
						next.handle(Future.failedFuture(sqlResult.cause()));
						return;
					}

					UpdateResult result = sqlResult.result();
					Whisky w = new Whisky(result.getKeys().getInteger(0), whisky.getName(), whisky.getOrigin());
					next.handle(Future.succeededFuture(w));
				}
		);
	}

	private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> future) {
		jdbc.getConnection(jdbcConnection -> {
			if(jdbcConnection.failed()) {
				future.fail(jdbcConnection.cause());
			} else {
				next.handle(Future.succeededFuture(jdbcConnection.result()));
			}
		});
	}

	private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
		vertx
			.createHttpServer()
			.requestHandler(routesHandler())
			.listen(httpPort(),
				next::handle);
	}

	private void completeStartup(AsyncResult<HttpServer> http, Future<Void> future) {
		if(http.failed()) {
			future.fail(http.cause());
		} else {
			future.complete();
		}
	}
	
	@Override
	public void start(Future<Void> future) {
		jdbc = JDBCClient.createShared(vertx, config(), "RestApplication");

		startBackend(
			(connection) -> createData(connection,
				(nothing) -> startWebApp(
					(http) -> completeStartup(http, future)
				), future
			), future);
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
		jdbc.getConnection(asyncResult -> {
			SQLConnection connection = asyncResult.result();

			connection.query("SELECT * FROM whiskies", queryResult -> {
				List<Whisky> whiskies =
					queryResult
						.result()
						.getRows()
						.stream()
						.map(Whisky::new)
						.collect(Collectors.toList());

				context.response()
					.putHeader("content-type", "application/json; charset=utf-8")
					.end(Json.encodePrettily(whiskies));

				connection.close();
			});
		});
	}
	
	private void create(RoutingContext context) {
		jdbc.getConnection(ar -> {
			SQLConnection connection = ar.result();
			Whisky newWhisky = Json.decodeValue(context.getBodyAsString(), Whisky.class);
			insert(newWhisky, connection, insertResult -> {
				context.response()
					.setStatusCode(201)
					.putHeader("content-type", "application/json; charset=utf-8")
					.end(Json.encodePrettily(insertResult.result()));
			});
		});
	}

	private void find(RoutingContext context) {
		jdbc.getConnection(ar -> {
			SQLConnection connection = ar.result();
			String idParam = context.request().getParam("id");
			if(idParam == null) {
				context.response().setStatusCode(400).end();
			} else {
				Integer id = Integer.parseInt(idParam);
				connection
					.queryWithParams("SELECT * FROM whiskies WHERE ID = ?", new JsonArray().add(id),
						queryResult -> {
							ResultSet rs = queryResult.result();

							if(rs.getNumRows() == 0) {
								context.response().setStatusCode(404).end();
							} else {
								Whisky whisky = rs.getRows().stream().map(Whisky::new).findFirst().get();
								context.response()
									.putHeader("content-type", "application/json; charset=utf-8")
									.end(Json.encodePrettily(whisky));
							}
						});
			}
		});

	}

	private void update(RoutingContext context) {
		String idParam = context.request().getParam("id");
		if(idParam == null) {
			context.response().setStatusCode(400).end();
		} else {
			Integer id = Integer.parseInt(idParam);

			jdbc.getConnection(ar -> {
				SQLConnection connection = ar.result();
				JsonObject formData = context.getBodyAsJson();
				Whisky whisky = new Whisky(id, formData.getString("name"), formData.getString("origin"));
				connection.updateWithParams("UPDATE whiskies SET name=?, origin=? WHERE id=?",
					new JsonArray().add(whisky.getName()).add(whisky.getOrigin()).add(whisky.getId()),

					queryResult -> {
						if (queryResult.result().getUpdated() == 0) {
							context.response().setStatusCode(404).end();
						} else {
							context.response()
								.putHeader("content-type", "application/json; charset=utf-8")
								.end(Json.encodePrettily(whisky));
						}

					}
				);
			});
		}
	}
	
	private void destroy(RoutingContext context) {
		String idParam = context.request().getParam("id");
		if(idParam == null) {
			context.response().setStatusCode(400).end();
		} else {
			jdbc.getConnection(ar -> {
				SQLConnection connection = ar.result();
				Integer id = Integer.parseInt(idParam);

				connection.updateWithParams("DELETE FROM whiskies WHERE id=?", new JsonArray().add(id),
					queryResult -> {
						if(queryResult.result().getUpdated() == 0) {
							context.response().setStatusCode(404);
						} else {
							context.response().setStatusCode(204).end();
						}
					});
			});
		}
	}

}
