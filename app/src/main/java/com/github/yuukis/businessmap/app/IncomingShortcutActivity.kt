/*
 * IncomingShortcutActivity.kt
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

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsGroup

class IncomingShortcutActivity : FragmentActivity(), ContactsGroupDialogFragment.OnSelectListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContactsGroupDialogFragment.showDialog(this)
    }

    override fun onContactsGroupSelected(group: ContactsGroup?) {
        createShortcut(group)
        finish()
    }

    private fun createShortcut(group: ContactsGroup?) {
        if (group == null) {
            setResult(RESULT_CANCELED, null)
            return
        }
        val groupId = group.id
        var shortcutTitle = group.title
        if (shortcutTitle.isNullOrEmpty()) {
            shortcutTitle = getString(R.string.app_name)
        }

        val shortcutIntent = Intent(applicationContext, MainActivity::class.java)
        shortcutIntent.action = Intent.ACTION_VIEW
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        shortcutIntent.putExtra(MainActivity.KEY_CONTACTS_GROUP_ID, groupId)

        val shortcutInfo = ShortcutInfo.Builder(this, "contacts_group_$groupId")
            .setShortLabel(shortcutTitle)
            .setIcon(Icon.createWithResource(applicationContext, R.drawable.ic_launcher))
            .setIntent(shortcutIntent)
            .build()
        val shortcutManager = getSystemService(ShortcutManager::class.java)
        if (shortcutManager == null) {
            setResult(RESULT_CANCELED, null)
            return
        }

        setResult(RESULT_OK, shortcutManager.createShortcutResultIntent(shortcutInfo))
    }
}
