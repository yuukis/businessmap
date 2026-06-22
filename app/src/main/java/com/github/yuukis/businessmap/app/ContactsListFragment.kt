/*
 * ContactsListFragment.kt
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

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.ListFragment
import androidx.fragment.app.activityViewModels
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsItem
import com.github.yuukis.businessmap.util.ActionUtils
import com.github.yuukis.businessmap.util.StringJUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ContactsListFragment : ListFragment(), SearchView.OnQueryTextListener {

    private val viewModel: MainActivityViewModel by activityViewModels()

    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_contacts_list, null)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        searchView = requireView().findViewById(R.id.searchview)
        searchView.setIconifiedByDefault(false)
        searchView.setOnQueryTextListener(this)
        searchView.isSubmitButtonEnabled = false
        contactsAdapter = ContactsAdapter()
        listAdapter = contactsAdapter
        applyEmptyText(getString(R.string.message_no_contacts))
        listView.isTextFilterEnabled = true
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        searchView.clearFocus()
        val contact = contactsAdapter.getItem(position) as? ContactsItem ?: return
        val mapFragment = parentFragmentManager.findFragmentById(R.id.contacts_map) as? ContactsMapFragment
        if (mapFragment != null) {
            val animate = true
            val result = mapFragment.showMarkerInfoWindow(contact, animate)
            if (result) {
                setVisibility(false)
                return
            }
        }

        val context = requireActivity()
        val title = contact.name
        val items = arrayOf(getString(R.string.action_contacts_detail))
        MaterialAlertDialogBuilder(requireActivity()).setTitle(title)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> ActionUtils.doShowContact(context, contact)
                }
            }.show()
    }

    override fun onQueryTextChange(newText: String): Boolean {
        val listView = listView
        if (TextUtils.isEmpty(newText)) {
            listView.clearTextFilter()
        } else {
            listView.setFilterText(newText)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        searchView.clearFocus()
        return false
    }

    fun notifyDataSetChanged() {
        searchView.clearFocus()
        contactsAdapter.notifyDataSetChanged()
    }

    fun getVisibility(): Boolean = viewModel.isContactsListVisible.value

    fun setVisibility(visible: Boolean) {
        if (!visible) {
            searchView.clearFocus()
        }
        viewModel.setContactsListVisible(visible)
    }

    private fun applyEmptyText(text: CharSequence) {
        val tv = listView.emptyView as TextView
        tv.text = text
    }

    private fun getContactsList(): List<ContactsItem>? = viewModel.currentGroupContactsList.value

    private class ViewHolder(val textView1: TextView, val textView2: TextView)

    private inner class ContactsAdapter : BaseAdapter(), Filterable {

        private var filterResultList: List<ContactsItem>? = null

        override fun getCount(): Int {
            val list = filterResultList ?: getContactsList()
            return list?.size ?: 0
        }

        override fun getItem(position: Int): Any? {
            val list = filterResultList ?: getContactsList()
            return list?.get(position)
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val holder: ViewHolder

            if (convertView == null) {
                view = requireActivity().layoutInflater.inflate(
                    android.R.layout.simple_list_item_2, null
                )
                holder = ViewHolder(
                    view.findViewById(android.R.id.text1),
                    view.findViewById(android.R.id.text2)
                )
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as ViewHolder
            }

            val contact = getItem(position) as ContactsItem
            val name = contact.name
            val address = contact.address ?: getString(R.string.message_no_data)
            holder.textView1.text = name
            holder.textView2.text = address

            return view
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val results = FilterResults()
                    val contactsList = getContactsList()
                    if (TextUtils.isEmpty(constraint)) {
                        results.values = null
                        results.count = 0
                    } else {
                        val filterResultData = ArrayList<ContactsItem>()
                        if (contactsList != null) {
                            for (contacts in contactsList) {
                                var query = constraint.toString()
                                query = StringJUtils.convertToKatakana(query)

                                var name = contacts.name
                                if (name != null) {
                                    name = StringJUtils.convertToKatakana(name)
                                    if (name.indexOf(query) >= 0) {
                                        filterResultData.add(contacts)
                                        continue
                                    }
                                }

                                var phonetic = contacts.phonetic
                                if (phonetic != null) {
                                    phonetic = StringJUtils.convertToKatakana(phonetic)
                                    if (phonetic.indexOf(query) >= 0) {
                                        filterResultData.add(contacts)
                                        continue
                                    }
                                }

                                var companyName = contacts.companyName
                                if (companyName != null) {
                                    companyName = StringJUtils.convertToKatakana(companyName)
                                    if (companyName.indexOf(query) >= 0) {
                                        filterResultData.add(contacts)
                                        continue
                                    }
                                }
                            }
                        }
                        results.values = filterResultData
                        results.count = filterResultData.size
                    }
                    return results
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                    filterResultList = results.values as? List<ContactsItem>
                    notifyDataSetChanged()
                }
            }
        }
    }
}
