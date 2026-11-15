-- Weekly join cohorts of the last 26 weeks and the share of each cohort that generated a VC N weeks after joining.
DECLARE start_date DATE DEFAULT DATE_SUB(CURRENT_DATE(), INTERVAL 26 WEEK);

WITH cohorts AS (
  SELECT guild_id, DATE_TRUNC(DATE(MIN(timestamp)), WEEK) AS cohort_week
  FROM `astro_bot.GUILD_EVENT`
  WHERE timestamp >= TIMESTAMP(start_date) AND action = 'JOINED'
  GROUP BY guild_id
),
activity AS (
  SELECT DISTINCT guild_id, DATE_TRUNC(DATE(timestamp), WEEK) AS active_week
  FROM `astro_bot.TEMPORARY_VC_GENERATION`
  WHERE timestamp >= TIMESTAMP(start_date)
)
SELECT
  c.cohort_week,
  COUNT(DISTINCT c.guild_id) AS guilds,
  ROUND(COUNT(DISTINCT IF(DATE_DIFF(a.active_week, c.cohort_week, WEEK) = 1, c.guild_id, NULL)) / COUNT(DISTINCT c.guild_id), 3) AS week_1,
  ROUND(COUNT(DISTINCT IF(DATE_DIFF(a.active_week, c.cohort_week, WEEK) = 4, c.guild_id, NULL)) / COUNT(DISTINCT c.guild_id), 3) AS week_4,
  ROUND(COUNT(DISTINCT IF(DATE_DIFF(a.active_week, c.cohort_week, WEEK) = 12, c.guild_id, NULL)) / COUNT(DISTINCT c.guild_id), 3) AS week_12
FROM cohorts c
LEFT JOIN activity a USING (guild_id)
GROUP BY c.cohort_week
ORDER BY c.cohort_week;
