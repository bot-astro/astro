-- Weekly join cohorts of the last 26 weeks and the share of each cohort that generated a VC N weeks after joining.
-- A column is NULL while the cohort is not yet N+1 weeks old, so a missing value is not read as churn.
WITH cohorts AS (
  SELECT guild_id, DATE_TRUNC(DATE(MIN(timestamp)), WEEK) AS cohort_week
  FROM `astro_bot.GUILD_EVENT`
  WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 26 WEEK)) AND action = 'JOINED'
  GROUP BY guild_id
),
activity AS (
  SELECT DISTINCT guild_id, DATE_TRUNC(DATE(timestamp), WEEK) AS active_week
  FROM `astro_bot.TEMPORARY_VC_GENERATION`
  WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 26 WEEK))
)
SELECT
  c.cohort_week,
  COUNT(DISTINCT c.guild_id) AS guilds,
  IF(DATE_DIFF(CURRENT_DATE(), c.cohort_week, WEEK) > 1,
    ROUND(COUNT(DISTINCT IF(DATE_DIFF(a.active_week, c.cohort_week, WEEK) = 1, c.guild_id, NULL)) / COUNT(DISTINCT c.guild_id), 3),
    NULL) AS week_1,
  IF(DATE_DIFF(CURRENT_DATE(), c.cohort_week, WEEK) > 4,
    ROUND(COUNT(DISTINCT IF(DATE_DIFF(a.active_week, c.cohort_week, WEEK) = 4, c.guild_id, NULL)) / COUNT(DISTINCT c.guild_id), 3),
    NULL) AS week_4,
  IF(DATE_DIFF(CURRENT_DATE(), c.cohort_week, WEEK) > 12,
    ROUND(COUNT(DISTINCT IF(DATE_DIFF(a.active_week, c.cohort_week, WEEK) = 12, c.guild_id, NULL)) / COUNT(DISTINCT c.guild_id), 3),
    NULL) AS week_12
FROM cohorts c
LEFT JOIN activity a USING (guild_id)
GROUP BY c.cohort_week
ORDER BY c.cohort_week
