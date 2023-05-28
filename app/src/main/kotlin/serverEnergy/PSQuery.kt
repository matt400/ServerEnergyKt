package serverEnergy

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.time.Instant

sealed class PSQuery {
    @Serializable
    data class Metric(val status: String, val data: MetricData)

    @Serializable
    data class MetricData(val resultType: String, val result: List<MetricDataResult>)

    @Serializable
    data class MetricDataResult(val metric: MetricInfo, val values: List<List<JsonElement>>)

    @Serializable
    data class MetricInfo(val instance: String, val job: String, val sensor: String? = null)

    data class MetricValues(val date: Instant, val resultValue: String)
    data class MetricValuesCombined(val date: Instant, val resultValue: String, val uptime: String)
}
