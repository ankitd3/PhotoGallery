package com.insomniac.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Created by Sanjeev on 1/8/2018.
 */

public class StartUpReceiver extends BroadcastReceiver{
    private static final String TAG = "StartUpReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"Got an intent" + intent.getAction());
        boolean isOn = QueryPreferences.isAlarmOn(context);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PollJobService.getJobScheduler(context,isOn);
        }*///else
            PollService.setServiceAlarm(context,isOn);
    }
}
