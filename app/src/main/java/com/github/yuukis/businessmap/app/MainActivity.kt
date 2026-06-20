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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsGroup
import com.github.yuukis.businessmap.model.ContactsItem
import com.github.yuukis.businessmap.widget.GroupAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(),
    ProgressDialogFragment.ProgressDialogFragmentListener,
    ContactsItemsDialogFragment.OnSelectListener {

    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var mapFragment: ContactsMapFragment
    private lateinit var listFragment: ContactsListFragment
    private lateinit var groupAdapter: GroupAdapter
    private lateinit var groupDropdown: MaterialAutoCompleteTextView
    private lateinit var progressBar: View
    private val groupListForAdapter = mutableListOf<ContactsGroup>()
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
        observeViewModel()
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
        if (viewModel.contactsList.value == null) {
            if (hasContactsPermission()) {
                viewModel.startContactsTask()
            } else {
                viewModel.clearContactsListIfPermissionMissing()
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
        if (hasContactsPermission() && viewModel.contactsList.value != null && !viewModel.isRunning.value) {
            viewModel.refreshGroupListPreservingSelection()
            viewModel.startContactsTask()
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
        outState.putInt(KEY_NAVIGATION_INDEX, viewModel.selectedGroupIndex.value)
        super.onSaveInstanceState(outState)
    }

    override fun onContactsSelected(contacts: ContactsItem?) {
        val animate = false
        mapFragment.showMarkerInfoWindow(contacts, animate)
    }

    override fun onProgressCancelled() {
        viewModel.cancelContactsTask()
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

    private fun initialize(savedInstanceState: Bundle?) {
        val args = intent.extras

        val fm = supportFragmentManager
        mapFragment = fm.findFragmentById(R.id.contacts_map) as ContactsMapFragment
        listFragment = fm.findFragmentById(R.id.contacts_list) as ContactsListFragment
        progressBar = findViewById(R.id.contacts_progressbar)

        groupAdapter = GroupAdapter(this, groupListForAdapter)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        groupDropdown = findViewById(R.id.dropdown_group)
        groupDropdown.setAdapter(groupAdapter)
        groupDropdown.setOnItemClickListener { _, _, position, _ -> viewModel.selectGroup(position) }

        val savedNavigationIndex = savedInstanceState?.getInt(KEY_NAVIGATION_INDEX)
        val intentGroupId = if (savedInstanceState == null) {
            args?.takeIf { it.containsKey(KEY_CONTACTS_GROUP_ID) }?.getLong(KEY_CONTACTS_GROUP_ID)
        } else {
            null
        }
        viewModel.initializeIfNeeded(hasContactsPermission(), savedNavigationIndex, intentGroupId)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.groupList.collect { groups ->
                        groupListForAdapter.clear()
                        groupListForAdapter.addAll(groups)
                        groupAdapter.notifyDataSetChanged()
                    }
                }
                launch {
                    combine(viewModel.groupList, viewModel.selectedGroupIndex) { groups, index ->
                        groups.getOrNull(index)?.title
                    }.collect { title -> groupDropdown.setText(title, false) }
                }
                launch {
                    viewModel.currentGroupContactsList.collect {
                        mapFragment.notifyDataSetChanged()
                        listFragment.notifyDataSetChanged()
                    }
                }
                launch {
                    viewModel.isRunning.collect { running ->
                        progressBar.visibility = if (running) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.progress.collect { progress -> updateProgressDialog(progress) }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is MainActivityEvent.ShowError -> showError(event.title, event.message)
                        }
                    }
                }
            }
        }
    }

    private fun updateProgressDialog(progress: GeocodingProgress?) {
        val dialog = getProgressDialogFragment()
        if (progress == null) {
            dialog?.dismissAllowingStateLoss()
            return
        }
        if (dialog == null) {
            showProgressDialog(progress.max)
        }
        getProgressDialogFragment()?.updateProgress(progress.current)
    }

    private fun showProgressDialog(max: Int) {
        val args = Bundle()
        args.putString(ProgressDialogFragment.TITLE, getString(R.string.title_geocoding))
        args.putString(ProgressDialogFragment.MESSAGE, getString(R.string.message_geocoding))
        args.putBoolean(ProgressDialogFragment.CANCELABLE, true)
        args.putInt(ProgressDialogFragment.MAX, max)
        val dialog = ProgressDialogFragment.newInstance()
        dialog.arguments = args
        dialog.show(supportFragmentManager, ProgressDialogFragment.TAG)
    }

    private fun getProgressDialogFragment(): ProgressDialogFragment? {
        return supportFragmentManager.findFragmentByTag(ProgressDialogFragment.TAG) as? ProgressDialogFragment
    }

    private fun showError(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    companion object {
        const val KEY_CONTACTS_GROUP_ID = "contacts_group_id"
        private const val KEY_NAVIGATION_INDEX = "navigation_index"
    }
}
