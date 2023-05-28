package serverEnergy

fun main() {
    val psValues = Prometheus.getCombinedValuesByDate()
    val dbEnergyData = Postgres.getNotPresentEnergyData(psValues)

    if (System.getenv("ENERGY_DB_CONN").isNullOrEmpty()) {
        println("Looks like you didn't set environment variable ENERGY_DB_CONN in your system.")
        return
    }

    if (System.getenv("ENERGY_PS_URL").isNullOrEmpty()) {
        println("Set ENERGY_PS_URL environment variable with Prometheus URL.")
        return
    }

    if (dbEnergyData != null)
        Postgres.insertNotPresentEnergyData(dbEnergyData, psValues)
    else
        println("There is not any missing data in the database :-)")
}
