package com.awareframework.android.sensor.connectivity

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.ServiceState.STATE_POWER_OFF
import android.telephony.TelephonyManager
import android.util.Log
import com.awareframework.android.core.AwareSensor
import com.awareframework.android.core.model.SensorConfig
import com.awareframework.android.sensor.connectivity.model.ConnectivityData
import com.awareframework.android.sensor.connectivity.model.TrafficData


/**
 * Connectivity context
 * - on/off events
 * - internet availability
 *
 * @author  sercant
 * @date 22/08/2018
 */
class ConnectivitySensor : AwareSensor() {

    companion object {
        const val TAG = "AWARE::Connectivity"

        /**
         * Network type: airplane (constant = -1 )
         */
        const val NETWORK_TYPE_AIRPLANE = -1

        /**
         * Network type: Wi-Fi ( constant = 1 )
         */
        const val NETWORK_TYPE_WIFI = 1

        /**
         * Network type: Bluetooth ( constant = 2 )
         */
        const val NETWORK_TYPE_BLUETOOTH = 2

        /**
         * Network type: GPS ( constant = 3 )
         */
        const val NETWORK_TYPE_GPS = 3

        /**
         * Network type: Mobile ( constant = 4 )
         */
        const val NETWORK_TYPE_MOBILE = 4

        /**
         * Network type: WIMAX ( constant = 5 )
         */
        const val NETWORK_TYPE_WIMAX = 5

        /**
         * Fired event: airplane is active
         */
        const val ACTION_AWARE_AIRPLANE_ON = "ACTION_AWARE_AIRPLANE_ON"

        /**
         * Fired event: airplane is inactive
         */
        const val ACTION_AWARE_AIRPLANE_OFF = "ACTION_AWARE_AIRPLANE_OFF"

        /**
         * Fired event: wifi is active
         */
        const val ACTION_AWARE_WIFI_ON = "ACTION_AWARE_WIFI_ON"

        /**
         * Fired event: wifi is inactive
         */
        const val ACTION_AWARE_WIFI_OFF = "ACTION_AWARE_WIFI_OFF"

        /**
         * Fired event: mobile is active
         */
        const val ACTION_AWARE_MOBILE_ON = "ACTION_AWARE_MOBILE_ON"

        /**
         * Fired event: mobile is inactive
         */
        const val ACTION_AWARE_MOBILE_OFF = "ACTION_AWARE_MOBILE_OFF"

        /**
         * Fired event: wimax is active
         */
        const val ACTION_AWARE_WIMAX_ON = "ACTION_AWARE_WIMAX_ON"

        /**
         * Fired event: wimax is inactive
         */
        const val ACTION_AWARE_WIMAX_OFF = "ACTION_AWARE_WIMAX_OFF"

        /**
         * Fired event: bluetooth is active
         */
        const val ACTION_AWARE_BLUETOOTH_ON = "ACTION_AWARE_BLUETOOTH_ON"

        /**
         * Fired event: bluetooth is inactive
         */
        const val ACTION_AWARE_BLUETOOTH_OFF = "ACTION_AWARE_BLUETOOTH_OFF"

        /**
         * Fired event: GPS is active
         */
        const val ACTION_AWARE_GPS_ON = "ACTION_AWARE_GPS_ON"

        /**
         * Fired event: GPS is inactive
         */
        const val ACTION_AWARE_GPS_OFF = "ACTION_AWARE_GPS_OFF"

        /**
         * Fired event: internet access is available
         */
        const val ACTION_AWARE_INTERNET_AVAILABLE = "ACTION_AWARE_INTERNET_AVAILABLE"

        /**
         * Fired event: internet access is unavailable
         */
        const val ACTION_AWARE_INTERNET_UNAVAILABLE = "ACTION_AWARE_INTERNET_UNAVAILABLE"

        /**
         * Network status is ON (constant = 1)
         */
        const val STATUS_ON = 1

        /**
         * Network status is OFF (constant = 0)
         */
        const val STATUS_OFF = 0

        /**
         * Extra for ACTION_AWARE_INTERNET_AVAILABLE
         * String "internet_access"
         */
        const val EXTRA_ACCESS = "internet_access"

        /**
         * Fired event: updated traffic information is available
         */
        const val ACTION_AWARE_NETWORK_TRAFFIC = "ACTION_AWARE_NETWORK_TRAFFIC"

        const val ACTION_AWARE_CONNECTIVITY_START = "com.awareframework.android.sensor.connectivity.SENSOR_START"
        const val ACTION_AWARE_CONNECTIVITY_STOP = "com.awareframework.android.sensor.connectivity.SENSOR_STOP"

        const val ACTION_AWARE_CONNECTIVITY_SET_LABEL = "com.awareframework.android.sensor.connectivity.SET_LABEL"
        const val EXTRA_LABEL = "label"

        const val ACTION_AWARE_CONNECTIVITY_SYNC = "com.awareframework.android.sensor.connectivity.SENSOR_SYNC"

        val CONFIG = Config()

        val REQUIRED_PERMISSIONS = arrayOf(ACCESS_NETWORK_STATE)

        fun start(context: Context, config: Config? = null) {
            if (config != null)
                CONFIG.replaceWith(config)
            context.startService(Intent(context, ConnectivitySensor::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ConnectivitySensor::class.java))
        }
    }

