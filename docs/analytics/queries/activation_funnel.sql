-- Guilds that joined in the last 90 days: did they configure a generator, did they generate a VC, how many days it took.
-- Only VCs after the join count, so a guild that left and came back does not get a negative time.
-- Generator creation is only visible from the daily snapshot, so it is accurate to the day.
DECLARE start_date DATE DEFAULT DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY);

WITH joined AS (
  SELECT guild_id, MIN(timestamp) AS joined_at
  FROM `astro_bot.GUILD_EVENT`
  WHERE timestamp >= TIMESTAMP(start_date) AND action = 'JOINED'
  GROUP BY guild_id
),
first_generator AS (
  SELECT guild_id, MIN(timestamp) AS at
  FROM `astro_bot.GUILD_CONFIG_SNAPSHOT`
  WHERE timestamp >= TIMESTAMP(start_date) AND generators > 0
  GROUP BY guild_id
),
first_vc AS (
  SELECT t.guild_id, MIN(t.timestamp) AS at
  FROM `astro_bot.TEMPORARY_VC_GENERATION` t
  JOIN joined j ON j.guild_id = t.guild_id AND t.timestamp >= j.joined_at
  WHERE t.timestamp >= TIMESTAMP(start_date)
  GROUP BY t.guild_id
)
SELECT
  COUNT(*) AS joined,
  COUNTIF(g.at IS NOT NULL) AS generator_created,
  COUNTIF(v.at IS NOT NULL) AS first_vc,
  COUNTIF(v.at IS NOT NULL AND v.at <= TIMESTAMP_ADD(j.joined_at, INTERVAL 7 DAY)) AS active_within_7d,
  COUNTIF(v.at IS NOT NULL AND v.at <= TIMESTAMP_ADD(j.joined_at, INTERVAL 30 DAY)) AS active_within_30d,
  APPROX_QUANTILES(TIMESTAMP_DIFF(v.at, j.joined_at, HOUR), 2)[OFFSET(1)] AS median_hours_to_first_vc
FROM joined j
LEFT JOIN first_generator g USING (guild_id)
LEFT JOIN first_vc v USING (guild_id);
