/*
 * ContactsGroup.java
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

public class ContactsGroup implements Serializable {

	public static final long ID_GROUP_ALL_CONTACTS = -1;
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
