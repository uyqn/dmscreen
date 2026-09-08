# dmscreen

MCP server that is the state, dice and rules engine for a D&D 5e campaign. An LLM client
(Claude, ChatGPT) narrates as dungeon master; this server never generates prose. Design decisions
live in `docs/design/`, read them before proposing changes.

## Working mode

The owner writes the code. Claude plans, reviews, explains and finds resources. Do not implement
features unless explicitly asked; answer with the smallest snippet or command that unblocks.

## Stack

Java 25 (Corretto), Spring Boot 4.1, Spring AI 2.0 (MCP server, Streamable HTTP), Spring Modulith 2.1,
Spring Data JPA, Flyway, PostgreSQL (pgvector image), Gradle Kotlin DSL. Boot 4 means Jackson 3
(`tools.jackson.*`), JUnit 6 and Testcontainers 2; prefer the 2.0/4.x reference docs over tutorials.

Run locally: `colima start`, then `./gradlew bootRun` (Docker Compose support starts Postgres).
Tests: `./gradlew test` (Testcontainers, needs Docker).

## Module rules

- Modules under `no.uyqn.dmscreen`: `account`, `character`, `campaign`, `session`, `rules`, `dice`,
  plus adapters `mcp` (tool classes) and `web` (REST/SSE). Adapters hold no logic.
- One Postgres schema per module. Cross-module references are IDs only, never JPA entities or
  foreign keys across schemas. Flyway migrations live per module.
- Cross-module communication goes through the module's public API or Modulith application
  events (`@ApplicationModuleListener`). Kafka externalization is a later step, not a dependency.
- `ApplicationModules.verify()` must pass; a failing boundary test is a design problem, not a
  test problem.

## Domain rules

- The server owns state, honest dice and rules lookups. The LLM adjudicates. Tools apply what
  the LLM decided (`apply_damage`), they do not narrate or decide.
- A character belongs to one campaign. Reuse means cloning into another campaign.
- Journal entries, NPC facts, roll DCs and event log entries carry visibility:
  player-visible or DM-only. Player-facing endpoints filter, MCP tools do not.
- Player dice are asynchronous: `request_roll` blocks up to a timeout for the dashboard or a
  typed physical result, then returns pending plus an id for `await_roll`.

## Code conventions

- Records for events, commands, tool inputs and results. No Lombok.
- Tool results are short and structured; voice clients read them aloud through the LLM.
- Structured JSON logging via `logging.structured.format.console`; put campaign and session ids
  in MDC per tool call.
