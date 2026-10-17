# Astro

This branch (`version/hyperion`) is the Hyperion rewrite and is the target of all new work.
Code under `*-old` modules (`bot-old`, `central-api-old`, `shared/old-core`) is legacy: use it as a reference for behaviour only, port to the new modules (`bot`, `api-central`, `shared/core`) and match their code style.

## Premium

Premium is **not implemented** in the rewrite right now and will be implemented differently from the legacy `PremiumRequirementDetector`.
Do not port premium limit checks (e.g. `canCreateGenerator`, `canCreateInterface`, `canCreateConnection`, `canCreateTemplate`) when migrating endpoints; leave a TODO if useful.

## Workflow

Never build the project or run compilation checks (no `./gradlew ... compile/build`). The user compiles and tests on their own.
