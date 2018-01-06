package com.insomniac.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by Sanjeev on 1/6/2018.
 */

public class QueryPreferences {

    private static final String PREF_SEARCH_QUERY = "searchQuery";
    private static final String PREF__LAST_RESULT_ID = "lastResultId";

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

    public static String getLastResultId(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF__LAST_RESULT_ID,null);
    }

    public static void setLastResultId(Context context,String lastResultId){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF__LAST_RESULT_ID,lastResultId).apply();
    }
}
