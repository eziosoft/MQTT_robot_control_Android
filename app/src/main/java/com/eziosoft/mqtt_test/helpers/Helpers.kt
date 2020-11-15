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
 * Copyright (c) 2019. Bartosz Szczygiel
 *
 */

package com.eziosoft.mqtt_test.helpers

import android.text.Html
import android.os.Build
import android.text.Spanned
import java.text.SimpleDateFormat
import java.util.*


fun decryptStringWithXORFromHex(input: String, key: String): String {
    var k = key
    val c = StringBuilder()
    while (k.length < input.length / 2) k += key

    var i = 0
    while (i < input.length) {
        val hexValueString = input.substring(i, i + 2)
        val value1 = Integer.parseInt(hexValueString, 16)
        val value2 = key[i / 2].toInt()

        val xorValue = value1 xor value2

        c.append(Character.toString(xorValue.toChar()))
        i += 2

    }
    return c.toString()
}

fun map(x: Float, in_min: Float, in_max: Float, out_min: Float, out_max: Float): Float {
    return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
}

fun map(x: Int, in_min: Int, in_max: Int, out_min: Int, out_max: Int): Int {
    return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
}

@Suppress("DEPRECATION")
fun fromHtml(html: String): Spanned {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
    } else {
        Html.fromHtml(html)
    }
}


fun getFormattedTime(milliSeconds: Long, dateFormat: String = "dd/MM/yyyy hh:mm:ss.SSS"): String {
    // Create a DateFormatter object for displaying date in specified format.
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())

    // Create a calendar object that will convert the date and time value in milliseconds to date.
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = milliSeconds
    return formatter.format(calendar.time)
}