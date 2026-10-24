# Astro

This branch (`version/hyperion`) is the Hyperion rewrite and is the target of all new work.
Code under `*-old` modules (`bot-old`, `central-api-old`, `shared/old-core`) is legacy: use it as a reference for behaviour only, port to the new modules (`bot`, `api-central`, `shared/core`) and match their code style.

## Steps
- [x] port over premium gates
- [x] verify sentry is implemented correctly
- [x] verify we have telemetry setup
- [x] update `../../Giuliopime/gport` migrating with the info from `./docs/infra-cost-migration.md`…
- [ ] enable git precommit hooks again and run them + fix issues they report
- [ ] clean up `./docs` and move `./ignored/notes/redis-migration.md` inside docs
- [ ] make sure all skills in `.claude` are symlinks to `.agents`
- [ ] create useful skills (will detail this more)
- [ ] update README.md
- [ ] verify semaphore pipelines are correct, and the infra too, in depth
- [ ] update justfile with useful tasks (run each service, run docker compose, etc..)
- [ ] stuff is currently deployed with old infra & bot version, write a clear step by step guide on how to deploy everything new, including `../infra` (do not miss any detail, I'm ok with some downtime on the bot, the gport repo has terraform code that defines Longhorn and other stuff so it's important that the migration is done right).
- [ ] clear error objects in db during migration
- [ ] add chargebee new webhooks
- [ ] symlink .CLAUDE to README.md


## Workflow

Never build the project or run compilation checks (no `./gradlew ... compile/build`). The user compiles and tests on their own.
