-- Most common configuration errors of the last 30 days, with the guilds affected and how many of them
-- generated a VC in the following 30 days (compare it with the overall retention to see which errors hurt).
DECLARE start_date DATE DEFAULT DATE_SUB(CURRENT_DATE(), INTERVAL 60 DAY);
DECLARE errors_from DATE DEFAULT DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY);

WITH errors AS (
  SELECT code, guild_id, COUNT(*) AS occurrences, MIN(timestamp) AS first_at
  FROM `astro_bot.CONFIGURATION_ERROR`
  WHERE timestamp >= TIMESTAMP(errors_from)
  GROUP BY code, guild_id
),
activity AS (
  SELECT DISTINCT guild_id, timestamp
  FROM `astro_bot.TEMPORARY_VC_GENERATION`
  WHERE timestamp >= TIMESTAMP(start_date)
)
SELECT
  e.code,
  SUM(e.occurrences) AS occurrences,
  COUNT(DISTINCT e.guild_id) AS guilds,
  ROUND(COUNT(DISTINCT IF(a.guild_id IS NOT NULL, e.guild_id, NULL)) / COUNT(DISTINCT e.guild_id), 3) AS share_active_after_error
FROM errors e
LEFT JOIN activity a ON a.guild_id = e.guild_id AND a.timestamp > TIMESTAMP_ADD(e.first_at, INTERVAL 1 DAY)
GROUP BY e.code
ORDER BY guilds DESC;
