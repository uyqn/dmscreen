# dmscreen design

Status: reviewed 2026-09-08 (Codex challenge, see project planning log). Decisions here are the
baseline; change them through a PR that edits this document.

## 1. Goals and non-goals

**Goals**

- An MCP server that lets an LLM (Claude, ChatGPT) run a D&D 5e table for up to a handful of
  players: honest dice, persistent campaign state, rules lookups, and DM-only secrets.
- Learning: Spring Modulith boundaries, MCP over Streamable HTTP, Postgres features, later
  Kafka externalization and extracting a module into a service.
- Voice-first play eventually: the server's tool design must work when nobody looks at a screen.

**Non-goals**

- The server never generates narrative prose. The LLM narrates; the server owns facts.
- No maps or positions. Theatre of the mind.
- No full rules engine. The server does not decide whether an attack hits; the LLM adjudicates
  and calls tools to apply the outcome. Mechanical resolvers may come later, additively.
- No multi-tenant SaaS. One deployment, one owner, a few friends.

## 2. Clients and transport

- Client-agnostic remote MCP server. Streamable HTTP is the only transport, locally and in
  production (`spring-ai-starter-mcp-server-webmvc`, `spring.ai.mcp.server.protocol=STREAMABLE`).
  No stdio, no legacy SSE transport.
- Local development: Claude Code or Claude Desktop attached to `http://localhost:8080/mcp`.
- Production: personal AWS account, HTTPS, OAuth 2.1 resource server so Claude.ai can add it
  as a custom connector. Voice mode is only reachable through Claude.ai, hence the public step.
- Table setup: one shared LLM conversation in the room. Each player may have the dashboard open
  on their own device. There is no per-player LLM session.

The LLM only learns things inside a tool result. Nothing can push into the conversation.
Every "real-time" feature is designed around that constraint (see section 6).

## 3. Modules

Root package `no.uyqn.dmscreen`. Spring Modulith application modules, verified by
`ApplicationModules.verify()` in the test suite.

| Module | Owns | May depend on |
|---|---|---|
| `dice` | Dice expression parsing, rolling, audit rows | nothing |
| `rules` | SRD 5.2.1 content: sections, spells, monsters, conditions, lookup tables | nothing |
| `character` | The sheet: species, class, level, abilities, max HP, AC, speed, proficiencies, inventory, known spells | `dice` (stat rolls), `rules` (lookup tables) |
| `campaign` | Campaign, party membership with live HP/conditions/XP/resources, NPCs, journal | `character` |
| `session` | Sessions, encounters, combatants (monster/NPC HP and conditions), initiative, roll requests and their fulfilment, event log. Orchestrates combat mutations: applies monster/NPC changes itself, delegates PC changes to `campaign` | `campaign`, `dice`, `rules` |
| `account` | Users, credentials, OAuth links (deferred) | nothing |
| `mcp` | MCP tools, prompts, server instructions. Adapter, no logic | all domain modules |
| `web` | REST and SSE for the player dashboard, roll submission. Adapter, no logic | all domain modules |

Rules:

- One Postgres schema per module (`dice`, `rules`, `character_`, `campaign`, `session`,
  `account`). Cross-module references are UUIDs, never JPA relations or foreign keys across
  schemas. Flyway migrations live per module under `db/migration/<module>`.
- Cross-module calls go through a module's public API (types in the module root package) or
  through application events. Internals live in sub-packages and are invisible.
- Nothing depends on `mcp` or `web`.
- Each module is service-shaped from day one so that extraction later is a deployment change.

## 4. Data model

UUID primary keys everywhere. Timestamps in UTC. `visibility` is an enum `PLAYER | DM`.

**dice**
- `roll`: expression, individual dice as int array, total, source (`SERVER | PHYSICAL`),
  reason, rolled_at.

**rules**
- `rule_section`: source, path (heading trail), title, body, `tsv` generated tsvector.
- `spell`: name, level, school, casting_time, range, components, duration, description, classes.
- `monster`: name, size, type, alignment, ac, hp, speed, abilities, cr, stat_block jsonb.
- `condition`: name, description.
- Lookup tables shipped as code, not rows: XP thresholds per level, proficiency bonus per level.

