package com.github.yuukis.businessmap;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.view.Menu;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;

public class MainActivity extends Activity implements
		ActionBar.OnNavigationListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// コンテンツプロバイダーを識別するURI
		Uri uri = ContactsContract.Groups.CONTENT_URI;

		// 取得するフィールド名
		String[] fields = {
				ContactsContract.Groups._ID,
				ContactsContract.Groups.TITLE,
				ContactsContract.Groups.ACCOUNT_NAME };

		// コンテンツリゾルバーの取得
		ContentResolver resolver = getContentResolver();

		// 検索条件
		String selection = ContactsContract.Groups.DELETED + "=0";

		// 検索の実行
		Cursor cursor = resolver.query(uri, fields, selection, null, null);

		// 検索結果
		CursorAdapter adapter = new SimpleCursorAdapter(
				this,
				android.R.layout.simple_list_item_2,
				cursor,
				new String[] {
						ContactsContract.Groups.TITLE,
						ContactsContract.Groups.ACCOUNT_NAME },
				new int[] { android.R.id.text1, android.R.id.text2 },
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}

}
