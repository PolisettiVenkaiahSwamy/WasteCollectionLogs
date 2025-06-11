-- db/data.sql

-- Clear existing data if you want to start fresh each time.
-- This is useful for development but be cautious in production environments.
-- DELETE FROM waste_log;

-- Insert sample WasteLog entries

-- Log 1: Completed collection for Zone 'Z001' by Vehicle 'RT001' and Worker 'W001' on 2024-06-03.
INSERT INTO waste_log (
    log_id,
    zone_id,
    vehicle_id,
    worker_id,
    collection_start_time,
    collection_end_time,
    weight_collected,
    created_date,
    created_by,
    updated_date,
    updated_by
) VALUES (
    '1',
    'Z001',
    'RT001',
    'W001',
    '2024-06-03 08:00:00',
    '2024-06-03 08:45:00',
    150.75,
    '2024-06-03 07:59:00',
    'System',
    '2024-06-03 08:45:00',
    'System'
);

-- Log 2: Completed collection for Zone 'Z002' by Vehicle 'RT001' and Worker 'W001' on 2024-06-03.
INSERT INTO waste_log (
    log_id,
    zone_id,
    vehicle_id,
    worker_id,
    collection_start_time,
    collection_end_time,
    weight_collected,
    created_date,
    created_by,
    updated_date,
    updated_by
) VALUES (
    '2',
    'Z002',
    'RT001',
    'W001',
    '2024-06-03 09:30:00',
    '2024-06-03 10:15:00',
    210.00,
    '2024-06-03 09:29:00',
    'System',
    '2024-06-03 10:15:00',
    'System'
);

-- Log 3: Completed collection for Zone 'Z001' by Vehicle 'RT003' and Worker 'W002' on 2024-06-03.
INSERT INTO waste_log (
    log_id,
    zone_id,
    vehicle_id,
    worker_id,
    collection_start_time,
    collection_end_time,
    weight_collected,
    created_date,
    created_by,
    updated_date,
    updated_by
) VALUES (
    '3',
    'Z001',
    'RT003',
    'W002',
    '2024-06-03 11:00:00',
    '2024-06-03 11:50:00',
    300.50,
    '2024-06-03 10:59:00',
    'System',
    '2024-06-03 11:50:00',
    'System'
);

-- Log 4: Ongoing collection for Zone 'Z009' by Vehicle 'PT001' and Worker 'W003' started on 2024-06-04.
INSERT INTO waste_log (
    log_id,
    zone_id,
    vehicle_id,
    worker_id,
    collection_start_time,
    collection_end_time,
    weight_collected,
    created_date,
    created_by,
    updated_date,
    updated_by
) VALUES (
    '100',
    'Z009',
    'PT001',
    'W003',
    '2024-06-04 09:00:00',
    NULL, -- collection_end_time is NULL as it's ongoing
    NULL, -- weight_collected is NULL as it's ongoing
    '2024-06-04 08:58:00',
    'System',
    '2024-06-04 08:58:00',
    'System'
);

-- Log 5: Completed collection for Zone 'Z010' by Vehicle 'PT004' and Worker 'W004' on 2024-06-05.
INSERT INTO waste_log (
    log_id,
    zone_id,
    vehicle_id,
    worker_id,
    collection_start_time,
    collection_end_time,
    weight_collected,
    created_date,
    created_by,
    updated_date,
    updated_by
) VALUES (
    '101',
    'Z010',
    'PT004',
    'W004',
    '2024-06-05 07:30:00',
    '2024-06-05 08:15:00',
    180.25,
    '2024-06-05 07:29:00',
    'System',
    '2024-06-05 08:15:00',
    'System'
);

-- Log 6: Completed collection for Zone 'Z001' by Vehicle 'RT001' and Worker 'W001' on 2024-06-05.
INSERT INTO waste_log (
    log_id,
    zone_id,
    vehicle_id,
    worker_id,
    collection_start_time,
    collection_end_time,
    weight_collected,
    created_date,
    created_by,
    updated_date,
    updated_by
) VALUES (
    '006',
    'Z001',
    'RT001',
    'W001',
    '2024-06-05 09:00:00',
    '2024-06-05 09:45:00',
    220.00,
    '2024-06-05 08:59:00',
    'System',
    '2024-06-05 09:45:00',
    'System'
);

-- Log 7: Completed collection for Zone 'Z100' by Vehicle 'PT001' and Worker 'W003' on 2024-06-06.
INSERT INTO waste_log (
    log_id,
    zone_id,
    vehicle_id,
    worker_id,
    collection_start_time,
    collection_end_time,
    weight_collected,
    created_date,
    created_by,
    updated_date,
    updated_by
) VALUES (
    '102',
    'Z100',
    'PT001',
    'W003',
    '2024-06-06 06:00:00',
    '2024-06-06 07:00:00',
    95.00,
    '2024-06-06 05:58:00',
    'System',
    '2024-06-06 07:00:00',
    'System'
);