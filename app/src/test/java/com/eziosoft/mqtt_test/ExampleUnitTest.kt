package com.eziosoft.mqtt_test

import android.util.Log
import com.eziosoft.mqtt_test.data.SensorParser
import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@ExperimentalCoroutinesApi
class ExampleUnitTest {
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }


    @ExperimentalUnsignedTypes
    @Test
    fun parserTest() {
        var count = 0
        val sensorParser = SensorParser(object : SensorParser.SensorListener {

            override fun onSensors(sensors: ArrayList<SensorParser.ParsedSensor>) {
                for (i in sensors.indices)
                    println("($i) ${sensors[i]}")
                count = sensors.size

                val testSensor0 = SensorParser.ParsedSensor(
                    sensorID = 29u,
                    b1 = 2u,
                    b2 = 25u,
                    value = 537,
                    name = "Cliff Front Left Signal"
                )
                val testSensor1 = SensorParser.ParsedSensor(
                    sensorID = 13u,
                    b1 = 0u,
                    b2 = 0u,
                    value = 0,
                    name = "Virtual Wall"
                )
                assertEquals("object are not identical", sensors[0], testSensor0)
                assertEquals("object are not identical", sensors[1], testSensor1)
            }

            override fun onChkSumError() {
                assert(false) { "check sum not correct" }
            }
        })



        runBlocking {
            launch(Dispatchers.Main) {
                val data1: UByteArray = ubyteArrayOf(5u, 5u, 8u, 65u, 18u, 18u, 19u, 5u, 29u, 2u)
                val data2: UByteArray = ubyteArrayOf(25u, 13u, 0u)
                val data3: UByteArray = ubyteArrayOf(163u, 5u, 5u, 8u, 65u, 18u, 18u)

                sensorParser.logging = false
                sensorParser.parse(data1)
                sensorParser.parse(data2)
                sensorParser.parse(data3)


            }
        }
        assert(count > 0) { "0 sensors returned" }
    }
}

