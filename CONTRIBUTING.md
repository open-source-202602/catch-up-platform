# Contributing to CatchUp Platform

Thank you for your interest in contributing. This document lists the conventions this codebase follows.

## Domain-Driven Design (DDD) Principles

- **Bounded Contexts**: keep domain logic within its bounded context. The `news` context handles favorite sources.
- **Aggregate Roots**: `FavoriteSource` enforces the unique `(newsApiKey, sourceId)` invariant and validates in its constructor.
- **Value Objects**: use records for objects defined by their attributes, not identity. `NewsApiKey` and `SourceId` are the examples here, each self-validating and each persisted through its own `AttributeConverter`.
- **CQRS**: commands (`application.commandservices`) and queries (`application.queryservices`) are separate contracts; one feature branch is one Command or one Query, unless two queries differ only by an optional filter and must share identical response semantics.
- **Railway-Oriented error handling**: a command handler returns `Result<T, E>` for an expected failure (a duplicate pair, a missing id), never an exception.
- **Architecture Decision Records**: architectural changes are documented in `docs/adrs.md`.

## Technical Stack

- **Java 27**: records, sealed interfaces, pattern matching in `switch`.
- **Spring Boot 4.x**: constructor injection, RESTful controllers, Spring Data JPA.
- **MySQL**: the real datastore behind every environment profile.
- **springdoc-openapi**: Swagger UI, generated from the controllers.
- **Maven**: use the bundled wrapper, `./mvnw`.

## Commit Message Guidelines

Conventional Commits, format `<type>(<scope>): <description>.` (lowercase, period at the end).

- **feat**: a new feature
- **fix**: a bug fix
- **docs**: documentation changes
- **refactor**: a code change that neither fixes a bug nor adds a feature
- **test**: adding or correcting tests
- **build**: changes to the build system or dependencies
- **chore**: anything that doesn't touch `src` or `test`

Example: `feat(favorite-source): add favorite source repository.`

## Versioning Policy

Semantic Versioning (SemVer).

## Git Flow Workflow

- **main**: production-ready; only stable releases merge here.
- **develop**: the integration branch; all work starts here.
- **feature/**: new features, branched off `develop`.
- **release/**: release preparation, branched off `develop`.

## Pull Request Process

1. Branch from `develop` with the `feature/` prefix.
2. Run the test suite and confirm it's green.
3. Follow the existing code style and naming.
4. Update `CHANGELOG.md`.
5. Describe the change clearly in the PR.
6. Once approved, it merges into `develop`.