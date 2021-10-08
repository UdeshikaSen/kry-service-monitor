# ReadMe

## Prerequisites

- node v14.17.0
- Vert.x 4.1.4
- JDK 16
- MySQL version 8.0.26

## Instructions to run the application

### Server

Project type : Vert.x project  
Folder name : **service-poller**  
Steps to run:

- MySQL database should be created as a prerequisite of the server startup.

```bash
CREATE DATABASE SERVICE_POLLER;
```

- Add the MySQL connection properties and credentials to the `service-poller/src/main/resources/application.properties` file.

- Build the gradle project.

```bash
./gradlew clean build
```

- Execute the following to run tests.

```bash
./gradlew test
```

- To build and start the application

```bash
./gradlew clean build
java -jar build/libs/service-poller-1.0.0-SNAPSHOT-fat.jar
```

> Server address - http://localhost:8090/services

### Client web application

Project type : React project  
Folder name : **service-poller-dashboard**  
Steps to run:

- Execute the following commands

```bash
cd service-poller-dashboard
npm install
npm run start
```

> Client address - http://localhost:3000/

Steps to check the functionality:

- Access the service monitoring dashboard through the above mentioned client address.
- Simulated Kry services  
  Service A (http://localhost:9090/health)  
  Service B (http://localhost:9091/health)  
  Service C (http://localhost:9092/health)
- Above services can be registered and monitored in the dashboard.

# Implemented System Requirements

Following features are implemented in the system.

- [x] Create/update/delete functionality for services.
- [x] Added services are persisted when the server is restarted.
- [x] Display the name, url, creation time and status of services.
- [x] The results from the poller are automatically shown to the user without a page refresh.
- [x] Poller is protected from misbehaving services.
- [x] Service URL Validation.
- [x] Server handles concurrent writes.

# Overview of the system

![System overview](/images/overview-diagram.jpg "System overview")
