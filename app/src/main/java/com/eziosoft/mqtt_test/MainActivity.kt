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
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.eziosoft.mqtt_test.data.MQTTcontrolTopic
import com.eziosoft.mqtt_test.data.MQTTtelemetryTopic
import com.eziosoft.mqtt_test.data.Mqtt
import com.eziosoft.mqtt_test.data.MqttRepository
import dagger.hilt.android.AndroidEntryPoint
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    val mainViewModel: MainViewModel by viewModels()

    private lateinit var navController: NavController

    @Inject
    lateinit var mqttRepository: MqttRepository
    lateinit var mqtt: Mqtt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.findNavController()
        val appBarConfiguration = AppBarConfiguration((navController.graph))
        setupActionBarWithNavController(navController, appBarConfiguration)

        mqtt = mqttRepository.mqtt


        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        mainViewModel.serverAddress.value =
            sharedPreferences?.getString("serverIP", "test.mosquitto.org:1883")

        mainViewModel.serverAddress.observe(this) { address ->
            sharedPreferences?.edit()?.putString("serverIP", address)?.apply()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private val mqttCallback = object : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            Log.d("aaa", "connectComplete")
            mainViewModel.tvString.value = "Connected"
            mqtt.subscribe(MQTTtelemetryTopic)
            mainViewModel.connectionStatus.value = true
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            Log.d("aaa", "messageArrived: $topic :" + message.toString())
            mainViewModel.tvString.value = message.toString()

            if (topic == MQTTcontrolTopic) {
                val b = message.toString().toByteArray()
                if (b[0] == '$'.toByte() && b[1] == 5.toByte()) {
                    val x: Float = -(b[2] - 100) / 100f
                    val y: Float = -(b[3] - 100) / 100f

                    mainViewModel.apply {
                        joyX.value = x
                        joyY.value = y
                    }

                }
            }
        }

        override fun connectionLost(cause: Throwable?) {
            Log.d("aaa", "connectionLost")
            mainViewModel.tvString.value = "Connection lost"

            mainViewModel.connectionStatus.value = false
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            Log.d("aaa", "deliveryComplete")
        }
    }


    fun connectToMQTT() {
        val url = "tcp://" + mainViewModel.serverAddress.value
        if (mqtt.isConnected()) mqtt.close()
        mainViewModel.tvString.value =
            "Connecting to ${mainViewModel.serverAddress.value}"
        mqtt.connect(
            this,
            url,
            "user${System.currentTimeMillis()}",
            mqttCallback
        )
    }


}
