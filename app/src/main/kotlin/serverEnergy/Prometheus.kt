package serverEnergy

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Instant

object Prometheus {
    private var kWhData: PSQuery.Metric? = null
    private var uptimeData: PSQuery.Metric? = null

    init {
        val httpClient = HttpClient.newBuilder().build()

        val kwhMetric = HttpRequest.newBuilder().uri(
            URI.create(
                Helpers.getPrometheusUrl(Helpers.KWH_METRIC, Helpers.START_DATE.toString(), Helpers.END_DATE.toString())
            )
        ).build()

        val uptimeMetric = HttpRequest.newBuilder().uri(
            URI.create(
                Helpers.getPrometheusUrl(Helpers.UPTIME_METRIC, Helpers.START_DATE.toString(), Helpers.END_DATE.toString())
            )
        ).build()

        val kwhMetricResult = httpClient.send(kwhMetric, HttpResponse.BodyHandlers.ofString())
        if (kwhMetricResult.statusCode() != 200)
            throw HttpTimeoutException("Prometheus seems unavailable, code: ${kwhMetricResult.statusCode()} - kWh metric")

        val uptimeMetricResult = httpClient.send(uptimeMetric, HttpResponse.BodyHandlers.ofString())
        if (uptimeMetricResult.statusCode() != 200)
            throw HttpTimeoutException("Prometheus seems unavailable, code ${uptimeMetricResult.statusCode()} - Uptime metric")

        kWhData = Json.decodeFromString<PSQuery.Metric>(kwhMetricResult.body())
        uptimeData = Json.decodeFromString<PSQuery.Metric>(uptimeMetricResult.body())
    }

    fun getCombinedValuesByDate(): List<PSQuery.MetricValuesCombined> {
        val energyValues = getConvertedValues(kWhData?.data?.result)
        val uptimeValues = getConvertedValues(uptimeData?.data?.result)

        val combinedValues = mutableListOf<PSQuery.MetricValuesCombined>()

        energyValues.zip(uptimeValues).forEach {
            if (it.first.date == it.second.date)
                combinedValues.add(
                    PSQuery.MetricValuesCombined(
                        it.first.date,
                        it.first.resultValue,
                        it.second.resultValue
                    )
                )
        }

        return combinedValues
    }

    fun getConvertedValues(result: List<PSQuery.MetricDataResult>?): List<PSQuery.MetricValues> {
        val kwhResult = result ?: throw NullPointerException("kwhResult cannot be null")
        val allValues = mutableListOf<PSQuery.MetricValues>()

        kwhResult.forEach { item -> item.values.forEach { values -> allValues.add(parseResultValues(values)) } }

        return allValues
    }

    private fun parseResultValues(data: List<JsonElement>): PSQuery.MetricValues {
        val datetime = Instant.ofEpochSecond(data[0].jsonPrimitive.long)
        val value = data[1].toString()
        return PSQuery.MetricValues(datetime, value.substring(1, value.length - 1))
    }
}