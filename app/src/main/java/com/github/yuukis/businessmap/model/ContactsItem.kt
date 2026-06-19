/*
 * ContactsItem.kt
 *
 * Copyright 2013 Yuuki Shimizu.
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
package com.github.yuukis.businessmap.model

import java.io.Serializable

class ContactsItem(
    @get:JvmName("getCID") val cid: Long,
    var name: String?,
    @get:JvmName("getPhontic") var phonetic: String?,
    var groupId: Long,
    var address: String?,
    var note: String?,
    var companyName: String?
) : Serializable {

    var lat: Double? = null
        private set

    var lng: Double? = null
        private set

    fun setLat(lat: Double) {
        this.lat = if (lat.isNaN()) null else lat
    }

    fun setLng(lng: Double) {
        this.lng = if (lng.isNaN()) null else lng
    }

    // groupId is intentionally excluded, matching the original Java
    // implementation: markers on the map are deduplicated by contact
    // identity regardless of which group they're being viewed under.
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (address?.hashCode() ?: 0)
        result = prime * result + (cid xor (cid ushr 32)).toInt()
        result = prime * result + (companyName?.hashCode() ?: 0)
        result = prime * result + (lat?.hashCode() ?: 0)
        result = prime * result + (lng?.hashCode() ?: 0)
        result = prime * result + (name?.hashCode() ?: 0)
        result = prime * result + (note?.hashCode() ?: 0)
        result = prime * result + (phonetic?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as ContactsItem
        return address == other.address &&
            cid == other.cid &&
            companyName == other.companyName &&
            lat == other.lat &&
            lng == other.lng &&
            name == other.name &&
            note == other.note &&
            phonetic == other.phonetic
    }

    companion object {
        private const val serialVersionUID = -5022460356265154213L
    }
}
