/*
 * MainActivityViewModel.kt
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
package com.github.yuukis.businessmap.app

import android.app.Application
import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.Data
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.data.GeocodingCacheDatabase
import com.github.yuukis.businessmap.model.ContactsGroup
import com.github.yuukis.businessmap.model.ContactsItem
import com.github.yuukis.businessmap.util.CacheUtils
import com.github.yuukis.businessmap.util.ContactUtils
import com.github.yuukis.businessmap.util.ContactsItemComparator
import com.github.yuukis.businessmap.util.CursorJoinerWithIntKey
import com.github.yuukis.businessmap.util.GeocoderUtils
import com.github.yuukis.businessmap.util.SerializationException
import com.github.yuukis.businessmap.util.SerializationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.FileNotFoundException
import java.io.IOException

data class GeocodingProgress(val max: Int, val current: Int)

sealed class MainActivityEvent {
    data class ShowError(val title: String, val message: String) : MainActivityEvent()
}

/**
 * Holds the contact and group data that previously lived as MainActivity
 * fields and in the retained ContactsTaskFragment. Because a ViewModel
 * survives configuration changes, rotation no longer requires re-querying
 * contacts or working around Activity recreation timing.
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val _groupList = MutableStateFlow<List<ContactsGroup>>(emptyList())
    val groupList: StateFlow<List<ContactsGroup>> = _groupList.asStateFlow()

    private val _contactsList = MutableStateFlow<List<ContactsItem>?>(null)
    val contactsList: StateFlow<List<ContactsItem>?> = _contactsList.asStateFlow()

    private val _selectedGroupIndex = MutableStateFlow(-1)
    val selectedGroupIndex: StateFlow<Int> = _selectedGroupIndex.asStateFlow()

    val currentGroupContactsList: StateFlow<List<ContactsItem>> = combine(
        _contactsList, _selectedGroupIndex, _groupList
    ) { contacts, index, groups ->
        val groupId = groups.getOrNull(index)?.id ?: return@combine emptyList()
        contacts?.filter { it.groupId == groupId } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _progress = MutableStateFlow<GeocodingProgress?>(null)
    val progress: StateFlow<GeocodingProgress?> = _progress.asStateFlow()

    private val _events = MutableSharedFlow<MainActivityEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MainActivityEvent> = _events.asSharedFlow()

    private var initialized = false
    private var pendingGroupId: Long? = null
    private var pendingNavigationIndex = 0
    private var contactsJob: Job? = null

    /**
     * Runs once per ViewModel instance, i.e. once per process lifetime
     * unless the Activity is recreated after process death. Rotation alone
     * keeps this ViewModel alive, so the existing group list and selection
     * are left untouched on later calls.
     */
    fun initializeIfNeeded(hasPermission: Boolean, savedNavigationIndex: Int?, intentGroupId: Long?) {
        if (initialized) {
            return
        }
        initialized = true
        pendingGroupId = intentGroupId
        pendingNavigationIndex = savedNavigationIndex ?: 0
        _groupList.value = if (hasPermission) ContactUtils.getContactsGroupList(getApplication()) else emptyList()
        selectGroup(resolvePendingIndex())
    }

    fun selectGroup(index: Int) {
        if (index < 0 || index >= _groupList.value.size) {
            return
        }
        _selectedGroupIndex.value = index
    }

    /**
     * Re-reads the group list (cheap) while keeping the current selection
     * when possible, falling back to the originally requested group.
     * Used after contacts permission is granted, since the group list may
     * have been empty until now.
     */
    fun refreshGroupListPreservingSelection() {
        val previousGroupId = _groupList.value.getOrNull(_selectedGroupIndex.value)?.id
        val newList = ContactUtils.getContactsGroupList(getApplication())
        _groupList.value = newList
        val index = previousGroupId
            ?.let { id -> newList.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 }
            ?: resolvePendingIndex()
        selectGroup(index)
    }

    private fun resolvePendingIndex(): Int {
        val groupId = pendingGroupId
        if (groupId != null) {
            val index = _groupList.value.indexOfFirst { it.id == groupId }
            if (index >= 0) {
                return index
            }
        }
        return pendingNavigationIndex.takeIf { it in _groupList.value.indices } ?: 0
    }

    fun clearContactsListIfPermissionMissing() {
        if (_contactsList.value == null) {
            _contactsList.value = emptyList()
        }
    }

    fun startContactsTask() {
        if (_isRunning.value) {
            return
        }
        _isRunning.value = true
        val job = viewModelScope.launch(Dispatchers.IO) {
            runContactsTask()
        }
        contactsJob = job
        // Reset on every completion path (success, cancellation, or an
        // uncaught exception) instead of only the happy path, so a failure
        // can never leave isRunning stuck true and block future runs. Guard
        // by identity in case a newer job has already replaced this one by
        // the time this one's cancellation finishes propagating.
        job.invokeOnCompletion {
            if (contactsJob === job) {
                contactsJob = null
                _isRunning.value = false
            }
        }
    }

    fun cancelContactsTask() {
        _progress.value = null
        contactsJob?.cancel()
    }

    private suspend fun runContactsTask() {
        val cachedList = readContactsListFromCache() ?: ArrayList()
        _contactsList.value = cachedList

        val geocodingResultCache = HashMap<String, Array<Double?>?>()
        val list = loadAllContacts(geocodingResultCache)
        if (list == null) {
            _events.tryEmit(
                MainActivityEvent.ShowError(
                    getApplication<Application>().getString(R.string.title_contacts_loaderror),
                    getApplication<Application>().getString(R.string.message_contacts_loaderror)
                )
            )
            return
        }
        _contactsList.value = list

        if (geocodingResultCache.isNotEmpty()) {
            geocodeMissingAddresses(list, geocodingResultCache)
        }

        if (!currentCoroutineContext().isActive) {
            return
        }

        writeContactsListInCache(list)
        _contactsList.value = list
    }

    /**
     * Returns null if the contacts provider failed to return a cursor
     * (ContentResolver.query() is allowed to return null, e.g. if the
     * provider crashes) instead of crashing on a non-null assertion.
     */
    private fun loadAllContacts(geocodingResultCache: MutableMap<String, Array<Double?>?>): MutableList<ContactsItem>? {
        val context = getApplication<Application>()

        val newContactsList = ArrayList<ContactsItem>()
        var groupCursor: Cursor? = null
        var postalCursor: Cursor? = null
        var noteCursor: Cursor? = null
        var companyCursor: Cursor? = null
        var db: GeocodingCacheDatabase? = null

        try {
            groupCursor = context.contentResolver.query(
                Data.CONTENT_URI,
                arrayOf(
                    GroupMembership.RAW_CONTACT_ID,
                    GroupMembership.CONTACT_ID,
                    GroupMembership.DISPLAY_NAME,
                    GroupMembership.PHONETIC_NAME,
                    GroupMembership.GROUP_ROW_ID
                ),
                "${Data.MIMETYPE}=?",
                arrayOf(GroupMembership.CONTENT_ITEM_TYPE),
                Data.RAW_CONTACT_ID
            )

            postalCursor = context.contentResolver.query(
                StructuredPostal.CONTENT_URI,
                arrayOf(
                    StructuredPostal.RAW_CONTACT_ID,
                    StructuredPostal.CONTACT_ID,
                    StructuredPostal.DISPLAY_NAME,
                    StructuredPostal.PHONETIC_NAME,
                    StructuredPostal.FORMATTED_ADDRESS
                ),
                null,
                null,
                StructuredPostal.RAW_CONTACT_ID
            )

            noteCursor = context.contentResolver.query(
                Data.CONTENT_URI,
                arrayOf(Note.RAW_CONTACT_ID, Note.NOTE),
                "${Data.MIMETYPE}=?",
                arrayOf(Note.CONTENT_ITEM_TYPE),
                Data.RAW_CONTACT_ID
            )

            companyCursor = context.contentResolver.query(
                Data.CONTENT_URI,
                arrayOf(Organization.RAW_CONTACT_ID, Organization.COMPANY),
                "${Data.MIMETYPE}=?",
                arrayOf(Organization.CONTENT_ITEM_TYPE),
                Data.RAW_CONTACT_ID
            )

            if (groupCursor == null || postalCursor == null) {
                return null
            }

            val noteMap = HashMap<Long, String>()
            noteCursor?.let {
                while (it.moveToNext()) {
                    val rowId = it.getLong(0)
                    val note = it.getString(1)
                    noteMap[rowId] = note
                }
            }

            val companyMap = HashMap<Long, String>()
            companyCursor?.let {
                while (it.moveToNext()) {
                    val rowId = it.getLong(0)
                    val company = it.getString(1)
                    companyMap[rowId] = company
                }
            }

            val joiner = CursorJoinerWithIntKey(
                groupCursor, arrayOf(Data.RAW_CONTACT_ID),
                postalCursor, arrayOf(Data.RAW_CONTACT_ID)
            )

            val geocodingDb = GeocodingCacheDatabase(context)
            db = geocodingDb
            var _rowId = -1L
            var _cid = -1L
            var _name: String? = null
            var _phonetic: String? = null
            var _note: String? = null
            var _companyName: String? = null
            val _groupIds = ArrayList<Long>()
            val _address = ArrayList<String>()

            for (result in joiner) {
                val rowId: Long
                val cid: Long
                val groupId: Long
                val name: String?
                val phonetic: String?
                val address: String?
                val note: String?
                val companyName: String?

                when (result) {
                    CursorJoinerWithIntKey.Result.LEFT -> {
                        rowId = groupCursor.getLong(0)
                        cid = groupCursor.getLong(1)
                        name = groupCursor.getString(2)
                        phonetic = groupCursor.getString(3)
                        groupId = groupCursor.getLong(4)
                        address = null
                        note = noteMap[rowId]
                        companyName = companyMap[rowId]
                    }

                    CursorJoinerWithIntKey.Result.RIGHT -> {
                        rowId = postalCursor.getLong(0)
                        cid = postalCursor.getLong(1)
                        name = postalCursor.getString(2)
                        phonetic = postalCursor.getString(3)
                        groupId = ContactsGroup.ID_GROUP_ALL_CONTACTS
                        address = postalCursor.getString(4)
                        note = noteMap[rowId]
                        companyName = companyMap[rowId]
                    }

                    CursorJoinerWithIntKey.Result.BOTH -> {
                        rowId = groupCursor.getLong(0)
                        cid = groupCursor.getLong(1)
                        name = groupCursor.getString(2)
                        phonetic = groupCursor.getString(3)
                        groupId = groupCursor.getLong(4)
                        address = postalCursor.getString(4)
                        note = noteMap[rowId]
                        companyName = companyMap[rowId]
                    }
                }

                if (_rowId != rowId) {
                    addContactsForRow(
                        newContactsList, geocodingDb, geocodingResultCache,
                        _groupIds, _address, _cid, _name, _phonetic, _note, _companyName
                    )
                    _rowId = rowId
                    _cid = cid
                    _name = name
                    _phonetic = phonetic
                    _groupIds.clear()
                    _groupIds.add(ContactsGroup.ID_GROUP_ALL_CONTACTS)
                    _address.clear()
                    _note = note
                    _companyName = companyName
                }

                if (!_groupIds.contains(groupId)) {
                    _groupIds.add(groupId)
                }
                if (address != null && !_address.contains(address)) {
                    _address.add(address)
                }
            }
            addContactsForRow(
                newContactsList, geocodingDb, geocodingResultCache,
                _groupIds, _address, _cid, _name, _phonetic, _note, _companyName
            )
        } finally {
            groupCursor?.close()
            postalCursor?.close()
            noteCursor?.close()
            companyCursor?.close()
            db?.close()
        }
        newContactsList.sortWith(ContactsItemComparator())
        return newContactsList
    }

    private fun addContactsForRow(
        newContactsList: MutableList<ContactsItem>,
        geocodingDb: GeocodingCacheDatabase,
        geocodingResultCache: MutableMap<String, Array<Double?>?>,
        groupIds: List<Long>,
        addresses: List<String>,
        cid: Long,
        name: String?,
        phonetic: String?,
        note: String?,
        companyName: String?
    ) {
        for (gid in groupIds) {
            if (addresses.isEmpty()) {
                newContactsList.add(ContactsItem(cid, name, phonetic, gid, null, note, companyName))
                continue
            }
            for (addr in addresses) {
                val contact = ContactsItem(cid, name, phonetic, gid, addr, note, companyName)
                val latlng = geocodingDb.get(addr)
                if (latlng != null && latlng.size == 2) {
                    contact.setLat(latlng[0])
                    contact.setLng(latlng[1])
                } else if (!geocodingResultCache.containsKey(addr)) {
                    geocodingResultCache[addr] = null
                }
                newContactsList.add(contact)
            }
        }
    }

    private suspend fun geocodeMissingAddresses(
        list: MutableList<ContactsItem>,
        geocodingResultCache: MutableMap<String, Array<Double?>?>
    ) {
        val context = getApplication<Application>()
        _progress.value = GeocodingProgress(max = geocodingResultCache.size, current = 0)

        val db = GeocodingCacheDatabase(context)
        var count = 0
        try {
            val iterator = geocodingResultCache.entries.iterator()
            while (iterator.hasNext()) {
                if (!currentCoroutineContext().isActive) {
                    return
                }
                val entry = iterator.next()
                val address = entry.key

                val latlng = GeocoderUtils.getFromLocationName(context, address)
                db.put(address, latlng)
                entry.setValue(latlng)

                count++
                _progress.value = GeocodingProgress(max = geocodingResultCache.size, current = count)
            }
        } catch (e: IOException) {
            _events.tryEmit(
                MainActivityEvent.ShowError(
                    context.getString(R.string.title_geocoding_ioerror),
                    context.getString(R.string.message_geocoding_ioerror)
                )
            )
            return
        } catch (e: JSONException) {
            _events.tryEmit(
                MainActivityEvent.ShowError(
                    context.getString(R.string.title_geocoding_jsonerror),
                    context.getString(R.string.message_geocoding_jsonerror)
                )
            )
            return
        } finally {
            db.close()
            _progress.value = null
        }

        for (j in list.indices) {
            val contact = list[j]
            val address = contact.address ?: continue
            if (contact.lat != null && contact.lng != null) {
                continue
            }
            val latlng = geocodingResultCache[address] ?: continue
            if (latlng.size == 2) {
                contact.setLat(latlng[0] ?: Double.NaN)
                contact.setLng(latlng[1] ?: Double.NaN)
                list[j] = contact
            }
        }
    }

    private fun readContactsListFromCache(): MutableList<ContactsItem>? {
        val context = getApplication<Application>()
        return try {
            val bytes = CacheUtils.read(context, FILENAME_CACHE_CONTACTS_LIST)
            @Suppress("UNCHECKED_CAST")
            SerializationUtils.deserialize(bytes) as? MutableList<ContactsItem>
        } catch (e: SerializationException) {
            null
        } catch (e: FileNotFoundException) {
            null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun writeContactsListInCache(contactsList: List<ContactsItem>) {
        val context = getApplication<Application>()
        val bytes = SerializationUtils.serialize(contactsList as java.io.Serializable)
        try {
            CacheUtils.write(context, bytes, FILENAME_CACHE_CONTACTS_LIST)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val FILENAME_CACHE_CONTACTS_LIST = "contacts_list.cache"
    }
}
