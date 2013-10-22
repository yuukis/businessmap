package com.github.yuukis.businessmap.utils;

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
