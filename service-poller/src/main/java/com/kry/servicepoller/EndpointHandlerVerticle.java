package com.kry.servicepoller;

import com.kry.servicepoller.entities.Service;
import com.kry.servicepoller.repositories.ServiceRepository;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.validation.BodyProcessorException;
import io.vertx.ext.web.validation.RequestPredicate;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.Bodies;
import io.vertx.ext.web.validation.builder.Parameters;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import static io.vertx.json.schema.common.dsl.Keywords.maxLength;
import static io.vertx.json.schema.common.dsl.Keywords.minLength;
import static io.vertx.json.schema.common.dsl.Schemas.intSchema;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

/**
 * Verticle responsible for starting the server and handling REST endpoints to serve the client dashboard.
 */
public class EndpointHandlerVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(EndpointHandlerVerticle.class);
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private final ServiceRepository dbCon;

    public EndpointHandlerVerticle(ServiceRepository dbCon) {
        this.dbCon = dbCon;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);
    
        SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
        SchemaParser schemaParser = SchemaParser.createDraft201909SchemaParser(schemaRouter);
        ObjectSchemaBuilder serviceSchemaBuilder = objectSchema()
                .requiredProperty("name", stringSchema().with(minLength(1)).with(maxLength(255)))
                .requiredProperty("url", stringSchema().with(minLength(1)).with(maxLength(255)));
    
        ValidationHandler serviceObjValidationHandler = ValidationHandler
                .builder(schemaParser)
                .predicate(RequestPredicate.BODY_REQUIRED)
                .body(Bodies.json(serviceSchemaBuilder))
                .build();
        ValidationHandler serviceIdValidationHandler
                
                = ValidationHandler
                .builder(schemaParser)
                .pathParameter(Parameters.param("id", intSchema()))
                .build();

        // event bus bridge defined
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        SockJSBridgeOptions options = new SockJSBridgeOptions();
        options.addOutboundPermitted(new PermittedOptions().setAddress("kry.services"));
        // mount the bridge on the router
        router.mountSubRouter("/eventbus", sockJSHandler.bridge(options));

        // CORS handling
        Set<HttpMethod> allowedMethods = new HashSet<>();
        allowedMethods.add(HttpMethod.GET);
        allowedMethods.add(HttpMethod.POST);
        allowedMethods.add(HttpMethod.DELETE);
        allowedMethods.add(HttpMethod.PATCH);
        allowedMethods.add(HttpMethod.OPTIONS);
        allowedMethods.add(HttpMethod.PUT);
        router.route().handler(CorsHandler.create()
                .addOrigin("*")
                .allowedHeader("*")
                .allowedMethods(allowedMethods));

        // define route paths
        router.route("/services*").handler(BodyHandler.create());
        
        // insert service path
        router.post("/services")
                .handler(LoggerHandler.create(LoggerFormat.DEFAULT))
                .handler(serviceObjValidationHandler)
                .handler(this::addService)
                .failureHandler(rc -> sendErrorResponse(rc, rc.failure()));
        
        // delete service path
        router.delete("/services/:id")
                .handler(LoggerHandler.create(LoggerFormat.DEFAULT))
                .handler(serviceIdValidationHandler)
                .handler(this::deleteService)
                .failureHandler(rc -> sendErrorResponse(rc, rc.failure()));
        
        // update service path
        router.put("/services")
                .handler(LoggerHandler.create(LoggerFormat.DEFAULT))
                .handler(serviceObjValidationHandler)
                .handler(this::updateService)
                .failureHandler(rc -> sendErrorResponse(rc, rc.failure()));
        
        // get all services path
        router.get("/services")
                .handler(LoggerHandler.create(LoggerFormat.DEFAULT))
                .handler(this::getAllServices);

        server.requestHandler(router).listen(8090, http -> {
            if (http.succeeded()) {
                startPromise.complete();
                LOG.info("HTTP service started on port 8090");
            } else {
                startPromise.fail(http.cause());
            }
        });
    }
    
    private void getAllServices(RoutingContext routingContext) {
        dbCon.getAll().onSuccess(services -> {
            JsonArray servicesJsonArray = new JsonArray();
            services.forEach(servicesJsonArray::add);
            JsonObject servicesJson = new JsonObject();
            servicesJson.put("services", servicesJsonArray);
            routingContext.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                    .end(Json.encode(servicesJson));
        }).onFailure(e -> {
            LOG.error("error occurred in DB when getting all the services", e);
            JsonObject jsonBody = new JsonObject();
            jsonBody.put("message", "system error occurred");
            routingContext.response()
                    .setStatusCode(500)
                    .putHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                    .end(Json.encode(jsonBody));
        });
    }

    private void addService(RoutingContext routingContext) {
        Service insertingService = Json.decodeValue(routingContext.getBodyAsString(), Service.class);
        LOG.debug("received a request on add service endpoint: " + insertingService);
        dbCon.insert(insertingService).onSuccess(insertResult -> {
            if (insertResult) {
                routingContext.response()
                        .setStatusCode(204)
                        .end();
            } else {
                LOG.error("service was not created in DB: " + insertingService);
                JsonObject jsonBody = new JsonObject();
                jsonBody.put("message", "system error occurred creating service");
                routingContext.response()
                        .setStatusCode(500)
                        .putHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .end(Json.encode(jsonBody));
            }
        }).onFailure(e -> sendErrorResponse(routingContext, e));
    }

    private void deleteService(RoutingContext routingContext) {
        String deletingServiceId = routingContext.pathParam("id");
        LOG.debug("received a request on delete service endpoint: " + deletingServiceId);
        dbCon.delete(Integer.parseInt(deletingServiceId)).onSuccess(deleteResult -> {
            if (deleteResult) {
                routingContext.response()
                        .setStatusCode(204)
                        .end();
            } else {
                LOG.error("service was not deleted from DB: " + deletingServiceId);
                JsonObject jsonBody = new JsonObject();
                jsonBody.put("error", "service not found to delete: " + deletingServiceId);
                routingContext.response()
                        .setStatusCode(404)
                        .putHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .end(Json.encode(jsonBody));
            }
        }).onFailure(e -> sendErrorResponse(routingContext, e));
    }

    private void updateService(RoutingContext routingContext) {
        Service updatingService = Json.decodeValue(routingContext.getBodyAsString(), Service.class);
        LOG.debug("received a request on update service endpoint: " + updatingService);
        dbCon.update(updatingService).onSuccess(updateResult -> {
            if (updateResult) {
                routingContext.response()
                        .setStatusCode(204)
                        .end();
            } else {
                LOG.error("service was not updated from DB: " + updatingService);
                JsonObject jsonBody = new JsonObject();
                jsonBody.put("error", "service not found to update: " + updatingService.getId());
                routingContext.response()
                        .setStatusCode(404)
                        .putHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                        .end(Json.encode(jsonBody));
            }
        }).onFailure(e -> sendErrorResponse(routingContext, e));
    }
    
    private static void sendErrorResponse(RoutingContext rc, Throwable throwable) {
        int status;
        String message;
        
        if (throwable instanceof DecodeException || throwable instanceof BodyProcessorException) {
            status = 400;
            message = "invalid service payload received";
        } else {
            status = 500;
            message = "internal server error";
        }
        
        rc.response()
            .setStatusCode(status)
            .putHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON)
            .end(new JsonObject().put("error", message).encode());
    }
}
