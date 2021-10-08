CREATE TABLE IF NOT EXISTS service (
    id              INT NOT NULL AUTO_INCREMENT,
    name            VARCHAR(255) NOT NULL,
    url             VARCHAR(255) NOT NULL,
    created_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_status  VARCHAR(5) NOT NULL DEFAULT 'FAIL',
    PRIMARY KEY (id)
 ) ENGINE=InnoDB;