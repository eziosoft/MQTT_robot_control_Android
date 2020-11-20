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

import java.util.ArrayList
import javax.inject.Singleton


enum class STATE {
    HEADER, N, PACKET_ID, PACKET_DATA1, PACKET_DATA2, CHKSUM_OR_NEXT_SENSOR
}

@ExperimentalUnsignedTypes
@Singleton
class SensorParser(val sensorListener: SensorListener) {

    interface SensorListener {
        fun onSensors(sensors: List<ParsedSensor>)
        fun onChkSumError()
    }

    data class ParsedSensor @ExperimentalUnsignedTypes constructor(
        var sensorID: UByte = 0u,
        var b1: UByte = 0u,
        var b2: UByte = 0u,
        var value: Int = 0,
        var name: String? = RoombaSensors.getSensor(sensorID.toInt())?.name,
        var units: String? = RoombaSensors.getSensor(sensorID.toInt())?.unit
    )

    var state = STATE.HEADER
    var n: UByte = 0u
    var ni = 0
    var chksum: UByte = 0u
    var b1: UByte = 0u
    var b2: UByte = 0u
    var value = 0
    var packetID: UByte = 0u

    var sensors = arrayListOf<ParsedSensor>()

    @ExperimentalUnsignedTypes
    suspend fun parse(bytes: UByteArray) {
        for (i in bytes.indices) {
            val b = bytes[i]

//            println(state.name)
            when (state) {
                STATE.HEADER -> if (b == 19.toUByte()) {
                    chksum = 0u
                    n = 0u
                    ni = 0
                    chksum = 0u
                    b1 = 0u
                    b2 = 0u
                    value = 0
                    packetID = 0u
                    chksum = (chksum + b).toUByte()
                    sensors.clear()
//                    println("$n == $ni")
                    state = STATE.N
                }
                STATE.N -> {
                    n = b
                    chksum = (chksum + b).toUByte()
                    state = STATE.PACKET_ID
//                    println("$n == $ni")
                }
                STATE.PACKET_ID -> {
                    packetID = b
                    chksum = (chksum + b).toUByte()
                    ni++
                    state = STATE.PACKET_DATA1
//                    println("$n == $ni")
                }
                STATE.PACKET_DATA1 -> {
                    b1 = b
                    chksum = (chksum + b).toUByte()
                    ni++
//                    println("$n == $ni")

                    when (RoombaSensors.getSensor(packetID.toInt())?.bytesCount) {
                        1 -> state = STATE.CHKSUM_OR_NEXT_SENSOR
                        2 -> state = STATE.PACKET_DATA2
                    }
                }

                STATE.PACKET_DATA2 -> {
                    b2 = b
                    chksum = (chksum + b).toUByte()
                    ni++
//                    println("$n == $ni")
                    state = STATE.CHKSUM_OR_NEXT_SENSOR
                }

                STATE.CHKSUM_OR_NEXT_SENSOR -> {
                    if (RoombaSensors.getSensor(packetID.toInt())?.bytesCount == 2) {
                        value = (b1.toInt() * 256 + b2.toInt()).toInt()
                    } else {
                        value = b1.toInt()
                    }
                    sensors.add(ParsedSensor(packetID, b1, b2, value))


//                    println("$n == $ni")
                    if (n.toInt() == ni) {
                        chksum = (chksum + b).toUByte()
                        if (chksum == 0.toUByte()) {//check sum correct
//                            println("chksum OK")
                            sensorListener.onSensors(sensors)
                        } else {
                            sensorListener.onChkSumError()
                            state = STATE.HEADER
                        }
                        state = STATE.HEADER
                    } else {
//                        println("->next packet")
                        packetID = b
                        chksum = (chksum + b).toUByte()
                        ni++
                        state = STATE.PACKET_DATA1
                    }
                }
            }
        }
    }
}