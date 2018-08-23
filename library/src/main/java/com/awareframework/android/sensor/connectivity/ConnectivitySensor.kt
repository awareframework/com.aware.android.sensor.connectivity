package com.awareframework.android.sensor.connectivity

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.Manifest.permission.READ_PHONE_STATE
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
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
         * Broadcasted event: airplane is active
         */
        const val ACTION_AWARE_AIRPLANE_ON = "ACTION_AWARE_AIRPLANE_ON"

        /**
         * Broadcasted event: airplane is inactive
         */
        const val ACTION_AWARE_AIRPLANE_OFF = "ACTION_AWARE_AIRPLANE_OFF"

        /**
         * Broadcasted event: wifi is active
         */
        const val ACTION_AWARE_WIFI_ON = "ACTION_AWARE_WIFI_ON"

        /**
         * Broadcasted event: wifi is inactive
         */
        const val ACTION_AWARE_WIFI_OFF = "ACTION_AWARE_WIFI_OFF"

        /**
         * Broadcasted event: mobile is active
         */
        const val ACTION_AWARE_MOBILE_ON = "ACTION_AWARE_MOBILE_ON"

        /**
         * Broadcasted event: mobile is inactive
         */
        const val ACTION_AWARE_MOBILE_OFF = "ACTION_AWARE_MOBILE_OFF"

        /**
         * Broadcasted event: wimax is active
         */
        const val ACTION_AWARE_WIMAX_ON = "ACTION_AWARE_WIMAX_ON"

        /**
         * Broadcasted event: wimax is inactive
         */
        const val ACTION_AWARE_WIMAX_OFF = "ACTION_AWARE_WIMAX_OFF"

        /**
         * Broadcasted event: bluetooth is active
         */
        const val ACTION_AWARE_BLUETOOTH_ON = "ACTION_AWARE_BLUETOOTH_ON"

        /**
         * Broadcasted event: bluetooth is inactive
         */
        const val ACTION_AWARE_BLUETOOTH_OFF = "ACTION_AWARE_BLUETOOTH_OFF"

        /**
         * Broadcasted event: GPS is active
         */
        const val ACTION_AWARE_GPS_ON = "ACTION_AWARE_GPS_ON"

        /**
         * Broadcasted event: GPS is inactive
         */
        const val ACTION_AWARE_GPS_OFF = "ACTION_AWARE_GPS_OFF"

        /**
         * Broadcasted event: internet access is available
         */
        const val ACTION_AWARE_INTERNET_AVAILABLE = "ACTION_AWARE_INTERNET_AVAILABLE"

        /**
         * Broadcasted event: internet access is unavailable
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

        logd("Connectivity service created!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (REQUIRED_PERMISSIONS.any { ContextCompat.checkSelfPermission(this, it) != PERMISSION_GRANTED }) {
            logw("Missing permissions detected.")
            return START_NOT_STICKY
        }

        logd("Connectivity service is active.")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(networkMonitor)
        teleManager?.listen(phoneListener, PhoneStateListener.LISTEN_NONE)

        dbEngine?.close()

        logd("Connectivity service terminated.")
    }

    override fun onSync(intent: Intent?) {
        dbEngine?.startSync(ConnectivityData.TABLE_NAME)
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