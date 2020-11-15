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
import com.eziosoft.mqtt_test.data.MqttHelper
import com.eziosoft.mqtt_test.data.MqttRepository
import dagger.hilt.android.AndroidEntryPoint
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var mqttRepository: MqttRepository
    lateinit var mqttHelper: MqttHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        mqttHelper = mqttRepository.getMqtt()

    }

    private val mqttCallback = object : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            Log.d("aaa", "connectComplete")
            mainViewModel.tvString.value = "Connected"
            mqttHelper.subscribe(mainViewModel.MQTTtelemetryTopic)
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Log.d("aaa", "messageArrived: $topic :" + message.toString())
            mainViewModel.tvString.value = message.toString()

            if (topic == mainViewModel.MQTTcontrolTopic) {
                val b = message.toString().toByteArray()
                if (b[0] == '$'.toByte() && b[1] == 5.toByte()) {
                    val x: Float = -(b[2] - 100) / 100f
                    val y: Float = -(b[3] - 100) / 100f

                    mainViewModel.apply {
                        joyX.value = x
                        joyY.value = y
                    }

                }
            }
        }

        override fun connectionLost(cause: Throwable?) {
            Log.d("aaa", "connectionLost")
            mainViewModel.tvString.value = "Connection lost"
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            Log.d("aaa", "deliveryComplete")
        }
    }


    fun connectToMQTT() {
        val url = "tcp://" + mainViewModel.serverAddress.value
        if (mqttHelper.isConnected()) mqttHelper.close()
        mainViewModel.tvString.value = "Connecting to ${mainViewModel.serverAddress.value}"
        mqttHelper.connect(
            url,
            "user${System.currentTimeMillis()}",
            mqttCallback
        )
    }


}
