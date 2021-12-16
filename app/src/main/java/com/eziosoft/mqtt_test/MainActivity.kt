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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.eziosoft.mqtt_test.helpers.to16UByteArray
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlin.random.Random

@ExperimentalUnsignedTypes
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    val TESTING = false
    val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.findNavController()
        val appBarConfiguration = AppBarConfiguration((navController.graph))
        setupActionBarWithNavController(navController, appBarConfiguration)


        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        viewModel.serverAddress.value =
            sharedPreferences?.getString("serverIP", "test.mosquitto.org:1883")

        viewModel.serverAddress.observe(this) { address ->
            sharedPreferences?.edit()?.putString("serverIP", address)?.apply()
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }


    fun test() {
        lifecycleScope.launch(Dispatchers.Main) {
            while (true) {
                delay(15)
                val v: UByteArray = (Random.nextInt(-2000, 2000)).to16UByteArray()

                val data1: ArrayList<UByte> =
                    arrayListOf(
                        19u,
                        35u,
                        46u,
                        10u,
                        Random.nextInt(255).toUByte(),
                        47u,
                        10u,
                        Random.nextInt(255).toUByte(),
                        48u,
                        10u,
                        Random.nextInt(255).toUByte(),
                        49u,
                        10u,
                        Random.nextInt(255).toUByte(),
                        50u,
                        10u,
                        Random.nextInt(255).toUByte(),
                        51u,
                        10u,
                        Random.nextInt(255).toUByte(),
                        26u,
                        100u,
                        0u,
                        25u,
                        80u,
                        Random.nextInt(255).toUByte(),
                        23u,
                        v[0],
                        v[1],
                        22u,
                        0u,
                        Random.nextInt(200).toUByte(),
                        29u,
                        2u,
                        Random.nextInt(200).toUByte(),
                        13u,
                        Random.nextInt(2).toUByte()
                    )
                val checksum = 256u - data1.sum()
                data1.add((checksum.toUByte()))
                viewModel.sensorParser.parse(data1.toUByteArray())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (TESTING) {
            viewModel.tvString.value = "TEST"
            test()
        }
    }
}
