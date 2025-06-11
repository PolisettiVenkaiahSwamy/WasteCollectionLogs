CREATE DATABASE IF NOT EXISTS waste_log;

USE waste_log;

CREATE TABLE IF NOT EXISTS waste_log (
    log_id BIGINT PRIMARY KEY AUTOINCREMENT,
    zone_id VARCHAR(255) NOT NULL,
    vehicle_id VARCHAR(255) NOT NULL,
    worker_id VARCHAR(255) NOT NULL,
    collection_start_time DATETIME(6) NOT NULL,
    collection_end_time DATETIME(6),
    weight_collected DECIMAL(10, 2),
    created_date DATETIME(6) NOT NULL,
    created_by VARCHAR(255),
    updated_date DATETIME(6),
    updated_by VARCHAR(255)
);