package space.astro.shared.core.daos

import com.influxdb.client.QueryApi
import com.influxdb.client.WriteApi
import com.influxdb.client.write.Point
import com.influxdb.query.FluxTable
import org.springframework.stereotype.Repository
import space.astro.shared.core.components.influx.InfluxConfig
import space.astro.shared.core.models.influx.ConfigurationErrorData

@Repository
class ConfigurationErrorDao(
    private val influxConfig: InfluxConfig,
    private val influxQueryApi: QueryApi,
    private val influxWriteApi: WriteApi
) {
    fun get(guildID: String, lookback: String = "-7d"): List<ConfigurationErrorData.ConfigurationErrorWithInstantData> {
        val query = "from(bucket:\"${influxConfig.bucket}\") |> range(start: $lookback) |> filter(fn: (r) => r[\"_measurement\"] == \"configuration_error\") |> filter(fn: (r) => r[\"guild_id\"] == \"$guildID\")"
        val results: List<FluxTable> = influxQueryApi.query(query)

        return results.map { fluxTable ->
            fluxTable.records.mapNotNull { fluxRecord ->
                val description = fluxRecord.getValueByKey("description")?.toString()
                if (description != null) {
                    ConfigurationErrorData.ConfigurationErrorWithInstantData(description, fluxRecord.time)
                } else {
                    null
                }
            }
        }.flatten()
    }

    fun save(guildId: String, configurationErrorData: ConfigurationErrorData) {
        val point = Point("configuration_error")
            .addTag("guild_id", guildId)
            .addField("description", configurationErrorData.description)

        influxWriteApi.writePoint(point)
    }
}