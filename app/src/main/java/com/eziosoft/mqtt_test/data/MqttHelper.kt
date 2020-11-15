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

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mqttConnectOptions: MqttConnectOptions
) {
    private val TAG = "aaa"

    lateinit var mqttClient: MqttAndroidClient

    fun connect(
        brokerURL: String,
        clientID: String,
        callbackExtended: MqttCallbackExtended
    ) {
        Log.d(TAG, "connect")
        mqttClient = MqttAndroidClient(context, brokerURL, clientID).apply {
            setCallback(callbackExtended)
            connect(mqttConnectOptions)
        }
    }

    fun subscribe(topic: String) {
        mqttClient.subscribe(topic, 0, null, object : IMqttActionListener {
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d(TAG, "subscribe to $topic FAILED")
            }

            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d(TAG, "subscribe to $topic SUCCESS")
            }
        })
    }

    fun close() {
        mqttClient.disconnect()
    }

    fun publish(topic: String, message: String) {
        Log.d(TAG, "publish: $message")
        mqttClient.publish(topic, message.toByteArray(), 0, false)
    }

    fun publish(topic: String, message: ByteArray) {
        Log.d(TAG, "publish: $message")
        mqttClient.publish(topic, message, 0, false)
    }

    fun isConnected(): Boolean {
        return if (!this::mqttClient.isInitialized) false
        else mqttClient.isConnected
    }
}


