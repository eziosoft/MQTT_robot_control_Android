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

data class ParsedSensor @ExperimentalUnsignedTypes constructor(
    var sensorID: UByte = 0u,
    var b1: UByte = 0u,
    var b2: UByte = 0u,
    var value: Int = 0,
    var name: String? = RoombaSensors.getSensor(sensorID.toInt())?.name,
    var units: String? = RoombaSensors.getSensor(sensorID.toInt())?.unit
) {

    fun toString1(): String = "$name=$value$units"
    fun toStringValueWithUnits(): String = "$value$units"

}