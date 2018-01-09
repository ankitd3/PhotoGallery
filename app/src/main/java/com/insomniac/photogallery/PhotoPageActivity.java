package com.insomniac.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.webkit.WebView;

/**
 * Created by Sanjeev on 1/10/2018.
 */

public class PhotoPageActivity extends SingleFragmentActivity{

    private WebView mWebView;

    @Override
    public Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }

    public static Intent newIntent(Context context, Uri photoPageUri){
        Intent intent = new Intent(context,PhotoPageActivity.class);
        intent.setData(photoPageUri);
        return intent;
    }

    public void getWebView(WebView webView){
        mWebView = webView;
    }


    @Override
    public void onBackPressed() {

        if(mWebView.canGoBack())
            mWebView.goBack();
        else
            super.onBackPressed();
    }
}
