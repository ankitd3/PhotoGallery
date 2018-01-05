package com.insomniac.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
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

    public static Fragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new FetchItemTask().execute();
        setRetainInstance(true);

        Handler responseHandler = new Handler();
        mPhotoHolderThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mPhotoHolderThumbnailDownloader.setThumbnailDownloaderListener(new ThumbnailDownloader.ThumbnailDownloaderListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(),thumbnail);
                target.bindDrawable(drawable);
            }
        });

        mPhotoHolderThumbnailDownloader.start();
        mPhotoHolderThumbnailDownloader.getLooper();
        Log.i(TAG,"Background thread started");
        Toast.makeText(getContext(),"Background Thread Started ",Toast.LENGTH_SHORT).show();

    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        View view = layoutInflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

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

    private static final int sColumnWidth = 120;

    public void calculateCellSize(){
        Toast.makeText(getContext(),"calculateCellSize",Toast.LENGTH_SHORT).show();
        int spanCount = (int) Math.ceil(mPhotoRecyclerView.getWidth()/convertDPtoPixels(sColumnWidth));
        ((GridLayoutManager)mPhotoRecyclerView.getLayoutManager()).setSpanCount(spanCount);
    }

    private float convertDPtoPixels(int sColumnWidth){
        Toast.makeText(getContext(),"convertDPtoPixels",Toast.LENGTH_SHORT).show();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float logicalDensity = displayMetrics.density;
        return sColumnWidth * logicalDensity;
    }

    public void updateItems() {
        Toast.makeText(getContext(),"Exec" + mCurrentPage,Toast.LENGTH_SHORT).show();
        new FetchItemTask().execute(mCurrentPage);
    }

    private void setUpAdapter() {
        if (isAdded())
            if (mPhotoAdapter == null)
                mPhotoAdapter = new PhotoAdapter(mItems);
            else {
                Toast.makeText(getContext(),"onScroll" + mCurrentPage,Toast.LENGTH_SHORT).show();
                mPhotoAdapter.notifyDataSetChanged();
            }
    }

    private class FetchItemTask extends AsyncTask<Integer, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            if (mCurrentPage == 0)
                return new FlickrFetchr().fetchItems();
            return new FlickrFetchr().fetchItems(params[0]);
        }

        @Override
        public void onPostExecute(List<GalleryItem> items) {
            mItems.addAll(items);
            Log.d(TAG, "onPostExecute");
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
            mPhotoHolderThumbnailDownloader.queueThumbnail(holder,galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
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
}