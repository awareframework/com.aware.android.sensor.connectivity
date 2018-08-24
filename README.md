# AWARE Connectivity

[![jitpack-badge](https://jitpack.io/v/awareframework/com.aware.android.sensor.connectivity.svg)](https://jitpack.io/#awareframework/com.aware.android.sensor.connectivity)

The network sensor provides information on the network sensors availability of the device. These include use of airplane mode, Wi-Fi, Bluetooth, GPS, mobile and WIMAX status and internet availability. This sensor can be leveraged to detect the availability of wireless sensors and internet on the device at any time. Moreover, this sensor also provides mobile and Wi-Fi interval traffic, in bytes and packets.

## Public functions

### ConnectivitySensor

+ `start(context: Context, config: ConnectivitySensor.Config?)`: Starts the connectivity sensor with the optional configuration.
+ `stop(context: Context)`: Stops the service.

### ConnectivitySensor.Config

Class to hold the configuration of the sensor.

#### Fields

+ `sensorObserver: ConnectivitySensor.Observer`: Callback for live state changes.
+ `enabled: Boolean` Sensor is enabled or not. (default = false)
+ `debug: Boolean` enable/disable logging to `Logcat`. (default = false)
+ `label: String` Label for the data. (default = "")
+ `deviceId: String` Id of the device that will be associated with the events and the sensor. (default = "")
+ `dbEncryptionKey` Encryption key for the database. (default =String? = null)
+ `dbType: Engine` Which db engine to use for saving data. (default = `Engine.DatabaseType.NONE`)
+ `dbPath: String` Path of the database. (default = "aware_connectivity")
+ `dbHost: String` Host for syncing the database. (Defult = `null`)

## Broadcasts

### Fired Broadcasts

+ `Network.ACTION_AWARE_AIRPLANE_ON`: fired when airplane mode is activated.
+ `Network.ACTION_AWARE_AIRPLANE_OFF`: fired when airplane mode is deactivated.
+ `Network.ACTION_AWARE_WIFI_ON`: fired when Wi-Fi is activated.
+ `Network.ACTION_AWARE_WIFI_OFF`: fired when Wi-Fi is deactivated.
+ `Network.ACTION_AWARE_MOBILE_ON`: fired when mobile network is activated.
+ `Network.ACTION_AWARE_MOBILE_OFF`: fired when mobile network is deactivated.
+ `Network.ACTION_AWARE_WIMAX_ON`: fired when WIMAX is activated.
+ `Network.ACTION_AWARE_WIMAX_OFF`: fired when WIMAX is deactivated.
+ `Network.ACTION_AWARE_BLUETOOTH_ON`: fired when Bluetooth is activated.
+ `Network.ACTION_AWARE_BLUETOOTH_OFF`: fired when Bluetooth is deactivated.
+ `Network.ACTION_AWARE_GPS_ON`: fired when GPS is activated.
+ `Network.ACTION_AWARE_GPS_OFF`: fired when GPS is deactivated.
+ `Network.ACTION_AWARE_INTERNET_AVAILABLE`: fired when the device is connected to the internet. One extra is included to provide the active internet access network:
  + `Network.EXTRA_ACCESS`: an integer with one of the following constants: 1=Wi-Fi, 2=Bluetooth, 4=Mobile, 5=WIMAX
+ `Network.ACTION_AWARE_INTERNET_UNAVAILABLE`: fired when the device is not connected to the internet.
+ `Network.ACTION_AWARE_NETWORK_TRAFFIC`: fired when new traffic information is available for both Wi-Fi and mobile data.

### Received Broadcasts

+ `ConnectivitySensor.ACTION_AWARE_CONNECTIVITY_START`: received broadcast to start the sensor.
+ `ConnectivitySensor.ACTION_AWARE_CONNECTIVITY_STOP`: received broadcast to stop the sensor.
+ `ConnectivitySensor.ACTION_AWARE_CONNECTIVITY_SYNC`: received broadcast to send sync attempt to the host.
+ `ConnectivitySensor.ACTION_AWARE_CONNECTIVITY_SET_LABEL`: received broadcast to set the data label. Label is expected in the `ConnectivitySensor.EXTRA_LABEL` field of the intent extras.

## Data Representations

### Connectivity Data

Contains the connectivity data.

| Field     | Type   | Description                                                                                         |
| --------- | ------ | --------------------------------------------------------------------------------------------------- |
| type      | Int    | the network type, one of the following: `1=AIRPLANE, 1=WIFi, 2=BLUETOOTH, 3=GPS, 4=MOBILE, 5=WIMAX` |
| subtype   | String | the text label of the TYPE, one of the following: `AIRPLANE, WIFI, BLUETOOTH, GPS, MOBILE, WIMAX`   |
| state     | Int    | the network status `1=ON, 0=OFF`                                                                    |
| deviceId  | String | AWARE device UUID                                                                                   |
| label     | String | Customizable label. Useful for data calibration or traceability                                     |
| timestamp | Long   | unixtime milliseconds since 1970                                                                    |
| timezone  | Int    | [Raw timezone offset][1] of the device                                                              |
| os        | String | Operating system of the device (ex. android)                                                        |

### Traffic Data

Contains the Wi-Fi and mobile data traffic, in bytes and packets.

| Field           | Type   | Description                                                     |
| --------------- | ------ | --------------------------------------------------------------- |
| type            | Int    | the network type, one of the following: `1=WIFi, 4=MOBILE`      |
| receivedBytes   | Long   | received bytes                                                  |
| sentBytes       | Long   | sent bytes                                                      |
| receivedPackets | Long   | received packets                                                |
| sentPackets     | Long   | sent packets                                                    |
| deviceId        | String | AWARE device UUID                                               |
| label           | String | Customizable label. Useful for data calibration or traceability |
| timestamp       | Long   | unixtime milliseconds since 1970                                |
| timezone        | Int    | [Raw timezone offset][1] of the device                          |
| os              | String | Operating system of the device (ex. android)                    |

## Example usage

```kotlin
// To start the service.
ConnectivitySensor.start(appContext, ConnectivitySensor.Config().apply {
    sensorObserver = object : ConnectivitySensor.Observer {
        override fun onInternetON() {
            // your code here...
        }

        override fun onInternetOFF() {
            // your code here...
        }

        override fun onGPSON() {
            // your code here...
        }

        override fun onGPSOFF() {
            // your code here...
        }

        override fun onBluetoothON() {
            // your code here...
        }

        override fun onBluetoothOFF() {
            // your code here...
        }

        override fun onWimaxON() {
            // your code here...
        }

        override fun onWimaxOFF() {
            // your code here...
        }

        override fun onNetworkDataON() {
            // your code here...
        }

        override fun onNetworkDataOFF() {
            // your code here...
        }

        override fun onWiFiON() {
            // your code here...
        }

        override fun onWiFiOFF() {
            // your code here...
        }

        override fun onAirplaneON() {
            // your code here...
        }

        override fun onAirplaneOFF() {
            // your code here...
        }

        override fun onNetworkTraffic(data: TrafficData) {
            // your code here...
        }

        override fun onWiFiTraffic(data: TrafficData) {
            // your code here...
        }

        override fun onIdleTraffic() {
            // your code here...
        }
    }
    dbType = Engine.DatabaseType.ROOM
    debug = true
    // more configuration...
})

// To stop the service
ConnectivitySensor.stop(appContext)
```

## License

Copyright (c) 2018 AWARE Mobile Context Instrumentation Middleware/Framework (http://www.awareframework.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

[1]: https://developer.android.com/reference/java/util/TimeZone#getRawOffset()