package com.insomniac.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * Created by Sanjeev on 1/6/2018.
 */

public class PollService extends IntentService{

    private static final String TAG = "PollService";
    private static final int POLL_INTERVAL = 1000 * 60;
    public static final String ACTION_SHOW_NOTIFICATION = "com.insomniac.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.insomniac.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    public static Intent newIntent(Context context){
        Intent intent = new Intent(context,PollService.class);
        return intent;
    }

    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i(TAG,"Received an Intent " + intent);
        Toast.makeText(this,"onHandleIntent",Toast.LENGTH_LONG).show();
        if(!isNetworkAvailableAndConnected())
            return;
        String lastQuery = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);
        List<GalleryItem> galleryItems;

        if(lastQuery == null)
            galleryItems = new FlickrFetchr().fetchRecentPhotos(0);
        else
            galleryItems = new FlickrFetchr().searchPhotos(lastQuery,0);

        if(galleryItems.size() == 0)
            return;

        String resultId = galleryItems.get(0).getId();
        if(resultId.equals(lastResultId)){
            Log.i(TAG,"Got a result " + resultId);
        }else {
            Log.i(TAG,"Got a new result " + resultId);

            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            /*NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);
            notificationManager.notify(0, notification);

            sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION),PERM_PRIVATE);*/

            showBackgroundNotification(0,notification);
        }

        QueryPreferences.setLastResultId(this,resultId);

    }

    private boolean isNetworkAvailableAndConnected(){
        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        //}
        boolean isNetworkAvailable = connectivityManager.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && connectivityManager.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }

    public static void setServiceAlarm(Context context,boolean isOn){
        Intent intent = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context,0,intent,0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(isOn){
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),POLL_INTERVAL,pendingIntent);
        }else{
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        QueryPreferences.setAlarmOn(context,isOn);
    }

    public static boolean isServiceAlarmOn(Context context){
        Intent intent = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context,0,intent,PendingIntent.FLAG_NO_CREATE);
        return pendingIntent != null;
    }

    private void showBackgroundNotification(int requestCode,Notification notification){
        Intent intent = new Intent(PollService.ACTION_SHOW_NOTIFICATION);
        intent.putExtra(REQUEST_CODE,requestCode);
        intent.putExtra(NOTIFICATION,notification);
        sendOrderedBroadcast(intent,PERM_PRIVATE,null,null, Activity.RESULT_OK,null,null);
    }

}
