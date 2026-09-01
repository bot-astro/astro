generate-env env="dev":
    infisical export --env={{env}} > .env

run service env="dev":
    infisical run --env={{env}} -- ./gradlew --console=plain :services:{{service}}:bootRun