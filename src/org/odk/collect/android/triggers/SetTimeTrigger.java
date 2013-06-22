package org.odk.collect.android.triggers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


//Set alarm notification for today only
public class SetTimeTrigger extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent arg1) {
		// TODO Auto-generated method stub
		Log.i("t","SetTimeReceived");
		context.startService(new Intent(context,SetTimeTriggerService.class));
	}
}
