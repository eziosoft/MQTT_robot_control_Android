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

package com.eziosoft.mqtt_test

import android.util.Log
import androidx.lifecycle.ViewModel
import com.eziosoft.mqtt_test.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

@ExperimentalUnsignedTypes
@HiltViewModel
class MainViewModel @Inject constructor(val repository: Repository) :
    ViewModel() {

    var serverAddress = MutableStateFlow("")

    var t: Long = 0

    val connectionStatus = repository.connectionStatus
    val logFlow = repository.logFlow
    val sensorFlow = repository.sensorsFlow


    fun connectMqtt(url: String) = repository.connectToMQTT(url)
    fun getSensorValue(id: Int) = repository.getSensorValue(id)
    fun publishMessage(message: String, topic: String) = repository.publishMessage(message, topic)
    fun publishMessage(bytes: ByteArray, topic: String) = repository.publishMessage(bytes, topic)


    fun isMqttConnected() = connectionStatus.value == Repository.ConnectionStatus.CONNECTED


    fun sendChannels(ch1: Int, ch2: Int, ch3: Int, ch4: Int) {
        val bytes =
            byteArrayOf(
                '$'.toByte(), 5,
                (ch1 + 100).toByte(),
                (ch2 + 100).toByte(),
                (ch3 + 100).toByte(),
                (ch4 + 100).toByte()
            )
        if (isMqttConnected()) publishMessage(
            bytes,
            Repository.MQTTcontrolTopic
        )
    }

    fun sendCommandsChannels(ch3: Int, ch4: Int) {
        sendChannels(0, 0, ch3, ch4)
    }

     fun sendJoystickData(angle: Int, strength: Int, precision: Boolean, watch: Boolean) {
        Log.d("aaa", "handleJoystick: angle=$angle  strength=$strength")

        var x = cos(Math.toRadians(angle.toDouble())) * strength / 100f
        var y = sin(Math.toRadians(angle.toDouble())) * strength / 100f

        var ch1 = 0
        var ch2 = 0
        val ch3 = 0
        val ch4 = 0

        if (precision) {
            x /= 4f
            y /= 4f
        }

        ch1 = (-x * 100).toInt()
        ch2 = (y * 100).toInt()

        if (BuildConfig.DEBUG) {
            Log.d("bbb", "$ch1 $ch2 $ch3 $ch4")
        }

        if ((ch1 == 0 && ch2 == 0) || (ch3 == 0 && ch4 == 0) || (System.currentTimeMillis() > t)) {
            t = System.currentTimeMillis() + 100
            if (!watch) {
                if (isMqttConnected()) {
                    sendChannels(ch1, ch2, ch3, ch4)
                }
            }
        }
    }
}