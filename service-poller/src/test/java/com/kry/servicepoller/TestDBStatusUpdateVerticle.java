package com.kry.servicepoller;

import com.kry.servicepoller.entities.Service;
import com.kry.servicepoller.entities.ServiceStatus;
import com.kry.servicepoller.repositories.ServiceRepository;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Properties;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(VertxExtension.class)
public class TestDBStatusUpdateVerticle {
    private ServiceRepository dbCon;
    private String serviceStatusDBID;
    
    @BeforeAll
    void setUp(Vertx vertx, VertxTestContext testContext) throws IOException {
        Properties testDBProperties = new Properties();
        testDBProperties.put("db.url", "jdbc:h2:mem:servicestatusdb;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        testDBProperties.put("db.username", "sa");
        testDBProperties.put("db.password", "");
        testDBProperties.put("db.poolsize", "10");
        testDBProperties.put("db.init.script.path", "h2-init.sql");
    
        dbCon = new ServiceRepository(testDBProperties);
        vertx.deployVerticle(new DBStatusUpdateVerticle(this.dbCon), testContext.succeeding(serviceStatusDBID -> {
            this.serviceStatusDBID = serviceStatusDBID;
            testContext.completeNow();
        }));
    }
    
    @Test
    void testServiceStatusDBUpdater(Vertx vertx, VertxTestContext testContext) {
        JsonObject serviceJson = new JsonObject();
        serviceJson.put("id", 1);
        serviceJson.put("name", "Service D");
        serviceJson.put("url", "http://localhost:9094");
        serviceJson.put("createdDate", "2020-07-14T17:45:55.9483536");
        serviceJson.put("currentStatus", "OK");
        
        this.dbCon.insert(serviceJson.mapTo(Service.class)).onSuccess(insertResult -> {
            if (insertResult) {
                vertx.eventBus().publish("kry.services", serviceJson);
    
                vertx.setTimer(5000, id -> {
                    this.dbCon.getAll().onSuccess(services -> {
                        Assertions.assertEquals(1, services.size());
                        Assertions.assertEquals(services.get(0).getName(), "Service D");
                        Assertions.assertEquals(services.get(0).getCurrentStatus(), ServiceStatus.OK);
                        testContext.completeNow();
                    }).onFailure(fail -> {
                        testContext.failNow("getting all services after status update failed");
                    });
                });
            } else {
                testContext.failNow("service was not added to the DB");
            }
        }).onFailure(testContext::failNow);
        
    }

    @AfterAll
    void cleanUp(Vertx vertx, VertxTestContext testContext) {
        vertx.undeploy(this.serviceStatusDBID);
        testContext.completeNow();
    }
}
