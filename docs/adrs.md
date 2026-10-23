# Architecture Decision Records

This document records significant architecture decisions for CatchUp Platform.
Each record captures the context, decision, and consequences so that future
changes can revisit decisions with their original trade-offs in view.

## Process

- Records are numbered sequentially and use the `ADR-NNNN` identifier.
- New records are appended; accepted records are not silently rewritten.
- When a decision changes, add a new ADR that supersedes the old one and update
  its status to `Superseded` with a link to the replacement.
- Use `Proposed`, `Accepted`, `Rejected`, `Deprecated`, or `Superseded` as status.
- Keep records focused on durable decisions, not implementation notes or
  temporary tasks.

## Index

| ADR | Title | Status |
|---|---|---|
| [ADR-0001](#adr-0001-resultt-e-for-railway-oriented-outcomes-instead-of-exceptions) | Result<T, E> for Railway-Oriented Outcomes Instead of Exceptions | Accepted |
| [ADR-0002](#adr-0002-newsapikey-masks-itself-rather-than-being-masked-ad-hoc-at-each-call-site) | NewsApiKey Masks Itself, Rather Than Being Masked Ad Hoc at Each Call Site | Accepted |
| [ADR-0003](#adr-0003-the-list-endpoint-always-returns-an-array-regardless-of-how-many-filters-are-applied) | The List Endpoint Always Returns an Array, Regardless of How Many Filters Are Applied | Accepted |
| [ADR-0004](#adr-0004-favoritesourcecommandfailure-as-a-sealed-interface-not-exceptions-or-an-enum) | FavoriteSourceCommandFailure as a Sealed Interface, Not Exceptions or an Enum | Accepted |
| [ADR-0005](#adr-0005-newsapikey-and-sourceid-as-value-objects-with-jpa-attributeconverters-not-plain-string-columns) | NewsApiKey and SourceId as Value Objects With JPA AttributeConverters, Not Plain String Columns | Accepted |

## ADR-0001: Result<T, E> for Railway-Oriented Outcomes Instead of Exceptions

- **Status:** Accepted
- **Date:** 2026-10-19

### Context

Command handlers need to report an expected failure — a duplicate `(newsApiKey, sourceId)` pair, later a missing id for delete — distinctly from an unexpected one (a database outage, a programming error). Expected failures aren't exceptional: an exception should signal a programming error or an infrastructure failure, not a routine business outcome a caller must plan for, and the interface layer needs an exhaustive, compiler-checked mapping from each failure kind to its own HTTP status. An early design considered throwing a custom exception per expected failure and catching it in the controller, but a caller can forget to catch a thrown exception.

### Decision

Every command handler returns a sealed `Result<T, E>` (`Success<T, E>`, `Failure<T, E>`) with static factories `success`/`failure` and a `fold(onSuccess, onFailure)` method, folded into an HTTP response at the interface boundary. `FavoriteSourceCommandService` returns one from every `handle` method; the response assemblers call `fold` to produce the response.

### Consequences

- A `handle()` signature states every possible outcome in its return type; a caller can't accidentally ignore a failure path.
- Adding a new failure case is a type-level change (extend the sealed interface), not a new, easy-to-miss `catch` clause somewhere downstream.
- More ceremony than a thrown exception for a single call site; `fold` is slightly less familiar than `try`/`catch` to a reader new to the pattern.

## ADR-0002: NewsApiKey Masks Itself, Rather Than Being Masked Ad Hoc at Each Call Site

- **Status:** Accepted
- **Date:** 2026-10-19

### Context

A `newsApiKey` is a secret: it must never appear unmasked in a log line or an HTTP response. The value object is the one place that always has the raw value in hand, at the moment it's constructed, and a secret-handling rule duplicated across call sites is a bug waiting for the next one that forgets it. An early version had the controller and the command service each define their own private `mask(String)` helper, duplicating the same "last four characters" rule, and the entity-to-resource assembler forwarded the raw value entirely.

### Decision

`NewsApiKey.maskedForLogging()` returns the last four characters prefixed with asterisks (or only asterisks for a short key). `FavoriteSourceResourceFromEntityAssembler` calls it when building a response; the command and query service implementations call it for log lines.

### Consequences

- A new call site can't forget to mask — it just calls the one method. `FavoriteSourceResource.newsApiKey()` is now the masked string, never the raw value, by construction.
- Couples a presentation concern (what a log line or response shows) to the domain value object; acceptable here because the masking rule is itself a fact about how this secret must be handled, not a UI formatting choice.

## ADR-0003: The List Endpoint Always Returns an Array, Regardless of How Many Filters Are Applied

- **Status:** Accepted
- **Date:** 2026-10-19

### Context

`GET /api/v1/favorite-sources` accepts a required `newsApiKey` and an optional `sourceId`. A client of a collection-filtering endpoint shouldn't have to branch on how many query parameters it sent to know whether to expect `{}` or `[]`; REST maturity (a uniform interface) says the resource being requested — "favorite sources matching this filter" — is a collection either way, and "no matches" is not the same thing as "missing resource". An earlier design treated "newsApiKey only" and "newsApiKey + sourceId" as two different queries with two different response shapes: the first returned an array, the second a single object (or `404` when missing).

### Decision

`getFavoriteSourcesWithParameters` always calls `toResponseEntityFromList`. When both parameters are present, `GetFavoriteSourceByNewsApiKeyAndSourceIdQuery`'s `Optional<FavoriteSource>` result is wrapped into a list of at most one element before reaching the response assembler.

### Consequences

- One predictable contract for every caller; an empty result is `200` with `[]`, never a surprising `404`.
- A caller that wants "the one favorite for this exact pair" must unwrap a one-element array instead of getting the object directly — acceptable, since this endpoint's job is listing, not single-resource retrieval (that's TS002, `GET .../{id}`).

## ADR-0004: FavoriteSourceCommandFailure as a Sealed Interface, Not Exceptions or an Enum

- **Status:** Accepted
- **Date:** 2026-10-19

### Context

`handle(CreateFavoriteSourceCommand)` can fail because the pair already exists; `handle(DeleteFavoriteSourceCommand)` can fail because no favorite source exists for that id. Each failure needs its own HTTP status and its own localized message, and the delete failure additionally carries the id that wasn't found. A plain enum can't carry per-case data, and the response assembler needs an exhaustive, compiler-checked mapping from failure to HTTP status, so a new failure case can't silently fall through unmapped.

### Decision

`FavoriteSourceCommandFailure` is a sealed interface permitting `Duplicate` and `NotFound` as record cases, each exposing `messageKey()`. `ResponseEntityFromFavoriteSourceCommandResultAssembler.statusFromFailure` switches exhaustively over the sealed `permits` list.

### Consequences

- Adding a new failure case (as happened when delete shipped) is a compile error everywhere a `switch` over the sealed interface isn't updated, not a runtime surprise.
- Every new command failure case touches both the sealed interface and the `switch` in the assembler; more files than a bare exception, acceptable for the compile-time safety.

## ADR-0005: NewsApiKey and SourceId as Value Objects With JPA AttributeConverters, Not Plain String Columns

- **Status:** Accepted
- **Date:** 2026-10-19

### Context

`FavoriteSource`'s two defining columns, a news API key and a source id, are both just strings at the database level, both needing the same shape of validation (non-blank, max 256 characters, a restricted character set) wherever they're constructed. A bare `String` field lets any code construct an invalid `FavoriteSource`, and the validation rule should exist in exactly one place and run every time one of these values is created, not just at the REST boundary.

### Decision

`NewsApiKey` and `SourceId` are records with a validating compact constructor, each persisted through its own `AttributeConverter<X, String>`. `FavoriteSource`'s columns are typed `NewsApiKey`/`SourceId`, not `String`, so a `FavoriteSource` cannot exist with an invalid value in either field, regardless of which code path constructed it.

### Consequences

- The domain model is always valid by construction. The two converters are trivial, interchangeable classes with no logic of their own.
- A converter per value object is boilerplate proportional to the number of persisted value objects; a shared generic converter was considered and rejected, since it would need reflection to know which VO constructor to call.