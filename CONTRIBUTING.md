# Contributing to dmscreen

dmscreen is a learning project. The bar for a change is: it does one thing, it is tested, and a
reviewer with ten minutes can follow it. This page says how work moves from an issue to `main`.

## Before you start

- Read the [design document](docs/design/2026-09-08-dmscreen-design.md). It is the baseline;
  a change that disagrees with it edits the document in the same PR and says why.
- Read [`CLAUDE.md`](CLAUDE.md) for module and code rules. They apply to humans too.
- Local setup is in the [README](README.md#getting-started): a JDK 25, Docker, `./gradlew bootRun`.

## Picking work

Everything is an issue. The [project board](https://github.com/users/uyqn/projects/3) has four
columns:

| Column | Meaning |
|---|---|
| Ready | No open blockers. Safe to start. |
| Backlog | Blocked by another issue, listed under "Blocked by" on the issue page. |
| In progress | Someone is on it. Move the card when you start. |
| Done | Merged. |

Take an issue from Ready, move it to In progress, and comment if you deviate from its suggested
approach. Issues carry `type:*`, `module:*` and `learning:*` labels and belong to a milestone.
Epics (`type:epic`) are broken into sub-issues when their milestone starts.

Found something outside your issue? Open a new issue, label it, link it, and keep your PR focused.

## Making a change

1. Branch from `main`: `type/short-description`, e.g. `feat/dice-expression-parser`,
   `fix/roll-request-expiry`, `docs/contributing`.
2. Write the tests first, then the code, then run `./gradlew test`. Do not change an existing test
   to make new code pass; a red pre-existing test is information about the change.
3. Keep module boundaries: `ApplicationModules.verify()` runs in the suite and fails the build on
   a wrong import. If it fails, the fix is in the design, not in the test.
4. Commit in small logical units using [Conventional Commits](https://www.conventionalcommits.org/):
   `feat(dice): parse advantage and disadvantage`, `test(session): cover roll expiry`,
   `docs: ...`, `chore: ...`. Imperative mood, under 72 characters, body only when the why is not
   obvious.
5. Open a pull request. The template asks for: the issue it closes, before and after, deviations
   from the issue, how to read the PR, and what was tested. Fill all of it; a reviewer should not
   have to reconstruct decisions from the diff.

## Review and merge

- `main` is protected: pull requests only, no force pushes, all review threads resolved, and the
  CI build green once the workflow exists.
- Every review comment gets a reply: either a fix with the commit named, or the decision that
  covers it. Threads are resolved by the person who opened them or by agreement, never left
  hanging.
- Squash or rebase merges are both fine; keep the resulting history readable.

## Rules content and licences

- Code is MIT. By contributing you agree your contribution is licensed the same way.
- Rules text comes from the SRD 5.2.1 under CC-BY-4.0 and lives under `src/main/resources/srd/`
  with its attribution file. Do not add rules text from any other Wizards of the Coast product;
  the Player's Handbook is not redistributable.
- No secrets in the repository, ever. Configuration comes from environment variables and the
  `local` profile defaults are for the Docker Compose database only.

## Questions

Open an issue with the `question` label. Design discussions happen in issues too, so the
reasoning is findable later.
