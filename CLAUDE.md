# Astro

This branch (`version/hyperion`) is the Hyperion rewrite and is the target of all new work.
Code under `*-old` modules (`bot-old`, `central-api-old`, `shared/old-core`) is legacy: use it as a reference for behaviour only, port to the new modules (`bot`, `api-central`, `shared/core`) and match their code style.

## Steps
- [x] port over premium gates
- [x] verify sentry is implemented correctly
- [x] verify we have telemetry setup
- [x] update `../../Giuliopime/gport` migrating with the info from `ignored/infrastructure.md`…
- [x] update justfile with useful tasks (run each service, run docker compose, etc..)
- [x] enable git precommit hooks again and run them + fix issues they report
- [x] clean up `./docs` and move `./ignored/notes/redis-migration.md` inside docs
- [x] move skills to the AstroDevToolkit plugin (`../astro-dev-claude-plugin`)
- [x] create useful skills (will detail this more)
- [x] update README.md
- [x] verify semaphore pipelines are correct, and the infra too, in depth
- [x] stuff is currently deployed with old infra & bot version, write a clear step by step guide on how to deploy everything new, including `../infra` (do not miss any detail, I'm ok with some downtime on the bot, the gport repo has terraform code that defines Longhorn and other stuff so it's important that the migration is done right).
- [x] deploy, see `docs/deploy-hyperion.md`
- [ ] try improving memory JVM, see `ignored/memory-improvement-plan.md`


## Workflow

Never build the project or run compilation checks (no `./gradlew ... compile/build`). The user compiles and tests on their own.

## Translations

Bot texts live in `shared/core/src/main/resources/i18n`, `en-US.json` is the source and the other languages are managed on Crowdin.

When you add, change or remove a text key, only edit `en-US.json` while working on the change. Once the PR is ready and the user confirmed the text changes are ok, as a pre-push step update the same keys in all the other languages (`ar`, `de`, `es-ES`, `fil`, `fr`, `id`, `it`, `ko`, `pt-BR`, `th`) so they stay in sync. Never do this earlier, the wording can still change during review.
