-- Removals of the last 90 days grouped by how long the guild had the bot, measured from the latest join before the removal.
-- Joins are looked up over the last 365 days, a removal without a join in that window (joined before tracking started) is "pre-tracking".
WITH removals AS (
  SELECT guild_id, timestamp AS removed_at
  FROM `astro_bot.GUILD_EVENT`
  WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY)) AND action = 'KICKED'
),
joins AS (
  SELECT guild_id, timestamp AS joined_at
  FROM `astro_bot.GUILD_EVENT`
  WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 365 DAY)) AND action = 'JOINED'
),
aged AS (
  SELECT r.guild_id, r.removed_at, MAX(j.joined_at) AS joined_at
  FROM removals r
  LEFT JOIN joins j ON j.guild_id = r.guild_id AND j.joined_at <= r.removed_at
  GROUP BY r.guild_id, r.removed_at
)
SELECT
  CASE
    WHEN joined_at IS NULL THEN 'pre-tracking'
    WHEN TIMESTAMP_DIFF(removed_at, joined_at, SECOND) < 60 THEN '<1min'
    WHEN TIMESTAMP_DIFF(removed_at, joined_at, MINUTE) < 60 THEN '<1h'
    WHEN TIMESTAMP_DIFF(removed_at, joined_at, HOUR) < 24 THEN '<1d'
    WHEN TIMESTAMP_DIFF(removed_at, joined_at, DAY) < 30 THEN '<30d'
    ELSE '30d+'
  END AS age,
  COUNT(*) AS removals
FROM aged
GROUP BY age
ORDER BY MIN(CASE
  WHEN joined_at IS NULL THEN 5
  WHEN TIMESTAMP_DIFF(removed_at, joined_at, SECOND) < 60 THEN 0
  WHEN TIMESTAMP_DIFF(removed_at, joined_at, MINUTE) < 60 THEN 1
  WHEN TIMESTAMP_DIFF(removed_at, joined_at, HOUR) < 24 THEN 2
  WHEN TIMESTAMP_DIFF(removed_at, joined_at, DAY) < 30 THEN 3
  ELSE 4
END)
