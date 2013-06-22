package org.odk.collect.android.triggers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class Utils {
	public static final String TAG = "Utils";
	public static final int NO_CONNECTION = 0;
	public static final int WAIT_FOR_WIFI = 1;
	public static final int HAS_CONNECTION = 2;
    public static final String EC2_URL = "http://ec2-54-226-45-247.compute-1.amazonaws.com/";
	
	static public void retryLater(Context context, Class<?> cls, int sec){
		Log.i(TAG,"Retry "+ cls.toString());
		
		Calendar todayEnd = Calendar.getInstance();
		todayEnd.set(Calendar.HOUR_OF_DAY, 22);
		todayEnd.set(Calendar.MINUTE, 59);
		todayEnd.set(Calendar.SECOND, 59);
		
		Calendar now = Calendar.getInstance();
		
		if (now.before(todayEnd)){
			AlarmManager nextAlarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent downloadRequest = new Intent(context, cls);
			PendingIntent nextCheckRequest = PendingIntent.getBroadcast(context, 6, downloadRequest, PendingIntent.FLAG_UPDATE_CURRENT);
			nextAlarm.set(AlarmManager.RTC_WAKEUP,
					now.getTimeInMillis()+ sec*1000,
					nextCheckRequest);
		}
	}
	
	static public int networkState(Context context){
		ConnectivityManager connectivityManager =
	            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
		
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	    
		if (ni != null && ni.isConnected()){
			return HAS_CONNECTION;
		}
		
		if ((ni!= null && ni.isConnectedOrConnecting()) || (wm != null && wm.isWifiEnabled())){
			//wm.reconnect();
			return WAIT_FOR_WIFI;
		}
		
		return NO_CONNECTION;
		
	}
	
	//return null if Internet error
	static public Map <String,List<Calendar>> getTimeTrigger(String user){
		Map<String,List<Calendar>> triggers = new HashMap<String,List<Calendar>>();
		Log.i(TAG,"getTimeTrigger for user: "+user);
		HttpURLConnection urlConnection = null;
		try{
			URL url = new URL(EC2_URL+"gettrigger/?u="+user);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(4000);
			urlConnection.setReadTimeout(4000);
			String line;
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			while((line = reader.readLine()) != null) {
				builder.append(line);
			}
			
			JSONObject json = new JSONObject(builder.toString());
			
	        Iterator<?> keys = json.keys();
	        String value = null;
	        while( keys.hasNext() ){
	            String key = (String) keys.next();
	            value =(String) json.getString(key);
	            if ((value != null) && (value.length()!=0))
	            	triggers.put(key, parseCalendars(value));
	        }
		} catch (Exception e) {
			triggers = null;
		}
		finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}
		return triggers;
	}
	
	static public List<Calendar> parseCalendars(String s){
		String[] time=s.split(" ");
		List<Calendar> calendars = new ArrayList<Calendar>();
		for (int i=0;i<time.length;i++){
			Calendar calendar= Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[i].substring(0,2)));
			calendar.set(Calendar.MINUTE, Integer.parseInt(time[i].substring(2)));
			calendar.set(Calendar.SECOND, 0);
			calendars.add(calendar);
		}	
		return calendars;
	}
}