    private var connectivityManager: ConnectivityManager? = null
    private var locationManager: LocationManager? = null //tracks gps status
    private var teleManager: TelephonyManager? = null //tracks phone network availability

    private val connectivityMonitor = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                ACTION_AWARE_CONNECTIVITY_SET_LABEL -> {
                    intent.getStringExtra(EXTRA_LABEL)?.let {
                        CONFIG.label = it
                    }
                }

                ACTION_AWARE_CONNECTIVITY_SYNC -> onSync(intent)
            }
        }
    }

    private val phoneListener = object : PhoneStateListener() {
        override fun onServiceStateChanged(serviceState: ServiceState?) {
            super.onServiceStateChanged(serviceState)
            serviceState ?: return

            val servicePowered = serviceState.state != STATE_POWER_OFF

            val data = ConnectivityData().apply {
                timestamp = System.currentTimeMillis()
                label = CONFIG.label
                deviceId = CONFIG.deviceId

                state = if (servicePowered) STATUS_ON else STATUS_OFF
                subtype = "MOBILE"
                type = NETWORK_TYPE_MOBILE
            }

            dbEngine?.save(data, ConnectivityData.TABLE_NAME)

            if (servicePowered) {
                CONFIG.sensorObserver?.onNetworkDataON()
                sendBroadcast(Intent(ACTION_AWARE_MOBILE_ON))
            } else {
                CONFIG.sensorObserver?.onNetworkDataOFF()
                sendBroadcast(Intent(ACTION_AWARE_MOBILE_OFF))
            }
        }
    }

    private val networkMonitor = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            val data = ConnectivityData().apply {
                timestamp = System.currentTimeMillis()
                label = CONFIG.label
                deviceId = CONFIG.deviceId
            }

            when (intent.action) {
                LocationManager.PROVIDERS_CHANGED_ACTION -> {
                    data.apply {
                        type = NETWORK_TYPE_GPS
                        subtype = "GPS"
                        state =
                                if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true)
                                    STATUS_ON
                                else
                                    STATUS_OFF
                    }

                    dbEngine?.save(data, ConnectivityData.TABLE_NAME)

                    if (data.state == STATUS_ON) {
                        CONFIG.sensorObserver?.onGPSON()
                        sendBroadcast(Intent(ACTION_AWARE_GPS_ON))
                    } else {
                        CONFIG.sensorObserver?.onGPSOFF()
                        sendBroadcast(Intent(ACTION_AWARE_GPS_OFF))
                    }
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    data.apply {
                        type = NETWORK_TYPE_AIRPLANE
                        subtype = "AIRPLANE"
                        state =
                                if (intent.getBooleanExtra("state", false))
                                    STATUS_ON
                                else
                                    STATUS_OFF
                    }

                    dbEngine?.save(data, ConnectivityData.TABLE_NAME)

                    if (data.state == STATUS_ON) {
                        CONFIG.sensorObserver?.onAirplaneON()
                        sendBroadcast(Intent(ACTION_AWARE_AIRPLANE_ON))
                    } else {
                        CONFIG.sensorObserver?.onAirplaneOFF()
                        sendBroadcast(Intent(ACTION_AWARE_AIRPLANE_OFF))
                    }
                }
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)

                    data.apply {
                        type = NETWORK_TYPE_WIFI
                        subtype = "WIFI"
                    }

                    if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                        data.state = STATUS_ON

                        dbEngine?.save(data, ConnectivityData.TABLE_NAME)
                        CONFIG.sensorObserver?.onWiFiON()
                        sendBroadcast(Intent(ACTION_AWARE_WIFI_ON))
                    } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                        data.state = STATUS_OFF

                        dbEngine?.save(data, ConnectivityData.TABLE_NAME)
                        CONFIG.sensorObserver?.onWiFiOFF()
                        sendBroadcast(Intent(ACTION_AWARE_WIFI_OFF))
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

                    data.apply {
                        type = NETWORK_TYPE_BLUETOOTH
                        subtype = "BLUETOOTH"
                    }

                    if (btState == BluetoothAdapter.STATE_ON) {
                        data.state = STATUS_ON

                        dbEngine?.save(data, ConnectivityData.TABLE_NAME)
                        CONFIG.sensorObserver?.onBluetoothON()
                        sendBroadcast(Intent(ACTION_AWARE_BLUETOOTH_ON))
                    } else if (btState == BluetoothAdapter.STATE_OFF) {
                        data.state = STATUS_OFF

                        dbEngine?.save(data, ConnectivityData.TABLE_NAME)
                        CONFIG.sensorObserver?.onBluetoothOFF()
                        sendBroadcast(Intent(ACTION_AWARE_BLUETOOTH_OFF))
                    }
                }
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    data.apply {
                        type = NETWORK_TYPE_WIMAX
                        subtype = "WIMAX"
                    }

                    val wimax = connectivityManager?.getNetworkInfo(ConnectivityManager.TYPE_WIMAX)

                    if (wimax != null && wimax.isAvailable) {
                        if (wimax.state == NetworkInfo.State.CONNECTED) {
                            data.state = STATUS_ON

                            dbEngine?.save(data, ConnectivityData.TABLE_NAME)
                            CONFIG.sensorObserver?.onWimaxON()
                            sendBroadcast(Intent(ACTION_AWARE_WIMAX_ON))
                        } else if (wimax.state == NetworkInfo.State.DISCONNECTED) {
                            data.state = STATUS_OFF

                            dbEngine?.save(data, ConnectivityData.TABLE_NAME)
                            CONFIG.sensorObserver?.onWimaxOFF()
                            sendBroadcast(Intent(ACTION_AWARE_WIMAX_OFF))
                        }
                    }
                }
            }

            val internet = connectivityManager?.activeNetworkInfo
            if (internet != null) {
                CONFIG.sensorObserver?.onInternetON()
                sendBroadcast(Intent(ACTION_AWARE_INTERNET_AVAILABLE).apply {
                    when (internet.type) {
                        ConnectivityManager.TYPE_BLUETOOTH -> putExtra(EXTRA_ACCESS, NETWORK_TYPE_BLUETOOTH)
                        ConnectivityManager.TYPE_MOBILE -> putExtra(EXTRA_ACCESS, NETWORK_TYPE_MOBILE)
                        ConnectivityManager.TYPE_WIFI -> putExtra(EXTRA_ACCESS, NETWORK_TYPE_WIFI)
                        ConnectivityManager.TYPE_WIMAX -> putExtra(EXTRA_ACCESS, NETWORK_TYPE_WIMAX)
                    }
                })
            } else {
                CONFIG.sensorObserver?.onInternetOFF()
                sendBroadcast(Intent(ACTION_AWARE_INTERNET_UNAVAILABLE))
            }
        }
    }

    /** TRAFFIC START **/
    data class TrafficUsage(
            var rxBytes: Long = 0L,
            var rxPackets: Long = 0L,
            var txBytes: Long = 0L,
            var txPackets: Long = 0L
    ) {

        operator fun plus(b: TrafficUsage): TrafficUsage {
            return TrafficUsage(
                    rxBytes + b.rxBytes,
                    rxPackets + b.rxPackets,
                    txBytes + b.txBytes,
                    txPackets + b.txPackets
            )
        }

        operator fun minus(b: TrafficUsage): TrafficUsage {
            return TrafficUsage(
                    rxBytes - b.rxBytes,
                    rxPackets - b.rxPackets,
                    txBytes - b.txBytes,
                    txPackets - b.txPackets
            )
        }
    }

    //All stats
    private var startTotalUsage = TrafficUsage()

    //Mobile stats
    private var mobileUsage = TrafficUsage()

    //WiFi stats
    private var wifiUsage = TrafficUsage()

    private val trafficHandler = Handler()
    private val trafficRunnable = Runnable {
        val currentMobileUsage = TrafficUsage(
                TrafficStats.getMobileRxBytes(),
                TrafficStats.getMobileRxPackets(),
                TrafficStats.getMobileTxBytes(),
                TrafficStats.getMobileTxPackets()
        )

        val currentWifiUsage = TrafficUsage(
                TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes(),
                TrafficStats.getTotalRxPackets() - TrafficStats.getMobileRxPackets(),
                TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes(),
                TrafficStats.getTotalTxPackets() - TrafficStats.getMobileTxPackets()
        )

        val deltaMobile = currentMobileUsage - mobileUsage
        val deltaWifi = currentWifiUsage - wifiUsage

        val wifiData = TrafficData().apply {
            timestamp = System.currentTimeMillis()
            deviceId = CONFIG.deviceId
            label = CONFIG.label

            type = NETWORK_TYPE_WIFI

            receivedBytes = deltaWifi.rxBytes
            receivedPackets = deltaWifi.rxPackets
            sentBytes = deltaWifi.txBytes
            sentPackets = deltaWifi.txPackets
        }
        dbEngine?.save(wifiData, TrafficData.TABLE_NAME)

        CONFIG.sensorObserver?.onWiFiTraffic(wifiData)
        logd("Wifi traffic: $wifiData")

        val mobileData = TrafficData().apply {
            timestamp = System.currentTimeMillis()
            deviceId = CONFIG.deviceId
            label = CONFIG.label

            type = NETWORK_TYPE_MOBILE

            receivedBytes = deltaMobile.rxBytes
            receivedPackets = deltaMobile.rxPackets
            sentBytes = deltaMobile.txBytes
            sentPackets = deltaMobile.txPackets
        }
        dbEngine?.save(mobileData, TrafficData.TABLE_NAME)

        CONFIG.sensorObserver?.onNetworkTraffic(mobileData)
        logd("Network traffic: $mobileData")

        sendBroadcast(Intent(ACTION_AWARE_NETWORK_TRAFFIC))

        //refresh old values
        //mobile
        mobileUsage = currentMobileUsage
        //wifi
        wifiUsage = currentWifiUsage
    }

    private val networkTrafficObserver = object : PhoneStateListener() {
        override fun onDataActivity(direction: Int) {
            super.onDataActivity(direction)

            when (direction) {
                TelephonyManager.DATA_ACTIVITY_IN,
                TelephonyManager.DATA_ACTIVITY_OUT,
                TelephonyManager.DATA_ACTIVITY_INOUT -> trafficHandler.post(trafficRunnable)

                TelephonyManager.DATA_ACTIVITY_NONE -> CONFIG.sensorObserver?.onIdleTraffic()
            }
        }
    }

    /** TRAFFIC END **/

    override fun onCreate() {
        super.onCreate()

        initializeDbEngine(CONFIG)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        teleManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        registerReceiver(networkMonitor, IntentFilter().apply {
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        })

        teleManager?.listen(phoneListener, PhoneStateListener.LISTEN_SERVICE_STATE)

        registerReceiver(connectivityMonitor, IntentFilter().apply {
            addAction(ACTION_AWARE_CONNECTIVITY_SET_LABEL)
            addAction(ACTION_AWARE_CONNECTIVITY_SYNC)
        })

        startTotalUsage = TrafficUsage(
                TrafficStats.getTotalRxBytes(),
                TrafficStats.getTotalRxPackets(),
                TrafficStats.getTotalTxBytes(),
                TrafficStats.getTotalTxPackets()
        )

        logd("Connectivity service created!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (REQUIRED_PERMISSIONS.any { ContextCompat.checkSelfPermission(this, it) != PERMISSION_GRANTED }) {
            logw("Missing permissions detected.")
            return START_NOT_STICKY
        }

        if (startTotalUsage.rxBytes == TrafficStats.UNSUPPORTED.toLong()) {
            logw("Device doesn't support traffic statistics!")
        } else {
            if (mobileUsage == TrafficUsage()) {
                mobileUsage = TrafficUsage(
                        TrafficStats.getMobileRxBytes(),
                        TrafficStats.getMobileRxPackets(),
                        TrafficStats.getMobileTxBytes(),
                        TrafficStats.getMobileTxPackets()
                )
            }

            if (wifiUsage == TrafficUsage()) {
                wifiUsage = startTotalUsage - mobileUsage
            }

            teleManager?.listen(networkTrafficObserver, PhoneStateListener.LISTEN_DATA_ACTIVITY)

            logd("Traffic listening is active.")
        }

        logd("Connectivity service is active.")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(networkMonitor)
        teleManager?.listen(phoneListener, PhoneStateListener.LISTEN_NONE)
        teleManager?.listen(networkTrafficObserver, PhoneStateListener.LISTEN_NONE);

        dbEngine?.close()

        logd("Connectivity service terminated.")
    }

    override fun onSync(intent: Intent?) {
        dbEngine?.startSync(ConnectivityData.TABLE_NAME)
        dbEngine?.startSync(TrafficData.TABLE_NAME)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    data class Config(
            var sensorObserver: Observer? = null
    ) : SensorConfig(dbPath = "aware_connectivity") {

        override fun <T : SensorConfig> replaceWith(config: T) {
            super.replaceWith(config)

            if (config is Config) {
                sensorObserver = config.sensorObserver
            }
        }
    }

    interface Observer {
        fun onInternetON()
        fun onInternetOFF()
        fun onGPSON()
        fun onGPSOFF()
        fun onBluetoothON()
        fun onBluetoothOFF()
        fun onWimaxON()
        fun onWimaxOFF()
        fun onNetworkDataON()
        fun onNetworkDataOFF()
        fun onWiFiON()
        fun onWiFiOFF()
        fun onAirplaneON()
        fun onAirplaneOFF()

        fun onNetworkTraffic(data: TrafficData)
        fun onWiFiTraffic(data: TrafficData)
        fun onIdleTraffic()
    }

    class ConnectivitySensorBroadcastReceiver : AwareSensor.SensorBroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return

            logd("Sensor broadcast received. action: " + intent?.action)

            when (intent?.action) {
                SENSOR_START_ENABLED -> {
                    logd("Sensor enabled: " + CONFIG.enabled)

                    if (CONFIG.enabled) {
                        start(context)
                    }
                }

                ACTION_AWARE_CONNECTIVITY_STOP,
                SENSOR_STOP_ALL -> {
                    logd("Stopping sensor.")
                    stop(context)
                }

                ACTION_AWARE_CONNECTIVITY_START -> {
                    start(context)
                }
            }
        }
    }
}

private fun logd(text: String) {
    if (ConnectivitySensor.CONFIG.debug) Log.d(ConnectivitySensor.TAG, text)
}

private fun logw(text: String) {
    Log.w(ConnectivitySensor.TAG, text)
}