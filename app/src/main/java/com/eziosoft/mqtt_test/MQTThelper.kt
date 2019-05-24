package com.eziosoft.mqtt_test

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


/**
 * Written by Bartosz Szczygiel <eziosoft@gmail.com>
 * Created on 12/02/2019.
 */


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


