package com.github.yuukis.businessmap;

import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

public class ContactsListFragment extends ListFragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Context context = getActivity();

		// コンテンツプロバイダーを識別するURI
		Uri uri = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI;

		// 取得するフィールド名
		String[] fields = {
				ContactsContract.CommonDataKinds.StructuredPostal._ID,
				ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS };

		// 並び替える基準のフィールド名
		String sortOrder = ContactsContract.Contacts.PHONETIC_NAME;

		// コンテンツリゾルバーの取得
		ContentResolver resolver = context.getContentResolver();

		// 検索の実行
		Cursor cursor = resolver.query(uri, fields, null, null, sortOrder);

		// 検索結果
		ListAdapter adapter = new SimpleCursorAdapter(
				context,
				android.R.layout.simple_list_item_2,
				cursor,
				new String[] {
						ContactsContract.CommonDataKinds.StructuredPostal.DISPLAY_NAME,
						ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS },
				new int[] { android.R.id.text1, android.R.id.text2 },
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		setListAdapter(adapter);
	}

}
