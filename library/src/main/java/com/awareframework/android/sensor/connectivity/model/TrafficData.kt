package com.awareframework.android.sensor.connectivity.model

import com.awareframework.android.core.model.AwareObject

/**
 * Contains the traffic data.
 *
 * @author  sercant
 * @date 24/08/2018
 */
data class TrafficData(
        var type: Int = -1,
        var receivedBytes: Long = 0L,
        var receivedPackets: Long = 0L,
        var sentBytes: Long = 0L,
        var sentPackets: Long = 0L
) : AwareObject(jsonVersion = 1) {

    companion object {
        const val TABLE_NAME = "trafficData"
    }

    override fun toString(): String = toJson()
}