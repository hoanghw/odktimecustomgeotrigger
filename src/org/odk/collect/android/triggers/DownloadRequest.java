package org.odk.collect.android.triggers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DownloadRequest extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("t","DownloadRequestReceived");
		context.startService(new Intent("downloadservice"));
	}
}
