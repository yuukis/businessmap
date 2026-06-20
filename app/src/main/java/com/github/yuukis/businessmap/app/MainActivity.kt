/*
 * MainActivity.kt
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
package com.github.yuukis.businessmap.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsGroup
import com.github.yuukis.businessmap.model.ContactsItem
import com.github.yuukis.businessmap.util.ContactUtils
import com.github.yuukis.businessmap.widget.GroupAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class MainActivity : AppCompatActivity(),
    ContactsTaskFragment.TaskCallback, ProgressDialogFragment.ProgressDialogFragmentListener,
    ContactsItemsDialogFragment.OnSelectListener {

    private lateinit var groupList: MutableList<ContactsGroup>
    private var contactsList: List<ContactsItem>? = null
    private lateinit var currentGroupContactsList: MutableList<ContactsItem>
    private lateinit var mapFragment: ContactsMapFragment
    private lateinit var listFragment: ContactsListFragment
    private lateinit var taskFragment: ContactsTaskFragment
    private lateinit var groupAdapter: GroupAdapter
    private lateinit var groupDropdown: MaterialAutoCompleteTextView
    private var selectedGroupIndex = -1
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onPermissionsResult()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setContentView(R.layout.activity_main)
        applyEdgeToEdgeInsets()
        initialize(savedInstanceState)
        requestMissingPermissions()
    }

    /**
     * Apps targeting Android 15 (API 35) are forced edge-to-edge. The
     * MaterialToolbar now lives inside our own layout rather than being
     * reserved by the system, so it must absorb the status bar inset itself.
     * Padding alone would squeeze its fixed-height content (the group
     * dropdown) into a shorter area and clip it, so the inset is added on top
     * of the toolbar's original height instead, with padding only offsetting
     * the content down into that extra space.
     */
    private fun applyEdgeToEdgeInsets() {
        val root = findViewById<View>(R.id.activity_main_root)
        val toolbar = findViewById<View>(R.id.toolbar)
        val toolbarContentHeight = toolbar.layoutParams.height
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.layoutParams = toolbar.layoutParams.apply {
                height = toolbarContentHeight + bars.top
            }
            toolbar.setPadding(toolbar.paddingLeft, bars.top, toolbar.paddingRight, toolbar.paddingBottom)
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onStart() {
        super.onStart()
        if (contactsList == null) {
            val retained = taskFragment.getContactsList()
            if (retained != null) {
                // Activity was recreated (e.g. rotation); the retained task
                // fragment already has the data in memory, so adopt it
                // directly instead of re-querying contacts from scratch.
                contactsList = retained
                notifyDataSetChanged()
            } else {
                contactsList = ArrayList()
                if (hasContactsPermission() && !taskFragment.isRunning()) {
                    taskFragment.start()
                }
            }
        }
    }

    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun requestMissingPermissions() {
        val missing = ArrayList<String>()
        if (!hasContactsPermission()) {
            missing.add(Manifest.permission.READ_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
            missing.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun onPermissionsResult() {
        mapFragment.enableMyLocationIfPermitted()
        if (hasContactsPermission() && contactsList != null && !taskFragment.isRunning()) {
            groupList.clear()
            groupList.addAll(ContactUtils.getContactsGroupList(this))
            groupAdapter.notifyDataSetChanged()
            if (selectedGroupIndex < 0) {
                selectGroup(0)
            }
            taskFragment.start()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_about) {
            AboutDialogFragment.showDialog(this)
            return true
        }
        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_NAVIGATION_INDEX, selectedGroupIndex)
        super.onSaveInstanceState(outState)
    }

    override fun onContactsLoaded(contactsList: List<ContactsItem>?) {
        this.contactsList = contactsList
        notifyDataSetChanged()
    }

    override fun onContactsSelected(contacts: ContactsItem?) {
        val animate = false
        mapFragment.showMarkerInfoWindow(contacts, animate)
    }

    override fun onProgressCancelled() {
        taskFragment.cancel()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    // バックキーを押下
                    val listFragment = supportFragmentManager
                        .findFragmentById(R.id.contacts_list) as? ContactsListFragment
                    if (listFragment != null && listFragment.getVisibility()) {
                        // 連絡先一覧が表示されている場合は、連絡先レイヤーを閉じる
                        listFragment.setVisibility(false)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    val currentContactsList: List<ContactsItem>
        get() = currentGroupContactsList

    private fun initialize(savedInstanceState: Bundle?) {
        val args = intent.extras

        val fm = supportFragmentManager
        mapFragment = fm.findFragmentById(R.id.contacts_map) as ContactsMapFragment
        listFragment = fm.findFragmentById(R.id.contacts_list) as ContactsListFragment
        taskFragment = fm.findFragmentById(R.id.contacts_task) as ContactsTaskFragment
        groupList = if (hasContactsPermission()) {
            ContactUtils.getContactsGroupList(this).toMutableList()
        } else {
            ArrayList()
        }

        currentGroupContactsList = ArrayList()
        groupAdapter = GroupAdapter(this, groupList)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        groupDropdown = findViewById(R.id.dropdown_group)
        groupDropdown.setAdapter(groupAdapter)
        groupDropdown.setOnItemClickListener { _, _, position, _ -> selectGroup(position) }

        var navigationIndex = 0
        if (savedInstanceState != null) {
            navigationIndex = savedInstanceState.getInt(KEY_NAVIGATION_INDEX)
        } else if (args != null) {
            if (args.containsKey(KEY_CONTACTS_GROUP_ID)) {
                val groupId = args.getLong(KEY_CONTACTS_GROUP_ID)
                for (i in groupList.indices) {
                    val contactsGroup = groupList[i]
                    if (groupId == contactsGroup.id) {
                        navigationIndex = i
                        break
                    }
                }
            }
        }
        selectGroup(navigationIndex)
    }

    private fun selectGroup(index: Int) {
        if (index < 0 || index >= groupList.size) {
            return
        }
        selectedGroupIndex = index
        groupDropdown.setText(groupList[index].title, false)
        notifyDataSetChanged()
    }

    private fun notifyDataSetChanged() {
        val index = selectedGroupIndex
        if (index < 0 || index >= groupList.size) {
            return
        }
        val group = groupList[index]
        val groupId = group.id
        changeCurrentGroup(groupId)
        mapFragment.notifyDataSetChanged()
        listFragment.notifyDataSetChanged()
    }

    private fun changeCurrentGroup(groupId: Long) {
        currentGroupContactsList.clear()
        contactsList?.let {
            for (contact in it) {
                if (contact.groupId == groupId) {
                    currentGroupContactsList.add(contact)
                }
            }
        }
    }

    companion object {
        const val KEY_CONTACTS_GROUP_ID = "contacts_group_id"
        private const val KEY_NAVIGATION_INDEX = "navigation_index"
    }
}
