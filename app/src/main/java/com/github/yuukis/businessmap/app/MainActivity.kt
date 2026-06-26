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
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsGroup
import com.github.yuukis.businessmap.model.ContactsItem
import com.github.yuukis.businessmap.util.ActionUtils
import com.github.yuukis.businessmap.util.StringJUtils
import com.github.yuukis.businessmap.widget.MapWrapperLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

open class MainActivity : AppCompatActivity(),
    ProgressDialogFragment.ProgressDialogFragmentListener,
    ContactsItemsDialogFragment.OnSelectListener {

    private val viewModel: MainActivityViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            onPermissionsResult()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setContent {
            AppTheme {
                MainScreen(
                    viewModel = viewModel,
                    fragmentManager = supportFragmentManager,
                    onAboutClick = { AboutDialogFragment.showDialog(this) },
                    onGroupSelected = ::handleGroupSelected,
                    onContactClick = ::handleContactClick,
                )
            }
        }
        initialize(savedInstanceState)
        observeViewModel()
        requestMissingPermissions()
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
        return isPermissionGranted(Manifest.permission.READ_CONTACTS)
    }

    protected open fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMissingPermissions() {
        val missing = ArrayList<String>()
        if (!hasContactsPermission()) {
            missing.add(Manifest.permission.READ_CONTACTS)
        }
        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
            missing.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun onPermissionsResult() {
        mapFragment()?.enableMyLocationIfPermitted()
        if (hasContactsPermission() && viewModel.contactsList.value != null && !viewModel.isRunning.value) {
            viewModel.refreshGroupListPreservingSelection()
            viewModel.startContactsTask()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_NAVIGATION_INDEX, viewModel.selectedGroupIndex.value)
        super.onSaveInstanceState(outState)
    }

    override fun onContactsSelected(contacts: ContactsItem?) {
        val animate = false
        mapFragment()?.showMarkerInfoWindow(contacts, animate)
    }

    override fun onProgressCancelled() {
        viewModel.cancelContactsTask()
        finish()
    }

    private fun mapFragment(): ContactsMapFragment? =
        supportFragmentManager.findFragmentById(R.id.contacts_map) as? ContactsMapFragment

    private fun handleGroupSelected(index: Int) {
        val previousIndex = viewModel.selectedGroupIndex.value
        viewModel.selectGroup(index)
        if (viewModel.selectedGroupIndex.value != previousIndex) {
            mapFragment()?.clearLongPressMarker()
        }
    }

    private fun handleContactClick(contact: ContactsItem) {
        val animate = true
        val handled = mapFragment()?.showMarkerInfoWindow(contact, animate) == true
        if (handled) {
            viewModel.setContactsListVisible(false)
            return
        }

        val title = contact.name
        val items = arrayOf(getString(R.string.action_contacts_detail))
        MaterialAlertDialogBuilder(this).setTitle(title)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> ActionUtils.doShowContact(this, contact)
                }
            }.show()
    }

    private fun initialize(savedInstanceState: Bundle?) {
        val args = intent.extras

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
                    viewModel.currentGroupContactsList.collect {
                        mapFragment()?.notifyDataSetChanged()
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

@Composable
private fun MainScreen(
    viewModel: MainActivityViewModel,
    fragmentManager: FragmentManager,
    onAboutClick: () -> Unit,
    onGroupSelected: (Int) -> Unit,
    onContactClick: (ContactsItem) -> Unit,
) {
    val groupList by viewModel.groupList.collectAsState()
    val selectedGroupIndex by viewModel.selectedGroupIndex.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val isListVisible by viewModel.isContactsListVisible.collectAsState()
    val contactsList by viewModel.currentGroupContactsList.collectAsState()

    BackHandler(enabled = isListVisible) {
        viewModel.setContactsListVisible(false)
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                groupList = groupList,
                selectedGroupIndex = selectedGroupIndex,
                onGroupSelected = onGroupSelected,
                onToggleContactsList = viewModel::toggleContactsListVisible,
                onAboutClick = onAboutClick,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapWrapperLayout(context).apply {
                        id = R.id.map_relative_layout
                        addView(
                            FragmentContainerView(context).apply {
                                id = R.id.contacts_map
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        )
                    }
                },
                update = { container ->
                    val mapContainer = container.findViewById<View>(R.id.contacts_map)
                    attachFragmentIfNeeded(fragmentManager, R.id.contacts_map, mapContainer) { ContactsMapFragment() }
                }
            )

            if (isRunning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            ContactsListPanel(
                visible = isListVisible,
                contactsList = contactsList,
                onDismiss = { viewModel.setContactsListVisible(false) },
                onContactClick = onContactClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopAppBar(
    groupList: List<ContactsGroup>,
    selectedGroupIndex: Int,
    onGroupSelected: (Int) -> Unit,
    onToggleContactsList: () -> Unit,
    onAboutClick: () -> Unit,
) {
    TopAppBar(
        title = {
            GroupDropdown(
                groupList = groupList,
                selectedGroupIndex = selectedGroupIndex,
                onGroupSelected = onGroupSelected,
            )
        },
        actions = {
            IconButton(onClick = onToggleContactsList) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.action_list),
                )
            }
            IconButton(onClick = onAboutClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.action_about),
                )
            }
        },
        // The original MaterialToolbar had android:elevation="4dp", which
        // Material Components renders as a slightly tinted surface (tonal
        // elevation overlay). TopAppBar has no elevation by default, so
        // without this it looks flatter/whiter than before.
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.shadow(4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDropdown(
    groupList: List<ContactsGroup>,
    selectedGroupIndex: Int,
    onGroupSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTitle = groupList.getOrNull(selectedGroupIndex)?.title.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        // menuAnchor(PrimaryNotEditable) already wires up click-to-expand on
        // its own; a TextField is what implements that wiring (and the
        // accessibility/focus handling around it), so we keep it and only
        // override its colors/text style to blend into the TopAppBar
        // instead of showing its own filled background.
        TextField(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            value = selectedTitle,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            groupList.forEachIndexed { index, group ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(group.title.orEmpty())
                            val accountName = group.accountName
                            if (!accountName.isNullOrEmpty()) {
                                Text(
                                    text = accountName,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    },
                    onClick = {
                        onGroupSelected(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * The contacts list panel stays in composition at all times - open/close is
 * purely a translateX animation - rather than being added/removed via e.g.
 * AnimatedVisibility, so the search field's focus/IME state isn't torn down
 * every time the panel closes.
 */
@Composable
private fun BoxScope.ContactsListPanel(
    visible: Boolean,
    contactsList: List<ContactsItem>,
    onDismiss: () -> Unit,
    onContactClick: (ContactsItem) -> Unit,
) {
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
        )
    }

    val panelWidth = contactsListPanelWidth()
    val offFraction by animateFloatAsState(targetValue = if (visible) 0f else 1f)

    var query by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(visible) {
        if (!visible) {
            focusManager.clearFocus(force = true)
        }
    }

    Column(
        modifier = (if (panelWidth != null) Modifier.width(panelWidth) else Modifier.fillMaxWidth())
            .fillMaxHeight()
            .align(Alignment.CenterEnd)
            .graphicsLayer { translationX = size.width * offFraction }
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ContactsSearchField(
            query = query,
            onQueryChange = { query = it },
        )

        val filteredList = remember(contactsList, query) {
            filterContactsList(contactsList, query)
        }
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.message_no_contacts),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredList) { contact ->
                    ContactsListRow(
                        contact = contact,
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onContactClick(contact)
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    // SearchBarDefaults.InputField is the standard M3 search field row (pill
    // shape, tokens for colors) used inside SearchBar/DockedSearchBar. We
    // only want that standard look, not the expand-into-suggestions
    // behavior, so expanded/onExpandedChange are wired to no-ops.
    val focusManager = LocalFocusManager.current
    SearchBarDefaults.InputField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { focusManager.clearFocus(force = true) },
        expanded = false,
        onExpandedChange = {},
        placeholder = { Text(stringResource(R.string.hint_searchview)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.action_clear_search),
                    )
                }
            }
        },
    )
}

@Composable
private fun ContactsListRow(
    contact: ContactsItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = contact.name.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = contact.address ?: stringResource(R.string.message_no_data),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun filterContactsList(contactsList: List<ContactsItem>, query: String): List<ContactsItem> {
    if (query.isEmpty()) {
        return contactsList
    }
    val needle = StringJUtils.convertToKatakana(query)
    return contactsList.filter { contact ->
        listOfNotNull(contact.name, contact.phonetic, contact.companyName).any {
            StringJUtils.convertToKatakana(it).contains(needle)
        }
    }
}

/**
 * Commits a fresh Fragment into [container] (with id [containerId]) unless
 * one is already attached there with a view that is actually part of
 * [container] right now.
 *
 * Compose can hand us a brand-new [container] instance without ever
 * destroying the previous Fragment through FragmentManager - e.g. after an
 * Activity recreation (screen rotation), where a new AndroidView container
 * is created before FragmentManager's restored Fragment gets a chance to
 * attach to it. `fragment.view` can be non-null while still pointing at a
 * view that is no longer attached anywhere, so checking for null alone
 * isn't enough - we have to check it's still parented by *this* container.
 * Recreating the Fragment is safe here because ContactsMapFragment reads
 * all of its state from MainActivityViewModel rather than its own instance
 * state.
 *
 * Uses `commitNow` rather than `commit` deliberately: `commit` only
 * enqueues the transaction for the next main-thread message, so a second
 * call to this function (e.g. from a rapid follow-up recomposition) could
 * run before that transaction has actually attached anything, see
 * `existing == null` again, and enqueue a duplicate replace - thrashing
 * the Fragment. Committing synchronously means the Fragment is fully
 * attached by the time this function returns, so there's no such window.
 */
private fun attachFragmentIfNeeded(
    fragmentManager: FragmentManager,
    containerId: Int,
    container: View,
    createFragment: () -> Fragment,
) {
    val existing = fragmentManager.findFragmentById(containerId)
    if (existing == null || existing.view?.parent !== container) {
        fragmentManager.commitNow {
            replace(containerId, createFragment())
        }
    }
}

/**
 * `contacts_list_drawer_width` is -1px (the MATCH_PARENT sentinel) on
 * phones so the panel fills the screen width, and a fixed dp value on
 * larger screens (see values-sw600dp-land/values-sw720dp). Null here means
 * "fill the available width".
 */
@Composable
private fun contactsListPanelWidth(): Dp? {
    val context = LocalContext.current
    val pixelSize = context.resources.getDimensionPixelSize(R.dimen.contacts_list_drawer_width)
    if (pixelSize < 0) {
        return null
    }
    return with(LocalDensity.current) { pixelSize.toDp() }
}
