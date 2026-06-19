/*
 * ContactsItemComparator.kt
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
package com.github.yuukis.businessmap.util

import com.github.yuukis.businessmap.model.ContactsItem
import java.text.Collator
import java.util.Locale

class ContactsItemComparator : Comparator<ContactsItem> {

    override fun compare(lhs: ContactsItem, rhs: ContactsItem): Int {
        var l = lhs.phonetic
        var r = rhs.phonetic
        if (l == null || l.isEmpty()) {
            l = lhs.name
        }
        if (r == null || r.isEmpty()) {
            r = rhs.name
        }

        if (l == null && r != null) {
            return 1
        } else if (l != null && r == null) {
            return -1
        }
        if (l != null && r != null) {
            val compare = Collator.getInstance(Locale.getDefault()).compare(l, r)
            if (compare != 0) {
                return compare
            }
        }

        return when {
            lhs.cid < rhs.cid -> -1
            lhs.cid > rhs.cid -> 1
            else -> 0
        }
    }
}
