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

package com.eziosoft.mqtt_test.ui.controlFragment

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.eziosoft.mqtt_test.BuildConfig
import com.eziosoft.mqtt_test.MainViewModel
import com.eziosoft.mqtt_test.R
import com.eziosoft.mqtt_test.databinding.ControlFragmentBinding
import com.eziosoft.mqtt_test.helpers.collectLatestLifecycleFLow
import com.eziosoft.mqtt_test.repository.Repository
import com.eziosoft.mqtt_test.repository.Repository.Companion.MQTTcontrolTopic
import com.eziosoft.mqtt_test.ui.customViews.JoystickView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@ExperimentalUnsignedTypes
@AndroidEntryPoint
class ControlFragment : Fragment(R.layout.control_fragment), View.OnClickListener {

    private var _binding: ControlFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModels<MainViewModel>()

    private var bumpers = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ControlFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupJoystick()
        setUpListeners()
        setUpCollectors()

        binding.precisionSwich.isChecked = true
    }

    //handle tableLayout buttons
    override fun onClick(v: View?) = with(binding) {
        when (v?.id) {
            buttonStart.id -> viewModel.sendCommandsChannels(2, 0)
            buttonStop.id -> viewModel.sendCommandsChannels(1, 0)
            buttonStopBrush.id -> viewModel.sendCommandsChannels(11, 0)
            buttonStartBrush.id -> viewModel.sendCommandsChannels(10, 0)
            buttonClean.id -> viewModel.sendCommandsChannels(12, 0)
            buttonDock.id -> viewModel.sendCommandsChannels(3, 0)
            buttonUnDock.id -> viewModel.sendCommandsChannels(4, 0)
            buttonPowerOff.id -> viewModel.sendCommandsChannels(5, 0)
            buttonStartStream.id -> viewModel.sendCommandsChannels(20, 0)
            buttonPauseStream.id -> viewModel.sendCommandsChannels(21, 0)
            buttonShowSensors.id ->
                findNavController().navigate(
                    ControlFragmentDirections.actionControlFragmentToSensorsFragment()
                )
        }
    }

    private fun setUpListeners() {
        binding.connectButton.setOnClickListener()
        {
            viewModel.connectMqtt(binding.serverIP.text.toString())
        }

        //Add this.onClickListener to buttons in tableLayout
        for (i in 0 until binding.gridLayout.childCount) {
            val button = binding.gridLayout.getChildAt(i)
            if (button is Button) button.setOnClickListener(this)
        }
    }

    private fun setUpCollectors() {
        collectLatestLifecycleFLow(viewModel.connectionStatus) { status ->
            when (status) {
                Repository.ConnectionStatus.CONNECTED -> {
                    binding.serverIP.isVisible = false
                    binding.connectButton.isVisible = false
                }
                Repository.ConnectionStatus.DISCONNECTED -> {
                    binding.serverIP.isVisible = true
                    binding.connectButton.isVisible = true
                }
            }
        }

        collectLatestLifecycleFLow(viewModel.logFlow) {
            binding.TV.text = it
        }

        collectLatestLifecycleFLow(viewModel.sensorFlow) { listOfSensors ->
            binding.progressBarBattery.max = viewModel.getSensorValue(26) ?: 0
            binding.progressBarBattery.progress = viewModel.getSensorValue(25) ?: 0

            viewModel.getSensorValue(7)?.let {
                bumpers = it
                binding.viewLeft.visibility =
                    if (bumpers == 2 || bumpers == 3) View.VISIBLE else View.INVISIBLE
                binding.viewRight.visibility =
                    if (bumpers == 1 || bumpers == 3) View.VISIBLE else View.INVISIBLE
            }
        }
    }


    private fun setupJoystick() {
        binding.joystickView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setBorderWidth(5)
            setBorderColor(Color.BLUE)
            setButtonColor(Color.BLUE)
            setFixedCenter(true)
            isAutoReCenterButton = true
            axisToCenter = JoystickView.AxisToCenter.BOTH
            isSquareBehaviour = true

            setOnMoveListener { angle, strength ->
                lifecycleScope.launch {
                    if (binding.avoidSwitch.isChecked) {
                        //left
                        if (bumpers == 2 || bumpers == 3) {
                            handleJoystick(270, 100)
                            delay(200)
                            handleJoystick(0, 100)
                            delay(300)
                        }
                        //right
                        if (bumpers == 1 || bumpers == 3) {
                            handleJoystick(270, 100)
                            delay(200)
                            handleJoystick(180, 100)
                            delay(300)
                        }
                    }
                    handleJoystick(angle, strength)
                }
            }
        }
    }


    private fun handleJoystick(angle: Int, strength: Int) {
        Log.d("aaa", "handleJoystick: angle=$angle  strength=$strength")

        var x = cos(Math.toRadians(angle.toDouble())) * strength / 100f
        var y = sin(Math.toRadians(angle.toDouble())) * strength / 100f

        var ch1 = 0
        var ch2 = 0
        val ch3 = 0
        val ch4 = 0

        if (binding.precisionSwich.isChecked) {
            x /= 4f
            y /= 4f
        }

        ch1 = (-x * 100).toInt()
        ch2 = (y * 100).toInt()

        if (BuildConfig.DEBUG)
            Log.d("bbb", "$ch1 $ch2 $ch3 $ch4")

        if ((ch1 == 0 && ch2 == 0) || (ch3 == 0 && ch4 == 0) || (System.currentTimeMillis() > viewModel.t)) {
            viewModel.t = System.currentTimeMillis() + 100
            if (!binding.watchSwitch.isChecked)
                if (viewModel.isMqttConnected())
                    viewModel.sendChannels(ch1, ch2, ch3, ch4)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
