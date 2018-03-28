package com.dedalexey.myapplication1111;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by Alexey on 06.02.2018.
 */

public class PollService extends IntentService {
    private final static String TAG = PollService.class.getSimpleName();

    private static final long POLL_INTERVAL = 60;//AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    public static final String ACTION_SHOW_NOTIFICATION =
            "com.dedalexey.android.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE =
            "com.dedalexey.android.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";


    public static Intent newIntent(Context context){
        return new Intent(context,PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean  isOn){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context,0,i,0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        if(isOn){
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(),POLL_INTERVAL,pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }
        QueryPreferences.setAlarmOn(context, isOn);
    }
    public PollService(){
        super(TAG);
    }
    public static boolean isServiceAlarmOn(Context context){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context,0,i,PendingIntent.FLAG_NO_CREATE);
        return  pi != null;
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(!isNetWorkAvailableAndConnected()){
            return;
        }
        String query = QueryPreferences.getStoredQuery(this);
        String lastResult = QueryPreferences.getLastResultId(this);

        List<GalleryItem> items;

        if(query == null){
            items = new FlickrFetchr().fetchRecentPhotos();
        } else {
            items = new FlickrFetchr().searchPhotos(query);
        }

        if(items.size()==0){
            return;
        }
        Log.i(TAG, "Query: " + query);
        String resultId =  items.get(0).getId();
        if(resultId.equals(lastResult)){
            Log.i(TAG, "Got an old result: " + resultId);
        } else {
            Log.i(TAG, "Got a new result: " + resultId);

            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();


            showBackgroundNotification(0, notification);
        }
        QueryPreferences.setLastResultId(this,resultId);
    }
    private void showBackgroundNotification(int requestCode,
                                            Notification notification) {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK , null, null);
    }
    private boolean isNetWorkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetWorkAvailable = cm.getActiveNetworkInfo() !=null;
        boolean isNetWorkConnected = isNetWorkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetWorkConnected;
    }
}