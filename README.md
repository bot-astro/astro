## Development
Astro requires the following services:
- MongoDB
- Redis

A Docker compose file for development purposes is available in `/docker/compose`, named `docker-compose-dev.yml`  
It creates all the required services and their web dashboards:

| Service | username | password | endpoint | web dashboard                           |    
|---------|:----------:|:----------:|:----------|:----------------------------------------|    
| MongoDB |   |   | localhost:27017 | [localhost:8081](http://localhost:8081) |    
| Redis |   |   | localhost:6379 | [localhost:8082](http://localhost:8082) |

> Important: editing documents with the MongoDB dashboard **is not recommended** as it tends to mess up data types!