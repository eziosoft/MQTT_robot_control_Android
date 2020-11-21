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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.eziosoft.mqtt_test.data.Mqtt
import com.eziosoft.mqtt_test.data.Mqtt.Companion.MQTTStreamTopic
import com.eziosoft.mqtt_test.data.Mqtt.Companion.MQTTcontrolTopic
import com.eziosoft.mqtt_test.data.Mqtt.Companion.MQTTtelemetryTopic
import com.eziosoft.mqtt_test.data.MqttRepository
import com.eziosoft.mqtt_test.data.RoombaSensors
import com.eziosoft.mqtt_test.data.SensorParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttMessage
import javax.inject.Inject

@ExperimentalUnsignedTypes
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    val mainViewModel: MainViewModel by viewModels()

    private lateinit var navController: NavController

    @Inject
    lateinit var mqttRepository: MqttRepository
    lateinit var mqtt: Mqtt

    lateinit var sensorParser: SensorParser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.findNavController()
        val appBarConfiguration = AppBarConfiguration((navController.graph))
        setupActionBarWithNavController(navController, appBarConfiguration)

        sensorParser = SensorParser(object : SensorParser.SensorListener {
            override fun onSensors(sensors: ArrayList<SensorParser.ParsedSensor>) {
                GlobalScope.launch(Dispatchers.Main) {
                    mainViewModel.sensorDataSet.clear()
                    mainViewModel.sensorDataSet.addAll(sensors)

                    mainViewModel.dataSetChanged.value = 0

                    var telemetry = ""
                    for (s in mainViewModel.sensorDataSet) {
                        telemetry += s.toString1() + "\n"
                    }
                    mainViewModel.tvString.value = telemetry
                }
            }

            override fun onChkSumError() {

            }
        })
        mqtt = mqttRepository.mqtt
        mqtt.setListener(mqttListener)


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


    @ExperimentalUnsignedTypes
    private val mqttListener = object : Mqtt.MqttListener {
        override fun onConnectComplete(reconnect: Boolean, serverURI: String?) {
            Log.d("aaa", "connectComplete")
            mainViewModel.tvString.value = "Connected"
            mqtt.subscribe(MQTTtelemetryTopic)
            mqtt.subscribe(MQTTStreamTopic)
            mainViewModel.connectionStatus.value = true
        }

        override fun onMessageArrived(topic: String?, message: MqttMessage?) {
            when (topic) {
                MQTTtelemetryTopic -> {
                    mainViewModel.tvString.value =
                        message.toString() + " " + mainViewModel.voltage.value.toString()
                }
                MQTTcontrolTopic -> {
                    Log.d("aaa", "messageArrived: $topic :" + message.toString())
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

                MQTTStreamTopic -> {
                    GlobalScope.launch(Dispatchers.IO) {
                        val bytes = message!!.payload!!.toUByteArray()
                        if (!bytes.isEmpty()) {
                            sensorParser.parse(bytes)
                        }
                    }
                }
            }
        }


        override fun onConnectionLost(cause: Throwable?) {
            Log.d("aaa", "connectionLost")
            mainViewModel.tvString.value = "Connection lost"
            mainViewModel.connectionStatus.value = false
        }

        override fun onDeliveryComplete(token: IMqttDeliveryToken?) {
        }

    }


    fun connectToMQTT() {
//        mainViewModel.serverAddress.value = "mqtt.flespi.io:1883" //T
        val userName = "27aQSfkPYPrH1WHfjsDejLIqJxTza4i21gIHlTn8wEDlqarztSBAr7O0swnsqvi"

        val url = "tcp://" + mainViewModel.serverAddress.value

        if (mqtt.isConnected()) mqtt.close()
        mainViewModel.tvString.value =
            getString(R.string.connectig_to, mainViewModel.serverAddress.value)

        mqtt.connect(
            this,
            url,
            "user${System.currentTimeMillis()}",
            userName
        )
    }

    fun test() {
        GlobalScope.launch(Dispatchers.Main) {
            while (true) {
                delay(10000)
                val data1: UByteArray = ubyteArrayOf(5u, 5u, 8u, 65u, 18u, 18u, 19u, 5u, 29u, 2u)
                val data2: UByteArray = ubyteArrayOf(25u, 13u, 0u)
                val data3: UByteArray = ubyteArrayOf(163u, 5u, 5u, 8u, 65u, 18u, 18u)
                sensorParser.parse(data1)
                sensorParser.parse(data2)
                sensorParser.parse(data3)
            }
        }
    }

    override fun onStart() {
        super.onStart()
//        test()
    }
}

