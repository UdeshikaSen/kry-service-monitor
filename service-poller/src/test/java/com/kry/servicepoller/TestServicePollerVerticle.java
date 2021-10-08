package com.kry.servicepoller;

import com.kry.servicepoller.entities.Service;
import com.kry.servicepoller.repositories.ServiceRepository;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
public class TestServicePollerVerticle {
    private ServiceRepository dbCon;
    private String servicePollerVerticleID;
    private String sampleServiceCID;
    
    @BeforeAll
    void setUp(Vertx vertx, VertxTestContext testContext) throws IOException {
        Properties testDBProperties = new Properties();
        testDBProperties.put("db.url", "jdbc:h2:mem:pollerdb;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        testDBProperties.put("db.username", "sa");
        testDBProperties.put("db.password", "");
        testDBProperties.put("db.poolsize", "10");
        testDBProperties.put("db.init.script.path", "h2-init.sql");
    
        this.dbCon = new ServiceRepository(testDBProperties);
        vertx.deployVerticle(new ServicePollerVerticle(this.dbCon), testContext.succeeding(servicePollerVerticleID -> {
            this.servicePollerVerticleID = servicePollerVerticleID;
            vertx.deployVerticle(new ServiceC(), testContext.succeeding(sampleServiceCID -> {
                this.sampleServiceCID = sampleServiceCID;
                testContext.completeNow();
            }));
        }));
    }
    
    @Test
    void testServicePoller(Vertx vertx, VertxTestContext testContext) throws MalformedURLException {
        vertx.eventBus().consumer("kry.services", msg -> {
            Service service = Json.decodeValue(msg.body().toString(), Service.class);
            Assertions.assertEquals("OK", service.getCurrentStatus().toString());
            testContext.completeNow();
        });

        vertx.setTimer(10000, id -> {
            testContext.failNow("event hub didn't receive backend service status");
        });

        Service svcC = new Service();
        svcC.setName("Service C");
        svcC.setUrl(new URL("http://localhost:9092/health"));
        dbCon.insert(svcC).onComplete(testContext.succeeding(response ->
                testContext.verify(() -> Assertions.assertEquals(true, response))
        ));
    }

    @AfterAll
    void cleanUp(Vertx vertx, VertxTestContext testContext) {
        vertx.undeploy(this.servicePollerVerticleID);
        vertx.undeploy(this.sampleServiceCID);
        testContext.completeNow();
    }
}