**character**
- `character`: campaign_id, owner_account_id (nullable until accounts exist), name, species,
  class, subclass, level, str/dex/con/int/wis/cha, max_hp, armor_class, speed, proficiencies
  jsonb, background, notes.
- `inventory_item`: character_id, name, quantity, equipped, weight, notes.
- `known_spell`: character_id, spell_name, prepared.

A character belongs to exactly one campaign. Reusing a character elsewhere means cloning it.

**campaign**
- `campaign`: name, setting, description, created_at.
- `party_member`: campaign_id, character_id, current_hp, temp_hp, life_state
  (`ALIVE | UNCONSCIOUS | STABLE | DEAD`), death_save_successes, death_save_failures,
  conditions text[], xp, resources jsonb (`{"spell_slot_1": {"used":1,"max":2}, "hit_dice":
  {...}}`), inspiration.

  `party_member` is the single source of truth for a PC's HP, life state and conditions,
  in and out of combat. Damage to 0 HP sets `UNCONSCIOUS`; overflow ≥ max HP sets `DEAD`;
  `heal` above 0 returns `ALIVE` unless `DEAD`; death saves and revival are adjudicated by the
  LLM and recorded through tools. Resource maxima are persisted, editable state entered from
  the sheet (`update_resource` with `max`), not derived from class tables. Deriving them from
  SRD progression is a later enhancement, not an MVP dependency.
- `npc`: campaign_id, name, public_description, dm_notes, stat_block_ref (monster name),
  life_state (`ALIVE | DEAD | UNKNOWN`). Changed only through `campaign` (`set_life_state`
  accepts NPC targets). Combat never flips it automatically: an NPC combatant at 0 HP raises a
  `CombatantDowned` event and the LLM adjudicates death with `set_life_state`.
- `journal_entry`: campaign_id, session_id (nullable), text, visibility, tags text[],
  `tsv` tsvector, created_at.

**session**
- `session`: campaign_id, number, started_at, ended_at, summary.
- `encounter`: session_id, name, round, active_index, status.
- `combatant`: encounter_id, kind (`PC | NPC | MONSTER`), ref_id (party_member or npc, nullable
  for ad hoc monsters), name, initiative, visibility, and for NPC/MONSTER only: current_hp,
  max_hp, armor_class, conditions text[]. PC rows carry no HP or conditions; reads and writes
  go to `party_member` through `campaign`.
- `roll_request`: session_id, target (party_member_id), expression, purpose, dc (DM-only),
  advantage (`NONE | ADVANTAGE | DISADVANTAGE`), status (`PENDING | FULFILLED | EXPIRED`),
  roll_id (dice.roll), fulfilled_via (`DASHBOARD | TOOL`), requested_at, resolved_at.
- Combat events: `CombatantDowned` when an NPC/MONSTER reaches 0 HP (no automatic death),
  `PartyMemberDowned` mirrored from `campaign`.
- `event`: session_id, seq (per session), type, payload jsonb, visibility, occurred_at.
  Append-only; no updates or deletes.

Combat mutation ownership: `session` exposes the combat-facing API (`applyDamage`, `heal`,
`setCondition` on a combatant). For NPC/MONSTER it mutates the combatant row; for PC it
delegates to `campaign`. Outside an encounter, PC changes go to `campaign` directly. Ending an
encounter needs no reconciliation because PC state was never copied.

## 5. MCP surface

Each tool is one action with a short, structured result record. Results are read aloud by a
voice client through the LLM, so no walls of text and no ambiguity.

**Targets.** Names are not unique (two goblins). Every result that lists party members,
NPCs or combatants returns `{id, label}` pairs, and every mutation tool takes a `target_id`.
Tools also accept a `target_name` for convenience; the resolver matches within the active
encounter first, then the party, then NPCs, and rejects an ambiguous name with the list of
candidate `{id, label}` pairs instead of picking one. Resolution lives in `session` (it knows
the active encounter), not in the adapter.

