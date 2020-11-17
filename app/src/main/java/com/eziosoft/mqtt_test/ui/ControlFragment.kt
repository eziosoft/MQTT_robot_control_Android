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

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TableRow
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.eziosoft.mqtt_test.BuildConfig
import com.eziosoft.mqtt_test.MainActivity
import com.eziosoft.mqtt_test.MainViewModel
import com.eziosoft.mqtt_test.R
import com.eziosoft.mqtt_test.data.MQTTcontrolTopic
import com.eziosoft.mqtt_test.data.MqttRepository
import com.eziosoft.mqtt_test.databinding.ControlFragmentBinding
import com.eziosoft.mqtt_test.ui.customViews.JoystickView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.control_fragment.*
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

@AndroidEntryPoint
class ControlFragment : Fragment(R.layout.control_fragment), View.OnClickListener {

    @Inject
    lateinit var mqttRepository: MqttRepository
    private val controlFragmentViewModel by activityViewModels<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val binding = ControlFragmentBinding.bind(view)
//        binding.apply { }


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
                switchVideo.isVisible = false
                webView.settings.loadWithOverviewMode = true;
                webView.settings.useWideViewPort = true;
                webView.loadUrl("http://192.168.0.56:8080/browserfs.html")
            }
        }

        watchSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mqttRepository.mqtt.subscribe(MQTTcontrolTopic)
            } else {
                mqttRepository.mqtt.mqttClient.unsubscribe(MQTTcontrolTopic)
            }
        }

        connectButton.setOnClickListener()
        {
            controlFragmentViewModel.serverAddress.value = serverIP.text.toString()
            (activity as MainActivity).connectToMQTT()
        }


        //Add this.onClickListener to buttons in tableLayout
        for (i in 0 until tableLayout.childCount) {
            val row = tableLayout.getChildAt(i) as TableRow
            for (j in 0 until row.childCount) {
                val button = row.getChildAt(j)
                if (button is Button) button.setOnClickListener(this)
            }
        }


        controlFragmentViewModel.tvString.observe(viewLifecycleOwner) { s ->
            TV.text = s
        }

        controlFragmentViewModel.serverAddress.observe(viewLifecycleOwner) { ip ->
            serverIP.setText(ip)
        }


        val joyObserver = Observer<Float> {
            joystickView.setPosition(
                controlFragmentViewModel.joyX.value!!,
                controlFragmentViewModel.joyY.value!!
            )
        }
        controlFragmentViewModel.joyX.observe(viewLifecycleOwner, joyObserver)
        controlFragmentViewModel.joyY.observe(viewLifecycleOwner, joyObserver)


        controlFragmentViewModel.connectionStatus.observe(viewLifecycleOwner) { connected ->
            if (connected) {
                serverIP.isVisible = false
                connectButton.isVisible = false

            } else {
                serverIP.isVisible = true
                connectButton.isVisible = true
            }

        }

    }

    //handle tableLayout buttons
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

        var ch1 = 0
        var ch2 = 0
        val ch3 = 0
        val ch4 = 0

        if (precisionSwich.isChecked) {
            x /= 4f
            y /= 4f
        }

        ch1 = (-x * 100).toInt()
        ch2 = (y * 100).toInt()

        if (BuildConfig.DEBUG)
            Log.d("bbb", "$ch1 $ch2 $ch3 $ch4")

        if ((ch1 == 0 && ch2 == 0) || (ch3 == 0 && ch4 == 0) || (System.currentTimeMillis() > controlFragmentViewModel.t)) {
            controlFragmentViewModel.t = System.currentTimeMillis() + 100
            if (!watchSwitch.isChecked)
                if (mqttRepository.mqtt.isConnected())
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
        if (mqttRepository.mqtt.isConnected()) mqttRepository.mqtt.publish(
            MQTTcontrolTopic,
            bytes
        )
    }


    private fun sendCommandsChannels(ch3: Int, ch4: Int) {
        sendChannels(0, 0, ch3, ch4)
    }


}
