package com.github.yuukis.businessmap.model;

import java.io.Serializable;

public class ContactsItem implements Serializable {

	private static final long serialVersionUID = -9036992472158055718L;

	private long cid;
	private String name;
	private String phonetic;
	private long groupId;
	private String address;
	private Double lat;
	private Double lng;

	public ContactsItem(long cid, String name, String phonetic, long groupId,
			String address) {
		this.cid = cid;
		this.name = name;
		this.phonetic = phonetic;
		this.groupId = groupId;
		this.address = address;
		this.lat = null;
		this.lng = null;
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

}
