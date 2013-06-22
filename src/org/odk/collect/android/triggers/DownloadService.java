package org.odk.collect.android.triggers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.odk.collect.android.listeners.DeleteFormsListener;
import org.odk.collect.android.listeners.FormDownloaderListener;
import org.odk.collect.android.listeners.FormListDownloaderListener;
import org.odk.collect.android.logic.FormDetails;
import org.odk.collect.android.provider.FormsProviderAPI.FormsColumns;
import org.odk.collect.android.tasks.DeleteFormsTask;
import org.odk.collect.android.tasks.DownloadFormListTask;
import org.odk.collect.android.tasks.DownloadFormsTask;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class DownloadService extends Service implements FormListDownloaderListener, 
FormDownloaderListener, DeleteFormsListener {
	public static final String TAG = "DOWNLOADSERVICE";
	private PowerManager.WakeLock wakeLock;
	private WifiManager.WifiLock wifiLock;
	
	@Override
	public void onCreate(){
		Log.i(TAG, "DownloadService onCreate");
		super.onCreate();
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DownloadService");
	    wakeLock.acquire();
	    
	    WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	    wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "DownloadService");
	    wifiLock.acquire();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		
		Log.i(TAG, "DownloadService onStart");
	    switch (Utils.networkState(this)){
	    	case Utils.NO_CONNECTION:
	    		Log.i(TAG, "DownloadServiceRetry NO_CONNECTION");
				Utils.retryLater(this, DownloadRequest.class, 3600);
				stopSelf();
				break;
	    	case Utils.WAIT_FOR_WIFI:
	    		Log.i(TAG, "DownloadService WAIT_FOR_WIFI");
				Utils.retryLater(this, DownloadRequest.class, 10);
				break;
	    	case Utils.HAS_CONNECTION:
	    		Log.i(TAG, "DownloadServiceRetry HAS_CONNECTION");
	    		if (hasInternet()){
	    			Log.i(TAG, "hasInternet True");
	    			fetchingForms();	
	    		}else{
	    			Log.i(TAG, "DownloadServiceRetry NO_INTERNET");
	    			Utils.retryLater(this, DownloadRequest.class, 3600);
	    			stopSelf();
	    		}
	    		break;
	    	default:
	    		stopSelf();
	    		break;
	    }	
		return START_STICKY;
	}
	
	//There must be a better way to check Internet while Airbears not logged in
	public boolean hasInternet(){
		SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String user = mSharedPreferences.getString("username","user");
		Map <String,List<Calendar>> triggers = Utils.getTimeTrigger(user);
		return triggers != null;
	}
	
	public void fetchingForms(){
		Log.i(TAG, "fetchingFormsCalled");
		
		mFormNamesAndURLs = new HashMap<String, FormDetails>();
        if (mDownloadFormListTask != null &&
        	mDownloadFormListTask.getStatus() != AsyncTask.Status.FINISHED) {
        	//Utils.retryLater(this, DownloadRequest.class, 3600);
        	Log.i(TAG, "fetchingForms Already doing the download");
			stopSelf();
        	return; // we are already doing the download!!!
        } else if (mDownloadFormListTask != null) {
        	mDownloadFormListTask.setDownloaderListener(null);
        	mDownloadFormListTask.cancel(true);
        	mDownloadFormListTask = null;
        }
        mDownloadFormListTask = new DownloadFormListTask();
        mDownloadFormListTask.setDownloaderListener(this);
        mDownloadFormListTask.execute();
	}
	
	@Override
	public void formListDownloadingComplete(HashMap<String, FormDetails> result) {
		// TODO Auto-generated method stub
		if (result == null){
			Log.i(TAG, "fetchingFormsDone No Result");
			Utils.retryLater(this, DownloadRequest.class, 3600);
			stopSelf();
			return;
		}
		if (result.containsKey(DownloadFormListTask.DL_AUTH_REQUIRED)) {
            // need authorization
			// refer to FormDownloadList/onCreateDiaglog(AUTH_DIALOG)
			// then call downloadForms() again
			Log.i(TAG, "fetchingFormsDone DL_AUTH_REQUIRED");
			Utils.retryLater(this, DownloadRequest.class, 3600);
			stopSelf();
            return;
        } else if (result.containsKey(DownloadFormListTask.DL_ERROR_MSG)) {
            // Download failed
        	Log.i(TAG, "fetchingFormsDone ERROR_MSG" + result.get(DownloadFormListTask.DL_ERROR_MSG));
        	Utils.retryLater(this, DownloadRequest.class, 3600);
        	stopSelf();
            return;
        } else {
            // Everything worked. Clear the list and add the results.
        	Log.i(TAG, "fetchingFormsDone Success");
            mFormNamesAndURLs = result;
            downloadAllForms();
        }
	}

	private DownloadFormListTask mDownloadFormListTask;
    private DownloadFormsTask mDownloadFormsTask;
    private HashMap<String, FormDetails> mFormNamesAndURLs = new HashMap<String,FormDetails>();
    ArrayList<FormDetails> filesToDownload = new ArrayList<FormDetails>();
	
    @SuppressWarnings("unchecked")
	private void downloadAllForms() {
		Log.i(TAG, "downloadAllFormsCalled");
        for (Map.Entry<String, FormDetails> entry : mFormNamesAndURLs.entrySet()) {
		    //String key = entry.getKey();
		    FormDetails value = entry.getValue();
            filesToDownload.add(value);
        }
        mDownloadFormsTask = new DownloadFormsTask();
        mDownloadFormsTask.setDownloaderListener(this);
        mDownloadFormsTask.execute(filesToDownload); 
    }
    
    @Override
	public void formsDownloadingComplete(HashMap<FormDetails, String> result) {
		// TODO Auto-generated method stub
    	Log.i(TAG, "downloadFormsDone");
		if (mDownloadFormsTask != null) {
            mDownloadFormsTask.setDownloaderListener(null);
        }
		deleteForms();
	}
    
    DeleteFormsTask mDeleteFormsTask = null;
	private void deleteForms() {	
		Log.i(TAG, "deleteFormsCalled");
		Context context = DownloadService.this;
		ArrayList<Long> deletingForms = new ArrayList<Long>();
		Cursor c = null;
		try{
			ContentResolver cr = context.getContentResolver();
			String where = FormsColumns.JR_FORM_ID +" NOT IN (\"";
			for (int i = 0; i<filesToDownload.size(); i++) {
	            where += filesToDownload.get(i).formID+"\",\"";
	        }
			where+="\")";
		    c = cr.query(FormsColumns.CONTENT_URI, null, where, null, null);
		    if (c == null) {
	            Log.e(TAG, "Forms Content Provider returned NULL");
	            stopSelf();
	            return;
	        }
	        c.moveToPosition(-1);
            while (c.moveToNext()) {
            	long k = c.getLong(c.getColumnIndex(FormsColumns._ID));
            	deletingForms.add(k);
            }
		} catch (Exception e){
			
		} finally { 
			c.close();
		}
		// only start if no other task is running
		if (mDeleteFormsTask == null) {
			mDeleteFormsTask = new DeleteFormsTask();
			mDeleteFormsTask.setContentResolver(getContentResolver());
			mDeleteFormsTask.setDeleteListener(this);
			mDeleteFormsTask.execute(deletingForms
					.toArray(new Long[deletingForms.size()]));
		}else{
			stopSelf();
		}
	}
	
	@Override
	public void deleteComplete(int deletedForms) {
		// TODO Auto-generated method stub
		Log.i(TAG, "deleteFormsDone " + deletedForms);
		stopSelf();
	}
	
	@Override
	public void progressUpdate(String currentFile, int progress, int total) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onDestroy(){
		releaseLocks();
		super.onDestroy();
	}
	
	public void releaseLocks(){
		wakeLock.release();
		wifiLock.release();
	}
}
