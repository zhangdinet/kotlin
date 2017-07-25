/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.numbers


import org.junit.Test
import kotlin.math.*

fun toRadians(degrees: Double) = PI * degrees / 180

fun computeDistanceInMilesCommonMath(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadius = 3958.75
    val rads = lat1 / PI
    val dLat = lat2 - lat1
    val dLng = lng2 - lng1
    val sindLat = sin(dLat / 2) // sin(dLat / 2)
    val sindLng = sin(dLng / 2)
    val a = pow(sindLat, 2.0) + pow(sindLng, 2.0) * cos(lat1) * cos(lat2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    val dist = earthRadius * c

    return dist
}

fun <E: Number> List<E>.sum() {
    val r0 = pow(E, this.sumByDouble { it.toDouble() })
    val r2 = E.pow(this.sumByDouble { it.toDouble() })
    val e2 = E.pow(2)
//    val r1 = r0.adjustExponent(+2)

}

