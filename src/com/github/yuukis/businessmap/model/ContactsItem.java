package com.github.yuukis.businessmap.model;

import java.util.Locale;

public class ContactsItem {

	private String name;
	private String address;
	private Double lat;
	private Double lng;

	public ContactsItem(String name, String address) {
		this.name = name;
		this.address = address;
		this.lat = null;
		this.lng = null;
	}

	public String getName() {
		return name;
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
		return String.format(Locale.getDefault(), "%s (%f,%f)",
				getDisplayAddress(), lat, lng);
	}

}
