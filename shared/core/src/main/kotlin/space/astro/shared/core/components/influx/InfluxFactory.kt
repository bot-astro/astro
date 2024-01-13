package space.astro.shared.core.components.influx

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.QueryApi
import com.influxdb.client.WriteApi
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class InfluxFactory {

    @Bean
    fun getInfluxClient(influxConfig: InfluxConfig) : InfluxDBClient {
        return InfluxDBClientFactory.create(
            influxConfig.url,
            influxConfig.token.toCharArray(),
            influxConfig.org,
            influxConfig.bucket
        )
    }

    @Bean
    fun getInfluxQueryApi(client: InfluxDBClient) : QueryApi {
        return client.queryApi
    }

    @Bean
    fun getInfluxWriteApi(client: InfluxDBClient) : WriteApi {
        return client.makeWriteApi()
    }
}