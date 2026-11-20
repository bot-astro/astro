-- Joins in the last 90 days by install source, and how many of those guilds are still active (generated a temporary VC)
-- or removed the bot afterwards. Installs that don't pass through the website (App Directory, Add App button, links
-- inside Discord) have no INSTALL_EVENT and are grouped as 'in_discord'.
WITH joins AS (
  SELECT guild_id, MIN(timestamp) AS joined_at
  FROM `astro_bot.GUILD_EVENT`
  WHERE action = 'JOINED' AND timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY))
  GROUP BY guild_id
),
sources AS (
  SELECT guild_id, ARRAY_AGG(source ORDER BY timestamp DESC LIMIT 1)[OFFSET(0)] AS source
  FROM `astro_bot.INSTALL_EVENT`
  WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY))
  GROUP BY guild_id
),
active AS (
  SELECT DISTINCT guild_id
  FROM `astro_bot.TEMPORARY_VC_GENERATION`
  WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY))
),
removed AS (
  SELECT DISTINCT j.guild_id
  FROM joins j
  JOIN `astro_bot.GUILD_EVENT` e ON e.guild_id = j.guild_id AND e.action = 'KICKED' AND e.timestamp >= j.joined_at
  WHERE e.timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY))
)
SELECT
  COALESCE(s.source, 'in_discord') AS source,
  COUNT(*) AS installs,
  COUNTIF(a.guild_id IS NOT NULL) AS active_guilds,
  COUNTIF(r.guild_id IS NOT NULL) AS removed_guilds
FROM joins j
LEFT JOIN sources s USING (guild_id)
LEFT JOIN active a USING (guild_id)
LEFT JOIN removed r USING (guild_id)
GROUP BY source
ORDER BY installs DESC
