package org.odk.collect.android.triggers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.android.gms.location.LocationClient;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

public class LocationUpdatesIntentService extends IntentService{

	public LocationUpdatesIntentService() {
		super("LU IntentService");
		// TODO Auto-generated constructor stub
	}
	private static final String TAG = "LU IntentService";

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		
		
		Location location = (Location) intent.getExtras().get(LocationClient.KEY_LOCATION_CHANGED);
		Log.d(TAG, "onHandleIntent: "+ location);

		String log = location.getLatitude()
				+" "+location.getLongitude()
				+" "+location.getAccuracy();
		
		Log.d(TAG, "Location: "+log);

        checkInActiveGeofences(location);
	}

    public void checkInActiveGeofences(Location location){
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedGeofences = mSharedPreferences.getString("setGeofences", null);
        if ( savedGeofences != null){
            SimpleGeofenceStore mPrefs = new SimpleGeofenceStore(this);

            Log.d(TAG,"checkInActiveGeofences savedGeofences: "+savedGeofences);
            String[] geofences = savedGeofences.split(" ");
            for (int i = 0; i<geofences.length; i++){
                SimpleGeofence geofence = mPrefs.getGeofence(geofences[i]);
                if (geofence != null) {
                    if (isInGeofence(location, geofence)){
                        Log.d(TAG,"isInGeofence true - geofence: "+geofence.getId());
                        if (!isAlreadyNotified(geofence)){
                            Log.d(TAG,"Not Notified Yet - geofence: "+geofence.getId());
                            sendNotification(geofence.getId());
                            addToNotifiedList(geofence);
                        }
                    }else{
                        Log.d(TAG,"isInGeofence false - geofence: "+geofence.getId());
                        removeFromNotifiedList(geofence);
                    }
                }
            }
        }
    }
    private final static String NOTIFIED_LIST = "NOTIFIEDLIST";
    public void addToNotifiedList(SimpleGeofence geofence){
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        String list = mSharedPreferences.getString(NOTIFIED_LIST,null);
        if (list == null){
            list = geofence.getId();
        }else{
            list +=" "+geofence.getId();

        }
        editor.putString(NOTIFIED_LIST, list);
        editor.commit();
        Log.d(TAG,"addToNotifiedList list: "+list)  ;
    }
    public void removeFromNotifiedList(SimpleGeofence geofence){
        ArrayList<String> list = listifyGeofences();
        if (list == null){
            return;
        }

        //no need to check this, remove() already takes care of this
        if (!list.contains(geofence.getId())){
            return;
        }

        list.remove(geofence.getId());

        String newList = "";
        for(int i=0; i<list.size();i++){
            newList += list.get(i)+" ";
        }

        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        //trim() return a copy with whitespace omitted
        editor.putString(NOTIFIED_LIST,newList.trim());
        editor.commit();
        Log.d(TAG,"removeFromNotifiedList newList: "+newList);
    }
    public ArrayList<String> listifyGeofences(){
        ArrayList<String> result = new ArrayList<String>();
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String list = mSharedPreferences.getString(NOTIFIED_LIST,null);
        if (list == null) return null;
        String[] geofences =  list.split(" ");
        for (int i = 0; i<geofences.length; i++){
            String id = geofences[i];
            if ((id != null)||(id.length()!=0)){
                result.add(id);
            }
        }
        return result;
    }
    public boolean isAlreadyNotified(SimpleGeofence geofence){
        ArrayList<String> list = listifyGeofences();
        if (list == null){
            return false;
        }else{
            return list.contains(geofence.getId());
        }
    }
    final static double METER_TO_DEGREE = 111000; //1 degree = 111000 m
    public boolean isInGeofence(Location location, SimpleGeofence geofence){
        double toDegree = geofence.getRadius()/METER_TO_DEGREE;
        double top= geofence.getLatitude() + toDegree;
        double bot= geofence.getLatitude() - toDegree;
        double right= geofence.getLongitude() + toDegree;
        double left= geofence.getLongitude() - toDegree;

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        return (latitude<top)&&(latitude>bot)&&(longitude>left)&&(longitude<right);
    }
    private void sendNotification(String ids) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 7);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);

        Calendar now = Calendar.getInstance();
        if (start.before(now) && end.after(now)){
            Log.d(TAG,"sendNotification not a right time");
        }
        else {
            Log.d(TAG,"sendNotification call alarm");
            Intent intent = new Intent("executetimetrigger");
            intent.putExtra("form", ids.split(";;")[0]);
            sendBroadcast(intent);
        }
    }
}
