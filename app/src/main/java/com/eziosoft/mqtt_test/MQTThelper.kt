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
 * Copyright (c) 2019. Bartosz Szczygiel
 *
 */

package com.eziosoft.mqtt_test

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*




class MqttHelper {
    var mqttClient: MqttAndroidClient? = null

    fun connect(context: Context, brokerURL: String, clientID: String, callbackExtended: MqttCallbackExtended) {
        Log.d("aaa", "connect")
        mqttClient = MqttAndroidClient(context, brokerURL, clientID)
        //Set call back class
        mqttClient?.setCallback(callbackExtended)
        val connOpts = MqttConnectOptions()
        val token = mqttClient?.connect(connOpts)
    }

    fun subscribe(topic: String) {
        mqttClient?.subscribe(topic, 0, null, object : IMqttActionListener {
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("aaa", "subscribe to $topic FAILED")
            }

            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("aaa", "subscribe to $topic SUCCESS")
            }
        })
    }

    fun close() {
        mqttClient?.disconnect()
    }

    fun publish(topic: String, message: String) {
        Log.d("aaa", "publish: $message")
        mqttClient?.publish(topic, message.toByteArray(), 0, false)
    }

    fun publish(topic: String, message: ByteArray) {
        Log.d("aaa", "publish: $message")
        mqttClient?.publish(topic, message, 0, false)
    }

    fun isConnected(): Boolean {
        if (mqttClient == null) return false
        else return mqttClient!!.isConnected
    }
}


