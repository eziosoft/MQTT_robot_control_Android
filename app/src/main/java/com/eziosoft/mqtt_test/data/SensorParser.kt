/*
 *  This file is part of MQTT_robot_control_Android.
 *
 *     MQTT_robot_control_Android is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Foobar is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020. Bartosz Szczygiel
 *
 */

package com.eziosoft.mqtt_test.data

import android.util.Log
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton


enum class STATE {
    NONE, AFTER_HEADER, AFTER_N, AFTER_SENSOR_ID, AFTER_1B, AFTER_2B
}

@Singleton
class SensorParser(private val sensorListener: SensorListener) {

    interface SensorListener {
        fun onSensor(parsedSensor: ParsedSensor)
    }

    data class ParsedSensor @ExperimentalUnsignedTypes constructor(
        var sensorID: UByte = 0u,
        var b1: UByte = 0u,
        var b2: UByte = 0u,
        var value: Int = 0,
        var name: String? = RoombaSensors.getSensor(sensorID.toInt())?.name,
        var units: String? = RoombaSensors.getSensor(sensorID.toInt())?.unit
    )

    @ExperimentalUnsignedTypes
    suspend fun parse(bytes: UByteArray) {
        var state = STATE.NONE
        var n: UByte = 0.toUByte()
        var chksum: UByte = 0.toUByte()
        var frameStartByte = 0
        var b1: UByte = 0u
        var b2: UByte = 0u
        var value: Int = 0
        var sensorID: UByte = 0u

        Log.d("sss", "s")
        for (i in bytes.indices) {
            val b = bytes[i]

            when (state) {
                STATE.NONE -> if (b == 19.toUByte()) {
                    state = STATE.AFTER_HEADER
                    frameStartByte = i
                }
                STATE.AFTER_HEADER -> {
                    n = b
                    state = STATE.AFTER_N
                }
                STATE.AFTER_N -> {
                    sensorID = b
                    state = STATE.AFTER_SENSOR_ID
                }
                STATE.AFTER_SENSOR_ID -> {
                    b1 = b
                    try {
                        when (RoombaSensors.getSensor(sensorID.toInt())?.bytesCount) {
                            1 -> state = STATE.AFTER_2B
                            2 -> state = STATE.AFTER_1B
                        }
                    } catch (e: Exception) {
                        state = STATE.NONE
                    }
                }

                STATE.AFTER_1B -> {
                    b2 = b
                    state = STATE.AFTER_2B
                }

                STATE.AFTER_2B -> {
                    if (RoombaSensors.getSensor(sensorID.toInt())?.bytesCount == 2) {
                        value = (b1.toInt() * 256 + b2.toInt()).toInt()
                    } else {
                        value = b1.toInt()
                    }

                    Log.d(
                        "sss",
                        "${RoombaSensors.getSensor(sensorID.toInt())?.name}=$value${
                            RoombaSensors.getSensor(
                                sensorID.toInt()
                            )?.unit
                        } ->  n=$n sensorID=$sensorID b1=$b1 b2=$b2 chksum=$chksum value=${value} "
                    )

                    sensorListener?.onSensor(ParsedSensor(sensorID, b1, b2, value))

                    if (i - frameStartByte - 2 == n.toInt()) {
                        chksum = b
                        var sum: UByte = 0u;
                        for (j in frameStartByte..i) {
                            sum = (sum.plus(bytes[j])).toUByte()
                        }
                        if (sum == 0.toUByte()) {//check sum correct
                            //TODO
//                                        withContext(Dispatchers.Main) {
//                                            mainViewModel.voltage.value = value.toFloat() / 1000f
//                                        }

                        }
                        state = STATE.NONE
                    } else {
                        sensorID = b
                        state = STATE.AFTER_SENSOR_ID
                    }
                }
            }
        }
    }
}