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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eziosoft.mqtt_test.data.SensorParser
import com.eziosoft.mqtt_test.databinding.RecycleViewItemBinding

@ExperimentalUnsignedTypes
class SensorsFragmentAdapter(private val dataSet: ArrayList<SensorParser.ParsedSensor>?) :
    RecyclerView.Adapter<SensorsFragmentAdapter.SensorsViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SensorsViewHolder {
        val binding =
            RecycleViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return SensorsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SensorsViewHolder, position: Int) {
        val currentItem = dataSet!![position]

        if (currentItem != null) {
            holder.bind(currentItem)
        }
    }

    override fun getItemCount(): Int = dataSet!!.size

    class SensorsViewHolder(private val binding: RecycleViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(currentItem: SensorParser.ParsedSensor) {

            binding.apply {
                sensorName.text = currentItem.name
                sensorValue.text = currentItem.value.toString()
            }
        }

    }


}