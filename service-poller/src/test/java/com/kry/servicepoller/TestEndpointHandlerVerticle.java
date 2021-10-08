package com.kry.servicepoller;

import com.kry.servicepoller.repositories.ServiceRepository;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Properties;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(VertxExtension.class)
public class TestEndpointHandlerVerticle {
    private ServiceRepository dbCon;
    private String httpVerticleID;
    
    @BeforeAll
    void setUp(Vertx vertx, VertxTestContext testContext) throws IOException {
        Properties testDBProperties = new Properties();
        testDBProperties.put("db.url", "jdbc:h2:mem:httpdb;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        testDBProperties.put("db.username", "sa");
        testDBProperties.put("db.password", "");
        testDBProperties.put("db.poolsize", "10");
        testDBProperties.put("db.init.script.path", "h2-init.sql");
    
        dbCon = new ServiceRepository(testDBProperties);
        vertx.deployVerticle(new EndpointHandlerVerticle(dbCon), testContext.succeeding(httpVerticleID -> {
            this.httpVerticleID = httpVerticleID;
            testContext.completeNow();
        }));
    }
    
    @Test
    void createServiceWithEmptyBody(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        webClient.post(8090, "localhost", "/services")
            .sendJsonObject(new JsonObject())
            .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                JsonObject errorPayload = new JsonObject();
                errorPayload.put("error", "invalid service payload received");
                
                Assertions.assertAll(
                        () -> Assertions.assertEquals(400, response.statusCode()),
                        () -> Assertions.assertEquals(Json.encodeToBuffer(errorPayload), response.body())
                );
                testContext.completeNow();
            })));
    }
    
    @Test
    void createServiceWithEmptyName(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        JsonObject serviceJson = new JsonObject();
        serviceJson.put("name", "");
        serviceJson.put("url", "http://localhost:222");
        webClient.post(8090, "localhost", "/services")
                .sendJsonObject(serviceJson)
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    JsonObject errorPayload = new JsonObject();
                    errorPayload.put("error", "invalid service payload received");
                    
                    Assertions.assertAll(
                            () -> Assertions.assertEquals(400, response.statusCode()),
                            () -> Assertions.assertEquals(Json.encodeToBuffer(errorPayload), response.body())
                    );
                    testContext.completeNow();
                })));
    }
    
    @Test
    void createServiceWithInvalidURL(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        JsonObject serviceJson = new JsonObject();
        serviceJson.put("name", "Service A");
        serviceJson.put("url", "foobar");
        webClient.post(8090, "localhost", "/services")
                .sendJsonObject(serviceJson)
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    JsonObject errorPayload = new JsonObject();
                    errorPayload.put("error", "invalid service payload received");
                    
                    Assertions.assertAll(
                            () -> Assertions.assertEquals(400, response.statusCode()),
                            () -> Assertions.assertEquals(Json.encodeToBuffer(errorPayload), response.body())
                    );
                    testContext.completeNow();
                })));
    }
    
    @Order(0)
    @Test
    void createService(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        JsonObject serviceJson = new JsonObject();
        serviceJson.put("name", "Service A");
        serviceJson.put("url", "http://localhost:9090");
        webClient.post(8090, "localhost", "/services")
                .sendJsonObject(serviceJson)
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(204, response.statusCode());
                    testContext.completeNow();
                })));
    }
    
    @Order(1)
    @Test
    void getServices(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        webClient.get(8090, "localhost", "/services")
                .send()
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    Buffer body = response.body();
                    JsonObject servicesJsonPayload = body.toJsonObject();
                    JsonArray servicesArrayJson = servicesJsonPayload.getJsonArray("services");
                    JsonObject serviceJson = servicesArrayJson.getJsonObject(0);
                    Assertions.assertAll(
                            () -> Assertions.assertEquals(200, response.statusCode()),
                            () -> Assertions.assertEquals(1, serviceJson.getInteger("id")),
                            () -> Assertions.assertEquals("Service A", serviceJson.getString("name")),
                            () -> Assertions.assertEquals("http://localhost:9090", serviceJson.getString("url")),
                            () -> Assertions.assertEquals("FAIL", serviceJson.getString("currentStatus"))
                    );
                    testContext.completeNow();
                })));
    }
    
    @Order(2)
    @Test
    void updateWithNonExistingService(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        JsonObject serviceJson = new JsonObject();
        serviceJson.put("id", 1000);
        serviceJson.put("name", "Service A");
        serviceJson.put("url", "http://localhost:9091");
        webClient.put(8090, "localhost", "/services")
                .sendJsonObject(serviceJson)
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    JsonObject errorPayload = new JsonObject();
                    errorPayload.put("error", "service not found to update: 1000");
    
                    Assertions.assertAll(
                            () -> Assertions.assertEquals(404, response.statusCode()),
                            () -> Assertions.assertEquals(Json.encodeToBuffer(errorPayload), response.body())
                    );
                    testContext.completeNow();
                })));
    }
    
    @Order(3)
    @Test
    void updateWithInvalidUrlService(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        JsonObject serviceJson = new JsonObject();
        serviceJson.put("id", 1);
        serviceJson.put("name", "Service A");
        serviceJson.put("url", "barbaz");
        webClient.put(8090, "localhost", "/services")
                .sendJsonObject(serviceJson)
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    JsonObject errorPayload = new JsonObject();
                    errorPayload.put("error", "invalid service payload received");
    
                    Assertions.assertAll(
                            () -> Assertions.assertEquals(400, response.statusCode()),
                            () -> Assertions.assertEquals(Json.encodeToBuffer(errorPayload), response.body())
                    );
                    testContext.completeNow();
                })));
    }
    
    @Order(4)
    @Test
    void updateService(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        JsonObject serviceJson = new JsonObject();
        serviceJson.put("id", 1);
        serviceJson.put("name", "Service A");
        serviceJson.put("url", "http://localhost:9091");
        webClient.put(8090, "localhost", "/services")
                .sendJsonObject(serviceJson)
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(204, response.statusCode());
                    testContext.completeNow();
                })));
    }
    
    @Order(5)
    @Test
    void getServicesAfterUpdate(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        webClient.get(8090, "localhost", "/services")
                .send()
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    Buffer body = response.body();
                    JsonObject servicesJsonPayload = body.toJsonObject();
                    JsonArray servicesArrayJson = servicesJsonPayload.getJsonArray("services");
                    JsonObject serviceJson = servicesArrayJson.getJsonObject(0);
                    Assertions.assertAll(
                            () -> Assertions.assertEquals(200, response.statusCode()),
                            () -> Assertions.assertEquals(1, serviceJson.getInteger("id")),
                            () -> Assertions.assertEquals("Service A", serviceJson.getString("name")),
                            () -> Assertions.assertEquals("http://localhost:9091", serviceJson.getString("url")),
                            () -> Assertions.assertEquals("FAIL", serviceJson.getString("currentStatus"))
                    );
                    testContext.completeNow();
                })));
    }
    
    @Order(6)
    @Test
    void deleteNonExistingService(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        webClient.delete(8090, "localhost", "/services/100")
                .send()
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    JsonObject errorPayload = new JsonObject();
                    errorPayload.put("error", "service not found to delete: 100");
    
                    Assertions.assertAll(
                            () -> Assertions.assertEquals(404, response.statusCode()),
                            () -> Assertions.assertEquals(Json.encodeToBuffer(errorPayload), response.body())
                    );
                    testContext.completeNow();
                })));
    }
    
    @Order(7)
    @Test
    void deleteService(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        webClient.delete(8090, "localhost", "/services/1")
                .send()
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    Assertions.assertEquals(204, response.statusCode());
                    testContext.completeNow();
                })));
    }
    
    
    @Order(8)
    @Test
    void getServicesAfterDelete(Vertx vertx, VertxTestContext testContext) {
        final WebClient webClient = WebClient.create(vertx);
        webClient.get(8090, "localhost", "/services")
                .send()
                .onComplete(testContext.succeeding(response -> testContext.verify(() -> {
                    Buffer body = response.body();
                    JsonObject servicesJsonPayload = body.toJsonObject();
                    JsonArray servicesArrayJson = servicesJsonPayload.getJsonArray("services");
                    
                    Assertions.assertAll(
                            () -> Assertions.assertEquals(200, response.statusCode()),
                            () -> Assertions.assertEquals(0, servicesArrayJson.size())
                    );
                    testContext.completeNow();
                })));
    }
    
    @AfterAll
    void cleanUp(Vertx vertx, VertxTestContext testContext) {
        vertx.undeploy(this.httpVerticleID);
        testContext.completeNow();
    }
}
