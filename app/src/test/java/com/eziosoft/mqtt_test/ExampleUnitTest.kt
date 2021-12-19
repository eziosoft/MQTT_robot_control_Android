package com.eziosoft.mqtt_test

import com.eziosoft.mqtt_test.repository.roomba.RoombaParsedSensor
import com.eziosoft.mqtt_test.repository.roomba.RoombaSensorParser
import com.eziosoft.mqtt_test.helpers.to16UByteArray
import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

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
        var checkSum =false
        val sensorParser = RoombaSensorParser(object : RoombaSensorParser.SensorListener {
            override fun onSensors(sensors: List<RoombaParsedSensor>, checksumOK: Boolean) {
                if(checksumOK) checkSum=true //check if checksum was OK at least once

                for (i in sensors.indices)
                    println("($i) ${sensors[i]}")
                count = sensors.size

                val testSensor0 = RoombaParsedSensor(
                    sensorID = 29,
                    b1 = 2u,
                    b2 = 25u,
                    unsignedValue = 537,
                    name = "Cliff Front Left Signal"
                )
                val testSensor1 = RoombaParsedSensor(
                    sensorID = 13,
                    b1 = 0u,
                    b2 = 0u,
                    unsignedValue = 0,
                    name = "Virtual Wall"
                )
//                assertEquals("object are not identical", sensors[0], testSensor0)
//                assertEquals("object are not identical", sensors[1], testSensor1)
                assert(checkSum) { "Checksum Error" }
            }
        })



        runBlocking {
            launch(Dispatchers.Main) {
                val data1: UByteArray = ubyteArrayOf(5u, 5u, 8u, 65u, 18u, 18u, 19u, 5u, 29u, 2u)
                val data2: UByteArray = ubyteArrayOf(25u, 13u, 0u)
                val data3: UByteArray = ubyteArrayOf(163u, 5u, 5u, 8u, 65u, 18u, 18u)

                sensorParser.logging = true
                sensorParser._parse(data1)
                sensorParser._parse(data2)
                sensorParser._parse(data3)


            }
        }
        assert(count > 0) { "0 sensors returned" }
    }
//        assert(count > 0) { "0 sensors returned" }


    @ExperimentalUnsignedTypes
    @Test
    fun parserTestSignedValues() {
        val sensorParser = RoombaSensorParser(object : RoombaSensorParser.SensorListener {

            override fun onSensors(sensors: List<RoombaParsedSensor>, checksumOK: Boolean) {

                for (s in sensors) {
                    println(s.toString())
                }
                assertEquals(
                    sensors[0],
                    RoombaParsedSensor(
                        sensorID = 23,
                        b1 = 255u,
                        b2 = 156u,
                        unsignedValue = 65436,
                        name = "Current",
                        units = "mA"
                    )
                )
                assertEquals(
                    sensors[1],
                    RoombaParsedSensor(
                        sensorID = 22,
                        b1 = 0u,
                        b2 = 30u,
                        unsignedValue = 30,
                        name = "Voltage",
                        units = "mV"
                    )
                )
                assertEquals(
                    sensors[2],
                    RoombaParsedSensor(
                        sensorID = 29,
                        b1 = 2u,
                        b2 = 15u,
                        unsignedValue = 527,
                        name = "Cliff Front Left Signal"
                    )
                )
                assertEquals(
                    sensors[3],
                    RoombaParsedSensor(
                        sensorID = 13,
                        b1 = 1u,
                        b2 = 0u,
                        unsignedValue = 1,
                        name = "Virtual Wall"
                    )
                )
                assertEquals(sensors[0].signedValue, -100)
            }


        })


        sensorParser.logging = true
        val v: UByteArray = (-100).to16UByteArray()

        val data1: ArrayList<UByte> =
            arrayListOf(
                19u,
                11u,
                23u,
                v[0],
                v[1],
                22u,
                0u,
                30u,
                29u,
                2u,
                15u,
                13u,
                1u
            )
        val checksum = 256u - data1.sum()
        data1.add((checksum.toUByte()))
        runBlocking(Dispatchers.Main) {
            sensorParser._parse(data1.toUByteArray())
        }
    }


}

