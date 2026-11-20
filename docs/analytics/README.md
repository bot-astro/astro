# Analytics queries

SQL for the BigQuery dataset (`astro_bot` in production). Replace the dataset in the queries if you use another one.
`create_views.sh` creates each query as a view named `ANALYTICS_<FILE>` (for example `ANALYTICS_RETENTION_COHORTS`) in `astro_bot`, so they can be opened from the BigQuery console or Looker Studio. Rerun it after editing a query. The queries use only date expressions that BigQuery can prune on, so they also work as views on partition-filtered tables.

All tables are partitioned by `timestamp` with a required partition filter, so every query filters on it.
Run them with a cost cap, for example `bq query --use_legacy_sql=false --maximum_bytes_billed=1000000000 < file.sql`.

"Active" means the guild generated at least one temporary VC (`TEMPORARY_VC_GENERATION`).
`GUILD_CONFIG_SNAPSHOT` only exists from the day it was first deployed, so queries that use it only cover guilds from then on.

| File | Question |
|---|---|
| `activation_funnel.sql` | Of the guilds that joined, how many configured a generator and generated a VC, and how fast |
| `retention_cohorts.sql` | Of the guilds that joined each week, how many are active 1, 4 and 12 weeks later |
| `feature_adoption.sql` | Share of active guilds that configured generators, interfaces, connections, templates |
| `premium_funnel.sql` | Guilds that hit a premium gate and became premium within 7 / 30 days |
| `top_configuration_errors.sql` | Most common errors, guilds affected and how many of them are still active a month later |
| `heavy_guilds.sql` | Top 100 guilds by temporary VCs generated, with premium flag |
| `daily_growth.sql` | Guilds that added and removed the bot per day, and the net change |
| `removals_by_age.sql` | How long guilds kept the bot before removing it (<1min, <1h, <1d, <30d, 30d+, pre-tracking) |
| `installs_by_source.sql` | Joins per install source (`in_discord` when there is no website install event) and how many of those guilds are active or removed the bot |
