package com.kry.servicepoller;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Router;

import java.util.Random;

public class ServiceC extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceC.class);
    private static final int PORT = 9092;

    @Override
    public void start(Promise<Void> startPromise) {
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.get("/health").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            response.putHeader("Content-Type", "text/plain");
            response.setStatusCode(200);
            response.end("OK");
        });

        httpServer.requestHandler(router).listen(PORT, http -> {
            if (http.succeeded()) {
                startPromise.complete();
                LOG.info("HTTP 'ServiceC' started on port " + PORT);
            } else {
                startPromise.fail(http.cause());
            }
        });
    }
}
