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
 * Copyright (c) 2021. Bartosz Szczygiel
 *
 */

package com.eziosoft.mqtt_test.repository.smallRobot

import com.eziosoft.mqtt_test.helpers.map
import com.eziosoft.mqtt_test.repository.roomba.RoombaParsedSensor
import javax.inject.Inject

class SmallRobotSensorParser @Inject constructor() {
    @ExperimentalUnsignedTypes
     fun parseSmallRobotTelemetry(message: String): List<RoombaParsedSensor> {
        val sensors = mutableListOf<RoombaParsedSensor>()

        if (message.take(2) == "TS") {
            val data = message.split(";")
            val time = data[1].toInt()
            val rssi = data[2].toInt()
            val vbat = data[3].toFloat()
            val current = data[4].toFloat()
            val used_mAh = data[5].toFloat()

            val batPercent: Int = map(vbat, 3.3f, 4.2f, 0.0f, 100.0f).toInt()
            sensors.add(
                RoombaParsedSensor(
                    26,
                    0u,
                    0u,
                    100,
                    "Max battery percentage",
                    ""
                )
            )
            sensors.add(
                RoombaParsedSensor(
                    25,
                    0u,
                    0u,
                    batPercent, "Battery Percentage", "%"
                )
            )
            sensors.add(
                RoombaParsedSensor(22, 0u, 0u, (vbat * 1000).toInt())
            )
            sensors.add(
                RoombaParsedSensor(23, 0u, 0u, current.toInt())
            )
            sensors.add(
                RoombaParsedSensor(100, 0u, 0u, time)
            )
            sensors.add(
                RoombaParsedSensor(101, 0u, 0u, rssi)
            )

            sensors.add(
                RoombaParsedSensor(102, 0u, 0u, used_mAh.toInt())
            )
        }
        return sensors
    }
}
