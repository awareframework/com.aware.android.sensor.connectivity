package com.awareframework.android.sensor.connectivity.model

import com.awareframework.android.core.model.AwareObject

/**
 * Contains the connectivity data.
 *
 * @author  sercant
 * @date 22/08/2018
 */
data class ConnectivityData(
        var type: Int = -1,
        var subtype: String = "",
        var state: Int = 0
) : AwareObject(jsonVersion = 1) {

    companion object {
        const val TABLE_NAME = "connectivityData"
    }

    override fun toString(): String = toJson()
}