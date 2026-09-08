# Development setup

Everything you need on a fresh machine to run dmscreen, run its tests, and talk to it from an
MCP client. The README has the two-command version; this page fills in the choices behind it.

## 1. JDK 25

The build targets Java 25 (the current LTS) through a Gradle toolchain, so Gradle itself can run
on an older JDK but needs a 25 to compile. Install one with a version manager rather than a
system package, so upgrades stay per project:

```bash
# sdkman
sdk install java 25-amzn      # Amazon Corretto, the same build the AWS box will run
# or mise
mise use -g java@corretto-25
```

Eclipse Temurin is an equally good choice. Avoid Oracle's non-LTS builds; they stop receiving
updates after six months.

IntelliJ can download a JDK for you in *Project Structure → SDK → Download JDK*; pick Corretto
or Temurin 25. Gradle finds JDKs installed by IntelliJ, sdkman and mise automatically.

If Gradle reports `Cannot find a Java installation ... matching: {languageVersion=25}`, no JDK 25
is installed where Gradle looks. Install one as above. Adding the foojay toolchain resolver plugin
to `settings.gradle.kts` would let Gradle download one itself; that is a pending small chore.

## 2. Docker

Postgres runs in a container, both for `bootRun` (via Spring Boot's Docker Compose support and
`compose.yaml`) and for tests (via Testcontainers). Any Docker engine works.

**Colima** (macOS, what the maintainer uses):

```bash
brew install colima docker docker-compose
colima start --cpu 4 --memory 8      # first start sets the profile; later just `colima start`
docker ps                            # should answer without errors
```

Two CPUs and 4 GB are enough for Postgres alone; give it more if you also run Kafka later. The
allocation is remembered in `~/.colima/default/colima.yaml`.

**Docker Desktop** works out of the box on macOS, Windows and Linux.

On Colima, Testcontainers needs two hints (verified 2026-09-08). It ignores `docker context`, so
it cannot find the socket, and its cleanup container Ryuk mounts the socket by path, which inside
the VM is `/var/run/docker.sock`, not the host path. Without them `./gradlew test` fails with
`Could not find a valid Docker environment`, then with `Container startup failed for image
testcontainers/ryuk`.

```properties
# ~/.testcontainers.properties
docker.host=unix:///Users/<you>/.colima/default/docker.sock
```

```bash
# ~/.zshrc (Testcontainers reads this one from the environment only)
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

Docker Desktop needs neither.

## 3. Build, test, run

```bash
git clone git@github.com:uyqn/dmscreen.git && cd dmscreen
./gradlew test        # the "you are ready" check: compiles, starts Postgres in Testcontainers, runs everything
./gradlew bootRun     # starts Postgres from compose.yaml, then the server on http://localhost:8080
```

The first `test` run pulls the `pgvector/pgvector:pg16` image and takes a minute or two. Later
runs reuse it.

`bootRun` uses Spring Boot DevTools: recompile in your IDE and the application restarts.

## 4. IDE

**IntelliJ IDEA**: *File → Open* the project directory; IntelliJ imports the Gradle build. Then
*Project Structure → Project → SDK*: choose the JDK 25. Run `Application` from the gutter, or
`TestDmscreenApplication` under `src/test` to run with Testcontainers instead of Compose.

Recommended settings: enable *Build project automatically* so DevTools restarts pick up
changes, and set Gradle to build and run with the project JDK (*Settings → Build Tools → Gradle
→ Gradle JVM*).

Any other IDE with Gradle support works; nothing depends on IntelliJ.

## 5. Talking to the server

The MCP endpoint is `http://localhost:8080/mcp` (Streamable HTTP).

- **Claude Code**: `claude mcp add --transport http dmscreen http://localhost:8080/mcp`, then
  `/mcp` inside a session lists the server and its tools.
- **MCP Inspector**: `npx @modelcontextprotocol/inspector`, choose Streamable HTTP, enter the
  URL. Best way to see raw requests, tool schemas and results.
- **Claude Desktop**: add the same URL under *Settings → Connectors → Add custom connector* (a
  remote-style connector; local HTTP works for development).

There is no authentication locally. Do not expose the port beyond your machine except for a
deliberate, time-boxed tunnel experiment.

## 6. Database access

Compose maps Postgres to a random host port on each start. Find it and connect:

```bash
docker compose ps                    # shows 0.0.0.0:XXXXX->5432/tcp
psql -h localhost -p XXXXX -U myuser mydatabase   # password: secret
```

Or use IntelliJ's Database tool with the same values. Schemas are per module (`dice`, `rules`,
`character_`, `campaign`, `session`, `account`); Flyway's history table lives in `public`.

Credentials are development defaults from `compose.yaml` and never used anywhere else.

## 7. Working on an issue

See [CONTRIBUTING.md](../CONTRIBUTING.md) for the workflow and
[`CLAUDE.md`](../CLAUDE.md) for module and code rules. The
[design document](design/2026-09-08-dmscreen-design.md) explains why things are shaped the way
they are.
