/*
 * GroupAdapter.kt
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
package com.github.yuukis.businessmap.widget

import android.app.Activity
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsGroup

class GroupAdapter(
    private val activity: Activity,
    private val groupList: List<ContactsGroup>
) : BaseAdapter(), Filterable {

    override fun getCount(): Int = groupList.size

    override fun getItem(position: Int): ContactsGroup = groupList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = activity.layoutInflater.inflate(LAYOUT_SPINNER_DROPDOWN_ITEM_RESOURCE_ID, null)
            holder = ViewHolder(
                view.findViewById(android.R.id.text1),
                view.findViewById(android.R.id.text2)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val group = getItem(position)
        holder.textView1.text = group.title

        val accountName = group.accountName
        if (TextUtils.isEmpty(accountName)) {
            holder.textView2?.visibility = View.GONE
            holder.textView2?.text = ""
        } else {
            holder.textView2?.visibility = View.VISIBLE
            holder.textView2?.text = accountName
        }

        return view
    }

    /**
     * The dropdown menu's text field is not user-editable (inputType="none"),
     * so filtering by typed text never applies. This filter always returns
     * the full group list, which is the standard way to back an
     * ExposedDropdownMenu's AutoCompleteTextView with a non-filtering list.
     */
    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return FilterResults().apply {
                values = groupList
                count = groupList.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence {
            return (resultValue as? ContactsGroup)?.title ?: ""
        }
    }

    private class ViewHolder(val textView1: TextView, val textView2: TextView?)

    companion object {
        private val LAYOUT_SPINNER_DROPDOWN_ITEM_RESOURCE_ID = R.layout.simple_spinner_dropdown_item_2line
    }
}
