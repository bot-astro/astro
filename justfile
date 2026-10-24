# container names in docker-compose.yml are built from this
export COMPOSE_PROJECT_NAME := "astro-dev-env"

default:
    @just --list


# --- env ---

# export secrets from infisical into .env
generate-env env="dev":
    infisical export --env={{env}} > .env


# --- services ---

# run a service (bot, api-central, support-bot, entitlements-expiration-job)
run service env="dev":
    infisical run --env={{env}} -- ./gradlew --console=plain :services:{{service}}:bootRun

bot env="dev": (run "bot" env)

api-central env="dev": (run "api-central" env)

support-bot env="dev": (run "support-bot" env)

entitlements-expiration-job env="dev": (run "entitlements-expiration-job" env)


# --- docker compose (mongo, redis + ui tools) ---

up *services:
    docker compose up -d {{services}}

down:
    docker compose down

logs *services:
    docker compose logs -f {{services}}

# --- quality ---

test service="":
    ./gradlew --console=plain {{ if service == "" { "test" } else { ":services:" + service + ":test" } }}

format:
    ./gradlew --console=plain ktlintFormat

lint:
    ./gradlew --console=plain detekt

hooks:
    prek run --all-files

deps:
    ./gradlew --console=plain dependencyUpdates
