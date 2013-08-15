package com.github.yuukis.businessmap;

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
		if (address == null) {
			return "(未登録)";
		}
		return address;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
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

}
