package com.insomniac.photogallery;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.support.v7.widget.SearchView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sanjeev on 12/22/2017.
 */

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "pg";
    private List<GalleryItem> mItems = new ArrayList<>();
    private RecyclerView mPhotoRecyclerView;
    private int mCurrentPage = 0;
    private PhotoAdapter mPhotoAdapter;
    private ThumbnailDownloader<PhotoHolder> mPhotoHolderThumbnailDownloader;
    private LruCache<String,Bitmap> mPhotoHolderBitmapLruCache;
    private ProgressBar mProgressBar;
    private Handler mMainHandler;


    public static Fragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateItems();
        setRetainInstance(true);
        setHasOptionsMenu(true);

        final int cacheSize = 1000;
        mPhotoHolderBitmapLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        mMainHandler = new Handler();
        mPhotoHolderThumbnailDownloader = new ThumbnailDownloader<>(mMainHandler);
        mPhotoHolderThumbnailDownloader.setThumbnailDownloaderListener(new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail,String url) {
                addBitmapToMemoryCache(url,thumbnail);
                loadBitmap(target,thumbnail);
            }
        });

        mPhotoHolderThumbnailDownloader.start();
        mPhotoHolderThumbnailDownloader.getLooper();
        Log.i(TAG,"Background thread started");
        //Toast.makeText(getContext(),"Background Thread Started ",Toast.LENGTH_SHORT).show();

        /*Intent intent = PollService.newIntent(getActivity());
        getActivity().startService(intent);*/

       // PollService.setServiceAlarm(getContext(),true);

    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        setUpAdapter();
        mPhotoRecyclerView.setAdapter(mPhotoAdapter);

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (isLastItemDisplaying()) {
                    mCurrentPage++;
                    updateItems();
                }
            }
        });

        ViewTreeObserver viewTreeObserver = mPhotoRecyclerView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                calculateCellSize();
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater){
        super.onCreateOptionsMenu(menu,menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery,menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG,"Query submitted" + s);
                if(s.equals(""))
                    submitQuery(null);
                else
                    submitQuery(s);
                searchItem.collapseActionView();
                searchView.clearFocus();
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(searchView.getWindowToken(),0);
                showProgressBar();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG,"QueryTextSubmit : " + s);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = QueryPreferences.getStoredQuery(getContext());
                searchView.setQuery(query,false);
            }
        });

        MenuItem toggleAlarmItem = menu.findItem(R.id.menu_item_polling);
        if(PollService.isServiceAlarmOn(getActivity()))
            toggleAlarmItem.setTitle(R.string.stop_polling);
        else
            toggleAlarmItem.setTitle(R.string.start_polling);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){
        switch (menuItem.getItemId()){
            case R.id.menu_item_clear : QueryPreferences.setStoredQuery(getContext(),null);
                                            submitQuery(null);
                                            Toast.makeText(getActivity(),"clear",Toast.LENGTH_SHORT).show();
                                            return true;
            case R.id.menu_item_polling : boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                                          PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
                                          getActivity().invalidateOptionsMenu();
                                          return  true;
            default : return super.onOptionsItemSelected(menuItem);
        }
    }

    public void updateItems() {
        String query = QueryPreferences.getStoredQuery(getContext());
        Toast.makeText(getContext(),"Exec" + mCurrentPage,Toast.LENGTH_SHORT).show();
        new FetchItemTask(query).execute(Integer.valueOf(mCurrentPage));
    }



    private void setUpAdapter() {
        if (isAdded())
            if (mPhotoAdapter == null)
                mPhotoAdapter = new PhotoAdapter(mItems);
            else {
                Toast.makeText(getContext(),"onScroll" + mCurrentPage,Toast.LENGTH_SHORT).show();
                mPhotoAdapter.setItems(mItems);
            }
    }

    private class FetchItemTask extends AsyncTask<Integer, Void, List<GalleryItem>> {

        String mQuery;

        public FetchItemTask(String query){
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            //String query = "cat";
            if(mQuery == null)
                return new FlickrFetchr().fetchRecentPhotos(params[0]);
            else
                return new FlickrFetchr().searchPhotos(mQuery,params[0]);
        }

        @Override
        public void onPostExecute(List<GalleryItem> items) {
            mItems.addAll(items);
            Log.d(TAG, "onPostExecute");
            hideProgressBar();
            setUpAdapter();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        List<GalleryItem> mGalleryItems = new ArrayList<>();

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.gallery_item,parent,false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeHolder = getResources().getDrawable(R.drawable.place_holder);
            holder.bindDrawable(placeHolder);
            Bitmap bitmap = getBitmapFromMemoryCache(galleryItem.getUrl());
            if(bitmap == null)
                mPhotoHolderThumbnailDownloader.queueThumbnail(holder,galleryItem.getUrl());
            else
                loadBitmap(holder,bitmap);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }

        public void setItems(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
            notifyDataSetChanged();
        }
    }

    public boolean isLastItemDisplaying(){
        RecyclerView.Adapter mPhotoAdapter = mPhotoRecyclerView.getAdapter();
        if(mPhotoAdapter != null && mPhotoAdapter.getItemCount() != 0){
            int lastItemVisiblePosition = ((GridLayoutManager)mPhotoRecyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            if(lastItemVisiblePosition != RecyclerView.NO_POSITION && lastItemVisiblePosition == mPhotoAdapter.getItemCount() - 1)
                return true;
        }
        return false;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mPhotoHolderThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mPhotoHolderThumbnailDownloader.quit();
        Toast.makeText(getContext(),"Background thrad stopped ",Toast.LENGTH_SHORT).show();
        Log.i(TAG,"Background THread stopeed ");
    }

    public void addBitmapToMemoryCache(String url,Bitmap bitmap){
        if(getBitmapFromMemoryCache(url) == null)
            mPhotoHolderBitmapLruCache.put(url,bitmap);
    }

    public Bitmap getBitmapFromMemoryCache(String url){
        return mPhotoHolderBitmapLruCache.get(url);
    }

    public void loadBitmap(PhotoHolder photoHolder,Bitmap bitmap){
        //Toast.makeText(getContext(),"cool",Toast.LENGTH_SHORT);
        Drawable drawable = new BitmapDrawable(getResources(),bitmap);
        photoHolder.bindDrawable(drawable);
    }

    public void submitQuery(String query){
        QueryPreferences.setStoredQuery(getActivity(),query);
        mCurrentPage = 0;
        mItems = new ArrayList<>();
        mPhotoAdapter.setItems(mItems);
        updateItems();
    }

    private static final int sColumnWidth = 120;

    public void calculateCellSize(){
        //Toast.makeText(getContext(),"calculateCellSize",Toast.LENGTH_SHORT).show();
        int spanCount = (int) Math.ceil(mPhotoRecyclerView.getWidth()/convertDPtoPixels(sColumnWidth));
        ((GridLayoutManager)mPhotoRecyclerView.getLayoutManager()).setSpanCount(spanCount);
    }

    private float convertDPtoPixels(int sColumnWidth){
        //Toast.makeText(getContext(),"convertDPtoPixels",Toast.LENGTH_SHORT).show();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float logicalDensity = displayMetrics.density;
        return sColumnWidth * logicalDensity;
    }

    public void hideProgressBar(){
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mProgressBar != null)
                    mProgressBar.setVisibility(View.GONE);
            }
        });
    }

    public void showProgressBar(){
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mProgressBar != null)
                    mProgressBar.setVisibility(View.VISIBLE);
            }
        });
    }
}