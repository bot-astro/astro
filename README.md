<a id="readme-top"></a>
# Astro

<img src="https://astro-bot.space/img/logo_bg.png" width="40%" align="right">

Astro is the most unique and complete discord bot for temporary voice channels and voice roles!  

This repository contains the code for the *backend* of the bot.

Other repositories:
- [Frontend](https://github.com/bot-astro/astro-bot.space)
- [Infrastructure](https://github.com/giuliopime/gport)

Resources:
- [Website](https://astro-bot.space)
- [Demo](https://youtu.be/6HF8EwMBanY)

## Table of Contents
- [About The Project](#about-the-project)
- [Development](#development)
    - [Prerequisites](#prerequisites)
    - [Understanding the codebase](#understanding-the-codebase)
        - [bot](#bot)
        - [central-api](#central-api)
        - [entitlements-expiration-job](#entitlements-expiration-job)
        - [support-bot](#support-bot)
        - [BigQuery](#bigquery)
    - [Running the application](#running-the-application)
        - [MongoDB and Redis dashboards](#mongodb-and-redis-dashboards)
        - [OpenAPI](#openapi)
- [Deployment](#deployment)
    - [Prerequisites](#prerequisites-1)
    - [Configure `gradle.properties`](#configure-gradleproperties)
    - [Create .env files](#create-env-files)
    - [Create Kubernetes resources](#create-kubernetes-resources)
        - [Namespace](#namespace)
        - [Config maps](#config-maps)
        - [Secrets](#secrets)
    - [Configure Helm values](#configure-helm-values)
    - [Configure Semaphore](#configure-semaphore)
- [Contributing](#contributing)
    - [Top contributors](#top-contributors)
- [License](#license)
- [Contact](#contact)


<!-- ABOUT THE PROJECT -->
## About The Project
I initially built this bot for my friends Discord, but then it grew way beyond my expectations.  
You can read the story of this project [on my blog post](https://giuliopime.dev/blog/so-i-built-a-discord-bot)!   

> [!WARNING]
> I do **not** provide support for self-hosting the bot.

## Development
### Prerequisites
- JDK 17
- Docker
- [IntelliJ](https://www.jetbrains.com/idea/) (not necessary but recommended)

### Understanding the codebase
Everything runs via SpringBoot and there is a `shared-core` module that contains the shared code between the services.  
Code is mostly well documented / self-explanatory, so you should be able to take a look at it and get an idea of how things work quite easily.  

This project contains four services:  
#### `bot`
The Discord bot itself, it receives events from Discord and responds to them.  
All events are bridged from JDA (the Java library to interact with the Discord API) to SpringBoot.  

Regarding interactions, like slash commands, buttons, menus and modal, their logic is almost all unified in some `interaction` classes:  
- `InteractionReplyHandler`: used by all interactions to submit replies to the user  
- `InteractionAction`: a type-independent interaction declaration, it's implementation can be a command, button, menu, etc... 
- `InteractionContext`: groups together everything needed by any type of interaction to run, has subclasses for interaction categories

It includes a small REST api for retrieving roles and channels that the dashboard needs to present the user with.  
The dashboard doesn't directly query the bot service, but instead queries `central-api` which in turn queries the bot service.  

It interacts with:
- BigQuery: for storing statistics about the bot usage
- MongoDB: storage for user / guild settings
- Redis: volatile cache and storage for temporary voice channels data

#### `central-api`
A REST api for the bot, it's used by the dashboard to retrieve settings and data from the bot database.  
The dashboard requests the list of roles and channels of a given guild, and to respond to this, this API queries the `bot` service api.  
Since they are both running on k8s and the bot is sharded and divided in pods, the central api needs to calculate the correct pod to send the request to.  
This is done by calculating the shard id of the guild via the guild id, and then the pod by knowing the amount of shards and pods.  

User authentication is managed via a combination of JWT tokens and session cookies.  

#### `entitlements-expiration-job`
Simple service that checks for expired Discord premium application entitlements and updates the bot database.  
Nothing fancy, ran as a k8s cronjob.  

#### `support-bot`
A Discord bot that manages the support server of the bot, mainly used to apply the premium role to premium users.
Includes a REST api used by the `bot` service, when it receives an entitlement event from Discord, it sends a request to the support bot to update the user's role.   

#### BigQuery
BigQuery is used for gathering statistics about the bot usage, mainly commands used, guilds joined / left and temporary voice channels generated.  
It is completely optional and you can skip configuring it if you don't need it.  

If you instead want to use it, you need to:
1) Create an account and a project on [Google Cloud](https://cloud.google.com/)
2) Enable BigQuery API
3) Create a dataset
4) Create 4 tables, all with the following partition settings: partitioned by `DAY` on field `timestamp` with no expiration and partition filter required
   1. `CONNECTION_INVOCATION`
    
    | Field Name     | Type       | Mode      |
    |----------------|-----------|-----------|
    | guild_id       | NUMERIC   | REQUIRED  |
    | user_id        | NUMERIC   | REQUIRED  |
    | connection_id  | NUMERIC   | REQUIRED  |
    | timestamp      | TIMESTAMP | REQUIRED  |
    
    2. `GUILD_EVENT`
    
    | Field Name    | Type      | Mode      |
    |---------------|----------|-----------|
    | guild_id      | NUMERIC  | REQUIRED  |
    | users_count   | INTEGER  | REQUIRED  |
    | action        | STRING   | REQUIRED  |
    | timestamp     | TIMESTAMP| REQUIRED  |
    
    3. `SLASH_COMMAND_INVOCATION`
    
    | Field Name        | Type       | Mode      |
    |-------------------|-----------|-----------|
    | name              | STRING    | REQUIRED  |
    | guild_id          | NUMERIC   | REQUIRED  |
    | channel_id        | NUMERIC   | REQUIRED  |
    | user_id           | NUMERIC   | REQUIRED  |
    | main_option_name  | STRING    | NULLABLE  |
    | main_option_value | STRING    | NULLABLE  |
    | raw_options       | STRING    | NULLABLE  |
    | timestamp         | TIMESTAMP | REQUIRED  |
    
    4. `TEMPORARY_VC_GENERATION`
    
    | Field Name   | Type      | Mode      |
    |--------------|----------|-----------|
    | guild_id     | NUMERIC  | REQUIRED  |
    | user_id      | NUMERIC  | REQUIRED  |
    | generator_id | NUMERIC  | REQUIRED  |
    | timestamp    | TIMESTAMP| REQUIRED  |
5) Create a service account with the `BigQuery Admin` permission (well that's a bit overkill so you can select the appropriate permissions if you need).  
6) Create and download a JSON key, you will be asked for a path to it in the .env files.

To configure authentication to BigQuery for local development, instead of using the JSON key, you can follow [these instructions](https://cloud.google.com/bigquery/docs/authentication#client-libs).  
While for production, you should use the JSON key.  

### Running the application
1) Run Docker compose
    ```shell
    docker compose -f docker/docker-compose-dev.yml up -d
    ```
2) Create the development `dev.env` files  
    The `/env` folder contains a `.env.template` file for each service + 1 common `.env.template` shared by all services.  
    Create a `dev.env` file for each service inside the `/env` folder and, in each of them, copy both the content of `/env/shared-core/.env.template` and the content of the `.env.template` file of the service.  
3) Fill the `dev.env` files, each variable has a comment explaining what it does.  
4) If you forked the repo, update the `ghcrOrg` value in `gradle.properties` to your GitHub username or organization name.  
5) Run the services.  
   If using IntelliJ, you will be provided with four run configurations, one for each service, already configured to pick up the correct environment files.  

All the services should be up and running at this point.  

#### MongoDB and Redis dashboards
You can use some web dashboards for local dev with MongoDB and Redis:

| Service | web dashboard                           |    
|---------|:----------------------------------------|
| MongoDB | [localhost:8081](http://localhost:8081) |    
| Redis   | [localhost:8082](http://localhost:8082) |

> [!CAUTION]
> Editing documents with the MongoDB dashboard **is not recommended** as it tends to mess up data types!

#### OpenAPI
You can access the OpenAPI documentation for each service at the following URLs:

| Service     | development url                                                         |
|-------------|-------------------------------------------------------------------------|
| bot         | [localhost:9000/swagger-ui.html](http://localhost:9000/swagger-ui.html) |
| central-api | [localhost:9001/swagger-ui.html](http://localhost:9001/swagger-ui.html) |
| support-bot | [localhost:9002/swagger-ui.html](http://localhost:9003/swagger-ui.html) |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Deployment
> [!WARNING]
> I do **not** provide support for self-hosting the bot.

### Prerequisites
- A fork of this repository
- Kubernetes cluster
- Redis instance
- MongoDB instance
- [Semaphore](http://semaphore.io) account
- [Sentry](https://sentry.io)
- [kubectl](https://kubernetes.io/docs/tasks/tools/) installed on your local machine and with access to your k8s cluster.

### Configure `gradle.properties`
Update the `ghcrOrg` value in `gradle.properties` to your GitHub username or organization name.  

### Create .env files
Create the production `prod.env` files (you can also use the development ones created in the previous section, just remember in the following steps that your files are called `dev.env`).    
The `/env` folder contains a `.env.template` file for each service + 1 common `.env.template` shared by all services.  
Create a `prod.env` file for each service inside the `/env` folder and, in each of them, copy both the content of `/env/shared-core/.env.template` and the content of the `.env.template` file of the service.

### Create Kubernetes resources
#### Namespace
```shell
kubectl create namespace astro
```
#### Config maps
```shell
kubectl -n astro create configmap bot-config --from-env-file=./env/bot/prod.env
kubectl -n astro create configmap central-api-config --from-env-file=./env/central-api/prod.env
kubectl -n astro create configmap entitlements-expiration-job-config --from-env-file=./env/entitlements-expiration-job/prod.env
kubectl -n astro create configmap support-bot-config --from-env-file=./env/support-bot/prod.env
```

#### Secrets
Follow the instructions in the [BigQuery section](#bigquery) to obtain the service account JSON key.  
You can skip this step if you don't wanna enable BigQuery.  
```shell
kubectl -n astro create secret generic gcp-bigquery-creds --from-file=gcp-bigquery-creds.json
```

This allows your cluster to pull images from GitHub Container Registry.  
Replace `your_github_username` and `your_github_token` with your GitHub username and token ([token instructions](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)).   
```shell
export DOCKER_USERNAME=your_github_username
export DOCKER_PASSWORD=your_github_token
kubectl create secret docker-registry docker-registry --docker-server=ghcr.io --docker-username=$DOCKER_USERNAME --docker-password=$DOCKER_PASSWORD -n astro
```

### Configure Helm values
Each service has a `/chart` folder that contains Helm charts that get deployed on Kubernetes using Semaphore promotions.    
You need to configure the value files, so for each service copy the `/service/{service_name}/chart/template.values.yaml` to `/service/{service_name}/chart/values.yaml` and fill the values.  

### Configure Semaphore
1) Create a new organization
2) Fork this repository
3) Create a new project in the newly created organization, using your forked repository
4) Update the `ghcrOrg` value in `gradle.properties` to your GitHub username or organization name.
5) Go into Organization settings > Secrets and add the following secrets:
   1) `github` with these environment variables:
   
      | Environment Variable | Description                                       |
      |----------------------|---------------------------------------------------|
      | GITHUB_ACTOR         | GitHub username for actions                       |
      | GITHUB_TOKEN         | GitHub token for authentication, [instructions](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) |
   2) `sentry` with these environment variables:  
   
      | Environment Variable | Description                          |
      |----------------------|--------------------------------------|
      | SENTRY_AUTH_TOKEN    | Sentry auth token, [instructions](https://docs.sentry.io/account/auth-tokens/)® |
   3) `kube` with the following configuration files:

      | Configuration Files | Description                                     |
      |---------------------|-------------------------------------------------|
      | `/home/semaphore/.kube/config`   | Upload the kubeconfig file for your k8s cluster |
   4) for each service, create a secret using the same name as the service and add in `Configuration Files`: `/home/semaphore/.values/{service-name}/production.yaml` as the path (replace `{service-name}`) and provide the `values.yaml` file to it (you should have configured them previously in `services/{service-name}/charts/values.yaml`).

Now when you commit to any branch, Semaphore will automatically:  
- build the Docker image for each service
- upload it to GitHub Container Registry
- give you a button to deploy the new image to your Kubernetes cluster


<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTRIBUTING -->
## Contributing
If you have an idea, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Top contributors:

<a href="https://github.com/bot-astro/astro/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=bot-astro/astro" alt="contrib.rocks image" />
</a>

<!-- LICENSE -->
## License

Distributed under the AGPL-3.0 license. See `LICENSE.txt` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->
## Contact
- [Discord server](https://astro-bot.space/support)
- [hi@astro-bot.space](mailto:hi@astro-bot.space)
- [giuliopime.dev](https://giuliopime.dev)

<p align="right">(<a href="#readme-top">back to top</a>)</p>
