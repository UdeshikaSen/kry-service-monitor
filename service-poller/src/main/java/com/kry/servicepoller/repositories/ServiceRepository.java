package com.kry.servicepoller.repositories;

import com.kry.servicepoller.entities.Service;
import com.kry.servicepoller.entities.ServiceStatus;
import com.kry.servicepoller.exceptions.DBException;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ServiceRepository {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceRepository.class);
    private static final String DB_URL_CONFIG = "db.url";
    private static final String DB_USERNAME_CONFIG = "db.username";
    private static final String DB_PASSWORD_CONFIG = "db.password";
    private static final String DB_POOLSIZE_CONFIG = "db.poolsize";
    private static final String DB_INIT_SCRIPT_CONFIG = "db.init.script.path";
    private final JDBCPool pool;
    
    public ServiceRepository(Properties properties) throws IOException {
        pool = JDBCPool.pool(Vertx.vertx(),
                // configure the db connection
                new JDBCConnectOptions()
                        .setJdbcUrl(properties.getProperty(DB_URL_CONFIG))
                        .setUser(properties.getProperty(DB_USERNAME_CONFIG))
                        .setPassword(properties.getProperty(DB_PASSWORD_CONFIG)),
                new PoolOptions()
                        .setMaxSize(Integer.parseInt(properties.getProperty(DB_POOLSIZE_CONFIG)))
        );
    
        try (InputStream is =
                     getClass().getClassLoader().getResourceAsStream(properties.getProperty(DB_INIT_SCRIPT_CONFIG))) {
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            // synchronously create the table
            CompletableFuture<Optional<Throwable>> tableCreateFuture = new CompletableFuture<>();;
            pool.query(sql)
                    .execute()
                    .onSuccess(rowSet -> {
                        tableCreateFuture.complete(Optional.empty());
                        LOG.info("service table created");})
                    .onFailure(e -> {
                        tableCreateFuture.complete(Optional.of(e));
                ;
            });
            
            try {
                Optional<Throwable> errorOccurred = tableCreateFuture.get(2, TimeUnit.SECONDS);
                if (errorOccurred.isPresent()) {
                    throw new RuntimeException("error occurred creating 'service' table", errorOccurred.get());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public Future<List<Service>> getAll() {
        return pool.query("SELECT id, name, url, created_time, current_status FROM service")
            .execute()
            .map(rowSet -> {
                List<Service> services = new ArrayList<>();
                for (Row row : rowSet) {
                    Service newService;
                    try {
                        newService = new Service(row.getInteger("id"),
                                row.getString("name"),
                                new URL(row.getString("url")),
                                Timestamp.valueOf(row.getLocalDateTime("created_time")),
                                ServiceStatus.valueOf(row.getString("current_status")));
                    } catch (MalformedURLException e) {
                        throw new DBException("invalid Service URL found in DB", e);
                    }
                    services.add(newService);
                }
                return services;
            })
            .onSuccess(success -> LOG.debug("retrieved services from DB"))
            .onFailure(e -> LOG.error("error getting services from DB", e));
    }
    
    public Future<Boolean> insert(Service service) {
        return pool.withTransaction(con -> pool.preparedQuery("INSERT INTO service(name, url) VALUES (?,?)")
                .execute(Tuple.tuple(List.of(service.getName(), service.getUrl().toString())))
                .map(rows -> rows.rowCount() == 1)
                .onFailure(e -> LOG.error("error when inserting service to the DB: " + service, e))
        );
    }
    
    public Future<Boolean> delete(int id) {
        return pool.preparedQuery("DELETE FROM service WHERE id = ?")
                .execute(Tuple.tuple(Collections.singletonList(id)))
                .map(rows -> rows.rowCount() == 1)
                .onFailure(e -> LOG.error("error when deleting service from DB: " + id, e));
    }
    
    public Future<Boolean> update(Service service) {
        return pool.preparedQuery("UPDATE service SET name = ?, url = ? WHERE id = ?")
                .execute(Tuple.tuple(List.of(service.getName(), service.getUrl().toString(), service.getId())))
                .map(rows -> rows.rowCount() == 1)
                .onFailure(e -> LOG.error("error when updating service in the DB: " + service, e));
    }
    
    public Future<Boolean> updateStatus(int id, ServiceStatus status) {
        return pool.preparedQuery("UPDATE service SET current_status = ? WHERE id = ?")
                .execute(Tuple.tuple(List.of(status.toString(), id)))
                .map(rows -> rows.rowCount() == 1)
                .onSuccess(success -> LOG.debug("service stats updated in DB: [" + id + ":" + status + "]"))
                .onFailure(e -> LOG.error("error when updating service status in the DB: [" + id + ":" + status + "]",
                        e));
    }

    public void close() {
        this.pool.close();
    }
}
