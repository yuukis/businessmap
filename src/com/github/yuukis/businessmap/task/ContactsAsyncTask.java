/*
 * ContactsAsyncTask.java
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
package com.github.yuukis.businessmap.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.data.GeocodingCacheDatabase;
import com.github.yuukis.businessmap.model.ContactsGroup;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.github.yuukis.businessmap.util.ContactsItemComparator;
import com.github.yuukis.businessmap.util.CursorJoinerWithIntKey;
import com.github.yuukis.businessmap.util.GeocoderUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.view.View;

public class ContactsAsyncTask extends AsyncTask<Void, Integer, Void> implements
		DialogInterface.OnCancelListener {

	public interface Callback {
		void onContactsLoaded(List<ContactsItem> contactsList);
	}

	private static final int STATE_START = 1;
	private static final int STATE_PROGRESS = 2;
	private static final int STATE_FINISH = 3;
	private static final int STATE_FAILED_IO_EXCEPTION = 101;
	private static final int STATE_FAILED_JSON_EXCEPTION = 102;

	private Context mContext;
	private Callback mCallback;
	private List<ContactsItem> mContactsList;
	private Map<String, Double[]> mGeocodingResultCache;
	private ProgressDialog mProgressDialog;
	private View mProgressBar;

	public ContactsAsyncTask(Activity activity, Callback callback) {
		mContext = activity;
		mCallback = callback;
		mContactsList = new ArrayList<ContactsItem>();
		mGeocodingResultCache = new HashMap<String, Double[]>();
		mProgressDialog = new ProgressDialog(activity);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setTitle(R.string.title_geocoding);
		mProgressDialog.setMessage(activity.getString(R.string.message_geocoding));
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setOnCancelListener(this);
		mProgressBar = activity.findViewById(R.id.contacts_progressbar);
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		mProgressBar.setVisibility(View.GONE);
		cancel(true);
	}

	@Override
	protected Void doInBackground(Void... params) {
		mProgressBar.setVisibility(View.VISIBLE);
		loadAllContacts();
		if (!mGeocodingResultCache.isEmpty()) {
			geocoding();
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);

		if (values.length == 0) {
			return;
		}
		int state = values[0];

		switch (state) {
		// 1. 開始時
		case STATE_START:
			mProgressDialog.show();
			break;

		// 2. 処理時
		case STATE_PROGRESS:
			int progress = values[1];
			mProgressDialog.setProgress(progress);
			break;

		// 3. 終了時
		case STATE_FINISH:
			mProgressDialog.setProgress(mProgressDialog.getMax());
			mProgressDialog.dismiss();
			break;

		// 4. 失敗時
		case STATE_FAILED_IO_EXCEPTION:
			mProgressDialog.dismiss();
			showErrorDialog(R.string.title_geocoding_ioerror,
					R.string.message_geocoding_ioerror);
			break;
		case STATE_FAILED_JSON_EXCEPTION:
			mProgressDialog.dismiss();
			showErrorDialog(R.string.title_geocoding_jsonerror,
					R.string.message_geocoding_jsonerror);
			break;
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		mProgressBar.setVisibility(View.GONE);

		if (mCallback != null) {
			mCallback.onContactsLoaded(mContactsList);
		}
	}

	private void loadAllContacts() {
		mGeocodingResultCache.clear();

		List<ContactsItem> contactsList = new ArrayList<ContactsItem>();
		Cursor groupCursor = null;
		Cursor postalCursor = null;
		Cursor noteCursor = null;
		GeocodingCacheDatabase db = null;

		try {
			groupCursor = mContext.getContentResolver().query(
					Data.CONTENT_URI,
					new String[] {
							GroupMembership.RAW_CONTACT_ID,
							GroupMembership.CONTACT_ID,
							GroupMembership.DISPLAY_NAME,
							GroupMembership.PHONETIC_NAME,
							GroupMembership.GROUP_ROW_ID },
					Data.MIMETYPE + "=?",
					new String[] {
							GroupMembership.CONTENT_ITEM_TYPE },
					Data.RAW_CONTACT_ID);

			postalCursor = mContext.getContentResolver().query(
					StructuredPostal.CONTENT_URI,
					new String[] {
							StructuredPostal.RAW_CONTACT_ID,
							StructuredPostal.CONTACT_ID,
							StructuredPostal.DISPLAY_NAME,
							StructuredPostal.PHONETIC_NAME,
							StructuredPostal.FORMATTED_ADDRESS },
					null,
					null,
					StructuredPostal.RAW_CONTACT_ID);

			noteCursor = mContext.getContentResolver().query(
					Data.CONTENT_URI,
					new String[] {
							Note.RAW_CONTACT_ID,
							Note.NOTE },
					Data.MIMETYPE + "=?",
					new String[] {
							Note.CONTENT_ITEM_TYPE },
					Data.RAW_CONTACT_ID);
			HashMap<Long, String> noteMap = new HashMap<Long, String>();
			while (noteCursor.moveToNext()) {
				long rowId = noteCursor.getLong(0);
				String note = noteCursor.getString(1);
				noteMap.put(rowId, note);
			}

			CursorJoinerWithIntKey joiner = new CursorJoinerWithIntKey(
					groupCursor, new String[] { Data.RAW_CONTACT_ID },
					postalCursor, new String[] { Data.RAW_CONTACT_ID });

			db = new GeocodingCacheDatabase(mContext);
			long _rowId = -1, _cid = -1;
			String _name = null, _phonetic = null, _note = null;
			List<Long> _groupIds = new ArrayList<Long>();
			List<String> _address = new ArrayList<String>();

			for (CursorJoinerWithIntKey.Result result : joiner) {
				long rowId, cid, groupId;
				String name, phonetic, address, note;

				switch (result) {
				case LEFT:
					rowId = groupCursor.getLong(0);
					cid = groupCursor.getLong(1);
					name = groupCursor.getString(2);
					phonetic = groupCursor.getString(3);
					groupId = groupCursor.getLong(4);
					address = null;
					note = noteMap.get(rowId);
					break;

				case RIGHT:
					rowId = postalCursor.getLong(0);
					cid = postalCursor.getLong(1);
					name = postalCursor.getString(2);
					phonetic = postalCursor.getString(3);
					groupId = ContactsGroup.ID_GROUP_ALL_CONTACTS;
					address = postalCursor.getString(4);
					note = noteMap.get(rowId);
					break;

				case BOTH:
					rowId = groupCursor.getLong(0);
					cid = groupCursor.getLong(1);
					name = groupCursor.getString(2);
					phonetic = groupCursor.getString(3);
					groupId = groupCursor.getLong(4);
					address = postalCursor.getString(4);
					note = noteMap.get(rowId);
					break;

				default:
					continue;
				}

				if (_rowId != rowId) {
					for (long gid : _groupIds) {
						if (_address.isEmpty()) {
							contactsList.add(new ContactsItem(_cid, _name,
									_phonetic, gid, null, _note));
							continue;
						}
						for (String addr : _address) {
							ContactsItem contact = new ContactsItem(_cid,
									_name, _phonetic, gid, addr, _note);
							double[] latlng = db.get(addr);
							if (latlng != null && latlng.length == 2) {
								contact.setLat(latlng[0]);
								contact.setLng(latlng[1]);
							} else {
								if (!mGeocodingResultCache.containsKey(addr)) {
									mGeocodingResultCache.put(addr, null);
								}
							}
							contactsList.add(contact);
						}
					}
					_rowId = rowId;
					_cid = cid;
					_name = name;
					_phonetic = phonetic;
					_groupIds.clear();
					_groupIds.add(ContactsGroup.ID_GROUP_ALL_CONTACTS);
					_address.clear();
					_note = note;
				}

				if (_groupIds.indexOf(groupId) < 0) {
					_groupIds.add(groupId);
				}
				if (address != null && _address.indexOf(address) < 0) {
					_address.add(address);
				}
			}
			// FIXME: 冗長
			for (long gid : _groupIds) {
				if (_address.isEmpty()) {
					contactsList.add(new ContactsItem(_cid, _name, _phonetic,
							gid, null, _note));
					continue;
				}
				for (String addr : _address) {
					ContactsItem contact = new ContactsItem(_cid, _name,
							_phonetic, gid, addr, _note);
					double[] latlng = db.get(addr);
					if (latlng != null && latlng.length == 2) {
						contact.setLat(latlng[0]);
						contact.setLng(latlng[1]);
					} else {
						if (!mGeocodingResultCache.containsKey(addr)) {
							mGeocodingResultCache.put(addr, null);
						}
					}
					contactsList.add(contact);
				}
			}
		} finally {
			if (groupCursor != null) {
				groupCursor.close();
			}
			if (postalCursor != null) {
				postalCursor.close();
			}
			if (noteCursor != null) {
				noteCursor.close();
			}
			if (db != null) {
				db.close();
			}
		}
		Collections.sort(contactsList, new ContactsItemComparator());
		mContactsList = contactsList;
	}

	private void geocoding() {
		mProgressDialog.setMax(mGeocodingResultCache.size());
		publishProgress(STATE_START);

		final GeocodingCacheDatabase db = new GeocodingCacheDatabase(mContext);
		int count = 0;
		try {
			for (Iterator<Entry<String, Double[]>> it = mGeocodingResultCache
					.entrySet().iterator(); it.hasNext();) {
				Entry<String, Double[]> entry = it.next();
				String address = entry.getKey();

				Double[] latlng = GeocoderUtils.getFromLocationName(
						mContext, address);
				db.put(address, latlng);
				entry.setValue(latlng);

				count++;
				final int progress = count;
				publishProgress(STATE_PROGRESS, progress);

				if (isCancelled()) {
					return;
				}
			}
		} catch (IOException e) {
			publishProgress(STATE_FAILED_IO_EXCEPTION);
			return;
		} catch (JSONException e) {
			publishProgress(STATE_FAILED_JSON_EXCEPTION);
			return;
		} finally {
			db.close();
		}

		for (int j = 0; j < mContactsList.size(); j++) {
			ContactsItem contact = mContactsList.get(j);
			String address = contact.getAddress();
			if (address == null) {
				continue;
			}
			if (contact.getLat() != null && contact.getLng() != null) {
				continue;
			}
			if (!mGeocodingResultCache.containsKey(address)) {
				continue;
			}

			Double[] latlng = mGeocodingResultCache.get(address);
			if (latlng != null && latlng.length == 2) {
				contact.setLat(latlng[0]);
				contact.setLng(latlng[1]);
				mContactsList.set(j, contact);
			}
		}
		publishProgress(STATE_FINISH);
	}

	private void showErrorDialog(int title, int message) {
		new AlertDialog.Builder(mContext)
			.setTitle(title)
			.setMessage(message)
			.setPositiveButton(android.R.string.ok, null)
			.show();
	}
}
