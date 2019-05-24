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
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import com.longdo.mjpegviewer.MjpegView
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    private val mqttHelper = MqttHelper()
    private lateinit var mjpegView: MjpegView
    private lateinit var TV: TextView
    private lateinit var joystickView: JoystickView
    private lateinit var watchSwich: Switch
    private lateinit var precisionSwich: Switch
    private lateinit var gimbalSwich: Switch
    private lateinit var serverIP: EditText


    private var t: Long = 0

    val robotName = "tank"
    val MQTTcontrolTopic = "$robotName/in"
    val MQTTtelemetryTopic = "$robotName/out"
    val MQTTvideoTopic = "$robotName/video"


    val mqttCallback = object : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            Log.d("aaa", "connectComplete")
            TV.text = "Connected"
            mqttHelper.subscribe(MQTTtelemetryTopic)
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Log.d("aaa", "messageArrived: $topic :" + message.toString())
            TV.text = message.toString()

            if (topic == MQTTvideoTopic) {
//                stopMJPEGStream()
                val decryptedURL = decryptStringWithXORFromHex(message.toString(), "tank")
                Log.d("aaa", decryptedURL)
                TV.text = decryptedURL
                startMJPEGStream(decryptedURL)
            }

            if (topic == MQTTcontrolTopic) {
                val b = message.toString().toByteArray()
                if (b[0] == '$'.toByte()) {
                    val x = (b[1] - 100) / 100f
                    val y = (b[2] - 100) / 100f
                    joystickView.setPosition(x, y)
                }
            }
        }

        override fun connectionLost(cause: Throwable?) {
            Log.d("aaa", "connectionLost")
            TV.text = "Connection lost"
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            Log.d("aaa", "deliveryComplete")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)

        serverIP = findViewById(R.id.serverIP)
        serverIP.setText(sharedPreferences.getString("serverIP", "test.mosquitto.org:1883"))

        TV = findViewById(R.id.TV)
        mjpegView = findViewById(R.id.mjpegview)
        watchSwich = findViewById(R.id.Watch)
        precisionSwich = findViewById(R.id.precisionSwich)
        gimbalSwich = findViewById(R.id.switchGimbal)
        joystickView = findViewById(R.id.joystickView2)
        joystickView.setBackgroundColor(Color.TRANSPARENT)
        joystickView.setBorderWidth(5)
        joystickView.setBorderColor(Color.BLUE)
        joystickView.setButtonColor(Color.BLUE)
        joystickView.setFixedCenter(true)
        joystickView.isAutoReCenterButton = true
        joystickView.axisToCenter = JoystickView.AxisToCenter.BOTH
        joystickView.isSquareBehaviour = true


        val swichVideo = findViewById<Switch>(R.id.switchVideo)
        swichVideo.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                mqttHelper.subscribe(MQTTvideoTopic)
                swichVideo.visibility = View.GONE
            }
        }

        watchSwich.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                mqttHelper.subscribe(MQTTcontrolTopic)
            } else {
                mqttHelper.mqttClient?.unsubscribe(MQTTcontrolTopic)
            }
        }

        joystickView.setOnMoveListener { angle, strength ->
            var x = Math.cos(Math.toRadians(angle.toDouble())) * strength / 100f
            var y = Math.sin(Math.toRadians(angle.toDouble())) * strength / 100f

            val ch1: Int
            val ch2: Int
            val ch3: Int
            val ch4: Int

            if (!gimbalSwich.isChecked) {
                if (precisionSwich.isChecked) {
                    x /= 4f
                    y /= 4f
                }

                ch1 = (-x * 100 + 100).toInt()
                ch2 = (y * 100 + 100).toInt()
                ch3 = 100 //middle position
                ch4 = 100 //middle position


            } else {
                ch1 = 100 //middle position
                ch2 = 100 //middle position
                ch3 = (-x * 100 + 100).toInt()
                ch4 = (y * 100 + 100).toInt()
            }

            if (BuildConfig.DEBUG)
                Log.d("bbb", "$ch1 $ch2 $ch3 $ch4")

            val bytes = byteArrayOf('$'.toByte(), 5, ch1.toByte(), ch2.toByte(), ch3.toByte(), ch4.toByte())
            if ((ch1 == 100 && ch2 == 100) || (ch3 == 100 && ch4 == 100) || (System.currentTimeMillis() > t)) {
                t = System.currentTimeMillis() + 100
                if (!watchSwich.isChecked)
                    if (mqttHelper.isConnected()) mqttHelper.publish(MQTTcontrolTopic, bytes)
            }
        }


        val connectButton = findViewById<Button>(R.id.connectButton)
        connectButton.setOnClickListener()
        {
            sharedPreferences.edit().putString("serverIP", serverIP.text.toString()).apply()
            connectToMQTT()
        }

    }


    override fun onResume() {
        super.onResume()
        connectToMQTT()
    }

    override fun onPause() {
        closeMQTT()
        stopMJPEGStream()
        super.onPause()
    }

    override fun onDestroy() {
        closeMQTT()
        stopMJPEGStream()
        super.onDestroy()
    }


    fun connectToMQTT() {
        val url = "tcp://" + serverIP.text
        if (mqttHelper.isConnected()) mqttHelper.close()
        mqttHelper.connect(this, url, "user${System.currentTimeMillis()}", mqttCallback)
    }

    fun closeMQTT() {
        mqttHelper.close()
    }

    fun startMJPEGStream(url: String) {
        Log.d("aaa", "startMJPEG")
        Toast.makeText(this, "starting video : $url", Toast.LENGTH_SHORT).show()
        mjpegView.mode = MjpegView.MODE_FIT_WIDTH
        mjpegView.isRecycleBitmap = true
        mjpegView.setUrl(url)
        mjpegView.startStream()
    }

    fun stopMJPEGStream() {
        try {
            mjpegView.stopStream()
        } catch (e: Exception) {
        }
    }
}
