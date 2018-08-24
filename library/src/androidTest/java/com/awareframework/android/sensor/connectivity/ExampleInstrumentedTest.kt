package com.awareframework.android.sensor.connectivity

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.awareframework.android.core.db.Engine
import com.awareframework.android.sensor.connectivity.model.TrafficData
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 * <p>
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()

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
    }
}