| Tool | Module | Purpose |
|---|---|---|
| `get_campaign_overview` | campaign | Party summary, known NPCs, recent journal, open threads. DM-only items marked. |
| `get_character_sheet` | character | Full sheet by character name. |
| `update_inventory` | character | Add, remove, or equip an item. |
| `apply_damage` | session → campaign | Damage a party member or combatant by name; returns new HP and life state. Monsters/NPCs mutated in `session`, PCs delegated to `campaign`. |
| `heal` | session → campaign | Restore HP; clears `UNCONSCIOUS`/`STABLE`, never `DEAD`. |
| `set_condition` | session → campaign | Add or remove a condition, same routing as `apply_damage`. |
| `record_death_save` | campaign | Record a death save success or failure; three of either resolves to `STABLE` or `DEAD`. |
| `set_life_state` | campaign | Explicit override for revival or adjudicated death, for a party member or an NPC. |
| `update_resource` | campaign | Spend or restore a named resource, or set its maximum (spell slot level, hit dice, class points). |
| `award_xp` | campaign | XP to the party; returns level-up-available flags. |
| `add_journal_entry` | campaign | Record lore or a secret with visibility. |
| `search_journal` | campaign | Full-text search over journal entries. |
| `start_session`, `end_session` | session | Open a session; close it with a summary. |
| `recap_campaign` | session | Past session summaries plus recent events, for the opening. |
| `get_session_snapshot` | session | Everything live right now, for a model that lost context. |
| `log_event` | session | Append a narrative or mechanical event with visibility. |
| `start_encounter`, `next_turn`, `end_encounter` | session | Combatants, initiative, rounds. |
| `roll_dice` | dice | Server rolls for the DM and monsters. Not linked to roll requests; use `fulfil_roll` for those. |
| `request_roll`, `await_roll`, `fulfil_roll` | session | Player roll flow, section 6. `fulfil_roll(id, physical_result?)` records a spoken physical result or has the server roll, and unblocks any waiter. |
| `search_rules` | rules | Full-text search over SRD sections. |
| `get_spell`, `get_monster`, `get_condition` | rules | Exact lookups. |

Also:

- MCP prompt `dungeon_master`: the persona and operating rules. Key rule: never invent state,
  call the tool; never reveal DM-only data to players; call `request_roll` for player rolls.
- Server `instructions` string: the same rules condensed, so clients without prompt support
  behave the same.

## 6. Player roll flow

```
LLM  -> request_roll(player, expression, purpose, dc?, advantage?)
server: create roll_request PENDING, publish RollRequested (dashboard shows "roll now")
server: wait up to N seconds (N configurable, default 30) for fulfilment
player: taps "roll" in the dashboard (server rolls via dice, source SERVER)
        or types a physical result (source PHYSICAL)
        or, without a dashboard, tells the LLM, which calls fulfil_roll(id, result?)
server: mark FULFILLED, return {total, dice, success?} to the LLM in the same tool call
timeout: return {status: PENDING, id}; the LLM calls await_roll(id) later
```

- The DC is stored DM-only and never sent to the dashboard. The dashboard shows "Roll Stealth
  (d20)", not the target.
- Two fulfilment paths, one API: `session.RollRequests.fulfil(id, physicalResult?)`. The
  `web` endpoint `POST /api/rolls/{id}/result` and the MCP tool `fulfil_roll` both call it.
  Fulfilling an already fulfilled or expired request is rejected. `roll_dice` never fulfils a
  request.
- The waiter is in-memory (`CompletableFuture` per request id, completed by the fulfilment
  path). `ponytail:` single-instance only; switch to Postgres `LISTEN/NOTIFY` if the server is
  ever scaled out.
- Open risk: the client's own tool-call timeout. If Claude.ai cuts tool calls before 30 s,
  lower N. Measure in the voice spike (M1).

## 7. Visibility

- `visibility` on `journal_entry`, `npc.dm_notes` (implicitly DM), `roll_request.dc`,
  `event`, and `combatant` (hidden enemies).
- MCP tools return everything; the LLM is the DM and is instructed to withhold.
- **Trust boundary.** Tool results are visible in the chat client that hosts the DM
  conversation. That conversation is therefore DM-side: it runs on one device, players agree
  not to inspect tool results, and in voice mode results are never spoken. The dashboard is the
  only player-facing surface and is filtered server-side. A stricter model (player-safe tool
  results plus a separate DM channel) would contradict the single-conversation setup and is out
  of scope; the `visibility` flags on result records keep that door open.
