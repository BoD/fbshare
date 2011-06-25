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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class MainActivity extends Activity {
    private static final String TAG = Constants.TAG + MainActivity.class.getSimpleName();

    protected static final int REQUEST_AUTHORIZE = 0;

    private EditText mMessageEditText;
    private Button mOkButton;
    private Button mCancelButton;

    private String mLink;
    private String mMessage;

    private final Facebook mFacebook = new Facebook(Constants.FACEBOOK_APP_ID);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final TextView linkTextView = (TextView) findViewById(R.id.link);
        mMessageEditText = (EditText) findViewById(R.id.message);
        mOkButton = (Button) findViewById(R.id.btn_ok);
        mOkButton.setOnClickListener(mOkOnClickListener);
        mCancelButton = (Button) findViewById(R.id.btn_cancel);
        mCancelButton.setOnClickListener(mCancelOnClickListener);

        mLink = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        mMessage = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);

        linkTextView.setText(Html.fromHtml(getString(R.string.sharing, mLink)));
        mMessageEditText.append(mMessage);
        mMessageEditText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    mOkButton.performClick();
                    return true;
                }
                return false;
            }
        });

        ensureFacebook();
    }

    private void ensureFacebook() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        mFacebook.setAccessToken(sharedPreferences.getString(Constants.PREF_FACEBOOK_TOKEN, null));
        mFacebook.setAccessExpires(sharedPreferences.getLong(Constants.PREF_FACEBOOK_EXPIRES, 0));
        if (!mFacebook.isSessionValid()) {
            mFacebook.authorize(MainActivity.this, new String[] { "publish_stream", "offline_access" }, REQUEST_AUTHORIZE,
                    new DialogListener() {
                        public void onComplete(final Bundle values) {
                            if (Config.LOGD) Log.d(TAG, "onComplete");
                            final Editor editor = sharedPreferences.edit();
                            editor.putString(Constants.PREF_FACEBOOK_TOKEN, mFacebook.getAccessToken());
                            editor.putLong(Constants.PREF_FACEBOOK_EXPIRES, mFacebook.getAccessExpires());
                            editor.commit();
                        }

                        public void onFacebookError(final FacebookError e) {
                            if (Config.LOGD) Log.d(TAG, "onFacebookError", e);
                            Toast.makeText(MainActivity.this, R.string.facebook_error, Toast.LENGTH_LONG).show();
                            finish();
                        }

                        public void onError(final DialogError e) {
                            if (Config.LOGD) Log.d(TAG, "onError", e);
                            Toast.makeText(MainActivity.this, R.string.facebook_error, Toast.LENGTH_LONG).show();
                            finish();
                        }

                        public void onCancel() {
                            if (Config.LOGD) Log.d(TAG, "onCancel");
                            Toast.makeText(MainActivity.this, R.string.facebook_error, Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
        }
    }

    private final OnClickListener mOkOnClickListener = new OnClickListener() {
        public void onClick(final View v) {
            if (Config.LOGD) Log.d(TAG, "mOkOnClickListener");
            startService(new Intent(MainActivity.this, PostService.class).putExtra(PostService.EXTRA_LINK, mLink).putExtra(
                    PostService.EXTRA_MESSAGE, mMessageEditText.getText().toString()));
            finish();
        }
    };

    private final OnClickListener mCancelOnClickListener = new OnClickListener() {
        public void onClick(final View v) {
            if (Config.LOGD) Log.d(TAG, "mCancelOnClickListener");
            finish();
        }
    };

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (Config.LOGD) Log.d(TAG, "onActivityResult");
        switch (requestCode) {
            case REQUEST_AUTHORIZE:
                mFacebook.authorizeCallback(requestCode, resultCode, data);
            break;
        }
    }
}