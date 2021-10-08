package com.kry.servicepoller.backend.simulators;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Router;

import java.util.Random;

public class ServiceA extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceA.class);
    private static final int PORT = 9090;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);

        router.get("/health").handler(ctx -> {
            Random random = new Random();
            String status = random.nextInt(10) < 8 ? "OK" : "FAIL";
            HttpServerResponse response = ctx.response();
            response.putHeader("Content-Type", "text/plain");
            response.setStatusCode(200);
            response.end(status);
        });

        httpServer.requestHandler(router).listen(PORT, http -> {
            if (http.succeeded()) {
                startPromise.complete();
                LOG.info("HTTP 'ServiceA' started on port " + PORT);
            } else {
                startPromise.fail(http.cause());
            }
        });
    }
}
