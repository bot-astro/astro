-- Most common configuration errors of the last 30 days, with the guilds affected, and the share of those guilds
-- that generated a VC in the 30 days after their first error. The share only counts guilds whose first error is at least
-- 30 days old, so the observation window is complete (compare it with the overall retention to see which errors hurt).
DECLARE start_date DATE DEFAULT DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY);
DECLARE errors_from DATE DEFAULT DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY);

WITH errors AS (
  SELECT code, guild_id, COUNT(*) AS occurrences, MIN(timestamp) AS first_at
  FROM `astro_bot.CONFIGURATION_ERROR`
  WHERE timestamp >= TIMESTAMP(errors_from)
  GROUP BY code, guild_id
),
mature_errors AS (
  SELECT code, guild_id, MIN(timestamp) AS first_at
  FROM `astro_bot.CONFIGURATION_ERROR`
  WHERE timestamp >= TIMESTAMP(start_date) AND timestamp < TIMESTAMP(errors_from)
  GROUP BY code, guild_id
),
activity AS (
  SELECT DISTINCT guild_id, timestamp
  FROM `astro_bot.TEMPORARY_VC_GENERATION`
  WHERE timestamp >= TIMESTAMP(start_date)
),
retention AS (
  SELECT
    e.code,
    COUNT(DISTINCT e.guild_id) AS guilds_observed,
    COUNT(DISTINCT IF(a.guild_id IS NOT NULL, e.guild_id, NULL)) AS guilds_active_after
  FROM mature_errors e
  LEFT JOIN activity a
    ON a.guild_id = e.guild_id
    AND a.timestamp > TIMESTAMP_ADD(e.first_at, INTERVAL 1 DAY)
    AND a.timestamp <= TIMESTAMP_ADD(e.first_at, INTERVAL 30 DAY)
  GROUP BY e.code
)
SELECT
  e.code,
  SUM(e.occurrences) AS occurrences,
  COUNT(DISTINCT e.guild_id) AS guilds,
  ROUND(SAFE_DIVIDE(r.guilds_active_after, r.guilds_observed), 3) AS share_active_after_error
FROM errors e
LEFT JOIN retention r USING (code)
GROUP BY e.code, r.guilds_active_after, r.guilds_observed
ORDER BY guilds DESC;
