package com.github.yuukis.businessmap.model;

import java.io.Serializable;

public class ContactsGroup implements Serializable {

	private static final long serialVersionUID = -3609429759764928253L;

	private long _id;
	private String title;
	private String accountName;

	public ContactsGroup(long _id, String title, String accountName) {
		this._id = _id;
		this.title = title;
		this.accountName = accountName;
	}

	public long getId() {
		return _id;
	}

	public String getTitle() {
		return title;
	}

	public String getAccountName() {
		return accountName;
	}

	@Override
	public String toString() {
		return getTitle();
	}

}
