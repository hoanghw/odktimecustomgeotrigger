package org.odk.collect.android.triggers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

import org.json.JSONObject;
import org.odk.collect.android.R;
import org.odk.collect.android.activities.SplashScreenActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LogInActivity extends Activity implements OnClickListener {
	private EditText etUsername;
	private EditText etPassword;
	private Button btnLogin;
	private TextView lblResult;
	private String username;
	private String password;
	private Activity activity;
	private Context context;
	private ProgressDialog dialog;
	
	public static final String TAG = "ODKLogin";
	@Override
	protected void onStart() {
		super.onStart();
		
		activity = LogInActivity.this;
		context = getApplicationContext();
		
    	Log.i(TAG, "User is not logged in - display login form");
		setContentView(R.layout.login);
		
		//ready dialog
		dialog = new ProgressDialog(activity);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setMessage("Connecting to QT server...");
		
		// Get the EditText and Button References
		etUsername = (EditText)findViewById(R.id.username);
		etPassword = (EditText)findViewById(R.id.password);
		btnLogin = (Button)findViewById(R.id.login_button);
		lblResult = (TextView)findViewById(R.id.result);
		
		// Set Click Listener
		btnLogin.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v == findViewById(R.id.login_button)) {
			username = this.etUsername.getText().toString().trim();
			password = this.etPassword.getText().toString().trim();
			
			if(username.equalsIgnoreCase("") || password.equalsIgnoreCase("")) {
				this.lblResult.setText("Please provide a username and password");
			} else {
				new Login().execute();
			}	
		}		
	}
	
	private class Login extends AsyncTask<Void, Void, Void> {
		
		private String message = null;
		
		@Override
		protected void onPreExecute() {
			//Initialize values and show dialog
			dialog.show();		
		}
		
		@Override
		protected Void doInBackground(Void... whatever) {
			try {
				if (authenticate(username,password)) {
					//authenticated
					Log.d(TAG, "authenticated as " + username);
					
					SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
			        Editor editor = mSharedPreferences.edit();
			        editor.putBoolean("IS_LOGGED_IN", true);
			        editor.putString("username", username);
			        editor.putString("password", password);
					editor.commit();
					
					//start new main activity
					Intent intent = new Intent(context, SplashScreenActivity.class);
		        	startActivity(intent);
		        	LogInActivity.this.finish();	
		        	
		        	//broadcast to other addon apps
		        	Intent intentUpdateUser = new Intent("updateuser");
		    		intentUpdateUser.putExtra("username", username);
		    		sendBroadcast(intentUpdateUser);
				} else {
					message = "Please enter a valid username and password";
				}
			} catch (Exception e) {
				Log.e(TAG, e.toString());
				message = e.getMessage();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void whatever) {
			dialog.dismiss();
			if (message!=null){
				Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
			}
		}
		
		public boolean authenticate(String user, String pass) throws Exception{
			ConnectivityManager connectivityManager =
		            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
		    
			if (ni == null || !ni.isConnected()){
	        	throw new Exception("Could not connect to server. Please check if you are connected to the internet.");
			}
			HttpURLConnection urlConnection = null;
			try {
				URL url = new URL(Utils.EC2_URL+"checkuser/?u="+user+"&p="+pass);
				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setConnectTimeout(5000);
				urlConnection.setReadTimeout(5000);
				String line;
				StringBuilder builder = new StringBuilder();
				BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
				while((line = reader.readLine()) != null) {
					builder.append(line);
				}
				
				JSONObject json = new JSONObject(builder.toString());
				
		        Iterator<?> keys = json.keys();
		        while( keys.hasNext() ){
		            String key = (String) keys.next();
		            String value =(String) json.getString(key);
		            return key.equals("user") && value.equals("true");	
		        }
			} catch (Exception e) {
				throw new Exception("Server is unavailable. Please try again later!");
			}
			finally {
				if (urlConnection != null)
					urlConnection.disconnect();
			}
			return false;
		}
	}
}
