-- For each premium gate (PREMIUM_* error code): guilds that hit it in the last 90 days (gates only fire for non-premium
-- guilds), and how many became premium within 7 / 30 days of their first hit.
DECLARE start_date DATE DEFAULT DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY);

WITH first_hit AS (
  SELECT guild_id, code, MIN(timestamp) AS hit_at
  FROM `astro_bot.CONFIGURATION_ERROR`
  WHERE timestamp >= TIMESTAMP(start_date) AND STARTS_WITH(code, 'PREMIUM_')
  GROUP BY guild_id, code
),
premium_days AS (
  SELECT guild_id, timestamp AS at
  FROM `astro_bot.GUILD_CONFIG_SNAPSHOT`
  WHERE timestamp >= TIMESTAMP(start_date) AND premium
)
SELECT
  h.code,
  COUNT(DISTINCT h.guild_id) AS guilds_hit,
  COUNT(DISTINCT IF(p.at <= TIMESTAMP_ADD(h.hit_at, INTERVAL 7 DAY), h.guild_id, NULL)) AS premium_within_7d,
  COUNT(DISTINCT IF(p.at <= TIMESTAMP_ADD(h.hit_at, INTERVAL 30 DAY), h.guild_id, NULL)) AS premium_within_30d
FROM first_hit h
LEFT JOIN premium_days p ON p.guild_id = h.guild_id AND p.at >= h.hit_at
GROUP BY h.code
ORDER BY guilds_hit DESC;
