## Development
Astro requires the following services:
- MongoDB
- Redis
- BigQuery (can be mocked for local dev)

A Docker compose file for development purposes is available in `/docker/compose`, named `docker-compose-dev.yml`  
It creates all the required services and their web dashboards:

| Service | username | password | endpoint | web dashboard                           |    
|---------|:----------:|:----------:|:----------|:----------------------------------------|    
| MongoDB |   |   | localhost:27017 | [localhost:8081](http://localhost:8081) |    
| Redis   |   |   | localhost:6379 | [localhost:8082](http://localhost:8082) |

> Important: editing documents with the MongoDB dashboard **is not recommended** as it tends to mess up data types!

### OpenAPI
| Service     | development url                                                         |
|-------------|-------------------------------------------------------------------------|
| bot         | [localhost:9000/swagger-ui.html](http://localhost:9000/swagger-ui.html) |
| central-api | [localhost:9001/swagger-ui.html](http://localhost:9001/swagger-ui.html) |
| support-bot | [localhost:9003/swagger-ui.html](http://localhost:9003/swagger-ui.html) |


## Deployment
### Kubernetes secrets creation
```
kubectl create namespace astro
kubectl create namespace astro-staging

kubectl -n astro-staging create configmap bot-staging-config --from-env-file=./env/bot/staging.env
kubectl -n astro-staging create configmap central-api-staging-config --from-env-file=./env/central-api/staging.env

kubectl -n astro create configmap bot-config --from-env-file=./env/bot/prod.env
kubectl -n astro create configmap central-api-config --from-env-file=./env/central-api/prod.env
kubectl -n astro create configmap entitlements-expiration-job-config --from-env-file=./env/entitlements-expiration-job/prod.env
kubectl -n astro create configmap support-bot-config --from-env-file=./env/support-bot/prod.env

kubectl -n astro-staging create secret generic gcp-bigquery-creds --from-file=gcp-bigquery-creds.json
kubectl -n astro create secret generic gcp-bigquery-creds --from-file=gcp-bigquery-creds.json

kubectl create secret docker-registry ghcr-secret --docker-server=ghcr.io --docker-username=Giuliopime --docker-password=ghp_PAT -n astro-staging
kubectl create secret docker-registry ghcr-secret --docker-server=ghcr.io --docker-username=Giuliopime --docker-password=ghp_PAT -n astro
```