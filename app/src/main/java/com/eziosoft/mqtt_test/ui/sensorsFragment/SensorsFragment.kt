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

package com.eziosoft.mqtt_test.ui.sensorsFragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.eziosoft.mqtt_test.MainViewModel
import com.eziosoft.mqtt_test.R
import com.eziosoft.mqtt_test.databinding.SensorsFragmentBinding
import com.eziosoft.mqtt_test.helpers.collectLatestLifecycleFLow
import dagger.hilt.android.AndroidEntryPoint

@ExperimentalUnsignedTypes
@AndroidEntryPoint
class SensorsFragment : Fragment(R.layout.sensors_fragment) {
    private var _binding: SensorsFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = SensorsFragmentBinding.bind(view)

        val adapter = SensorsFragmentAdapter()

        binding.apply {
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = adapter
        }

        collectLatestLifecycleFLow(viewModel.sensorFlow) { listOfSensors ->
            adapter.submitList(listOfSensors)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
