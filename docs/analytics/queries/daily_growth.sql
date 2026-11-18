-- Guilds that added and removed the bot per day over the last 90 days, with the net change.
SELECT
  DATE(timestamp) AS day,
  COUNTIF(action = 'JOINED') AS joins,
  COUNTIF(action = 'KICKED') AS removals,
  COUNTIF(action = 'JOINED') - COUNTIF(action = 'KICKED') AS net
FROM `astro_bot.GUILD_EVENT`
WHERE timestamp >= TIMESTAMP(DATE_SUB(CURRENT_DATE(), INTERVAL 90 DAY))
GROUP BY day
ORDER BY day
