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

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eziosoft.mqtt_test.repository.roomba.RoombaParsedSensor
import com.eziosoft.mqtt_test.repository.roomba.SensorParser
import com.eziosoft.mqtt_test.helpers.map
import com.eziosoft.mqtt_test.repository.mqtt2.MQTTStreamTopic
import com.eziosoft.mqtt_test.repository.mqtt2.MQTTcontrolTopic
import com.eziosoft.mqtt_test.repository.mqtt2.MQTTtelemetryTopic
import com.eziosoft.mqtt_test.repository.mqtt2.Mqtt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttMessage
import javax.inject.Inject

@ExperimentalUnsignedTypes
@HiltViewModel
class MainViewModel @Inject constructor(val mqtt: Mqtt) :
    ViewModel() {

    val sensorParser = SensorParser(object : SensorParser.SensorListener {
        override fun onSensors(sensors: List<RoombaParsedSensor>, checksumOK: Boolean) {
            if (checksumOK) processParsedSensors(sensors)
            else
                Log.e("aaa", "CHECKSUM ERROR")
        }
    })

    val sensorDataSet = arrayListOf<RoombaParsedSensor>()
    val dataSetChanged = MutableLiveData<Int>()
    val tvString = MutableLiveData<String>()
    val serverAddress = MutableLiveData<String>()
    val joyX = MutableLiveData(0F)
    val joyY = MutableLiveData(0F)

    var t: Long = 0

    val connectionStatus = MutableLiveData(false)


    @RequiresApi(Build.VERSION_CODES.N)
    fun connectToMQTT(context: Context) {
//        mainViewModel.serverAddress.value = "mqtt.flespi.io:1883" //T
        val userName = "27aQSfkPYPrH1WHfjsDejLIqJxTza4i21gIHlTn8wEDlqarztSBAr7O0swnsqvi"
        val url = serverAddress.value

        if (mqtt.isConnected()) mqtt.disconnectFromBroker { }
        tvString.value = "connecting to ${serverAddress.value}"

        mqtt.connectToBroker(url!!, "user${System.currentTimeMillis()}") { status, error ->
            if (status) {
                mqtt.subscribeToTopic(MQTTtelemetryTopic)
                mqtt.subscribeToTopic(MQTTStreamTopic)
            }
            connectionStatus.postValue(status)
        }
    }

    private fun setMqttObservers() {
        viewModelScope.launch {
            mqtt.messageFlow.collect { message ->
                when (message.topic) {
                    MQTTtelemetryTopic -> {
                        tvString.value =
                            String(message.message)
                        parseSmallRobotTelemetry(message.toString())
                    }
                    MQTTcontrolTopic -> {
                        Log.d(
                            "aaa",
                            "messageArrived: ${message.topic} :" + String(message.message)
                        )
                        val b = message.message.toUByteArray()
                        if (b[0] == '$'.code.toUByte() && b[1] == 5.toUByte()) {
                            val x: Float = -((b[2].toFloat() - 100) / 100)
                            val y: Float = -((b[3].toFloat() - 100) / 100)
                            Log.d("aaa", "setMqttObservers: x=$x y=$y")
                            joyX.postValue(x)
                            joyY.postValue(y)
                        }
                    }

                    MQTTStreamTopic -> {
                        val bytes = message.message.toUByteArray()
                        if (!bytes.isEmpty()) {
                            viewModelScope.launch(Dispatchers.IO) {
                                sensorParser.parse(bytes)
                            }
                        }
                    }
                }
            }
        }
    }


    var timer = 0L
    fun processParsedSensors(sensors: List<RoombaParsedSensor>) {
        if (timer < System.currentTimeMillis()) {
            timer = System.currentTimeMillis() + 250

            sensorDataSet.clear()
            sensorDataSet.addAll(sensors)

            dataSetChanged.value = 0
        }
        timer++
    }

    fun getSensorValue(id: Int): Int? {
        return sensorDataSet.find { it.sensorID == id }?.signedValue
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
            dataSetChanged.value = 0

        }
    }

    init {
        setMqttObservers()
    }
}