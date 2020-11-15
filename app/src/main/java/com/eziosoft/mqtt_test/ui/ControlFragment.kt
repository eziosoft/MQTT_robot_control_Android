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

package com.eziosoft.mqtt_test.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TableRow
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.eziosoft.mqtt_test.BuildConfig
import com.eziosoft.mqtt_test.MainActivity
import com.eziosoft.mqtt_test.R
import com.eziosoft.mqtt_test.ui.customViews.JoystickView
import com.longdo.mjpegviewer.MjpegView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.control_fragment.*
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class ControlFragment : Fragment(R.layout.control_fragment), View.OnClickListener {

    private val controlFragmentViewModel: ControlFragmentViewModel by activityViewModels()

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
                (activity as MainActivity).mqtt.subscribe(controlFragmentViewModel.MQTTvideoTopic)
                switchVideo.visibility = View.GONE
            }
        }

        watchSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                (activity as MainActivity).mqtt.subscribe(controlFragmentViewModel.MQTTcontrolTopic)
            } else {
                (activity as MainActivity).mqtt.mqttClient.unsubscribe(controlFragmentViewModel.MQTTcontrolTopic)
            }
        }

        connectButton.setOnClickListener()
        {
            sharedPreferences?.edit()?.putString("serverIP", serverIP.text.toString())?.apply()
            controlFragmentViewModel.serverAddress.value = serverIP.text.toString()
            (activity as MainActivity).connectToMQTT()
        }

        val tvStringObserver = Observer<String> { s ->
            TV.text = s
        }
        controlFragmentViewModel.tvString.observe(viewLifecycleOwner, tvStringObserver)

        val serverAddressObserver = Observer<String> { ip ->
            serverIP.setText(ip)
        }
        controlFragmentViewModel.serverAddress.observe(viewLifecycleOwner, serverAddressObserver)


        val joyObserver = Observer<Float> {
            joystickView.setPosition(
                controlFragmentViewModel.joyX.value!!,
                controlFragmentViewModel.joyY.value!!
            )
        }
        controlFragmentViewModel.joyX.observe(viewLifecycleOwner, joyObserver)
        controlFragmentViewModel.joyY.observe(viewLifecycleOwner, joyObserver)

        //Add this.onClickListener to buttons in tableLayout
        Log.d("bbb", tableLayout.childCount.toString())
        for (i in 0 until tableLayout.childCount) {
            val row = tableLayout.getChildAt(i) as TableRow
            for (j in 0 until row.childCount) {
                val button = row.getChildAt(j)
                if (button is Button) button.setOnClickListener(this)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            buttonStart.id -> sendCommandsChannels(2, 0)
            buttonStop.id -> sendCommandsChannels(1, 0)
            buttonStopBrush.id -> sendCommandsChannels(11, 0)
            buttonStartBrush.id -> sendCommandsChannels(10, 0)
            buttonClean.id -> sendCommandsChannels(12, 0)
            buttonDock.id -> sendCommandsChannels(3, 0)
            buttonUnDock.id -> sendCommandsChannels(4, 0)
            buttonGetSensors.id -> sendCommandsChannels(20, 0)
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

        ch1 = (-x * 100).toInt()
        ch2 = (y * 100).toInt()
        ch3 = 0 //middle position
        ch4 = 0 //middle position

        if (BuildConfig.DEBUG)
            Log.d("bbb", "$ch1 $ch2 $ch3 $ch4")

        if ((ch1 == 0 && ch2 == 0) || (ch3 == 0 && ch4 == 0) || (System.currentTimeMillis() > controlFragmentViewModel.t)) {
            controlFragmentViewModel.t = System.currentTimeMillis() + 100
            if (!watchSwitch.isChecked)
                if ((activity as MainActivity).mqtt.isConnected())
                    sendChannels(ch1, ch2, ch3, ch4)
        }
    }

    private fun sendChannels(ch1: Int, ch2: Int, ch3: Int, ch4: Int) {
        val bytes =
            byteArrayOf(
                '$'.toByte(),
                5,
                (ch1 + 100).toByte(),
                (ch2 + 100).toByte(),
                (ch3 + 100).toByte(),
                (ch4 + 100).toByte()
            )
        if ((activity as MainActivity).mqtt.isConnected()) (activity as MainActivity).mqtt.publish(
            controlFragmentViewModel.MQTTcontrolTopic,
            bytes
        )
    }


    private fun sendCommandsChannels(ch3: Int, ch4: Int) {
        sendChannels(0, 0, ch3, ch4)
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
}
