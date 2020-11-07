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
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.longdo.mjpegviewer.MjpegView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.control_fragment.*
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class ControlFragment : Fragment(R.layout.control_fragment) {

    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject
    lateinit var mqttHelper: MqttHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val sharedPreferences = activity?.getSharedPreferences("settings", Context.MODE_PRIVATE)
        serverIP.setText(sharedPreferences?.getString("serverIP", "test.mosquitto.org:1883"))

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
                (activity as MainActivity).mqttHelper.subscribe(mainViewModel.MQTTvideoTopic)
                switchVideo.visibility = View.GONE
            }
        }

        watchSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mqttHelper.subscribe(mainViewModel.MQTTcontrolTopic)
            } else {
                mqttHelper.mqttClient.unsubscribe(mainViewModel.MQTTcontrolTopic)
            }
        }

        connectButton.setOnClickListener()
        {
            sharedPreferences?.edit()?.putString("serverIP", serverIP.text.toString())?.apply()
            mainViewModel.serverAddress.value = serverIP.text.toString()
            (activity as MainActivity).connectToMQTT()
        }

        val tvStringObserver = Observer<String> { s ->
            TV.text = s
        }
        mainViewModel.tvString.observe(viewLifecycleOwner, tvStringObserver)

        val serverAddressObserver = Observer<String> { ip ->
            serverIP.setText(ip)
        }
        mainViewModel.serverAddress.observe(viewLifecycleOwner, serverAddressObserver)


        val joyObserver = Observer<Float> {
            joystickView.setPosition(mainViewModel.joyX.value!!, mainViewModel.joyY.value!!)
        }

        mainViewModel.joyX.observe(viewLifecycleOwner, joyObserver)
        mainViewModel.joyY.observe(viewLifecycleOwner, joyObserver)


        buttonStart.setOnClickListener {
            sendChannels(0, 0, 2, 0)
        }

        buttonStop.setOnClickListener {
            sendChannels(0, 0, 1, 0)
        }

        buttonStopBrush.setOnClickListener {
            sendChannels(0, 0, 11, 0)
        }

        buttonStartBrush.setOnClickListener{
            sendChannels(0, 0, 10, 0)
        }
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

        if (precisionSwich.isChecked) {
            x /= 4f
            y /= 4f
        }

        ch1 = (-x * 100 + 100).toInt()
        ch2 = (y * 100 + 100).toInt()
        ch3 = 100 //middle position
        ch4 = 100 //middle position

        if (BuildConfig.DEBUG)
            Log.d("bbb", "$ch1 $ch2 $ch3 $ch4")

        val bytes =
            byteArrayOf('$'.toByte(), 5, ch1.toByte(), ch2.toByte(), ch3.toByte(), ch4.toByte())
        if ((ch1 == 100 && ch2 == 100) || (ch3 == 100 && ch4 == 100) || (System.currentTimeMillis() > mainViewModel.t)) {
            mainViewModel.t = System.currentTimeMillis() + 100
            if (!watchSwitch.isChecked)
                if (mqttHelper.isConnected()) mqttHelper.publish(
                    mainViewModel.MQTTcontrolTopic,
                    bytes
                )
        }
    }

    fun sendChannels(ch1: Int, ch2: Int, ch3: Int, ch4: Int) {
        val bytes =
            byteArrayOf(
                '$'.toByte(),
                5,
                (ch1 + 100).toByte(),
                (ch2 + 100).toByte(),
                (ch3 + 100).toByte(),
                (ch4 + 100).toByte()
            )
        if (mqttHelper.isConnected()) mqttHelper.publish(
            mainViewModel.MQTTcontrolTopic,
            bytes
        )

    }
}