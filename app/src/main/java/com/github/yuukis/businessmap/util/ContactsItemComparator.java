/*
 * ContactsItemComparator.java
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
package com.github.yuukis.businessmap.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import com.github.yuukis.businessmap.model.ContactsItem;

public class ContactsItemComparator implements Comparator<ContactsItem> {

	@Override
	public int compare(ContactsItem lhs, ContactsItem rhs) {
		String l = lhs.getPhontic();
		String r = rhs.getPhontic();
		if (l == null || l.isEmpty()) {
			l = lhs.getName();
		}
		if (r == null || r.isEmpty()) {
			r = rhs.getName();
		}

		if (l == null && r != null) {
			return 1;
		} else if (l != null && r == null) {
			return -1;
		}
		if (l != null && r != null) {
			int compare = Collator.getInstance(Locale.getDefault()).compare(l,
					r);
			if (compare != 0) {
				return compare;
			}
		}

		if (lhs.getCID() < rhs.getCID()) {
			return -1;
		} else if (lhs.getCID() > rhs.getCID()) {
			return 1;
		}
		return 0;
	}

}
