# Astro

## Workflow

Never build the project or run compilation checks (no `./gradlew ... compile/build`). The user compiles and tests on their own.

## Translations

Bot texts live in `shared/core/src/main/resources/i18n`, `en-US.json` is the source and the other languages are managed on Crowdin.

When you add, change or remove a text key, only edit `en-US.json` while working on the change. Once the PR is ready and the user confirmed the text changes are ok, as a pre-push step update the same keys in all the other languages (`ar`, `de`, `es-ES`, `fil`, `fr`, `id`, `it`, `ko`, `pt-BR`, `th`) so they stay in sync. Never do this earlier, the wording can still change during review.

## Metrics

The bot pods push their Micrometer metrics to Prometheus over OTLP, and a series is identified only by its name and tags. The pods do not get a pod label automatically, so every pod writes into the same series and `rate()` / `increase()` return garbage.
Every **counter, timer and summary** in the bot must have the `pod_id` tag (`podProperties.getParsedOrdinal().toString()`, see `VCEventHandler`). Gauges that are meant to be summed per shard also need it (see `ShardGauges`). Metrics that are the same on every pod (e.g. `astro.vc.active`) and JVM / Spring metrics don't need it.
Queries on counters and timers must aggregate (`sum(...)`, `sum by (le) (...)`), otherwise they return one line per pod.
