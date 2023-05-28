package serverEnergy

import java.sql.*
import java.util.*
import kotlin.math.roundToInt

object Postgres {
    private val POSTGRES_JDBC = "jdbc:${System.getenv("ENERGY_DB_CONN")}"
    private val properties = Properties()

    private var singleRow: MutableList<String> = mutableListOf()
    private var multipleRows: MutableList<MutableList<String>> = mutableListOf()

    private val getSingleRow: List<String>
        get() {
            val result = singleRow.toList()
            cleanResults()
            return result
        }

    private val getMultipleRows: List<List<String>>
        get() {
            val results = multipleRows.toList()
            cleanResults()
            return results
        }

    private fun cleanResults() = when {
        multipleRows.isNotEmpty() -> multipleRows.clear()
        singleRow.isNotEmpty() -> singleRow.clear()
        else -> {}
    }

    private fun runQuery(query: String, isReadable: Boolean = true) {
        var conn: Connection? = null
        var st: Statement? = null
        var result: ResultSet? = null

        try {
            conn = DriverManager.getConnection(POSTGRES_JDBC)
            st = conn.createStatement()

            if (isReadable) {
                result = st.executeQuery(query)
                while (result.next()) {
                    when (val numberOfColumns = result.metaData.columnCount) {
                        1 -> singleRow.add(result.getString(1))
                        else -> {
                            if (!result.isLast) {
                                val columns: MutableList<String> = mutableListOf()
                                for (i in 1..numberOfColumns) columns.add(result.getString(i))
                                multipleRows.add(columns)
                            } else for (i in 1..numberOfColumns) singleRow.add(result.getString(i))
                        }
                    }
                }
            } else st.execute(query)
        } catch (e: SQLException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            result?.close()
            st?.close()
            conn?.close()
        }
    }

    private fun getValidRate(date: String): List<String> {
        runQuery("""
            select "Id", "RateValue" from "EnergyRates"
            where "ValidTo" >= '${date}' and "Active" = true
            order by "ValidTo" asc
            limit 1
        """.trimIndent())
        return getSingleRow.ifEmpty {
            throw NullPointerException("Cannot get any rate value for $date, check if table 'EnergyRates' exists or table is not empty.")
        }
    }

    fun getNotPresentEnergyData(psData: List<PSQuery.MetricValuesCombined>): List<String>? {
        val prepareDates = psData.joinToString(",") { it.date.toString().substring(0, 10) }
        runQuery("""
            select * from unnest('{$prepareDates}'::date[]) as t("Created")
            except select "Created" from "EnergyHistory"
        """.trimIndent())
        return getSingleRow.ifEmpty { null }
    }

    fun insertNotPresentEnergyData(notPresentDataInDb: List<String>, psData: List<PSQuery.MetricValuesCombined>) =
        notPresentDataInDb.forEach { dateStr ->
            // Potential date should exist in Prometheus API
            val psSingle = psData.singleOrNull { it.date.toString() == dateStr + "T23:59:59Z" }
            if (psSingle != null) {
                try {
                    // Rate
                    val getRate = getValidRate(dateStr)
                    val rateId = getRate[0]
                    val rateValue = getRate[1].toDouble()

                    // Downtime
                    val uptime = psSingle.uptime.toDouble().roundToInt()
                    val downtime = 86400 - uptime

                    val kwh = psSingle.resultValue.toDouble()
                    val cost: Double = kwh * rateValue

                    runQuery("""
                        insert into "EnergyHistory"("Created", "Kwh", "Cost", "Downtime", "EnergyRateId")
                        values('$dateStr', '$kwh', '$cost', '$downtime', $rateId)
                    """, false)

                    println("Data from $dateStr, [kWh -> $kwh, cost -> $cost, rate -> $rateValue] was added to db.")
                } catch(e: NullPointerException) { println("[ERROR] ${e.message}") }
            }
        }
}
