package com.insomniac.photogallery;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sanjeev on 12/22/2017.
 */

public class FlickrFetchr {

    private static final String TAG = "ff";
	private static final String API_KEY = "a230a5b562b7b3afb8b1ba3bc6f3bf9e";
	//private static Context mContext = null;
    private static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";


    public byte[] getURLBytes(String urlSpec) throws IOException{

        URL url = new URL(urlSpec);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = httpURLConnection.getInputStream();
            if(httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(httpURLConnection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) > 0){
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();
        }finally {
            httpURLConnection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getURLBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems(){
        List<GalleryItem> galleryItems = new ArrayList<>();
        try{
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();
            String jsonString = getUrlString(url);
            //gsonParseItems(galleryItems,jsonString);
            Log.i(TAG,"Received JSON : " + jsonString);
            //Toast.makeText(mContext,"Received JSON ",Toast.LENGTH_SHORT).show();
            JSONObject jsonObject = new JSONObject(jsonString);
            parseItems(galleryItems,jsonObject);
        }catch (IOException e){
            Log.e(TAG,"Failed",e);
           // Toast.makeText(mContext,"Failed IOException",Toast.LENGTH_SHORT).show();
        }catch (JSONException j){
           // Toast.makeText(mContext,"Failed JSONEXcepption",Toast.LENGTH_SHORT).show();
            Log.e(TAG,"failed to parse JSOn",j);
        }
        return galleryItems;
    }

    public List<GalleryItem> fetchItems(Integer mPage){
        List<GalleryItem> galleryItems = new ArrayList<>();
        try{
            String uri = Uri.parse("https://api.flickr.com/services/rest/").buildUpon().appendQueryParameter("method","flickr.photos.getRescent").appendQueryParameter("api_key",API_KEY).appendQueryParameter("format","json").appendQueryParameter("nojsoncallback","1").appendQueryParameter("extras","url_s").build().buildUpon().appendQueryParameter("method",FETCH_RECENT_METHOD).appendQueryParameter("page",Integer.toString(mPage)).build().toString();
            String jsonString = getUrlString(uri);
            Log.i(TAG,"Received JSON : " + jsonString);
           // Toast.makeText(mContext,"Received JSON recent",Toast.LENGTH_SHORT).show();
            JSONObject jsonObject = new JSONObject(jsonString);
            parseItems(galleryItems,jsonObject);
        }catch (IOException e){
           // Toast.makeText(mContext,"Failed IOEx recent ",Toast.LENGTH_SHORT).show();
            Log.e(TAG,"Failed",e);
        }catch (JSONException e){
           // Toast.makeText(mContext,"Failed JSONEx",Toast.LENGTH_SHORT).show();
            Log.e(TAG,"failed to parse JSON",e);
        }
        return galleryItems;
    }

    private void parseItems(List<GalleryItem> galleryItems,JSONObject jsonObject) throws IOException,JSONException{
        JSONObject photoJsonObject = jsonObject.getJSONObject("photos");
        JSONArray photoJsonArray = photoJsonObject.getJSONArray("photo");

        for(int i = 0;i < photoJsonArray.length();i++){
            JSONObject photoJsonObject1 = photoJsonArray.getJSONObject(i);

            GalleryItem galleryItem = new GalleryItem();
            galleryItem.setId(photoJsonObject1.getString("id"));
            galleryItem.setCaption(photoJsonObject1.getString("title"));

            if(!photoJsonObject1.has("url_s")){
                continue;
            }

            galleryItem.setUrl(photoJsonObject1.getString("url_s"));
            //Toast.makeText(mContext,"parsed",Toast.LENGTH_SHORT).show();
            galleryItems.add(galleryItem);

        }
    }

    private void gsonParseItems(List<GalleryItem> galleryItems,String jsonObject) throws IOException,JSONException{
        JsonElement jelement = new JsonParser().parse(jsonObject);
        JsonObject jobject = jelement.getAsJsonObject();
        jobject = jobject.getAsJsonObject("photos");
        JsonArray photoArray = jobject.getAsJsonArray("photo");
        Gson gson = new Gson();
        for (int i = 0; i < photoArray.size(); i++) {
            JsonObject photo = photoArray.get(i).getAsJsonObject();
            if (!photo.has("url_s")) {
                continue;
            }
            GalleryItem galleryItem = gson.fromJson(photo, GalleryItem.class);
            galleryItems.add(galleryItem);
        }
    }
}
