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

package com.eziosoft.mqtt_test.repository

import android.util.Log
import com.eziosoft.mqtt_test.helpers.map
import com.eziosoft.mqtt_test.repository.mqtt.Mqtt
import com.eziosoft.mqtt_test.repository.roomba.RoombaParsedSensor
import com.eziosoft.mqtt_test.repository.roomba.SensorParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalUnsignedTypes::class)
@Singleton
class Repository @Inject constructor(
    private val mqtt: Mqtt,
    private val sensorParser: SensorParser
) :
    SensorParser.SensorListener {

    private val sensorDataSet = arrayListOf<RoombaParsedSensor>()
    private val _logFlow = MutableStateFlow<String>("")
    val logFlow = _logFlow.asStateFlow()
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus = _connectionStatus.asStateFlow()

    val sensorsFlow = MutableStateFlow<List<RoombaParsedSensor>>(emptyList())


    enum class ConnectionStatus {
        DISCONNECTED, CONNECTED
    }

    init {
        sensorParser.setListener(this)
        setupObservers()
    }

    private fun setupObservers() {
        CoroutineScope(Dispatchers.IO).launch {
            mqtt.messageFlow.collect { message ->
                when (message.topic) {
                    MQTTtelemetryTopic -> {
                        toLogFlow(String(message.message))
                        parseSmallRobotTelemetry(message.toString())
                    }
                    MQTTStreamTopic -> {
                        val bytes = message.message.toUByteArray()
                        if (!bytes.isEmpty()) {
                            sensorParser.parse(bytes)
                        }
                    }
                }
            }
        }
    }


    fun getSensorValue(id: Int): Int? {
        return sensorDataSet.find { it.sensorID == id }?.signedValue
    }


    fun connectToMQTT(url: String) {
        if (mqtt.isConnected()) mqtt.disconnectFromBroker { }
        toLogFlow("connecting to $url")

        mqtt.connectToBroker(url, "user${System.currentTimeMillis()}") { status, error ->
            if (status) {
                mqtt.subscribeToTopic(MQTTtelemetryTopic)
                mqtt.subscribeToTopic(MQTTStreamTopic)
            }
            _connectionStatus.value = if (status) ConnectionStatus.CONNECTED else
                ConnectionStatus.DISCONNECTED
        }
    }

    fun publishMessage(message: String, topic: String) {
        mqtt.publishMessage(message, topic, false) { _, _ -> }
    }

    fun publishMessage(bytes: ByteArray, topic: String) {
        mqtt.publishMessage(bytes, topic, false) { _, _ -> }
    }

    override fun onSensors(sensors: List<RoombaParsedSensor>, checksumOK: Boolean) {
        if (checksumOK) processParsedSensors(sensors)
        else
            Log.e("aaa", "CHECKSUM ERROR")
    }

    var timer = 0L
    private fun processParsedSensors(sensors: List<RoombaParsedSensor>) {
        if (timer < System.currentTimeMillis()) {
            timer = System.currentTimeMillis() + 250

            sensorDataSet.clear()
            sensorDataSet.addAll(sensors)
            sensorsFlow.value = sensorDataSet
        }
    }

    private fun toLogFlow(string: String) {
        _logFlow.value = (string)
    }


    private fun parseSmallRobotTelemetry(message: String) {
        if (message.take(2) == "TS") {
            val data = message.split(";")
            val time = data[1].toInt()
            val rssi = data[2].toInt()
            val vbat = data[3].toFloat()
            val current = data[4].toFloat()
            val used_mAh = data[5].toFloat()

            val batPercent: Int = map(vbat, 3.3f, 4.2f, 0.0f, 100.0f).toInt()
            sensorDataSet.clear()
            sensorDataSet.add(
                RoombaParsedSensor(
                    26,
                    0u,
                    0u,
                    100,
                    "Max battery percentage",
                    ""
                )
            )
            sensorDataSet.add(
                RoombaParsedSensor(
                    25,
                    0u,
                    0u,
                    batPercent, "Battery Percentage", "%"
                )
            )
            sensorDataSet.add(
                RoombaParsedSensor(22, 0u, 0u, (vbat * 1000).toInt())
            )
            sensorDataSet.add(
                RoombaParsedSensor(23, 0u, 0u, current.toInt())
            )
            sensorDataSet.add(
                RoombaParsedSensor(100, 0u, 0u, time)
            )
            sensorDataSet.add(
                RoombaParsedSensor(101, 0u, 0u, rssi)
            )

            sensorDataSet.add(
                RoombaParsedSensor(102, 0u, 0u, used_mAh.toInt())
            )
            sensorsFlow.value = sensorDataSet
        }
    }
    companion object{
        val robotName = "tank"
        val MQTTcontrolTopic = "$robotName/in"
        val MQTTtelemetryTopic = "$robotName/out"
        val MQTTStreamTopic = "$robotName/stream"
    }
}