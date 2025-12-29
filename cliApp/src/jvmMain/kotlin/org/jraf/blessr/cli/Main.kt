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

package org.jraf.blessr.cli

import com.github.ajalt.clikt.core.main
import org.jraf.blessr.cli.arguments.Arguments
import org.jraf.blessr.engine.Blessr
import org.jraf.klibnanolog.LogLevel
import org.jraf.klibnanolog.logLevel
import org.jraf.klibnanolog.logToStdErr
import org.jraf.klibnanolog.logd

class Main {
  suspend fun run(arguments: Arguments) {
    logToStdErr = true
    logLevel = when (arguments.logLevel) {
      Arguments.LogLevel.DEBUG -> LogLevel.DEBUG
      Arguments.LogLevel.INFO -> LogLevel.INFO
      Arguments.LogLevel.WARNING -> LogLevel.WARNING
      Arguments.LogLevel.ERROR -> LogLevel.ERROR
    }
    logd("Hello, World!")
    Blessr(
      fitbitClientId = arguments.fitbitClientId,
      deviceName = arguments.deviceName,
      circumferenceMeters = arguments.circumference,
    ) { authorizeUrl ->
      println("Please authorize the app by visiting this URL:")
      println(authorizeUrl)
      print("Then enter the authorization URL: ")
      val authorizationUrl = readln().trim()
      authorizationUrl
    }.run()
  }
}

suspend fun main(av: Array<String>) {
  val arguments = Arguments()
  arguments.main(av)

  Main().run(arguments)
}
