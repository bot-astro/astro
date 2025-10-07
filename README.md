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
- [Demo](https://youtube.com)


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
This project contains 4 services:
- `bot`: the Discord bot itself
- `central-api`: the REST api for the bot
- `entitlements-expiration-job`: simple service that checks for expired Discord premium application entitlements and updates the bot database
- `support-bot`: a Discord bot that manages the support server of the bot, mainly used to apply the premium role to premium users

##### BigQuery
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
4) Run the services.  
   If using IntelliJ, you will be provided with four run configurations, one for each service, already configured to pick up the correct environment files.  

All the services should be up and running at this point.  

##### MongoDB and Redis dashboards
You can use some web dashboards for local dev with MongoDB and Redis:

| Service | web dashboard                           |    
|---------|:----------------------------------------|
| MongoDB | [localhost:8081](http://localhost:8081) |    
| Redis   | [localhost:8082](http://localhost:8082) |

> [!CAUTION]
> Editing documents with the MongoDB dashboard **is not recommended** as it tends to mess up data types!

##### OpenAPI
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
- Kubernetes cluster
- Redis instance
- MongoDB instance
- [Semaphore](http://semaphore.io) account
- [kubectl](https://kubernetes.io/docs/tasks/tools/) installed on your local machine

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



<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

* []()
* []()
* []()

<p align="right">(<a href="#readme-top">back to top</a>)</p>