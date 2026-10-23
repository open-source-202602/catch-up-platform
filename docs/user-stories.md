# CatchUp Platform REST API Technical Stories

## Overview
This document contains API-focused technical stories intended for frontend or mobile developers integrating with the catch-up-platform REST API.

Base path: `/api/v1`

## Requirement Traceability Matrix

| Technical Story | Scenario                            | Implementation                                                        |
|------------------|--------------------------------------|------------------------------------------------------------------------|
| TS001            | Successful create                   | `FavoriteSourcesController.createFavoriteSource()`                     |
| TS001            | Validation error                    | `CreateFavoriteSourceResource` (`@NotBlank`, `@Size`, `@Pattern`)       |
| TS001            | Duplicate favorite                  | `FavoriteSourceCommandServiceImpl.handle(CreateFavoriteSourceCommand)` |
| TS002            | Found                               | `FavoriteSourcesController.getFavoriteSourceById()`                     |
| TS002            | Not found                           | `ResponseEntityFromFavoriteSourceQueryResultAssembler.notFound()`       |
| TS002            | Invalid id                          | `GetFavoriteSourceByIdQuery`                                           |
| TS003            | Returns one or more favorites       | `FavoriteSourcesController.getFavoriteSourcesWithParameters()`          |
| TS003            | Returns no favorites                | `ResponseEntityFromFavoriteSourceQueryResultAssembler.toResponseEntityFromList()` |
| TS003            | Invalid / missing parameter         | `ResponseEntityFromFavoriteSourceQueryResultAssembler.badRequest()`     |
| TS004            | Successful removal                  | `FavoriteSourcesController.deleteFavoriteSource()`                      |
| TS004            | Not found                           | `FavoriteSourceCommandFailure.NotFound`                                |
| TS004            | Invalid id                          | `DeleteFavoriteSourceCommand`                                          |

## Technical Stories

### TS001 — Create a favorite source
**Endpoint:** `POST /api/v1/favorite-sources`

As a frontend developer, I want to add a favorite source through the API so that I can implement the creation feature in my application.
#### Acceptance criteria:
- Scenario: Successful create
    - Given a creation request that includes required fields (`newsApiKey`, `sourceId`) is received
    - When the API validates and persists the new resource successfully
    - Then the API responds with `201 Created`, and returns the created resource representation (`id`, `newsApiKey` masked to its last four characters, `sourceId`).
- Scenario: Validation error
    - Given a creation request with missing required fields or containing invalid values is received
    - When the API validation fails
    - Then the API responds with `400 Bad Request` and an error payload describing validation errors.
- Scenario: Duplicate favorite
    - Given a creation request for a `newsApiKey` + `sourceId` combination that already exists is received
    - When the API detects the duplicate constraint violation
    - Then the API responds with `409 Conflict` and an explanatory error payload.
---
### TS002 — Retrieve a favorite source by id
**Endpoint:** `GET /api/v1/favorite-sources/{id}`

As a frontend developer, I want to request a favorite source by its `{id}` so that I can implement the details view in my application.
#### Acceptance criteria:
- Scenario: Found
    - Given a request for a favorite source identified by `{id}` is received
    - When the API finds the resource
    - Then the API responds with `200 OK` and returns the favorite source resource (`id`, `newsApiKey` masked to its last four characters, `sourceId`).
- Scenario: Not found
    - Given a request for a favorite source identified by a non-existent `{id}` is received
    - When the API does not find the resource
    - Then the API responds with `404 Not Found` and a `ProblemDetail` payload describing the missing resource.
- Scenario: Invalid id
    - Given a request for a favorite source where `{id}` is less than or equal to `0` is received
    - When the API validates the path parameter
    - Then the API responds with `400 Bad Request` and an error payload describing the invalid id.
---
### TS003 — List favorite sources by NewsAPI key, optionally filtered by Source Id
**Endpoint:** `GET /api/v1/favorite-sources?newsApiKey={newsApiKey}` or `GET /api/v1/favorite-sources?newsApiKey={newsApiKey}&sourceId={sourceId}`

As a frontend developer, I want to request the API to list favorite sources for a given `{newsApiKey}`, optionally narrowed to one `{sourceId}`, so that I can implement both the listing feature and the "is this source a favorite" UI state in my application with a single, consistently-shaped endpoint.

**Note:** `sourceId` is just an optional additional filter, never a reason to change the response shape: a collection-filtering endpoint should always return a collection, regardless of how many parameters narrow the filter.

#### Acceptance criteria:
- Scenario: Returns one or more favorites
    - Given a request to list favorite sources filtered by `{newsApiKey}` (and, optionally, `{sourceId}`) is received
    - When the API finds one or more matching favorite resources
    - Then the API responds with `200 OK` and returns a non-empty array of favorite source resources, with each resource including `id`, `newsApiKey` (masked to its last four characters), and `sourceId`.
- Scenario: Returns no favorites
    - Given a request to list favorite sources filtered by `{newsApiKey}` (and, optionally, `{sourceId}`) is received
    - When the API finds no matching resources
    - Then the API responds with `200 OK` and returns an empty array — never `404`, an empty filter result is not a missing resource.
- Scenario: Invalid parameter value
    - Given a request with a `newsApiKey` or `sourceId` value that is blank, exceeds 256 characters, or contains disallowed characters is received
    - When the API validates the parameters
    - Then the API responds with `400 Bad Request` and an error payload describing the invalid parameter.
- Scenario: Missing required parameter
    - Given a request with no `newsApiKey` is received
    - When the API validates the parameters
    - Then the API responds with `400 Bad Request` and an error payload describing the missing parameter.
---
### TS004 — Remove a favorite source
**Endpoint:** `DELETE /api/v1/favorite-sources/{id}`

As a frontend developer, I want to remove a favorite source by its `{id}` so that I can implement the "unfavorite" action in my application.
#### Acceptance criteria:
- Scenario: Successful removal
    - Given a request to remove a favorite source identified by `{id}` is received
    - When the API finds and removes the resource
    - Then the API responds with `204 No Content`.
- Scenario: Not found
    - Given a request to remove a favorite source identified by a non-existent `{id}` is received
    - When the API does not find the resource
    - Then the API responds with `404 Not Found` and a `ProblemDetail` payload describing the missing resource.
- Scenario: Invalid id
    - Given a request to remove a favorite source where `{id}` is less than or equal to `0` is received
    - When the API validates the path parameter
    - Then the API responds with `400 Bad Request` and an error payload describing the invalid id.