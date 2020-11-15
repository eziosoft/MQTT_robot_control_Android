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

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.eziosoft.mqtt_test.data.Mqtt
import com.eziosoft.mqtt_test.data.MqttRepository
import com.eziosoft.mqtt_test.ui.ControlFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    val controlFragmentViewModel: ControlFragmentViewModel by viewModels()

    @Inject
    lateinit var mqttRepository: MqttRepository
    lateinit var mqtt: Mqtt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        mqtt = mqttRepository.getMqtt()

    }

    private val mqttCallback = object : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            Log.d("aaa", "connectComplete")
            controlFragmentViewModel.tvString.value = "Connected"
            mqtt.subscribe(controlFragmentViewModel.MQTTtelemetryTopic)
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Log.d("aaa", "messageArrived: $topic :" + message.toString())
            controlFragmentViewModel.tvString.value = message.toString()

            if (topic == controlFragmentViewModel.MQTTcontrolTopic) {
                val b = message.toString().toByteArray()
                if (b[0] == '$'.toByte() && b[1] == 5.toByte()) {
                    val x: Float = -(b[2] - 100) / 100f
                    val y: Float = -(b[3] - 100) / 100f

                    controlFragmentViewModel.apply {
                        joyX.value = x
                        joyY.value = y
                    }

                }
            }
        }

        override fun connectionLost(cause: Throwable?) {
            Log.d("aaa", "connectionLost")
            controlFragmentViewModel.tvString.value = "Connection lost"
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            Log.d("aaa", "deliveryComplete")
        }
    }


    fun connectToMQTT() {
        val url = "tcp://" + controlFragmentViewModel.serverAddress.value
        if (mqtt.isConnected()) mqtt.close()
        controlFragmentViewModel.tvString.value = "Connecting to ${controlFragmentViewModel.serverAddress.value}"
        mqtt.connect(
            this,
            url,
            "user${System.currentTimeMillis()}",
            mqttCallback
        )
    }


}
