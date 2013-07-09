package org.odk.collect.android.messages;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.json.JSONObject;
import org.odk.collect.android.R;
import org.odk.collect.android.triggers.Utils;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

public class WidgetDisplay extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new MyTime(context, appWidgetManager), 1, 1000*60*60);
    }

    private class MyTime extends TimerTask {
        RemoteViews remoteViews;
        AppWidgetManager appWidgetManager;
        ComponentName thisWidget;
        Context mContext;

        public MyTime(Context context, AppWidgetManager appWidgetManager) {
            mContext = context;
            this.appWidgetManager = appWidgetManager;
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            thisWidget = new ComponentName(context, WidgetDisplay.class);
        }

        @Override
        public void run() {
            remoteViews.setTextViewText(R.id.widget_textview, getMessages(mContext));
            appWidgetManager.updateAppWidget(thisWidget, remoteViews);
        }

    }
    public static String getMessages(Context context){
        String response = "";
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String user = mSharedPreferences.getString("username","user");
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(Utils.EC2_URL+"getmessages/?u="+user);
            urlConnection = (HttpURLConnection) url.openConnection();
            String line;
            StringBuilder builder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            while((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONObject json = new JSONObject(builder.toString());
            String error = json.optString("ERROR");
            if ((error == null)||(error.length()==0))
                response = json.optString("LINE1")+"\n"
                        +json.optString("LINE2")+"\n"
                        +json.optString("LINE3")+"\n"
                        +json.optString("LINE4");
            else
                response = error;
        } catch (Exception e) {
            response = "Please check your internet connection";
        }
        finally {
            urlConnection.disconnect();
        }
        return response;
    }
}