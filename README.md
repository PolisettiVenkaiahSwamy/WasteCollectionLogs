
# Waste Collection Logs Service

The **Waste Collection Logs** Module is designed to efficiently manage and track waste collection activities. It provides a robust system for initiating, completing, and generating insightful reports on waste collection operations. This document provides an overview of the project, including how to clone the repository, set up the database, and get started with the module.

## Features
- **Initiate Waste Collection Logs:** Start new collection logs with details like zone, vehicle, and worker.
- **Complete Waste Collection Logs:** Update existing logs with completion time and total collected weight.
- **Generate Zone-based Reports:** Retrieve daily summaries including total weight collected and unique vehicles for specific zones over a period.
- **Generate Vehicle-based Reports:** Access detailed collection logs for individual vehicles within a specified date range.
- **Database Integration:** Seamlessly stores and retrieves all collection data in a MySQL database.
- **RESTful APIs:** Provides clear and structured APIs for easy integration with other systems.

## Table of Contents
- Getting Started
- Prerequisites
- Installation
- Database Configuration
- Usage
- API Endpoints
- HTTP Status Codes
- Examples
- Project Structure

## Getting Started
To clone the Waste Collection Logs Module repository and set it up on your local machine, follow the steps below:

### 1. Open a Terminal
Open a terminal or command prompt on your computer where Git is installed.

### 2. Run the Git Clone Command
Use the following command to clone the repository to your local machine:
```bash
git clone https://github.com/PolisettiVenkaiahSwamy/WasteCollectionLogs/tree/master
```

### 3. Navigate to the Cloned Directory
After cloning, move into the project directory by running:
```bash
cd WasteCollection
```

### 4. Verify the Repository
Ensure the repository is cloned correctly by listing the files in the directory:
- **Mac/Linux:**
  ```bash
  ls
  ```
- **Windows:**
  ```bash
  dir
  ```

You should see the project files, including:
- `src/`
- `pom.xml`
- `README.md`

Once you've cloned and verified, proceed to build and run the application.

## Prerequisites
Ensure you have the following installed on your system:
- Java 21 (as the project uses Java SDK 21)
- Maven (to manage project dependencies)
- MySQL Database (or another DBMS if configured differently)
- A modern IDE like Eclipse for development.

## Installation
### Run the Application
1. **Build the project using Maven:**
   ```bash
   mvn clean install
   ```
2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

## Database Configuration
### 1. Database Overview
You can find the database schema in the `db/schema.sql` file.

### 2. Set Up the Database
1. **Create a Database:** Open your database client and create a new database:
   ```sql
   CREATE DATABASE wastelogsdb;
   ```
2. **Run the Schema Script:** Execute the script located at `db/schema.sql` to create all necessary tables:
   ```sql
   SOURCE /path/to/db/schema.sql;
   ```
   (Replace `/path/to/db/schema.sql` with the actual path to your schema file.)
3. **Update Database Credentials:** Update the database configurations in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/wastelogsdb
   spring.datasource.username=<YOUR_USERNAME>
   spring.datasource.password=<YOUR_PASSWORD>
   spring.jpa.hibernate.ddl-auto=update
   ```

### 3. Seed Test Data (Optional)
You can populate the database with initial data using the `db/data.sql` file:
```sql
SOURCE /path/to/db/data.sql;
```

## Usage
## API Endpoints
The following REST API endpoints are available in the Waste Collection Logs Module. These endpoints allow you to initiate and complete logs, and generate various reports.

**Base URL:** All endpoints are prefixed with:
`http://localhost:8081/wastewise/admin/wastelogs`

