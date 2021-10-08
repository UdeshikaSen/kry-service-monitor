package com.kry.servicepoller;

import com.kry.servicepoller.backend.simulators.ServiceA;
import com.kry.servicepoller.backend.simulators.ServiceB;
import com.kry.servicepoller.backend.simulators.ServiceC;
import com.kry.servicepoller.repositories.ServiceRepository;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import java.io.InputStream;
import java.util.Properties;

public class PollerInitializer extends AbstractVerticle {
    private ServiceRepository dbCon;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        // get configuration properties
        Properties properties = new Properties();
        try (InputStream is = PollerInitializer.class.getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(is);
        }

        this.dbCon = new ServiceRepository(properties);
        vertx.deployVerticle(new EndpointHandlerVerticle(this.dbCon));
        vertx.deployVerticle(new ServicePollerVerticle(this.dbCon));
        vertx.deployVerticle(new DBStatusUpdateVerticle(this.dbCon));
        vertx.deployVerticle(ServiceA.class.getName());
        vertx.deployVerticle(ServiceB.class.getName());
        vertx.deployVerticle(ServiceC.class.getName());

        startPromise.complete();
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        this.dbCon.close();
        vertx.deploymentIDs().forEach(a -> vertx.undeploy(a));
        stopPromise.complete();
    }
}
