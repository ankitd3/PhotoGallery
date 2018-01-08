package com.insomniac.photogallery;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by Sanjeev on 1/7/2018.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService{

    private PollTask mPollTask;
   // private Context mContext;


    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Toast.makeText(getApplicationContext(),"onStartJob",Toast.LENGTH_SHORT).show();
        if(isJobSchedule(getApplicationContext()))
            getJobScheduler(getApplicationContext(),isJobSchedule(getApplicationContext()));
        mPollTask = new PollTask();
        mPollTask.execute(jobParameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if(mPollTask != null)
            mPollTask.cancel(true);
        Toast.makeText(getApplicationContext(),"onStopJob",Toast.LENGTH_SHORT).show();
        return true;
    }
    private class PollTask extends AsyncTask<JobParameters,Void,Void>{

        @Override
        protected Void doInBackground(JobParameters... jobParameters) {
            JobParameters jobP = jobParameters[0];
            //Toast.makeText(getApplicationContext(),"doInBackground",Toast.LENGTH_SHORT).show();

            String lastQuery = QueryPreferences.getStoredQuery(PollJobService.this);
            String lastResultId = QueryPreferences.getLastResultId(PollJobService.this);
            List<GalleryItem> galleryItems;

            if(lastQuery == null)
                galleryItems = new FlickrFetchr().fetchRecentPhotos(0);
            else
                galleryItems = new FlickrFetchr().searchPhotos(lastQuery,0);

            String resultId = galleryItems.get(0).getId();
            if(resultId.equals(lastResultId))
                Log.i(TAG,"Got an old result");
            else{
                Log.i(TAG,"Got a new result");
                //Toast.makeText(getApplicationContext(),"else",Toast.LENGTH_SHORT).show();

                Resources resources = getResources();
                Intent intent = PhotoGalleryActivity.newIntent(PollJobService.this);
                PendingIntent pendingIntent = PendingIntent.getService(PollJobService.this,0,intent,0);
                Notification notification = new NotificationCompat.Builder(PollJobService.this)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(PollJobService.this);
                notificationManagerCompat.notify(0,notification);
            }

            jobFinished(jobP,false);

            QueryPreferences.setLastResultId(PollJobService.this,resultId);
            return null;
        }
    }

    public static boolean isJobSchedule(Context context){
        final int JOB_ID = 1;

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        boolean hasBeenScheduled = false;
        for(JobInfo jobInfo : jobScheduler.getAllPendingJobs()){
            if(jobInfo.getId() == JOB_ID){
                hasBeenScheduled = true;
            }
        }
        Toast.makeText(context,"has been Scheduled" + hasBeenScheduled,Toast.LENGTH_SHORT).show();
        return hasBeenScheduled;
    }

    public static void getJobScheduler(Context context,boolean isJobOn){
        final int JOB_ID = 1;
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if(!isJobOn){
            Toast.makeText(context,"isJobOn" + isJobOn,Toast.LENGTH_SHORT).show();
            JobInfo jobInfo = new JobInfo.Builder(
                    JOB_ID, new ComponentName(context,PollJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(1000 * 60)
                    .setPersisted(true)
                    .setRequiresDeviceIdle(false)
                    .build();

            jobScheduler.schedule(jobInfo);
        }else{
            Toast.makeText(context,"isJobOn" + isJobOn,Toast.LENGTH_SHORT).show();
            jobScheduler.cancel(JOB_ID);
        }
    }
}
