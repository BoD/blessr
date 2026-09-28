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

package org.jraf.blessr.repository

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jraf.blessr.util.createDirectories
import org.jraf.blessr.util.delete
import org.jraf.blessr.util.exists
import org.jraf.blessr.util.getHomePath
import org.jraf.blessr.util.readString
import org.jraf.blessr.util.writeString
import org.jraf.klibghealth.client.GoogleHealthClient
import org.jraf.klibghealth.client.GoogleHealthClient.Configuration
import org.jraf.klibghealth.client.GoogleHealthClient.Configuration.Auth
import org.jraf.klibghealth.client.GoogleHealthClient.Configuration.Auth.OAuthTokens
import org.jraf.klibghealth.client.GoogleHealthClient.Configuration.Auth.Scope
import org.jraf.klibghealth.client.GoogleHealthClient.Configuration.Http
import org.jraf.klibghealth.model.DataPoint
import org.jraf.klibghealth.model.ExerciseType
import org.jraf.klibghealth.model.OAuthAuthorizationUrlAndCodeVerifier
import org.jraf.klibnanolog.logd
import org.jraf.klibnanolog.loge
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class Repository(
  private val googleHealthClientId: String,
  private val googleHealthClientSecret: String,
) {
  private val currentWalkPath = Path(getHomePath(), ".blessr", "current-walk.json")
  private val googleHealthCredentialsPath = Path(getHomePath(), ".blessr", "google-health-credentials.json")

  private val googleHealthClient by lazy {
    val oAuthTokens = if (googleHealthCredentialsPath.exists()) {
      Json.decodeFromString<GoogleHealthCredentials>(googleHealthCredentialsPath.readString())
    } else {
      null
    }?.let {
      OAuthTokens(
        accessToken = it.accessToken,
        refreshToken = it.refreshToken,
      )
    }
    GoogleHealthClient(
      Configuration(
        auth = Auth(
          clientId = googleHealthClientId,
          clientSecret = googleHealthClientSecret,
          oAuthTokens = oAuthTokens,
        ),
        http = Http(
          loggingLevel = Http.LoggingLevel.ALL,
        ),
      ),
    ) { oAuthTokens ->
      logd("Got new OAuth tokens, saving them")
      googleHealthCredentialsPath.parent!!.createDirectories()
      googleHealthCredentialsPath.writeString(
        Json.encodeToString<GoogleHealthCredentials>(
          GoogleHealthCredentials(
            accessToken = oAuthTokens.accessToken,
            refreshToken = oAuthTokens.refreshToken,
          ),
        ),
      )
    }
  }

  fun loadCurrentWalkValues(): CurrentWalkValues {
    if (!currentWalkPath.exists()) return CurrentWalkValues(
      startedAt = Clock.System.now(),
      distanceMeters = 0.0,
      duration = Duration.ZERO,
    )
    val jsonText = currentWalkPath.readString()
    return Json.decodeFromString<CurrentWalkValues>(jsonText)
  }

  fun saveCurrentWalkValues(startedAt: Instant, distanceKilometers: Double, duration: Duration) {
    currentWalkPath.parent!!.createDirectories()
    currentWalkPath.writeString(
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
    if (currentWalkPath.exists()) {
      currentWalkPath.delete()
    }
  }

  fun hasAuthorized(): Boolean {
    return googleHealthCredentialsPath.exists()
  }

  private var oAuthAuthorizationUrlAndCodeVerifier: OAuthAuthorizationUrlAndCodeVerifier? = null

  fun getAuthorizationUrl(): String {
    oAuthAuthorizationUrlAndCodeVerifier = googleHealthClient.oAuth.createAuthorizationUrl(
      Scope.ActivityAndFitness.ReadOnly,
      Scope.ActivityAndFitness.WriteOnly,
      Scope.Sleep.ReadOnly,
      Scope.Sleep.WriteOnly,
    )
    return oAuthAuthorizationUrlAndCodeVerifier!!.authorizeUrl
  }

  suspend fun handleAuthorizationCallback(callbackUrl: String) {
    googleHealthClient.oAuth.fetchTokens(oAuthAuthorizationUrlAndCodeVerifier!!, callbackUrl)
    oAuthAuthorizationUrlAndCodeVerifier = null
  }

  suspend fun loadTodayDailyValues(): DailyValues {
    val activityList = googleHealthClient.dataPoint.getDataPointList(today())
      .getOrElse { throwable ->
        loge(throwable, "Failed to load activities")
        return DailyValues(
          distanceKilometers = 0.0,
          duration = Duration.ZERO,
        )
      }
      .filterIsInstance<DataPoint.Exercise>()
    return DailyValues(
      distanceKilometers = activityList.sumOf { it.distanceMeters } / 1000.0,
      duration = activityList.fold(Duration.ZERO) { acc, d -> acc + d.activeDuration },
    )
  }

  suspend fun saveWalk(startedAt: Instant, distanceMeters: Double, duration: Duration) {
    logd("Saving activity: distanceMeters=$distanceMeters, duration=$duration")
    googleHealthClient.dataPoint.createDataPoint(
      exerciseType = ExerciseType.TreadmillWalk,
      distanceMeters = distanceMeters,
      startTime = startedAt,
      activeDuration = duration,
    ).getOrElse { throwable ->
      loge(throwable, "Failed to save activity")
    }
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
data class GoogleHealthCredentials(
  val accessToken: String,
  val refreshToken: String,
)
