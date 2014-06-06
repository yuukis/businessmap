/*
 * ContactsItem.java
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
package com.github.yuukis.businessmap.model;

import java.io.Serializable;

public class ContactsItem implements Serializable {

	private static final long serialVersionUID = -5022460356265154213L;

	private long cid;
	private String name;
	private String phonetic;
	private long groupId;
	private String address;
	private Double lat;
	private Double lng;
	private String note;
	private String companyName;

	public ContactsItem(long cid, String name, String phonetic, long groupId,
			String address, String note, String companyName) {
		this.cid = cid;
		this.name = name;
		this.phonetic = phonetic;
		this.groupId = groupId;
		this.address = address;
		this.lat = null;
		this.lng = null;
		this.note = note;
		this.companyName = companyName;
	}

	public long getCID() {
		return cid;
	}

	public String getName() {
		return name;
	}

	public String getPhontic() {
		return phonetic;
	}

	public long getGroupId() {
		return groupId;
	}

	public String getAddress() {
		return address;
	}

	public Double getLat() {
		return lat;
	}

	public Double getLng() {
		return lng;
	}

	public String getNote() {
		return note;
	}
	public String getCompanyName() {
		return companyName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPhonetic(String phonetic) {
		this.phonetic = phonetic;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setLat(double lat) {
		if (Double.isNaN(lat)) {
			this.lat = null;
		} else {
			this.lat = lat;
		}
	}

	public void setLng(double lng) {
		if (Double.isNaN(lng)) {
			this.lng = null;
		} else {
			this.lng = lng;
		}
	}

	public void setNote(String note) {
		this.note = note;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o instanceof ContactsItem == false) {
			return false;
		}
		ContactsItem other = (ContactsItem) o;
		return cid == other.cid
				&& name == other.name
				&& phonetic == other.phonetic
				&& groupId == other.groupId
				&& address == other.address
				&& lat == other.lat
				&& lng == other.lng
				&& note == other.note
				&& companyName == other.companyName;
	}

}
