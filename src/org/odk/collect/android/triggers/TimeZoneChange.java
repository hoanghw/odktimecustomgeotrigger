package org.odk.collect.android.triggers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TimeZoneChange extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Intent main = new Intent(context, MainService.class);
		context.stopService(main);
		context.startService(main);
		Log.i("t","TimeZoneChangeReceived");
	}

}
