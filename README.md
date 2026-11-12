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
    - [Running the application](#running-the-application)
- [Deployment](#deployment)
- [Contributing](#contributing)
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
- [Infisical CLI](https://infisical.com/docs/cli/overview), secrets and configs live there, there are no `.env` files to fill by hand
- [IntelliJ](https://www.jetbrains.com/idea/) (not necessary but recommended)

### Understanding the codebase
Everything runs via SpringBoot and there is a `shared/core` module that contains the code shared between the services.  
Code is mostly well documented / self-explanatory, so you should be able to take a look at it and get an idea of how things work quite easily.  
More in depth stuff is in the [docs](docs) folder.

This project contains four services:  
#### `bot`
The Discord bot itself, it receives events from Discord and responds to them.  
All events are bridged from JDA (the Java library to interact with the Discord API) to SpringBoot.  
Interactions (slash commands, buttons, menus...) all go through the same `interactions` package: a handler per interaction, a context with everything it needs, guards (permissions, premium, votes) and a single responder to reply to the user.  
Messages are built with Components V2, using the blocks in `ui/Containers.kt`, and texts live in `shared/core/src/main/resources/i18n` (managed on Crowdin).  

It also has a small REST api, used only by `api-central`, to retrieve what the dashboard needs from Discord (roles, channels...) and to edit the guild profile.

It interacts with:
- BigQuery: for storing statistics about the bot usage
- MongoDB: storage for user / guild settings
- Redis: volatile cache and storage for temporary voice channels data

#### `api-central`
The REST api used by the dashboard, to read and edit the settings of a guild.  
The bot is sharded and divided in pods, so to reach the right one `api-central` calculates the shard of the guild from its id, and then the pod from the amount of shards and pods.  
It also handles the login with Discord, the Chargebee webhooks and the media (images) uploads to R2.  
Authentication is explained in [docs/authentication.md](docs/authentication.md).

#### `entitlements-expiration-job`
Simple service that checks for expired Discord premium application entitlements and updates the bot database.  
Nothing fancy, ran as a k8s cronjob.  

#### `support-bot`
A Discord bot that manages the support server of the bot, mainly used to apply the premium role to premium users.  
Includes a REST api used by the `bot` service, when it receives an entitlement event from Discord, it sends a request to the support bot to update the user's role.   

#### BigQuery
Used for statistics about the bot usage: commands used, guilds joined / left and temporary voice channels generated.  
It is completely optional, with `BOT__BIGQUERY_ENABLED=false` the bot doesn't need it.  
Datasets and tables are created by Terraform, you can find them in [`infra/terraform/gcp`](https://github.com/bot-astro/infra/tree/master/terraform/gcp).

### Running the application
Run `just` to see all the available tasks.
1) Start MongoDB and Redis (with their web dashboards)
    ```shell
    just docker-up
    ```
2) Run the services, secrets are pulled from the `dev` environment of Infisical
    ```shell
    just run-bot
    just run-api-central
    just run-support-bot
    just run-entitlements-expiration-job
    ```

| Service     | Port | OpenAPI docs                            |
|-------------|------|-----------------------------------------|
| bot         | 9000 | [localhost:9000/docs](http://localhost:9000/docs) |
| api-central | 9001 | [localhost:9001/docs](http://localhost:9001/docs) |
| support-bot | 9002 | [localhost:9002/docs](http://localhost:9002/docs) |

The management ports (`9100`, `9101`, `9102`, health and metrics) are never exposed outside the cluster.

| Service | web dashboard                           |    
|---------|:----------------------------------------|
| MongoDB | [localhost:8081](http://localhost:8081) |    
| Redis   | [localhost:8082](http://localhost:8082) |

> [!CAUTION]
> Editing documents with the MongoDB dashboard **is not recommended** as it tends to mess up data types!

Before committing run `just hooks` (ktlint and detekt).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Deployment
> [!WARNING]
> I do **not** provide support for self-hosting the bot.

Everything is deployed on Kubernetes by ArgoCD, from the [infra](https://github.com/bot-astro/infra) repository, secrets come from Infisical.  
Semaphore does the rest, on every commit to `master`:
- builds and tests each service that changed
- pushes the Docker image (tagged with the commit sha) to GitHub Container Registry
- updates the version of the service in `infra`, automatically for `astro-staging` and with a manual promotion for `astro` (production)

Secrets needed by Semaphore:

| Secret             | Content                                                           |
|--------------------|-------------------------------------------------------------------|
| `github`           | `GITHUB_ACTOR` and `GITHUB_TOKEN` (`write:packages`)               |
| `sentry`           | `SENTRY_AUTH_TOKEN`                                               |
| `infra-deploy-key` | private key (file `/home/semaphore/.ssh/infra_deploy_key`) of a deploy key with write access to `infra` |

An image only exists for the commits where that service was built, so promote a service to production from a build that actually included it.  
The steps I followed to migrate everything to this setup are in [docs/deploy-hyperion.md](docs/deploy-hyperion.md).

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
