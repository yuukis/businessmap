/*
 * OssLicensesUtils.kt
 *
 * Copyright 2026 Yuuki Shimizu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.yuukis.businessmap.util

import android.content.Context
import com.github.yuukis.businessmap.model.OssLicense
import org.json.JSONArray
import org.json.JSONException

/**
 * Reads `open_source_licenses.json`, which the gradle-license-plugin regenerates
 * from the app's current dependencies on every build (see app/build.gradle.kts).
 */
object OssLicensesUtils {

    private const val FILEPATH = "open_source_licenses.json"

    @JvmStatic
    fun loadLicenses(context: Context): List<OssLicense> {
        val json = AssetUtils.getText(context, FILEPATH) ?: return emptyList()
        return try {
            parse(json)
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private fun parse(json: String): List<OssLicense> {
        val array = JSONArray(json)
        val licenses = mutableListOf<OssLicense>()
        for (i in 0 until array.length()) {
            val entry = array.getJSONObject(i)
            val name = entry.optString("project").ifBlank { entry.optString("dependency") }
            val firstLicense = entry.optJSONArray("licenses")?.optJSONObject(0)
            licenses.add(
                OssLicense(
                    name = name,
                    version = entry.optString("version"),
                    licenseName = firstLicense?.optString("license").orEmpty(),
                    licenseUrl = firstLicense?.optString("license_url")?.takeIf { it.isNotBlank() }
                )
            )
        }
        return licenses.sortedBy { it.name.lowercase() }
    }
}
