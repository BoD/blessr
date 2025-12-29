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

@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package org.jraf.blessr.engine

import com.juul.kable.Bluetooth
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.State
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.jraf.blessr.repository.Repository
import org.jraf.klibnanolog.logd
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

// See https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/CSCS_v1.0/out/en/index-en.html#UUID-13a6d96c-b74e-a2ec-8f05-3ab21f119c35
private const val GATT_SERVICE_CYCLING_SPEED_AND_CADENCE = 0x1816
private const val GATT_CHARACTERISTIC_CYCLING_SPEED_AND_CADENCE_MEASUREMENT = 0x2A5B

class Blessr(
  fitbitClientId: String,
  private val deviceName: String,
  private val circumferenceMeters: Double,
  private val onAuthorize: suspend (String) -> String,
) {
  private val repository = Repository(fitbitClientId)

  suspend fun run() {
    if (!repository.hasAuthorized()) {
      repository.handleAuthorizationCallback(onAuthorize(repository.getAuthorizationUrl()))
    }

    println("Distance: -- km. Duration: --. Speed: -- km/h. Total distance: 0 km. Total duration: 0m.")
    while (true) {
      logd("Scanning for $deviceName device...")
      val advertisement = Scanner {
        logging {
          engine = NanoLogEngine()
        }
      }.advertisements.first { it.name?.contains(deviceName, ignoreCase = true) == true }
      logd("Found $deviceName device: ${advertisement.identifier}")

      // Get today's daily values
      val dailyValues = repository.loadTodayDailyValues()

      // Also add any saved current walk values. This is to avoid losing data if the program is killed / restarted during a walk.
      val savedCurrentWalkValues = repository.loadCurrentWalkValues()
      val wheelRevolutionsCounter = WheelRevolutionsCounter(
        speedSlidingWindow = 10.seconds,
        wheelCircumferenceMeters = circumferenceMeters,
        initialDistanceMeters = savedCurrentWalkValues.distanceMeters,
        initialDuration = savedCurrentWalkValues.duration,
      )

      val peripheral = Peripheral(advertisement) {
        logging {
          engine = NanoLogEngine()
        }
      }
      peripheral.connect().launch {
        logd("Connected to peripheral: ${peripheral.identifier}")
        val services = peripheral.services.first { it != null }!!
        val characteristic = services.first { it.serviceUuid == Bluetooth.BaseUuid + GATT_SERVICE_CYCLING_SPEED_AND_CADENCE }
          .characteristics.first { it.characteristicUuid == Bluetooth.BaseUuid + GATT_CHARACTERISTIC_CYCLING_SPEED_AND_CADENCE_MEASUREMENT }
        peripheral.observe(characteristic).collect { byteArray ->
          val receivedWheelRevolutions = byteArray[1].toUByte().toInt() or
            (byteArray[2].toUByte().toInt() shl 8) or
            (byteArray[3].toUByte().toInt() shl 16) or
            (byteArray[4].toUByte().toInt() shl 24)

//          val time = byteArray[5].toUByte().toInt() or
//            (byteArray[6].toUByte().toInt() shl 8)

          wheelRevolutionsCounter += receivedWheelRevolutions
          val distanceKilometers = wheelRevolutionsCounter.distanceKilometers
          val formattedDistance = String.format("%.2f", distanceKilometers)

          val duration = wheelRevolutionsCounter.duration
          val formattedDuration = duration.formatted()

          val formattedSpeed = String.format("%.1f", wheelRevolutionsCounter.speedKilometersPerHour)

          val totalDistanceKilometers = dailyValues.distanceKilometers + distanceKilometers
          val formattedTotalDistance = String.format("%.1f", totalDistanceKilometers)

          val totalDuration = dailyValues.duration + duration
          val formattedTotalDuration = totalDuration.formatted()
          println("Distance: $formattedDistance km. Duration: $formattedDuration. Speed: $formattedSpeed km/h. Total distance: $formattedTotalDistance km. Total duration: $formattedTotalDuration.")

          repository.saveCurrentWalkValues(
            startedAt = savedCurrentWalkValues.startedAt,
            distanceKilometers = distanceKilometers,
            duration = duration,
          )
        }
      }

      peripheral.state
        .onEach { state ->
          if (state is State.Disconnected) {
            peripheral.close()
          }
        }
        .takeWhile { it !is State.Disconnected }
        .collect()

      repository.clearCurrentWalkValues()
      repository.saveWalk(
        startedAt = savedCurrentWalkValues.startedAt,
        distanceMeters = wheelRevolutionsCounter.distanceMeters,
        duration = wheelRevolutionsCounter.duration,
      )

      val totalDistanceKilometers = dailyValues.distanceKilometers + wheelRevolutionsCounter.distanceKilometers
      val formattedTotalDistance = String.format("%.1f", totalDistanceKilometers)

      val totalDuration = dailyValues.duration + wheelRevolutionsCounter.duration
      val formattedTotalDuration = totalDuration.formatted()
      println("Distance: -- km. Duration: --. Speed: -- km/h. Total distance: $formattedTotalDistance km. Total duration: $formattedTotalDuration.")
    }
  }
}

private fun Duration.formatted(): String {
  val totalMinutes = inWholeMinutes
  if (totalMinutes < 60) {
    return "${totalMinutes}m"
  }
  val hours = totalMinutes / 60
  val minutes = totalMinutes % 60
  if (minutes == 0L) {
    return "${hours}h"
  }
  return "${hours}h ${minutes}m"
}