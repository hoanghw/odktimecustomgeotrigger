package org.odk.collect.android.triggers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class UpdateTriggers extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent arg1) {
		// TODO Auto-generated method stub
		Log.d("UPDATETRIGGERS", "onReceive");
		
		context.startService(new Intent(context, UpdateTriggersService.class));
		
		
	}
}
