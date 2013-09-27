package com.github.yuukis.businessmap.app;

import com.github.yuukis.businessmap.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ReplacedAppReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent data) {
		if (Intent.ACTION_PACKAGE_REPLACED.equals(data.getAction())) {
			if (data.getData().getSchemeSpecificPart()
					.equals(context.getPackageName())) {
				Toast.makeText(context, R.string.beta__message_updated,
						Toast.LENGTH_SHORT).show();

				Intent intent = new Intent(context, MainActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			}
		}
	}

}
