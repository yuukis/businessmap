package com.github.yuukis.businessmap.model;

import java.io.Serializable;
import java.util.Locale;

public class ContactsItem implements Serializable {

	private static final long serialVersionUID = 3136270897831481870L;

	private long cid;
	private String name;
	private String phonetic;
	private String address;
	private Double lat;
	private Double lng;

	public ContactsItem(long cid, String name, String phonetic, String address) {
		this.cid = cid;
		this.name = name;
		this.phonetic = phonetic;
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

	public String getAddress() {
		return address;
	}

	public String getDisplayAddress() {
		if (address == null) {
			return "(未登録)";
		}
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

	public void setAddress(String address) {
		this.address = address;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	@Override
	public String toString() {
		if (lat == null || lng == null) {
			return getDisplayAddress();
		}
		if (phonetic == null) {
			return String.format(Locale.getDefault(), "%s (%f,%f)",
					getDisplayAddress(), lat, lng);
		}
		return String.format(Locale.getDefault(), "%s (%f,%f) | %s",
				getDisplayAddress(), lat, lng, phonetic);
	}

}
