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
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.longdo.mjpegviewer.MjpegView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var mqttHelper: MqttHelper

    private var t: Long = 0
    private val robotName = "tank"
    private val MQTTcontrolTopic = "$robotName/in"
    private val MQTTtelemetryTopic = "$robotName/out"
    private val MQTTvideoTopic = "$robotName/video"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        serverIP.setText(sharedPreferences.getString("serverIP", "test.mosquitto.org:1883"))

        precisionSwich.isChecked = true

        joystickView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setBorderWidth(5)
            setBorderColor(Color.BLUE)
            setButtonColor(Color.BLUE)
            setFixedCenter(true)
            isAutoReCenterButton = true
            axisToCenter = JoystickView.AxisToCenter.BOTH
            isSquareBehaviour = true

            setOnMoveListener { angle, strength ->
                handleJoystick(angle, strength)
            }
        }

        switchVideo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mqttHelper.subscribe(MQTTvideoTopic)
                switchVideo.visibility = View.GONE
            }
        }

        watchSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mqttHelper.subscribe(MQTTcontrolTopic)
            } else {
                mqttHelper.mqttClient.unsubscribe(MQTTcontrolTopic)
            }
        }

        connectButton.setOnClickListener()
        {
            sharedPreferences.edit().putString("serverIP", serverIP.text.toString()).apply()
            connectToMQTT()
        }

    }

    private val mqttCallback = object : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            Log.d("aaa", "connectComplete")
            TV.text = "Connected"
            mqttHelper.subscribe(MQTTtelemetryTopic)
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Log.d("aaa", "messageArrived: $topic :" + message.toString())
            TV.text = message.toString()

            if (topic == MQTTvideoTopic) {
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


    override fun onResume() {
        super.onResume()
        connectToMQTT()
        startMJPEGStream("http://77.140.59.128:82/videostream.cgi?user=ezio&pwd=tgyhtgyh")
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


    private fun connectToMQTT() {
        val url = "tcp://" + serverIP.text
        if (mqttHelper.isConnected()) mqttHelper.close()
        mqttHelper.connect(this, url, "user${System.currentTimeMillis()}", mqttCallback)
    }

    private fun closeMQTT() {
        mqttHelper.close()
    }

    private fun startMJPEGStream(url: String) {
//        Log.d("aaa", "startMJPEG")
//        Toast.makeText(this, "starting video : $url", Toast.LENGTH_SHORT).show()
        mjpegview.apply {
            mode = MjpegView.MODE_FIT_WIDTH
            isRecycleBitmap = true
            setUrl(url)
            startStream()
        }
    }

    private fun stopMJPEGStream() {
        try {
            mjpegview.stopStream()
        } catch (e: Exception) {
        }
    }


    private fun handleJoystick(angle: Int, strength: Int) {
        var x = cos(Math.toRadians(angle.toDouble())) * strength / 100f
        var y = sin(Math.toRadians(angle.toDouble())) * strength / 100f

        val ch1: Int
        val ch2: Int
        val ch3: Int
        val ch4: Int

        if (!switchGimbal.isChecked) {
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

        val bytes =
            byteArrayOf('$'.toByte(), 5, ch1.toByte(), ch2.toByte(), ch3.toByte(), ch4.toByte())
        if ((ch1 == 100 && ch2 == 100) || (ch3 == 100 && ch4 == 100) || (System.currentTimeMillis() > t)) {
            t = System.currentTimeMillis() + 100
            if (!watchSwitch.isChecked)
                if (mqttHelper.isConnected()) mqttHelper.publish(MQTTcontrolTopic, bytes)
        }
    }
}
