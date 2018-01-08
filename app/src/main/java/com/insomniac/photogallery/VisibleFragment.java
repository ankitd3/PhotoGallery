package com.insomniac.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Sanjeev on 1/8/2018.
 */

public abstract class VisibleFragment extends Fragment{
    private static final String TAG = "VisibleFragment";

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().registerReceiver(mOnShowNotification,intentFilter,PollJobService.PERM_PRIVATE,null);
        }*///else
            getActivity().registerReceiver(mOnShowNotification,intentFilter,PollService.PERM_PRIVATE,null);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mOnShowNotification);
    }

    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getActivity(),"Got a Broadcast: " + intent.getAction(),Toast.LENGTH_SHORT).show();
            Log.i(TAG,"cancelling notification");
            setResultCode(Activity.RESULT_CANCELED);
        }
    };
}
