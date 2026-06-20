/*
 * ContactsActionFragment.kt
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

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsItem
import com.github.yuukis.businessmap.util.ActionUtils
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors

class ContactsActionFragment : BottomSheetDialogFragment(), AdapterView.OnItemClickListener {

    private var contact: ContactsItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        contact = requireArguments().getSerializable(KEY_CONTACTS) as? ContactsItem

        val view = inflater.inflate(R.layout.fragment_contacts_action, container, false)

        val titleView = view.findViewById<TextView>(R.id.title)
        titleView.text = contact?.name

        val adapter = MenuAdapter(requireActivity(), R.layout.gridview_contents, ACTION_ITEMS)
        val gridView = view.findViewById<GridView>(R.id.gridview)
        gridView.adapter = adapter
        gridView.onItemClickListener = this

        return view
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        val context = requireActivity()
        val data = ACTION_ITEMS[position % ACTION_ITEMS.size]
        val item = contact ?: return

        when (data.itemId) {
            ID_SHOW_CONTACTS -> ActionUtils.doShowContact(context, item)
            ID_DIRECTION -> ActionUtils.doShowDirections(context, item)
            ID_NAVIGATION -> ActionUtils.doStartDriveNavigation(context, item)
        }
    }

    private class BindData(val itemId: Int, val iconId: Int, val titleId: Int)

    private class ViewHolder(val imageView: ImageView, val textView: TextView)

    private class MenuAdapter(
        context: Context,
        private val layoutId: Int,
        objects: Array<BindData>
    ) : ArrayAdapter<BindData>(context, layoutId, objects) {

        private val inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val holder: ViewHolder

            if (convertView == null) {
                view = inflater.inflate(layoutId, parent, false)
                holder = ViewHolder(
                    view.findViewById(android.R.id.icon),
                    view.findViewById(android.R.id.title)
                )
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as ViewHolder
            }
            val data = getItem(position)
            if (data != null) {
                holder.textView.setText(data.titleId)
                holder.imageView.setImageResource(data.iconId)
                holder.imageView.imageTintList =
                    ColorStateList.valueOf(MaterialColors.getColor(holder.imageView, MaterialR.attr.colorOnSurface))
            }

            return view
        }
    }

    companion object {
        private const val TAG = "ContactsActionFragment"
        private const val KEY_CONTACTS = "contacts"
        private const val ID_SHOW_CONTACTS = 1
        private const val ID_DIRECTION = 2
        private const val ID_NAVIGATION = 3
        private val ACTION_ITEMS = arrayOf(
            BindData(ID_SHOW_CONTACTS, R.drawable.ic_action_person, R.string.action_contacts_detail),
            BindData(ID_DIRECTION, R.drawable.ic_action_directions, R.string.action_directions),
            BindData(ID_NAVIGATION, R.drawable.ic_action_navigation, R.string.action_drive_navigation)
        )

        @JvmStatic
        fun newInstance(contact: ContactsItem): ContactsActionFragment {
            val fragment = ContactsActionFragment()
            val args = Bundle()
            args.putSerializable(KEY_CONTACTS, contact)
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun showDialog(activity: FragmentActivity, contact: ContactsItem) {
            val manager = activity.supportFragmentManager
            newInstance(contact).show(manager, TAG)
        }
    }
}