- `web` endpoints filter to `PLAYER` unconditionally. There is no DM login in the dashboard.
- Tests assert the filter at the `web` boundary, not in each domain module.

## 8. Messaging

- Domain modules publish application events (records) for facts other modules care about:
  `PartyMemberDamaged`, `ConditionChanged`, `XpAwarded`, `RollRequested`, `RollFulfilled`,
  `SessionStarted`, `SessionEnded`.
- Consumers use `@ApplicationModuleListener` (async, transactional, tracked by the Modulith
  event publication registry, JPA-backed). The `session` module turns campaign events into
  event-log rows. `web` turns `RollRequested` into SSE messages.
- No Kafka in the MVP. Later: `@Externalized` on session events plus
  `spring-modulith-events-kafka`, with a separate consumer as the first split-out service.

## 9. Cross-cutting

**Logging.** Logback via Boot. Human-readable console in the `local` profile, ECS JSON
(`logging.structured.format.console=ecs`) in `aws`. MDC keys `campaignId` and `sessionId`
set per tool call in the `mcp` adapter. Micrometer Tracing for trace ids once on AWS.

**Testing.** JUnit 6, AssertJ, Mockito from `spring-boot-starter-test`. Testcontainers with
the `pgvector/pgvector:pg16` image via `@ServiceConnection`. Per module:
`@ApplicationModuleTest` with the `Scenario` API for event flows. Whole app:
`ApplicationModules.verify()` and `Documenter`. MCP: a handful of end-to-end tests with a real
Spring AI `McpSyncClient` over Streamable HTTP against a `RANDOM_PORT` context, asserting tool
schemas and JSON results as a client sees them. Tests are written before the implementation.

**Configuration.** YAML, profiles `local` (default, Docker Compose Postgres) and `aws`.
Secrets only from environment variables.

**Persistence.** Spring Data JPA with `ddl-auto=validate`; Flyway owns the schema. Records
for events, commands, tool inputs and results. No Lombok.

## 10. Milestones

| Milestone | Outcome |
|---|---|
| M0 Foundations | CI, Modulith skeleton with boundary test, per-module Flyway schemas, structured logging, MCP server wired, Dependabot |
| M1 First roll | `dice` module, `roll_dice` end to end through Claude Code, `dungeon_master` prompt, voice spike through a tunnel |
| M2 Party | `character` and `campaign`: sheets, party, HP, conditions, resources, XP, NPCs, journal, and their tools |
| M3 Session | Sessions, event log, recap, snapshot, encounters, blocking roll requests, and their tools |
| M4 Rules | SRD 5.2.1 import, full-text search, spells, monsters, conditions, and their tools |
| MVP | M0–M4 done and a one-shot playtested with two players through Claude Code. Repo goes public. |
| M5 Dashboard | `web`: SSE event stream, roll prompt, sheet view |
| M6 Go public | OAuth 2.1, AWS, HTTPS, backups, Claude.ai connector, voice |
| M7 Learning extensions | pgvector RAG, Kafka externalization and a split-out consumer, observability, accounts |

## 11. Risks and open questions

- **Voice latency and narration.** Unknown how Claude voice mode paces tool calls. Spike in M1
  before building M2–M4 tooling in detail.
- **Tool-call timeout vs blocking roll.** Client-side limits are undocumented. Default 30 s,
  measure, adjust.
- **SRD 5.2.1 parsing.** The SRD is published as a PDF. Markdown conversions exist in the
  community; quality varies. Budget a full evening for the importer and accept imperfect chunks
  for prose sections; spells and monsters need structured parsing and will take longer.
- **OAuth 2.1 for Claude.ai.** Dynamic client registration and PKCE with Spring Authorization
  Server is the largest single piece of infrastructure. Deferred to M6 and isolated in
  `account`.
- **Secrets in the shared chat.** Mitigated by the trust boundary in section 7, not by
  technology. Revisit if the game ever leaves the friends-only setting.
- **Two adapters, one server.** `mcp` and `web` in the same deployable is fine now. If the
  dashboard grows, `web` is the first candidate to extract.
