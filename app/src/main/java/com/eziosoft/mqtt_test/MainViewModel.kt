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

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.eziosoft.mqtt_test.data.RoombaParsedSensor

@ExperimentalUnsignedTypes
class MainViewModel @ViewModelInject constructor() : ViewModel() {

    val sensorDataSet = arrayListOf<RoombaParsedSensor>()

    fun getSensorValue(id: Int): Int? {
        return sensorDataSet.find { it.sensorID == id }?.signedValue
    }


    val dataSetChanged: MutableLiveData<Int> by lazy { MutableLiveData<Int>() }

    val tvString: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val serverAddress: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    val joyX: MutableLiveData<Float> by lazy { MutableLiveData<Float>() }
    val joyY: MutableLiveData<Float> by lazy { MutableLiveData<Float>() }


    var t: Long = 0

    val connectionStatus: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
}