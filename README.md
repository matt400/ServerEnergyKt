# ServerEnergy
It's a simple program which collects data from Prometheus, more specifically from Tasmota metrics (smart plug), processes them and saves in Postgres database.

I'm creating this for learning Kotlin and for the need of knowledge how much energy my server takes and how much it costs.

## How it works
It simply gets data from Prometheus API (in 24h period) based on two metrics:
- `tasmota_sensors_today_`
- `node_time_seconds - node_boot_time_seconds` 

Then uses this data to calculate cost, using valid rate value from database and saves processed data there.

## How to run
Configure `~/.bashrc` first by adding an env variables:
```
export ENERGY_DB_CONN="postgresql://localhost/test?user=fred&password=secret"
export ENERGY_PS_URL="https://prometheus_url"
```
Then you can run this app from the main directory: 
```
./gradlew run
```

## TODO
- [x] Change hard-coded Postgres connection to env variable
- [x] ~~Utilize Kotlin Native~~ - no support for Postgres yet 

## Other information
### Tasmota metrics
I have built my own Tasmota with `#define USE_PROMETHEUS` via Gitpod, this way Prometheus gained access to HTTP `/metrics`. [Here](https://tasmota.github.io/docs/Compile-your-build/) is how to compile custom image.

### Database schema
In the future I might automate this.
#### EnergyHistory
```
column_name  | data_type |
-------------+-----------+
Id           | integer   |
Created      | date      |
Kwh          | real      |
Cost         | numeric   |
Downtime     | integer   |
EnergyRateId | integer   |
```
#### EnergyRates
```
column_name | data_type |
------------+-----------+
Id          | integer   |
RateValue   | real      |
ValidTo     | date      |
Active      | boolean   |
```