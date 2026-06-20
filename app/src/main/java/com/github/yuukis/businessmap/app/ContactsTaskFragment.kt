/*
 * ContactsTaskFragment.kt
 *
 * Copyright 2014 Yuuki Shimizu.
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

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.Data
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.data.GeocodingCacheDatabase
import com.github.yuukis.businessmap.model.ContactsGroup
import com.github.yuukis.businessmap.model.ContactsItem
import com.github.yuukis.businessmap.util.CacheUtils
import com.github.yuukis.businessmap.util.ContactsItemComparator
import com.github.yuukis.businessmap.util.CursorJoinerWithIntKey
import com.github.yuukis.businessmap.util.GeocoderUtils
import com.github.yuukis.businessmap.util.SerializationException
import com.github.yuukis.businessmap.util.SerializationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONException
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ContactsTaskFragment : Fragment() {

    interface TaskCallback {
        fun onContactsLoaded(contactsList: List<ContactsItem>?)
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    private var callback: TaskCallback? = null
    private var contactsTask: ContactsTask? = null
    private var running = false
    private var contactsList: MutableList<ContactsItem>? = null
    private var geocodingResultCache: MutableMap<String, Array<Double?>?>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is TaskCallback) { "Activity must implement the TaskCallback interface." }
        callback = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_contacts_task, null)
    }

    override fun onStart() {
        super.onStart()
        setProgressBarVisible(running)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    fun start() {
        if (!running) {
            val task = ContactsTask()
            contactsTask = task
            task.start()
            setRunning(true)
        }
    }

    fun cancel() {
        if (running) {
            contactsTask?.cancel()
            contactsTask = null
            setRunning(false)
        }
    }

    fun isRunning(): Boolean = running

    /**
     * Returns the contacts list already loaded by this retained fragment, or
     * null if [start] has never completed at least once. Lets a recreated
     * Activity adopt the in-memory result directly instead of re-querying
     * contacts on every configuration change.
     */
    fun getContactsList(): List<ContactsItem>? = contactsList

    private fun setRunning(value: Boolean) {
        running = value
        setProgressBarVisible(running)
    }

    private fun setProgressBarVisible(visible: Boolean) {
        val progressBar = view?.findViewById<View>(R.id.contacts_progressbar) ?: return
        progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun showProgress() {
        val title = getString(R.string.title_geocoding)
        val message = getString(R.string.message_geocoding)
        val max = geocodingResultCache?.size ?: 0

        val args = Bundle()
        args.putString(ProgressDialogFragment.TITLE, title)
        args.putString(ProgressDialogFragment.MESSAGE, message)
        args.putBoolean(ProgressDialogFragment.CANCELABLE, true)
        args.putInt(ProgressDialogFragment.MAX, max)
        val dialog = ProgressDialogFragment.newInstance()
        dialog.arguments = args
        if (activity != null) {
            Handler(Looper.getMainLooper()).post {
                activity?.let {
                    dialog.show(it.supportFragmentManager, ProgressDialogFragment.TAG)
                }
            }
        }
    }

    private fun onProgressUpdate(value: Int) {
        val progress = getProgressDialogFragment() ?: return
        progress.updateProgress(value)
    }

    private fun hideProgress() {
        val progress = getProgressDialogFragment() ?: return
        progress.dismissAllowingStateLoss()
    }

    private fun getProgressDialogFragment(): ProgressDialogFragment? {
        val fragment = activity?.supportFragmentManager
            ?.findFragmentByTag(ProgressDialogFragment.TAG)
        return fragment as? ProgressDialogFragment
    }

    private fun showError(title: String, message: String) {
        val currentActivity = activity ?: return
        Handler(Looper.getMainLooper()).post {
            MaterialAlertDialogBuilder(currentActivity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private inner class ContactsTask {

        @Volatile
        private var cancelled = false

        fun start() {
            onPreExecute()
            executor.execute {
                loadAllContacts()
                if (geocodingResultCache?.isNotEmpty() == true) {
                    geocoding()
                }
                handler.post { onPostExecute() }
            }
        }

        fun cancel() {
            cancelled = true
        }

        fun isCancelled(): Boolean = cancelled

        private fun onPreExecute() {
            var list = readContactsListFromCache()
            if (list == null) {
                list = ArrayList()
            }
            contactsList = list
            geocodingResultCache = HashMap()
            callback?.onContactsLoaded(contactsList)
            setRunning(true)
        }

        private fun onProgressUpdate(value: Int) {
            this@ContactsTaskFragment.onProgressUpdate(value)
        }

        private fun onPostExecute() {
            if (cancelled) {
                setRunning(false)
                return
            }

            writeContactsListInCache(contactsList)
            callback?.onContactsLoaded(contactsList)
            setRunning(false)
        }

        private fun loadAllContacts() {
            val context = requireActivity()
            geocodingResultCache?.clear()

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
                    groupCursor!!, arrayOf(Data.RAW_CONTACT_ID),
                    postalCursor!!, arrayOf(Data.RAW_CONTACT_ID)
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
                        for (gid in _groupIds) {
                            if (_address.isEmpty()) {
                                newContactsList.add(
                                    ContactsItem(_cid, _name, _phonetic, gid, null, _note, _companyName)
                                )
                                continue
                            }
                            for (addr in _address) {
                                val contact = ContactsItem(_cid, _name, _phonetic, gid, addr, _note, _companyName)
                                val latlng = geocodingDb.get(addr)
                                if (latlng != null && latlng.size == 2) {
                                    contact.setLat(latlng[0])
                                    contact.setLng(latlng[1])
                                } else {
                                    geocodingResultCache?.let {
                                        if (!it.containsKey(addr)) {
                                            it[addr] = null
                                        }
                                    }
                                }
                                newContactsList.add(contact)
                            }
                        }
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
                // FIXME: 冗長
                for (gid in _groupIds) {
                    if (_address.isEmpty()) {
                        newContactsList.add(
                            ContactsItem(_cid, _name, _phonetic, gid, null, _note, _companyName)
                        )
                        continue
                    }
                    for (addr in _address) {
                        val contact = ContactsItem(_cid, _name, _phonetic, gid, addr, _note, _companyName)
                        val latlng = geocodingDb.get(addr)
                        if (latlng != null && latlng.size == 2) {
                            contact.setLat(latlng[0])
                            contact.setLng(latlng[1])
                        } else {
                            geocodingResultCache?.let {
                                if (!it.containsKey(addr)) {
                                    it[addr] = null
                                }
                            }
                        }
                        newContactsList.add(contact)
                    }
                }
            } finally {
                groupCursor?.close()
                postalCursor?.close()
                noteCursor?.close()
                db?.close()
            }
            newContactsList.sortWith(ContactsItemComparator())
            contactsList = newContactsList
        }

        private fun geocoding() {
            val context = requireActivity()
            showProgress()

            val db = GeocodingCacheDatabase(context)
            var count = 0
            try {
                val cache = geocodingResultCache ?: return
                val iterator = cache.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    val address = entry.key

                    val latlng = GeocoderUtils.getFromLocationName(context, address)
                    db.put(address, latlng)
                    entry.setValue(latlng)

                    count++
                    val progress = count
                    handler.post { onProgressUpdate(progress) }

                    if (isCancelled()) {
                        return
                    }
                }
            } catch (e: IOException) {
                hideProgress()
                val title = getString(R.string.title_geocoding_ioerror)
                val message = getString(R.string.message_geocoding_ioerror)
                showError(title, message)
                return
            } catch (e: JSONException) {
                hideProgress()
                val title = getString(R.string.title_geocoding_jsonerror)
                val message = getString(R.string.message_geocoding_jsonerror)
                showError(title, message)
                return
            } finally {
                db.close()
            }

            val list = contactsList
            if (list != null) {
                for (j in list.indices) {
                    val contact = list[j]
                    val address = contact.address ?: continue
                    if (contact.lat != null && contact.lng != null) {
                        continue
                    }
                    val cache = geocodingResultCache ?: continue
                    if (!cache.containsKey(address)) {
                        continue
                    }

                    val latlng = cache[address]
                    if (latlng != null && latlng.size == 2) {
                        contact.setLat(latlng[0] ?: Double.NaN)
                        contact.setLng(latlng[1] ?: Double.NaN)
                        list[j] = contact
                    }
                }
            }
            hideProgress()
        }

        private fun readContactsListFromCache(): MutableList<ContactsItem>? {
            val context = requireActivity()
            var result: MutableList<ContactsItem>? = null
            try {
                val bytes = CacheUtils.read(context, FILENAME_CACHE_CONTACTS_LIST)
                @Suppress("UNCHECKED_CAST")
                result = SerializationUtils.deserialize(bytes) as? MutableList<ContactsItem>
            } catch (e: SerializationException) {
                // デシリアライズに失敗
                result = null
            } catch (e: FileNotFoundException) {
                // Nothing to do.
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return result
        }

        private fun writeContactsListInCache(contactsList: List<ContactsItem>?) {
            val context = requireActivity()
            if (contactsList == null) {
                return
            }

            val bytes = SerializationUtils.serialize(contactsList as java.io.Serializable)
            try {
                CacheUtils.write(context, bytes, FILENAME_CACHE_CONTACTS_LIST)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val FILENAME_CACHE_CONTACTS_LIST = "contacts_list.cache"
    }
}
