-- Installs through the website invite in the last 90 days by source, and how many of those guilds are still active
-- (generated a temporary VC) or removed the bot. Installs from the App Directory never pass through the website, they have no row here.
WITH installs AS (
  SELECT source, guild_id, MIN(timestamp) AS installed_at
  FROM `astro_bot.INSTALL_EVENT`
  WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY))
  GROUP BY source, guild_id
),
active AS (
  SELECT DISTINCT guild_id
  FROM `astro_bot.TEMPORARY_VC_GENERATION`
  WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY))
),
removed AS (
  SELECT DISTINCT i.guild_id
  FROM installs i
  JOIN `astro_bot.GUILD_EVENT` e ON e.guild_id = i.guild_id AND e.action = 'KICKED' AND e.timestamp >= i.installed_at
  WHERE e.timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY))
)
SELECT
  i.source,
  COUNT(*) AS installs,
  COUNTIF(a.guild_id IS NOT NULL) AS active_guilds,
  COUNTIF(r.guild_id IS NOT NULL) AS removed_guilds
FROM installs i
LEFT JOIN active a USING (guild_id)
LEFT JOIN removed r USING (guild_id)
GROUP BY i.source
ORDER BY installs DESC
