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
package org.jraf.android.fbshare.app.main;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import org.jraf.android.fbshare.Config;
import org.jraf.android.fbshare.Constants;
import org.jraf.android.fbshare.R;
import org.jraf.android.fbshare.app.about.AboutActivity;
import org.jraf.android.fbshare.app.service.PostService;

import com.facebook.Session;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class MainActivity extends Activity {
    private static final String TAG = Constants.TAG + MainActivity.class.getSimpleName();

    protected static final int REQUEST_AUTHORIZE = 0;

    private EditText mEdtMessage;
    private Button mBtnOk;
    private Button mBtnCancel;

    private String mLink;
    private String mMessage;

    private final Facebook mFacebook = new Facebook(Constants.FACEBOOK_APP_ID);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final TextView linkTextView = (TextView) findViewById(R.id.txtLink);
        mEdtMessage = (EditText) findViewById(R.id.edtMessage);
        mBtnOk = (Button) findViewById(R.id.btnOk);
        mBtnOk.setOnClickListener(mOkOnClickListener);
        mBtnCancel = (Button) findViewById(R.id.btnCancel);
        mBtnCancel.setOnClickListener(mCancelOnClickListener);

        mLink = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        mMessage = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);

        if (mLink == null || !(mLink.startsWith("http://") || mLink.startsWith("https://"))) {
            Toast.makeText(MainActivity.this, R.string.not_a_link, Toast.LENGTH_LONG).show();
            finish();

        }

        linkTextView.setText(Html.fromHtml(getString(R.string.sharing, mLink)));
        if (mMessage != null) {
            mEdtMessage.append(mMessage);
        }
        mEdtMessage.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    mBtnOk.performClick();
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
            mFacebook.authorize(MainActivity.this, new String[] { "publish_stream", "offline_access" }, REQUEST_AUTHORIZE, new DialogListener() {
                @Override
                public void onComplete(final Bundle values) {
                    if (Config.LOGD) Log.d(TAG, "onComplete");
                    final Editor editor = sharedPreferences.edit();
                    editor.putString(Constants.PREF_FACEBOOK_TOKEN, mFacebook.getAccessToken());
                    editor.putLong(Constants.PREF_FACEBOOK_EXPIRES, mFacebook.getAccessExpires());
                    editor.commit();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, R.string.facebook_ok, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onFacebookError(final FacebookError e) {
                    if (Config.LOGD) Log.d(TAG, "onFacebookError", e);
                    Toast.makeText(MainActivity.this, R.string.facebook_error, Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(final DialogError e) {
                    if (Config.LOGD) Log.d(TAG, "onError", e);
                    Toast.makeText(MainActivity.this, R.string.facebook_error, Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onCancel() {
                    if (Config.LOGD) Log.d(TAG, "onCancel");
                    Toast.makeText(MainActivity.this, R.string.facebook_error, Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }
    }

    private final OnClickListener mOkOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (Config.LOGD) Log.d(TAG, "mOkOnClickListener");
            startService(new Intent(MainActivity.this, PostService.class).putExtra(PostService.EXTRA_LINK, mLink).putExtra(PostService.EXTRA_MESSAGE,
                    mEdtMessage.getText().toString()));
            finish();
        }
    };

    private final OnClickListener mCancelOnClickListener = new OnClickListener() {
        @Override
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


    /*
     * Menu.
     */

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                new AsyncTask<Void, Void, Boolean>() {
                    private ProgressDialog mProgressDialog;

                    @Override
                    protected void onPreExecute() {
                        mProgressDialog = new ProgressDialog(MainActivity.this);
                        mProgressDialog.setMessage(getString(R.string.dialog_wait_message));
                        mProgressDialog.setCancelable(false);
                        mProgressDialog.show();
                    }

                    @Override
                    protected Boolean doInBackground(final Void... params) {
                        try {
                            // Initializes a static context that is used in the logout method...
                            new Session(MainActivity.this);
                            // Logout
                            mFacebook.logout(MainActivity.this);
                            return true;
                        } catch (final Exception e) {
                            Log.e(TAG, "logout", e);
                            return false;
                        }
                    }

                    @Override
                    protected void onPostExecute(final Boolean result) {
                        mProgressDialog.dismiss();
                        if (!result) {
                            Toast.makeText(MainActivity.this, R.string.facebook_error_logout, Toast.LENGTH_LONG).show();
                        } else {
                            final Editor editor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();
                            editor.remove(Constants.PREF_FACEBOOK_TOKEN);
                            editor.remove(Constants.PREF_FACEBOOK_EXPIRES);
                            editor.commit();
                            ensureFacebook();
                        }
                    }
                }.execute();
                break;

            case R.id.menu_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
