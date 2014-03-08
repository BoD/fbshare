/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2011 Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.android.fbshare;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

    private static final int MAX_RETRY = 8;

    private volatile Facebook mFacebook;
    private final Handler mHandler = new Handler();

    public PostService() {
        super(PostService.class.getSimpleName());
    }

    public synchronized Facebook getFacebook() {
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

        final String message = intent.getStringExtra(EXTRA_MESSAGE);
        final String link = intent.getStringExtra(EXTRA_LINK);

        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        final Notification postingNotification = createNotificationPosting();
        final int postingNotificationId = (int) System.currentTimeMillis();
        notificationManager.notify(postingNotificationId, postingNotification);

        int retry = 0;
        int wait = 1000;
        while (retry < MAX_RETRY) {
            final boolean ok = post(message, link); // blocking
            if (Config.LOGD) Log.d(TAG, "post returned " + ok);
            if (ok) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(PostService.this, R.string.post_ok, Toast.LENGTH_LONG).show();
                    }
                });
                notificationManager.cancel(postingNotificationId);
                return;
            }
            if (Config.LOGD) Log.d(TAG, "Could not post: retry=" + retry + " wait=" + wait);
            try {
                Thread.sleep(wait);
            } catch (final InterruptedException e) {}
            wait *= 2;
            retry++;
        }
        if (retry == MAX_RETRY) {
            if (Config.LOGD) Log.d(TAG, "Could not post after " + retry + " retries");
            notificationManager.cancel(postingNotificationId);
            final Notification problemNotification = createNotificationProblem(message, link);
            notificationManager.notify(postingNotificationId, problemNotification);
        }
    }

    private Notification createNotificationPosting() {
        final Notification notification = new Notification();
        notification.icon = R.drawable.ic_stat_fb;
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        final String text = getString(R.string.posting);
        notification.setLatestEventInfo(this, text, text, PendingIntent.getBroadcast(this, 0, new Intent(), 0));// <- dummy PendingIntent mandatory
        return notification;
    }

    private Notification createNotificationProblem(final String message, final String link) {
        final Notification notification = new Notification();
        notification.icon = R.drawable.ic_stat_fb;
        final String text = getString(R.string.posting_error);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        final Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, link);
        intent.putExtra(Intent.EXTRA_SUBJECT, message);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        notification.setLatestEventInfo(this, text, text, PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT));
        return notification;
    }

    private synchronized boolean post(final String message, final String link) {
        if (Config.LOGD) Log.d(TAG, "post message=" + message + " link=" + link);
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
