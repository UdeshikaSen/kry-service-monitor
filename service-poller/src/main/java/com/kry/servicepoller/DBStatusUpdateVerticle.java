package com.kry.servicepoller;

import com.kry.servicepoller.repositories.ServiceRepository;
import com.kry.servicepoller.entities.Service;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.Json;

/**
 * Verticle responsible for updating the current service status in the DB, upon the polling response from monitoring
 * services.
 */
public class DBStatusUpdateVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(DBStatusUpdateVerticle.class);
    private final ServiceRepository dbCon;

    public DBStatusUpdateVerticle(ServiceRepository dbCon) {
        this.dbCon = dbCon;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        EventBus eb = vertx.eventBus();
        eb.consumer("kry.services", message -> {
            Service service = Json.decodeValue(message.body().toString(), Service.class);
            dbCon.updateStatus(service.getId(), service.getCurrentStatus()).onFailure(e -> {
                LOG.error("error updating the service status in the DB");
            }).onSuccess(updateResult -> {
                if (!updateResult) {
                    LOG.error("service does not exists to update in the DB");
                }
            });
        });
    
        startPromise.complete();
    }
}
