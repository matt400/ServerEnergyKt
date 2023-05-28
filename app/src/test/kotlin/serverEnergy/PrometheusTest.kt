package serverEnergy

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class PrometheusTest {
    @Test
    fun getConvertedValues_shouldJoinAllResultValuesToOneList() {
        val metricDataResults: List<PSQuery.MetricDataResult> = Json.decodeFromString("""
            [{ "metric": { "instance": "10.0.0.1", "job": "tasmota1", "sensor": "energy" },
                "values": [[1683072000, "0.198"], [1683158400, "0.231"], [1683331200, "0.123"], [1683417600, "0.943"]] },
             { "metric": { "instance": "10.0.0.2", "job": "tasmota2", "sensor": "energy" },
                 "values": [[1683849600, "0.238"], [1683936000, "0.361"], [1684022400, "0.278"], [1684108800, "1.166"]] }]
        """.trimIndent())

        val testData = Prometheus.getConvertedValues(metricDataResults)

        assertEquals(8, testData.size)
        assertEquals("1.166", testData[7].resultValue)
    }

    @Test
    fun getConvertedValues_shouldJoinSingleResultValueToOneList() {
        val metricDataResult: List<PSQuery.MetricDataResult> = Json.decodeFromString("""
            [{ "metric":  { "instance":  "10.0.0.1", "job":  "tasmota1", "sensor": "energy" },
                "values": [[1683072000, "0.198"], [1683158400, "0.231"], [1683331200, "0.123"], [1683417600, "0.943"]] }]
        """.trimIndent())

        val testData = Prometheus.getConvertedValues(metricDataResult)

        assertEquals(4, testData.size)
        assertEquals(Instant.ofEpochSecond(1683331200), testData[2].date)
    }
}
