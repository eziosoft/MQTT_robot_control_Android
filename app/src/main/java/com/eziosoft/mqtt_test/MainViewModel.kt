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

import androidx.lifecycle.ViewModel
import com.eziosoft.mqtt_test.repository.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

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
}