package com.insomniac.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by Sanjeev on 1/6/2018.
 */

public class QueryPreferences {

    private static final String PREF_SEARCH_QUERY = "searchQuery";

    public static String getStoredQuery(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SEARCH_QUERY,null);
    }

    public static void setStoredQuery(Context context,String query){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SEARCH_QUERY,query)
                .apply();
    }
}
