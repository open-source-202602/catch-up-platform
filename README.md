# CatchUp Platform

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE.md)

## Overview

A Spring Boot REST API, built with Domain-Driven Design and CQRS, for tracking favorite news sources. The project is organized around a `news` bounded context and a `shared` kernel, using application-layer command/query contracts, a `Result` type for command outcomes, and JPA persistence.

This service is designed to work with [NewsAPI.org](https://newsapi.org/):
- `newsApiKey` refers to an API key issued by NewsAPI.org
- `sourceId` refers to a NewsAPI.org source identifier (for example, `bbc-news`)
- This service stores favorite source mappings; it does not proxy NewsAPI.org content endpoints

## Features
- **POST /api/v1/favorite-sources**: create a favorite source for a given `newsApiKey` + `sourceId`.
- **GET /api/v1/favorite-sources/{id}**: retrieve a favorite source by id.
- **GET /api/v1/favorite-sources?newsApiKey=...&sourceId=...**: list favorite sources by `newsApiKey`, optionally narrowed to one `sourceId`; always returns an array.
- **DELETE /api/v1/favorite-sources/{id}**: remove a favorite source.
- **GET /health**: liveness check.
- **Interactive API docs**: Swagger UI at `/swagger-ui.html`, generated from the controllers.

## Technologies
- Java 27 and Spring Boot 4
- Apache Maven (or the bundled `./mvnw` wrapper)
- Spring Web (REST controllers)
- Spring Data JPA (Hibernate) with MySQL
- Spring Validation (Jakarta Bean Validation)
- SpringDoc OpenAPI 3 / Swagger UI
- Lombok (compile-time helpers)
- PlantUML (architecture diagrams in `docs/`)

## Prerequisites
- Java 27 or higher
- Maven (or the bundled `./mvnw` wrapper)
- MySQL 8+ running locally

## Environment variables
The application resolves the active profile and database credentials from the environment. Set the following before running locally:

| Variable | Description |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev` locally) |
| `DATABASE_USER` | MySQL username |
| `DATABASE_PASSWORD` | MySQL password |

## Spring profiles

| Profile | Usage |
|---|---|
| *(default)* | Uses `localhost:3306` with SSL enabled; SQL logging off |
| `dev` | Uses `localhost:3306` with SSL disabled; SQL logging on |
| `prod` | Builds the JDBC URL from env vars |

## User Stories
See [`docs/user-stories.md`](docs/user-stories.md).

## Architecture Decisions
See [`docs/adrs.md`](docs/adrs.md).

## Class Diagram
See [`docs/class-diagram.puml`](docs/class-diagram.puml). Open it with a PlantUML plugin or viewer to render it.

## Architecture notes
- `news.application.commandservices` and `news.application.queryservices` define the application-layer service contracts
- `news.application.internal.commandservices` and `news.application.internal.queryservices` contain the implementations
- `shared.application.result.Result` models command outcomes without introducing HTTP or domain concepts
- `news.interfaces.rest.transform.ResponseEntityFromFavoriteSourceCommandResultAssembler` maps command results to `ResponseEntity` values for the REST boundary
- `news.interfaces.rest.transform.ResponseEntityFromFavoriteSourceQueryResultAssembler` maps query results and localized query errors to `ResponseEntity` values for the REST boundary

## Error responses
- Validation and domain input errors return `400 Bad Request` with a `ProblemDetail` payload.
- Duplicate favorite creation returns `409 Conflict` with a localized `ProblemDetail` payload.
- Not-found lookups (`GET /api/v1/favorite-sources/{id}` and `DELETE /api/v1/favorite-sources/{id}`) return `404 Not Found` with a localized `ProblemDetail` payload.

## Getting Started
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd catch-up-platform
   ```
2. Set the `SPRING_PROFILES_ACTIVE`, `DATABASE_USER`, and `DATABASE_PASSWORD` environment variables (see Project Setup step 5).
3. Build the project:
   ```bash
   ./mvnw clean install
   ```
4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
5. Access the API:
    - Swagger UI: `http://localhost:8080/swagger-ui.html`
    - Or send requests directly, for example:
      ```bash
      curl -X POST http://localhost:8080/api/v1/favorite-sources \
           -H "Content-Type: application/json" \
           -d '{"newsApiKey": "news-api-key-123", "sourceId": "bbc-news"}'
      ```

## Contributing
See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License
MIT, see [`LICENSE.md`](LICENSE.md).