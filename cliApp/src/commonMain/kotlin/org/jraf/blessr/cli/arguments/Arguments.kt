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

package org.jraf.blessr.cli.arguments

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.enum

class Arguments : CliktCommand("blessr") {
  init {
    context {
      helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true) }
    }
  }

  enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
  }

  val logLevel: LogLevel by option(
    "-l",
    "--log-level",
    help = "Set the log level",
  )
    .enum<LogLevel> { it.name.lowercase() }
    .default(LogLevel.INFO)

  val fitbitClientId: String by option(
    "-f",
    "--fitbit-client-id",
    help = "Fitbit Client ID to use for OAuth2 authorization",
  )
    .required()

  val deviceName: String by option(
    "-d",
    "--device-name",
    help = "Name of the Bluetooth LE device to connect to",
  )
    .required()

  val circumference: Double by option(
    "-c",
    "--circumference",
    help = "Wheel circumference in meters",
  )
    .double()
    .default(0.3615)

  override fun run() = Unit
}
