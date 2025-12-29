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

@file:OptIn(ExperimentalTime::class)

package org.jraf.blessr.repository

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jraf.klibfitbit.client.FitbitClient
import org.jraf.klibfitbit.client.configuration.ClientConfiguration
import org.jraf.klibfitbit.client.configuration.HttpConfiguration
import org.jraf.klibfitbit.client.configuration.HttpLoggingLevel
import org.jraf.klibfitbit.client.configuration.OAuthTokens
import org.jraf.klibfitbit.model.ActivityType
import org.jraf.klibfitbit.model.OAuthAuthorizationUrlResult
import org.jraf.klibnanolog.logd
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class Repository(
  private val fitbitClientId: String,
) {
  private val currentWalkFile = File("${System.getProperty("user.home")}/.blessr/current-walk.json")
  private val fitbitCredentialsFile = File("${System.getProperty("user.home")}/.blessr/fitbit-credentials.json")

  private val fitbitClient by lazy {
    val oAuthTokens = if (fitbitCredentialsFile.exists()) {
      Json.decodeFromString<FitbitCredentials>(fitbitCredentialsFile.readText())
    } else {
      null
    }?.let {
      OAuthTokens(
        accessToken = it.accessToken,
        refreshToken = it.refreshToken,
      )
    }
    FitbitClient.newInstance(
      ClientConfiguration(
        clientId = fitbitClientId,
        oAuthTokens = oAuthTokens,
        httpConfiguration = HttpConfiguration(
          loggingLevel = HttpLoggingLevel.ALL,
        ),
      ),
    ) { oAuthTokens ->
      logd("Got new OAuth tokens, saving them")
      fitbitCredentialsFile.parentFile.mkdirs()
      fitbitCredentialsFile.writeText(
        Json.encodeToString(
          FitbitCredentials(
            accessToken = oAuthTokens.accessToken,
            refreshToken = oAuthTokens.refreshToken,
          ),
        ),
      )
    }

  }

  fun loadCurrentWalkValues(): CurrentWalkValues {
    if (!currentWalkFile.exists()) return CurrentWalkValues(
      startedAt = Clock.System.now(),
      distanceMeters = 0.0,
      duration = Duration.ZERO,
    )
    val jsonText = currentWalkFile.readText()
    return Json.decodeFromString<CurrentWalkValues>(jsonText)
  }

  fun saveCurrentWalkValues(startedAt: Instant, distanceKilometers: Double, duration: Duration) {
    currentWalkFile.parentFile.mkdirs()
    currentWalkFile.writeText(
      Json.encodeToString(
        CurrentWalkValues(
          startedAt = startedAt,
          distanceMeters = distanceKilometers,
          duration = duration,
        ),
      ),
    )
  }

  fun clearCurrentWalkValues() {
    if (currentWalkFile.exists()) {
      currentWalkFile.delete()
    }
  }

  fun hasAuthorized(): Boolean {
    return fitbitCredentialsFile.exists()
  }

  private var oAuthAuthorizationUrlResult: OAuthAuthorizationUrlResult? = null

  fun getAuthorizationUrl(): String {
    oAuthAuthorizationUrlResult = fitbitClient.oAuthCreateAuthorizationUrl(listOf("activity"))
    return oAuthAuthorizationUrlResult!!.authorizeUrl
  }

  suspend fun handleAuthorizationCallback(callbackUrl: String) {
    fitbitClient.oAuthFetchTokens(oAuthAuthorizationUrlResult!!, callbackUrl)
    oAuthAuthorizationUrlResult = null
  }

  suspend fun loadTodayDailyValues(): DailyValues {
    val activityList = fitbitClient.getActivityList(today().atTime(0, 0, 0))
    return DailyValues(
      distanceKilometers = activityList.sumOf { it.distanceMeters } / 1000.0,
      duration = activityList.fold(Duration.ZERO) { acc, d -> acc + d.duration },
    )
  }

  suspend fun saveWalk(startedAt: Instant, distanceMeters: Double, duration: Duration) {
    logd("Saving activity: distanceMeters=$distanceMeters, duration=$duration")
    fitbitClient.createActivity(
      activityType = ActivityType.TreadmillWalk,
      start = startedAt.toLocalDateTime(TimeZone.currentSystemDefault()),
      duration = duration,
      distanceMeters = distanceMeters,
    )
  }

}

private fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

data class DailyValues(
  val distanceKilometers: Double,
  val duration: Duration,
)

@Serializable
data class CurrentWalkValues(
  val startedAt: Instant,
  val distanceMeters: Double,
  val duration: Duration,
)

@Serializable
data class FitbitCredentials(
  val accessToken: String,
  val refreshToken: String,
)