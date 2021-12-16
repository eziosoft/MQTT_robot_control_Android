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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eziosoft.mqtt_test.repository.roomba.RoombaParsedSensor
import com.eziosoft.mqtt_test.databinding.RecycleViewItemBinding

@ExperimentalUnsignedTypes
class SensorsFragmentAdapter() :
    ListAdapter<RoombaParsedSensor, SensorsFragmentAdapter.SensorsViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SensorsViewHolder {
        val binding =
            RecycleViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return SensorsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SensorsViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null) {
            holder.bind(currentItem)
        }
    }

    class SensorsViewHolder(private val binding: RecycleViewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(currentItem: RoombaParsedSensor) {
            binding.apply {
                sensorName.text = currentItem.getNameAndSensorID()
                sensorValue.text = currentItem.toStringValueWithUnits()
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<RoombaParsedSensor>() {
        override fun areItemsTheSame(
            oldItem: RoombaParsedSensor,
            newItem: RoombaParsedSensor
        ): Boolean {
            return oldItem.sensorID == newItem.sensorID
        }

        override fun areContentsTheSame(
            oldItem: RoombaParsedSensor,
            newItem: RoombaParsedSensor
        ): Boolean {
            return oldItem == newItem
        }
    }
}
