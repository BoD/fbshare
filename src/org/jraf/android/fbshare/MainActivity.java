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
import android.os.Bundle;
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

public class MainActivity extends Activity {
    private static final String TAG = Constants.TAG + MainActivity.class.getSimpleName();

    private EditText mTextEditText;
    private Button mOkButton;
    private Button mCancelButton;

    private String mUrl;
    private String mText;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final TextView urlTextView = (TextView) findViewById(R.id.url);
        mTextEditText = (EditText) findViewById(R.id.text);
        mOkButton = (Button) findViewById(R.id.btn_ok);
        mOkButton.setOnClickListener(mOkOnClickListener);
        mCancelButton = (Button) findViewById(R.id.btn_cancel);
        mCancelButton.setOnClickListener(mCancelOnClickListener);

        mUrl = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        mText = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);

        urlTextView.setText(Html.fromHtml(getString(R.string.sharing, mUrl)));
        mTextEditText.append(mText);
        mTextEditText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    mOkButton.performClick();
                    return true;
                }
                return false;
            }
        });
    }

    private final OnClickListener mOkOnClickListener = new OnClickListener() {
        public void onClick(final View v) {
            if (Config.LOGD) Log.d(TAG, "mOkOnClickListener");
        }
    };

    private final OnClickListener mCancelOnClickListener = new OnClickListener() {
        public void onClick(final View v) {
            if (Config.LOGD) Log.d(TAG, "mCancelOnClickListener");
            finish();
        }
    };
}