# Astro

## Workflow

Never build the project or run compilation checks (no `./gradlew ... compile/build`). The user compiles and tests on their own.

## Translations

Bot texts live in `shared/core/src/main/resources/i18n`, `en-US.json` is the source and the other languages are managed on Crowdin.

When you add, change or remove a text key, only edit `en-US.json` while working on the change. Once the PR is ready and the user confirmed the text changes are ok, as a pre-push step update the same keys in all the other languages (`ar`, `de`, `es-ES`, `fil`, `fr`, `id`, `it`, `ko`, `pt-BR`, `th`) so they stay in sync. Never do this earlier, the wording can still change during review.