| Endpoint          | Description                                               | Method | URL Path                                      | Request Body (Example)                                                                 | Response (Example)                                                                 |
|-------------------|-----------------------------------------------------------|--------|-----------------------------------------------|---------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| Start Collection  | Initiates a new waste collection log.                     | POST   | /start                                        | json `{ "zoneId": "Z001", "vehicleId": "RT001", "workerId": "W123" }`       | 201 Created: json `{ "message": "Waste Collection Log Recorded Successfully", "logId": "LOG_ID GENERATED" }` |
| End Collection    | Completes an existing waste collection log with end time and collected weight. | PUT    | /end                                          | json `{ "logId": "LOG001", "weightCollected": 150.5 }`                          | 200 OK: json `{ "message": "Waste Collection Log Completed Successfully", "logId": "LOG_001" }` |
| Get Zone Report   | Retrieves a daily summary report for a specific waste collection zone. | GET    | /reports/zone/{zoneId}                        | (N/A)                                                                                 | 200 OK: json `[ { "zoneId": "Z001", "date": "2024-06-05", "VehiclesUsed": 2, "totalWeightCollectedKg": 500.0 } ]` |
| Get Vehicle Report| Retrieves collection logs for a specific vehicle within a given date range. | GET    | /reports/vehicle/{vehicleId}                  | (N/A)                                                                                 | 200 OK: json `[ { "vehicleId": "RT001", "zoneId": "Z001", "weightCollected": 120.0, "collectionDate": "2024-06-05" } ]`<br>204 No Content if no logs found. |

### Path Parameters
- `{id}` (String): The unique identifier of the waste log. Used in DELETE and GET `/waste-logs/{id}` (though this endpoint wasn't in the provided controller, it's common).
- `{zoneId}` (String): The unique identifier of the zone. Used in GET `/reports/zone/{zoneId}`.
- `{vehicleId}` (String): The unique identifier of the vehicle. Used in GET `/reports/vehicle/{vehicleId}`.

### Query Parameters (for Reports)
- `startDate` (LocalDate): The start date for the report range (format: YYYY-MM-DD).
- `endDate` (LocalDate): The end date for the report range (format: YYYY-MM-DD).

## HTTP Status Codes
These endpoints use the following HTTP status codes:
- `200 OK`: The request was successful.
- `201 Created`: A new resource was successfully created.
- `204 No Content`: The resource was successfully deleted (or no content available for a GET request, like in getVehicleReport when empty).
- `400 Bad Request`: The request was invalid (e.g., invalid data or missing fields).
- `404 Not Found`: The requested resource could not be found.
- `500 Internal Server Error`: An unexpected error occurred on the server.

## Examples
### Start Collection
```bash
curl -X POST http://localhost:8081/wastewise/admin/wastelogs/start \
-H "Content-Type: application/json" \
-d '{
      "zoneId": "Z001",
      "vehicleId": "RT001",
      "workerId": "W003"
    }'
```
**Response (Example):**
```json
{
  "message": "Waste Collection Log Recorded Successfully",
  "logId": "LOG001"
}
```

### End Collection
```bash
curl -X PUT http://localhost:8081/wastewise/admin/wastelogs/end \
-H "Content-Type: application/json" \
-d '{
      "logId": "LOG001",
      "weightCollected": 125.75
    }'
```
**Response (Example):**
```json
{
  "message": "Waste Collection Log Completed Successfully",
  "logId": "LOG001"
}
```

### Get Zone Report
```bash
curl -X GET "http://localhost:8081/wastewise/admin/wastelogs/reports/zone/Z001?startDate=2024-06-01&endDate=2024-06-07"
```
**Response (Example):**
```json
[
  {
    "zoneId": "Z001",
    "date": "2024-06-05",
    "VehiclesUsed": 2,
    "totalWeightCollectedKg": 500.0
  },
  {
    "zoneId": "Z001",
    "date": "2024-06-06",
    "VehiclesUsed": 1,
    "totalWeightCollectedKg":400.0
}
```

---

### Get Vehicle Report

Retrieves collection logs for a specific vehicle within a given date range.

**Method:** `GET`  
**URL:** `/reports/vehicle/{vehicleId}`  
**Query Parameters:**
- `startDate` (format: `YYYY-MM-DD`)
- `endDate` (format: `YYYY-MM-DD`)

**Example Request:**
```bash
curl -X GET "http://localhost:8081/wastewise/admin/wastelogs/reports/vehicle/RT001?startDate=2024-06-01&endDate=2024-06-07"
```

**Example Response:**
```json
[
  {
    "vehicleId": "RT001",
    "zoneId": "Z001",
    "weightCollected": 120.0,
    "collectionDate": "2024-06-05"
  },
  {
    "vehicleId": "PT001",
    "zoneId": "Z002",
    "weightCollected": 180.0,
    "collectionDate": "2024-06-05"
  }
]
```

**Notes:**
- If no logs are found for the given vehicle and date range, the response will be `204 No Content` with an empty body.
- This endpoint is useful for tracking the performance and workload of individual vehicles over time.
---
