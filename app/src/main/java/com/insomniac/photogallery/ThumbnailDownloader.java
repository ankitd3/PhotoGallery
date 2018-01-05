package com.insomniac.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Sanjeev on 1/4/2018.
 */

public class ThumbnailDownloader<Q> extends HandlerThread{

    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 1;
    private Handler mResponseHandler;
    private ThumbnailDownloaderListener<Q> mThumbnailDownloaderListener;
    private static String urlString;

    public interface ThumbnailDownloaderListener<Q>{
        void onThumbnailDownloaded(Q target,Bitmap thumbnail,String urlString);
    }

    public void setThumbnailDownloaderListener(ThumbnailDownloaderListener<Q> listener){
        mThumbnailDownloaderListener = listener;
    }

    private Handler mRequestHandler;
    private ConcurrentMap<Q,String> mRequestMap = new ConcurrentHashMap<>();

    public ThumbnailDownloader(String name) {
        super(name);
    }

    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler = responseHandler;

    }

    public ThumbnailDownloader(String name, int priority) {
        super(name, priority);
    }

    public void queueThumbnail(Q target,String url){
        Log.i(TAG,"Got a URL" + url);

        urlString = url;

        if(url == null)
            mRequestMap.remove(target);
        else{
            mRequestMap.put(target,url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target).sendToTarget();
        }
    }

    @Override
    protected void onLooperPrepared(){
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message message){
                if(message.what == MESSAGE_DOWNLOAD){
                    Q target = (Q) message.obj;
                    Log.i(TAG,"Got a url request" + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    private void handleRequest(final Q target){
        try{
            final String url = mRequestMap.get(target);

            if(url == null)
                return;

            byte[] bitmapBytes = new FlickrFetchr().getURLBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mRequestMap.get(target) != url){
                        return;
                    }

                    mRequestMap.remove(target);
                    mThumbnailDownloaderListener.onThumbnailDownloaded(target,bitmap,urlString);
                }
            });

            Log.i(TAG,"Bitmap Created");
        }catch (IOException e){
            Log.e(TAG,"Error downloading msg",e);
        }
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }
}
