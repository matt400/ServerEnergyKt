package serverEnergy

import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

object Helpers {
    private val PROMETHEUS_URL = System.getenv("ENERGY_PS_URL")
    const val KWH_METRIC = "max_over_time(tasmota_sensors_today_[1d])"
    const val UPTIME_METRIC = "increase((node_time_seconds - node_boot_time_seconds)[1d:1m])"

    // Instant outputs date in ISO 8601 format
    val START_DATE: Instant = Instant.parse("2000-01-01T23:59:59Z")
    private val INSTANT_YESTERDAY = Instant.now().minus(1, ChronoUnit.DAYS)
    val END_DATE: Instant = INSTANT_YESTERDAY.atZone(ZoneOffset.UTC)
        .withHour(23)
        .withMinute(59)
        .withSecond(59)
        .withNano(0)
        .toInstant()

    fun getPrometheusUrl(metric: String, startDate: String, endDate: String, queryRange: Boolean = true): String =
        "$PROMETHEUS_URL/api/v1/${if (queryRange) "query_range" else "query"}?query=" +
                "${URLEncoder.encode(metric, "UTF-8")}&step=1d&start=${startDate}&end=${endDate}"
}
