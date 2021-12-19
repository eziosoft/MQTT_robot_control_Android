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
import com.eziosoft.mqtt_test.MainViewModel
import com.eziosoft.mqtt_test.R
import com.eziosoft.mqtt_test.databinding.ControlFragmentBinding
import com.eziosoft.mqtt_test.helpers.collectLatestLifecycleFLow
import com.eziosoft.mqtt_test.repository.Repository
import com.eziosoft.mqtt_test.repository.getSensorValue
import com.eziosoft.mqtt_test.ui.customViews.JoystickView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        viewModel.repository.reportSensorsInterval = 0
        collectLatestLifecycleFLow(viewModel.sensorFlow) { listOfSensors ->
            binding.progressBarBattery.max = listOfSensors.getSensorValue(26) ?: 0
            binding.progressBarBattery.progress = listOfSensors.getSensorValue(25) ?: 0

            listOfSensors.getSensorValue(7)?.let {
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
                            viewModel.sendJoystickData(
                                270,
                                100,
                                binding.precisionSwich.isChecked,
                                binding.watchSwitch.isChecked
                            )
                            delay(200)
                            viewModel.sendJoystickData(
                                0,
                                100,
                                binding.precisionSwich.isChecked,
                                binding.watchSwitch.isChecked
                            )
                            delay(300)
                        }
                        //right
                        if (bumpers == 1 || bumpers == 3) {
                            viewModel.sendJoystickData(
                                270,
                                100,
                                binding.precisionSwich.isChecked,
                                binding.watchSwitch.isChecked
                            )
                            delay(200)
                            viewModel.sendJoystickData(
                                180,
                                100,
                                binding.precisionSwich.isChecked,
                                binding.watchSwitch.isChecked
                            )
                            delay(300)
                        }
                    }
                    viewModel.sendJoystickData(
                        angle,
                        strength,
                        binding.precisionSwich.isChecked,
                        binding.watchSwitch.isChecked
                    )
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
