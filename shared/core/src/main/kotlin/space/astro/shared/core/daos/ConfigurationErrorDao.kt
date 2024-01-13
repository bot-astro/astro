package space.astro.shared.core.daos

import com.influxdb.client.QueryApi
import com.influxdb.client.WriteApi
import com.influxdb.client.write.Point
import org.springframework.stereotype.Repository
import space.astro.shared.core.models.influx.ConfigurationErrorDto

@Repository
class ConfigurationErrorDao(
    private val influxQueryApi: QueryApi,
    private val influxWriteApi: WriteApi
) {
    fun save(guildId: String, configurationErrorDto: ConfigurationErrorDto) {
        val point = Point("configuration_error")
            .addTag("guild_id", guildId)
            .addField("description", configurationErrorDto.description)

        influxWriteApi.writePoint(point)
    }
}