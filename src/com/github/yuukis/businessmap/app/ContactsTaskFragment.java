/*
 * ContactsTaskFragment.java
 *
 * Copyright 2014 Yuuki Shimizu.
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
package com.github.yuukis.businessmap.app;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.app.ProgressDialogFragment;
import com.github.yuukis.businessmap.data.GeocodingCacheDatabase;
import com.github.yuukis.businessmap.model.ContactsGroup;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.github.yuukis.businessmap.util.CacheUtils;
import com.github.yuukis.businessmap.util.ContactsItemComparator;
import com.github.yuukis.businessmap.util.CursorJoinerWithIntKey;
import com.github.yuukis.businessmap.util.GeocoderUtils;
import com.github.yuukis.businessmap.util.SerializationException;
import com.github.yuukis.businessmap.util.SerializationUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ContactsTaskFragment extends SherlockFragment {

	public interface TaskCallback {
		void onContactsLoaded(List<ContactsItem> contactsList);
	}

	private static final String FILENAME_CACHE_CONTACTS_LIST = "contacts_list.cache";

	private TaskCallback mCallback;
	private ContactsAsyncTask mContactsTask;
	private boolean mRunning = false;
	private List<ContactsItem> mContactsList;
	private Map<String, Double[]> mGeocodingResultCache;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof TaskCallback)) {
			throw new IllegalStateException(
					"Activity must implement the TaskCallback interface.");
		}
		mCallback = (TaskCallback) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_contacts_task, null);
	}

	@Override
	public void onStart() {
		super.onStart();
		setProgressBarVisibie(mRunning);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cancel();
	}

	public void start() {
		if (!mRunning) {
			mContactsTask = new ContactsAsyncTask();
			mContactsTask.execute();
			setRunning(true);
		}
	}

	public void cancel() {
		if (mRunning) {
			mContactsTask.cancel(false);
			mContactsTask = null;
			setRunning(false);
		}
	}

	public boolean isRunning() {
		return mRunning;
	}

	private void setRunning(boolean running) {
		mRunning = running;
		setProgressBarVisibie(mRunning);
	}

	private void setProgressBarVisibie(boolean visible) {
		View view = getView();
		if (view != null) {
			View progressBar = view.findViewById(R.id.contacts_progressbar);
			if (visible) {
				progressBar.setVisibility(View.VISIBLE);
			} else {
				progressBar.setVisibility(View.GONE);
			}
		}
	}

	private void showProgress() {
		String title = getString(R.string.title_geocoding);
		String message = getString(R.string.message_geocoding);
		int max = mGeocodingResultCache.size();

		Bundle args = new Bundle();
		args.putString(ProgressDialogFragment.TITLE, title);
		args.putString(ProgressDialogFragment.MESSAGE, message);
		args.putBoolean(ProgressDialogFragment.CANCELABLE, true);
		args.putInt(ProgressDialogFragment.MAX, max);
		final SherlockDialogFragment dialog = ProgressDialogFragment.newInstance();
		dialog.setArguments(args);
		if (getActivity() != null) {
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					dialog.show(getActivity().getSupportFragmentManager(),
							ProgressDialogFragment.TAG);
				}
			});
		}
	}

	private void updateProgress(Integer... values) {
		ProgressDialogFragment progress = getProgressDialogFragment();
		if (progress == null) {
			return;
		}
		progress.updateProgress(values[0]);
	}

	private void hideProgress() {
		ProgressDialogFragment progress = getProgressDialogFragment();
		if (progress == null) {
			return;
		}
		progress.dismissAllowingStateLoss();
	}

	private ProgressDialogFragment getProgressDialogFragment() {
		Fragment fragment = getSherlockActivity().getSupportFragmentManager()
				.findFragmentByTag(ProgressDialogFragment.TAG);
		return (ProgressDialogFragment) fragment;
	}

	private void showError(final String title, final String message) {
		if (getActivity() != null){
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					new AlertDialog.Builder(getActivity())
							.setTitle(title)
							.setMessage(message)
							.setPositiveButton(android.R.string.ok, null)
							.show();
				}
			});
		}
	}

	private class ContactsAsyncTask extends AsyncTask<Void, Integer, Void>
	implements DialogInterface.OnCancelListener {

		@Override
		public void onCancel(DialogInterface dialog) {
			cancel(true);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			mContactsList = readContactsListFromCache();
			if (mContactsList == null) {
				mContactsList = new ArrayList<ContactsItem>();
			}
			mGeocodingResultCache = new HashMap<String, Double[]>();
			if (mCallback != null) {
				mCallback.onContactsLoaded(mContactsList);
			}
			setRunning(true);
		}

		@Override
		protected Void doInBackground(Void... params) {
			loadAllContacts();
			if (!mGeocodingResultCache.isEmpty()) {
				geocoding();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			updateProgress(values);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			setRunning(false);
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			writeContactsListInCache(mContactsList);
			if (mCallback != null) {
				mCallback.onContactsLoaded(mContactsList);
			}
			setRunning(false);
		}

		private void loadAllContacts() {
			Context context = getActivity();
			mGeocodingResultCache.clear();

			List<ContactsItem> contactsList = new ArrayList<ContactsItem>();
			Cursor groupCursor = null;
			Cursor postalCursor = null;
			Cursor noteCursor = null;
			Cursor companyCursor = null;
			GeocodingCacheDatabase db = null;

			try {
				groupCursor = context.getContentResolver().query(
						Data.CONTENT_URI,
						new String[] {
								GroupMembership.RAW_CONTACT_ID,
								GroupMembership.CONTACT_ID,
								GroupMembership.DISPLAY_NAME,
								GroupMembership.PHONETIC_NAME,
								GroupMembership.GROUP_ROW_ID
						},
						Data.MIMETYPE + "=?",
						new String[] { GroupMembership.CONTENT_ITEM_TYPE },
						Data.RAW_CONTACT_ID);

				postalCursor = context.getContentResolver().query(
						StructuredPostal.CONTENT_URI,
						new String[] {
								StructuredPostal.RAW_CONTACT_ID,
								StructuredPostal.CONTACT_ID,
								StructuredPostal.DISPLAY_NAME,
								StructuredPostal.PHONETIC_NAME,
								StructuredPostal.FORMATTED_ADDRESS
						},
						null,
						null,
						StructuredPostal.RAW_CONTACT_ID);

				noteCursor = context.getContentResolver().query(
						Data.CONTENT_URI,
						new String[] {
								Note.RAW_CONTACT_ID,
								Note.NOTE
						},
						Data.MIMETYPE + "=?",
						new String[] { Note.CONTENT_ITEM_TYPE },
						Data.RAW_CONTACT_ID);

				companyCursor = context.getContentResolver().query(
						Data.CONTENT_URI,
						new String[] {
								Organization.RAW_CONTACT_ID,
								Organization.COMPANY
						},
						Data.MIMETYPE + "=?",
						new String[] { Organization.CONTENT_ITEM_TYPE },
						Data.RAW_CONTACT_ID);

				HashMap<Long, String> noteMap = new HashMap<Long, String>();
				while (noteCursor.moveToNext()) {
					long rowId = noteCursor.getLong(0);
					String note = noteCursor.getString(1);
					noteMap.put(rowId, note);
				}

				HashMap<Long, String> companyMap = new HashMap<Long, String>();
				while (companyCursor.moveToNext()) {
					long rowId = companyCursor.getLong(0);
					String company = companyCursor.getString(1);
					companyMap.put(rowId, company);
				}

				CursorJoinerWithIntKey joiner = new CursorJoinerWithIntKey(
						groupCursor, new String[] { Data.RAW_CONTACT_ID },
						postalCursor, new String[] { Data.RAW_CONTACT_ID });

				db = new GeocodingCacheDatabase(context);
				long _rowId = -1, _cid = -1;
				String _name = null, _phonetic = null, _note = null, _companyName = null;
				List<Long> _groupIds = new ArrayList<Long>();
				List<String> _address = new ArrayList<String>();

				for (CursorJoinerWithIntKey.Result result : joiner) {
					long rowId, cid, groupId;
					String name, phonetic, address, note, companyName;

					switch (result) {
					case LEFT:
						rowId = groupCursor.getLong(0);
						cid = groupCursor.getLong(1);
						name = groupCursor.getString(2);
						phonetic = groupCursor.getString(3);
						groupId = groupCursor.getLong(4);
						address = null;
						note = noteMap.get(rowId);
						companyName = companyMap.get(rowId);
						break;

					case RIGHT:
						rowId = postalCursor.getLong(0);
						cid = postalCursor.getLong(1);
						name = postalCursor.getString(2);
						phonetic = postalCursor.getString(3);
						groupId = ContactsGroup.ID_GROUP_ALL_CONTACTS;
						address = postalCursor.getString(4);
						note = noteMap.get(rowId);
						companyName = companyMap.get(rowId);
						break;

					case BOTH:
						rowId = groupCursor.getLong(0);
						cid = groupCursor.getLong(1);
						name = groupCursor.getString(2);
						phonetic = groupCursor.getString(3);
						groupId = groupCursor.getLong(4);
						address = postalCursor.getString(4);
						note = noteMap.get(rowId);
						companyName = companyMap.get(rowId);
						break;

					default:
						continue;
					}

					if (_rowId != rowId) {
						for (long gid : _groupIds) {
							if (_address.isEmpty()) {
								contactsList.add(new ContactsItem(_cid, _name,
										_phonetic, gid, null, _note, _companyName));
								continue;
							}
							for (String addr : _address) {
								ContactsItem contact = new ContactsItem(_cid,
										_name, _phonetic, gid, addr, _note, _companyName);
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
						_companyName = companyName;
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
								gid, null, _note, _companyName));
						continue;
					}
					for (String addr : _address) {
						ContactsItem contact = new ContactsItem(_cid, _name,
								_phonetic, gid, addr, _note, _companyName);
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
			Context context = getActivity();
			showProgress();

			final GeocodingCacheDatabase db = new GeocodingCacheDatabase(context);
			int count = 0;
			try {
				for (Iterator<Entry<String, Double[]>> it = mGeocodingResultCache
						.entrySet().iterator(); it.hasNext();) {
					Entry<String, Double[]> entry = it.next();
					String address = entry.getKey();

					Double[] latlng = GeocoderUtils.getFromLocationName(
							context, address);
					db.put(address, latlng);
					entry.setValue(latlng);

					count++;
					final int progress = count;
					publishProgress(progress);

					if (isCancelled()) {
						return;
					}
				}
			} catch (IOException e) {
				hideProgress();
				String title = getString(R.string.title_geocoding_ioerror);
				String message = getString(R.string.message_geocoding_ioerror);
				showError(title, message);
				return;
			} catch (JSONException e) {
				hideProgress();
				String title = getString(R.string.title_geocoding_jsonerror);
				String message = getString(R.string.message_geocoding_jsonerror);
				showError(title, message);
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
			hideProgress();
		}

		@SuppressWarnings("unchecked")
		private List<ContactsItem> readContactsListFromCache() {
			Context context = getActivity();
			Object object = null;
			try {
				byte[] bytes = CacheUtils.read(context, FILENAME_CACHE_CONTACTS_LIST);
				object = SerializationUtils.deserialize(bytes);
			} catch (SerializationException e) {
				// デシリアライズに失敗
				object = null;
			} catch (FileNotFoundException e) {
				// Nothing to do.
			} catch (IOException e) {
				e.printStackTrace();
			}
			return (List<ContactsItem>) object;
		}

		private void writeContactsListInCache(List<ContactsItem> contactsList) {
			Context context = getActivity();
			if (contactsList == null) {
				return;
			}

			byte[] bytes = SerializationUtils.serialize((Serializable) contactsList);
			try {
				CacheUtils.write(context, bytes, FILENAME_CACHE_CONTACTS_LIST);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
