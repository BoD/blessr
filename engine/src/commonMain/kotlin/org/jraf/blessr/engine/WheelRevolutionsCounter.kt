/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2025-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jraf.blessr.engine

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class WheelRevolutionsCounter(
  private val speedSlidingWindow: Duration,
  private val wheelCircumferenceMeters: Double,
  private val initialDistanceMeters: Double,
  private val initialDuration: Duration,
) {

  private class RevolutionsElement(
    val timestamp: Instant,
    val wheelRevolutions: Int,
  )

  private val data = mutableListOf<RevolutionsElement>()

  fun add(wheelRevolutions: Int) {
    data.add(RevolutionsElement(timestamp = Clock.System.now(), wheelRevolutions = wheelRevolutions))
  }

  operator fun plusAssign(wheelRevolutions: Int) = add(wheelRevolutions)

  private fun oldestElementInWindowIndex(): Int {
    val lastTimestamp = data.last().timestamp
    for (i in data.indices.reversed()) {
      if (lastTimestamp - data[i].timestamp >= speedSlidingWindow) {
        return i
      }
    }
    return 0
  }

  val revolutionsPerSecond: Double
    get() = if (data.size < 2) {
      0.0
    } else {
      val first = data[oldestElementInWindowIndex()]
      val last = data.last()
      val revolutionsDelta = last.wheelRevolutions - first.wheelRevolutions
      val timeDeltaSeconds = (last.timestamp - first.timestamp).inWholeMilliseconds.toDouble() / 1000.0
      revolutionsDelta / timeDeltaSeconds
    }

  val speedMetersPerSecond: Double
    get() = revolutionsPerSecond * wheelCircumferenceMeters

  val speedKilometersPerHour: Double
    get() = speedMetersPerSecond * 3.6

  val distanceMeters: Double
    get() = if (data.isEmpty()) {
      initialDistanceMeters
    } else {
      val revolutionsDelta = data.last().wheelRevolutions - data.first().wheelRevolutions
      initialDistanceMeters + revolutionsDelta * wheelCircumferenceMeters
    }

  val distanceKilometers: Double
    get() = distanceMeters / 1000.0

  val duration: Duration
    get() = if (data.size < 2) {
      initialDuration
    } else {
      initialDuration + (data.last().timestamp - data.first().timestamp)
    }
}