package org.odk.collect.android.triggers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import org.odk.collect.android.R;

public class UpdateTriggersService extends Service {
	
	private static final String TAG = "UPDATETRIGGERSSERVICE";
    
	// Persistent storage for geofences
    private SimpleGeofenceStore mPrefs;
    
	// Store a list of geofences to add
    List<Geofence> mCurrentGeofences;
    
    //
    private String geofenceList;

    SharedPreferences mSharedPreferences;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    @Override
    public IBinder onBind(Intent intent) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
	public void onCreate() {
        super.onCreate();
		// TODO Auto-generated method stub
		// Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<Geofence>();

        // Instantiate a new geofence storage area
        mPrefs = new SimpleGeofenceStore(this);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //
        geofenceList = "";

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SetLocService");
        wakeLock.acquire();

        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "SetLocService");
        wifiLock.acquire();
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.i(TAG, "LocTriggersService onStart");
        switch (Utils.networkState(this)){
            case Utils.NO_CONNECTION:
                Log.i(TAG, "LocTriggersService NO_CONNECTION");
                Utils.retryLater(this, UpdateTriggers.class, 3600);
                stopSelf();
                break;
            case Utils.WAIT_FOR_WIFI:
                Log.i(TAG, "LocTriggersService WAIT_FOR_WIFI");
                Utils.retryLater(this, UpdateTriggers.class, 10);
                break;
            case Utils.HAS_CONNECTION:
                Log.i(TAG, "LocTriggersService HAS_CONNECTION");
                String username = mSharedPreferences.getString("username", "user");
                if (!retreiveLocTriggers(username))  {
                    Log.i(TAG,"LocTriggersService NO_INTERNET OR ERROR PARSING");
                    Utils.retryLater(this,UpdateTriggers.class, 3600);
                    stopSelf();
                    break;
                }
                updateGeofences();
                stopSelf();
                break;
            default:
                stopSelf();
                break;
        }
        return START_STICKY;
    }

    /**
     * Called when the user clicks the "Register geofences" button.
     * Get the geofence parameters for each geofence and add them to
     * a List. Create the PendingIntent containing an Intent that
     * Location Services sends to this app's broadcast receiver when
     * Location Services detects a geofence transition. Send the List
     * and the PendingIntent to Location Services.
     */
    public void updateGeofences() {
        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!servicesConnected()) {
        	Log.d(TAG,"Service not connected");
            return;
        }
        
        Log.d(TAG,"updateGeofences");

        Editor editor = mSharedPreferences.edit();
        editor.putString("setGeofences", geofenceList);
		editor.commit();
        
    }
    
    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // In debug mode, log the status
            Log.d(TAG, getString(R.string.play_services_available));

            // Continue
            return true;

        // Google Play services was not available for some reason
        } else {

            return false;
        }
    }
    
    public boolean retreiveLocTriggers(String user){
    	Log.d(TAG,"retreiveLocTriggers");
		boolean result = true;
		HttpURLConnection urlConnection = null;
		try{
			URL url = new URL(Utils.EC2_URL+"getloc/?u="+user);
			urlConnection = (HttpURLConnection) url.openConnection();
			String line;
			StringBuilder builder = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			while((line = reader.readLine()) != null) {
				builder.append(line);
			}
			
		    JSONObject response = new JSONObject(builder.toString());
		    Log.d(TAG,"retreiveLocTriggers response: "+response);
		    
	        Iterator<?> forms = response.keys();
	        String triggers = null;
	        while( forms.hasNext() ){
	            String form = (String) forms.next();
	            triggers =(String) response.getString(form);
	            if ((triggers != null) && triggers.length()!=0){
	            	
	            	JSONObject geofencesJSON = new JSONObject(triggers);
	    			Iterator<?> geofences = geofencesJSON.keys();
	    	        String loc = null;
	    	        while( geofences.hasNext() ){
	    	            String geofence = (String) geofences.next();
	    	            loc =(String) geofencesJSON.getString(geofence);
	    	            if ((loc != null) && loc.length()!=0){
	    	            	String[] props = loc.split(",");
	    	            	String geofenceId = form+";;"+geofence;
	    	            	SimpleGeofence mGeofence = new SimpleGeofence(
	    	                        geofenceId,
	    	                        // Get latitude, longitude, and radius from the UI
	    	                        Double.valueOf(props[0]),
	    	                        Double.valueOf(props[1]),
	    	                        Float.valueOf(props[2]),
	    	                        // Set the expiration time
	    	                        Geofence.NEVER_EXPIRE, //edited by Hoang GEOFENCE_EXPIRATION_IN_MILLISECONDS,
	    	                        // Only detect entry transitions
	    	                        //Geofence.GEOFENCE_TRANSITION_ENTER
	    	                        Integer.valueOf(props[3]));
	    	            	Log.d(TAG,"Geofence: "+mGeofence);
	    	            	mPrefs.setGeofence(geofenceId, mGeofence);
	    	            	geofenceList+=geofenceId+" ";
	    	            }
	    	        }
	            }
	      
	        }
		} catch (Exception e) {
			result = false;
		}
		finally {
		     urlConnection.disconnect();
		}
		return result;
	}

    @Override
    public void onDestroy(){
        releaseLocks();
        Log.d(TAG,"UpdateTriggersService onDestroy()");
        super.onDestroy();
    }
    public void releaseLocks(){
        wakeLock.release();
        wifiLock.release();
    }
}
