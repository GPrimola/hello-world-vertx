package edu.giorgio;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MyFirstVerticle extends AbstractVerticle {

	@Override
	public void start(Future<Void> future) {
		vertx
		.createHttpServer()
		.requestHandler(request -> {
			Logger log = LoggerFactory.getLogger(MyFirstVerticle.class);
			log.info("Received request from " + request.remoteAddress());
			request.response()
			.end("<h1>Hello World Vert.x 3 application!</h1>");
		})
		.listen(httpPort(), result -> {
			if(result.succeeded()) {
				future.complete();
			} else {
				future.fail(result.cause());
			}
		});
	}
	
	private Integer httpPort() {
		return config().getInteger("http.port", 8080);
	}

}
