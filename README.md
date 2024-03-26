## Development
Astro requires the following services:
- MongoDB
- Redis
- InfluxDB
- BigQuery

A Docker compose file for development purposes is available in `/docker/compose`, named `docker-compose-dev.yml`  
It creates all the required services and their web dashboards:

| Service | username | password | endpoint | web dashboard                           |    
|---------|:----------:|:----------:|:----------|:----------------------------------------|    
| MongoDB |   |   | localhost:27017 | [localhost:8081](http://localhost:8081) |    
| Redis   |   |   | localhost:6379 | [localhost:8082](http://localhost:8082) |
| Influx  |   |   | localhost:8086 |  |

> Important: editing documents with the MongoDB dashboard **is not recommended** as it tends to mess up data types!

### Kubernetes secrets creation
```
kubectl create namespace astro
kubectl -n astro create configmap bot-config --from-env-file=./env/bot/prod.env
kubectl -n astro create configmap central-api-config --from-env-file=./env/central-api/prod.env
kubectl -n astro create configmap entitlements-expiration-job-config --from-env-file=./env/entitlements-expiration-job/prod.env
kubectl -n astro create configmap support-bot-config --from-env-file=./env/support-bot/prod.env
```