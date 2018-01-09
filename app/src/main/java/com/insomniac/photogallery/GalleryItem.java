package com.insomniac.photogallery;


import android.net.Uri;

public class GalleryItem {

    private String mCaption;

    public String getOwner() {
        return mOwner;
    }

    public Uri getPhotoPageUri(){
        return Uri.parse("http://www.flickr.com/photos/").buildUpon().appendPath(mCaption).appendPath(mId).build();
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    private String mOwner;

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    private String mId;
    private String mUrl;

    @Override
    public String toString(){
        return mCaption;
    }
}
