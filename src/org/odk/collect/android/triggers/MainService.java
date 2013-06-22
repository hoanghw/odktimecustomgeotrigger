package org.odk.collect.android.triggers;

import java.util.Calendar;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MainService extends Service {

	@Override
	public void onCreate(){
		super.onCreate();
		//These receiver are called below when getBroadcast
		//sendBroadcast(new Intent("settimetrigger"));
        //sendBroadcast(new Intent("downloadrequest"));
        
		Log.i("t", "MainServiceCalled");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 4);
		calendar.set(Calendar.MINUTE, 6);
		calendar.set(Calendar.SECOND, 0);
		
		AlarmManager cron = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
		//Schedule settimetrigger task
		Intent timeTrigger = new Intent(this, SetTimeTrigger.class);
		PendingIntent pTimeTrigger = PendingIntent.getBroadcast(this, 0, timeTrigger, PendingIntent.FLAG_UPDATE_CURRENT);
		cron.setRepeating(AlarmManager.RTC_WAKEUP, 
				calendar.getTimeInMillis(),
				AlarmManager.INTERVAL_DAY, 
				pTimeTrigger);
		
		//Schedule download task
		Intent downloadRequest = new Intent(this, DownloadRequest.class);
		PendingIntent pDownloadRequest = PendingIntent.getBroadcast(this, 0, downloadRequest, PendingIntent.FLAG_UPDATE_CURRENT);
		cron.setRepeating(AlarmManager.RTC_WAKEUP,
				calendar.getTimeInMillis(),
				AlarmManager.INTERVAL_DAY,
				pDownloadRequest);

        //Schedule loctrigger task
        Intent locTrigger = new Intent(this, UpdateTriggers.class);
        PendingIntent pLocTrigger = PendingIntent.getBroadcast(this, 0, locTrigger, 0);
        cron.setRepeating(AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pLocTrigger);

        startService(new Intent(this,LocationService.class));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    // We don't want this service to continue running if it is explicitly
	    // stopped, so return not sticky.
	    return START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
