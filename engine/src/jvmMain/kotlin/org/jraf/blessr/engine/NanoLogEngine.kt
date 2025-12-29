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

import com.juul.kable.logs.LogEngine
import org.jraf.klibnanolog.logd
import org.jraf.klibnanolog.loge
import org.jraf.klibnanolog.logi
import org.jraf.klibnanolog.logw

class NanoLogEngine : LogEngine {
  override fun verbose(throwable: Throwable?, tag: String, message: String) {
    if (throwable != null) {
      logd(throwable, "$tag: $message")
    } else {
      logd("$tag: $message")
    }
  }

  override fun debug(throwable: Throwable?, tag: String, message: String) {
    if (throwable != null) {
      logd(throwable, "$tag: $message")
    } else {
      logd("$tag: $message")
    }
  }

  override fun info(throwable: Throwable?, tag: String, message: String) {
    if (throwable != null) {
      logi(throwable, "$tag: $message")
    } else {
      logi("$tag: $message")
    }
  }

  override fun warn(throwable: Throwable?, tag: String, message: String) {
    if (throwable != null) {
      logw(throwable, "$tag: $message")
    } else {
      logw("$tag: $message")
    }
  }

  override fun error(throwable: Throwable?, tag: String, message: String) {
    if (throwable != null) {
      loge(throwable, "$tag: $message")
    } else {
      loge("$tag: $message")
    }
  }

  override fun assert(throwable: Throwable?, tag: String, message: String) {
    if (throwable != null) {
      loge(throwable, "$tag: $message")
    } else {
      loge("$tag: $message")
    }
  }
}