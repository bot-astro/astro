-- Among guilds active in the last 30 days, the share that has configured each feature (latest snapshot).
DECLARE start_date DATE DEFAULT DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY);

WITH active AS (
  SELECT DISTINCT guild_id
  FROM `astro_bot.TEMPORARY_VC_GENERATION`
  WHERE timestamp >= TIMESTAMP(start_date)
),
latest AS (
  SELECT *
  FROM `astro_bot.GUILD_CONFIG_SNAPSHOT`
  WHERE timestamp >= TIMESTAMP(start_date)
  QUALIFY ROW_NUMBER() OVER (PARTITION BY guild_id ORDER BY timestamp DESC) = 1
)
SELECT
  COUNT(*) AS active_guilds,
  ROUND(AVG(IF(l.generators > 1, 1, 0)), 3) AS multiple_generators,
  ROUND(AVG(IF(l.interfaces > 0, 1, 0)), 3) AS interfaces,
  ROUND(AVG(IF(l.connections > 0, 1, 0)), 3) AS voice_roles,
  ROUND(AVG(IF(l.templates > 0, 1, 0)), 3) AS templates,
  ROUND(AVG(IF(l.premium, 1, 0)), 3) AS premium
FROM active
JOIN latest l USING (guild_id);
