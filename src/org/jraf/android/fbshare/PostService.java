/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright 2011 Benoit 'BoD' Lubek (BoD@JRAF.org).  All Rights Reserved.
 */
package org.jraf.android.fbshare;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.facebook.android.Facebook;

public class PostService extends IntentService {
    private static final String TAG = Constants.TAG + PostService.class.getSimpleName();

    public static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";
    public static final String EXTRA_LINK = "EXTRA_LINK";

    private Facebook mFacebook;
    private final Handler mHandler = new Handler();

    public PostService() {
        super("PostService");
    }

    public Facebook getFacebook() {
        if (mFacebook == null) {
            mFacebook = new Facebook(Constants.FACEBOOK_APP_ID);
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            mFacebook.setAccessToken(sharedPreferences.getString(Constants.PREF_FACEBOOK_TOKEN, null));
            mFacebook.setAccessExpires(sharedPreferences.getLong(Constants.PREF_FACEBOOK_EXPIRES, 0));
        }
        return mFacebook;
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (Config.LOGD) Log.d(TAG, "onHandleIntent");
        if (!getFacebook().isSessionValid()) {
            // TODO: notify error message
            return;
        }

        int retry = 0;
        int wait = 1000;
        while (retry < 8) {
            final boolean ok = post(intent.getStringExtra(EXTRA_MESSAGE), intent.getStringExtra(EXTRA_LINK));
            if (Config.LOGD) Log.d(TAG, "post returned " + ok);
            if (ok) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(PostService.this, R.string.post_ok, Toast.LENGTH_LONG).show();
                    }
                });
                break;
            }
            if (Config.LOGD) Log.d(TAG, "Could not post: retry=" + retry + " wait=" + wait);
            try {
                Thread.sleep(wait);
            } catch (final InterruptedException e) {}
            wait *= 2;
            retry++;
        }
        if (retry == 4) {
            // TODO NOTIF
            if (Config.LOGD) Log.d(TAG, "Could not post after " + retry + " retries");
        }
    }

    private boolean post(final String message, final String link) {
        final Bundle params = new Bundle();
        params.putString("message", message);
        params.putString("link", link);
        try {
            final String res = getFacebook().request("me/feed", params, "POST");
            if (Config.LOGD) Log.d(TAG, "res=" + res);
            if (res != null && res.startsWith("{\"id\":")) {
                return true;
            }
            return false;
        } catch (final Exception e) {
            Log.e(TAG, "onHandleIntent", e);
            return false;
        }
    }
}
