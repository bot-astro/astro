<a id="readme-top"></a>
# Astro

<img src="https://astro-bot.space/img/logo_bg.png" width="40%" align="right">

Astro is the most unique and complete discord bot for temporary voice channels and voice roles!  

This repository contains the code for the *backend* of the bot.

Other repositories:
- [Frontend](https://github.com/bot-astro/astro-bot.space)
- [Infrastructure](https://github.com/bot-astro/infra) (GitOps) and [cluster](https://github.com/giuliopime/gport) (Terraform)

Resources:
- [Website](https://astro-bot.space)
- [Demo](https://youtu.be/6HF8EwMBanY)

## Table of Contents
- [About The Project](#about-the-project)
- [Development](#development)
    - [Prerequisites](#prerequisites)
    - [Understanding the codebase](#understanding-the-codebase)
        - [bot](#bot)
        - [api-central](#api-central)
        - [entitlements-expiration-job](#entitlements-expiration-job)
        - [support-bot](#support-bot)
        - [BigQuery](#bigquery)
    - [Running the application](#running-the-application)
        - [MongoDB and Redis dashboards](#mongodb-and-redis-dashboards)
        - [OpenAPI](#openapi)
- [Deployment](#deployment)
    - [Prerequisites](#prerequisites-1)
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
- JDK 25
- Docker
- [just](https://github.com/casey/just)
- [Infisical CLI](https://infisical.com/docs/cli/overview), secrets and configs are there, there are no `.env` files to fill by hand
- [IntelliJ](https://www.jetbrains.com/idea/) (not necessary but recommended)

### Understanding the codebase
Everything runs via SpringBoot and there is a `shared/core` module that contains the shared code between the services.  
Code is mostly well documented / self-explanatory, so you should be able to take a look at it and get an idea of how things work quite easily.  

This project contains four services:  
#### `bot`
The Discord bot itself, it receives events from Discord and responds to them.  
All events are bridged from JDA (the Java library to interact with the Discord API) to SpringBoot.  

Regarding interactions, like slash commands, buttons, menus and modal, their logic is almost all unified in the `interactions` package:  
- `InteractionResponder`: used by all interactions to submit replies to the user  
- `InteractionHandlers`: the type-independent interaction declarations, their implementation can be a command, button, menu, etc... 
- `InteractionContext`: groups together everything needed by any type of interaction to run, has subclasses for interaction categories
- `guards`: checks that run before an interaction (permissions, premium, votes)

Messages are built with Components V2, using the blocks in `ui/Containers.kt`, and texts live in `shared/core/src/main/resources/i18n` (managed on Crowdin).  

It includes a small REST api for retrieving roles and channels that the dashboard needs to present the user with.  
The dashboard doesn't directly query the bot service, but instead queries `api-central` which in turn queries the bot service.  

It interacts with:
- BigQuery: for storing statistics about the bot usage
- MongoDB: storage for user / guild settings
- Redis: volatile cache and storage for temporary voice channels data

#### `api-central`
A REST api for the bot, it's used by the dashboard to retrieve settings and data from the bot database.  

User authentication is managed via a combination of JWT tokens and session cookies, [docs/authentication.md](docs/authentication.md) explains how.  

#### `entitlements-expiration-job`
Simple service that checks for expired Discord premium application entitlements and updates the bot database.  
Nothing fancy, ran as a k8s cronjob.  

#### `support-bot`
A Discord bot that manages the support server of the bot, mainly used to apply the premium role to premium users.
Includes a REST api used by the `bot` service, when it receives an entitlement event from Discord, it sends a request to the support bot to update the user's role.   

#### BigQuery
BigQuery is used for gathering statistics about the bot usage, mainly commands used, guilds joined / left and temporary voice channels generated.  
It is completely optional and you can skip configuring it if you don't need it (`BOT__BIGQUERY_ENABLED=false`).  

If you instead want to use it, you need to:
1) Create an account and a project on [Google Cloud](https://cloud.google.com/)
2) Enable BigQuery API
3) Create a dataset
4) Create the 5 tables (`CONNECTION_INVOCATION`, `GUILD_EVENT`, `GUILD_SNAPSHOT`, `SLASH_COMMAND_INVOCATION`, `TEMPORARY_VC_GENERATION`), all partitioned by `DAY` on the `timestamp` field.  
   The schemas are in [`infra/terraform/gcp/main.tf`](https://github.com/bot-astro/infra/blob/master/terraform/gcp/main.tf), which can also create everything for you.
5) Create a service account with the `BigQuery Admin` permission (well that's a bit overkill so you can select the appropriate permissions if you need).  
6) Create and download a JSON key, you will be asked for its content in the config.

To configure authentication to BigQuery for local development, instead of using the JSON key, you can follow [these instructions](https://cloud.google.com/bigquery/docs/authentication#client-libs).  
While for production, you should use the JSON key.  

### Running the application
1) Run Docker compose
    ```shell
    just docker-up
    ```
2) Run the services, `just` pulls the configs from the `dev` environment of Infisical (each variable of `application.yml` is there)
    ```shell
    just run-bot
    just run-api-central
    just run-support-bot
    just run-entitlements-expiration-job
    ```
   If using IntelliJ you can also run them from the IDE, using `just generate-env` to export the variables to a `.env` file.  
3) If you forked the repo, set `GHCR_ORGANIZATION` (see [Configure Semaphore](#configure-semaphore)) to your GitHub username or organization name.  

All the services should be up and running at this point.  
Run `just` to see all the other tasks, and `just hooks` before committing.  

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

| Service     | development url                                   |
|-------------|---------------------------------------------------|
| bot         | [localhost:9000/docs](http://localhost:9000/docs) |
| api-central | [localhost:9001/docs](http://localhost:9001/docs) |
| support-bot | [localhost:9002/docs](http://localhost:9002/docs) |

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Deployment
> [!WARNING]
> I do **not** provide support for self-hosting the bot.

### Prerequisites
- A fork of this repository
- Kubernetes cluster with [ArgoCD](https://argo-cd.readthedocs.io), [Infisical operator](https://infisical.com/docs/integrations/platforms/kubernetes) and sealed-secrets
- A fork of the [infra](https://github.com/bot-astro/infra) repository, that is what ArgoCD deploys
- Redis instance
- MongoDB instance
- [Infisical](https://infisical.com) project
- [Semaphore](http://semaphore.io) account
- [Sentry](https://sentry.io)

All the configs and secrets of the services live in Infisical, the Infisical operator syncs them in the cluster, check the `application.yml` of each service to see what they need.  
How to set up the cluster and deploy with ArgoCD is explained in the [infra](https://github.com/bot-astro/infra) repository.

### Configure Semaphore
1) Create a new organization
2) Fork this repository
3) Create a new project in the newly created organization, using your forked repository
4) Update the `GHCR_ORGANIZATION` variable in `.semaphore/semaphore.yml` to your GitHub username or organization name.
5) Go into Organization settings > Secrets and add the following secrets:
   1) `github` with these environment variables:
   
      | Environment Variable | Description                                       |
      |----------------------|---------------------------------------------------|
      | GITHUB_ACTOR         | GitHub username for actions                       |
      | GITHUB_TOKEN         | GitHub token for authentication, [instructions](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens) |
   2) `sentry` with these environment variables:  
   
      | Environment Variable | Description                          |
      |----------------------|--------------------------------------|
      | SENTRY_AUTH_TOKEN    | Sentry auth token, [instructions](https://docs.sentry.io/account/auth-tokens/) |
   3) `infra-deploy-key` with the following configuration files:

      | Configuration Files | Description                                     |
      |---------------------|-------------------------------------------------|
      | `/home/semaphore/.ssh/infra_deploy_key`   | Private key of a [deploy key](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys) with write access to your `infra` fork |

Now when you commit to `master`, Semaphore will automatically:  
- build the Docker image for each service that changed
- upload it to GitHub Container Registry
- update the version in `infra`, automatically for staging and via a promotion button for production, then ArgoCD deploys it

> [!NOTE]
> Images are tagged with the commit sha and are only built for the services that changed in that commit, so promote a service to production from a build that actually included it.  

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTRIBUTING -->
## Contributing
If you have an idea, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks!

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
