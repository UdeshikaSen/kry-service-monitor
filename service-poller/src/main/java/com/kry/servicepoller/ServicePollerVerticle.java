package com.kry.servicepoller;

import com.kry.servicepoller.repositories.ServiceRepository;
import com.kry.servicepoller.entities.Service;
import com.kry.servicepoller.entities.ServiceStatus;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;

/**
 * Verticle responsible for polling the backend services to obtain the status.
 */
public class ServicePollerVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(ServicePollerVerticle.class);
    private final ServiceRepository dbCon;

    public ServicePollerVerticle(ServiceRepository dbCon) {
        this.dbCon = dbCon;
    }

    @Override
    public void start(Promise<Void> startPromise) {

        // polling done through the circuit breaker to protect the verticle from misbehaving services
        CircuitBreaker breaker = CircuitBreaker.create("kry-svc-circuit-breaker", vertx,
            new CircuitBreakerOptions().setMaxFailures(5).setTimeout(2000)
        );

        // periodically obtain the status of the backend services stored in the database
        vertx.setPeriodic(3000L, l -> {
            this.dbCon.getAll().onSuccess(event -> {
                for (Service service : event) {
                    breaker.<ServiceStatus>execute(promise -> {
                        // call the GET endpoint of a backend service
                        this.vertx.createHttpClient().request(HttpMethod.GET,
                                        service.getUrl().getPort(),
                                        service.getUrl().getHost(),
                                        service.getUrl().getPath())
                                .compose(req -> req
                                        .send()
                                        .compose(resp -> {
                                            if (resp.statusCode() != 200) {
                                                return Future.succeededFuture(ServiceStatus.FAIL);
                                            } else {
                                                return resp.body().map(buffer ->
                                                        ServiceStatus.valueOf(buffer.toString()));
                                            }
                                        })).onComplete(promise);
                    }).onComplete(ar -> {
                        if (ar.succeeded()) {
                            service.setCurrentStatus(ar.result());
                        } else {
                            service.setCurrentStatus(ServiceStatus.FAIL);
                        }
                        vertx.eventBus().publish("kry.services", Json.encode(service));
                    });
                }
            });
        });
    
        startPromise.complete();
    }
}
