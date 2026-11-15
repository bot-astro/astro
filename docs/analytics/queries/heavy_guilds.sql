-- Top 100 guilds by temporary VCs generated in the last 30 days, with their premium flag and configuration size.
WITH usage AS (
  SELECT guild_id, COUNT(*) AS vcs, COUNT(DISTINCT user_id) AS users
  FROM `astro_bot.TEMPORARY_VC_GENERATION`
  WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY))
  GROUP BY guild_id
),
latest AS (
  SELECT *
  FROM `astro_bot.GUILD_CONFIG_SNAPSHOT`
  WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY))
  QUALIFY ROW_NUMBER() OVER (PARTITION BY guild_id ORDER BY timestamp DESC) = 1
)
SELECT u.guild_id, u.vcs, u.users, l.premium, l.generators, l.interfaces, l.connections, l.templates
FROM usage u
LEFT JOIN latest l USING (guild_id)
ORDER BY u.vcs DESC
LIMIT 100
