package com.github.yuukis.businessmap;

import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ArrayAdapter;

public class ContactsListFragment extends ListFragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Context context = getActivity();

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_list_item_1);

		// コンテンツプロバイダーを識別するURI
		Uri uri = ContactsContract.Contacts.CONTENT_URI;

		// 取得するフィールド名
		String[] fields = { ContactsContract.Contacts.DISPLAY_NAME };

		// 並び替える基準のフィールド名
		String sortOrder = ContactsContract.Contacts.DISPLAY_NAME;

		// コンテンツリゾルバーの取得
		ContentResolver resolver = context.getContentResolver();

		// 検索の実行
		Cursor cursor = resolver.query(uri, fields, null, null, sortOrder);

		// 検索結果
		while (cursor.moveToNext()) {
			String displayName = cursor.getString(0);
			adapter.add(displayName);
		}

		setListAdapter(adapter);
	}

}
