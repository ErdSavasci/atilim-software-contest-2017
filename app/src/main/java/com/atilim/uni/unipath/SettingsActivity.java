package com.atilim.uni.unipath;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.atilim.uni.unipath.cons.Globals;
import com.atilim.uni.unipath.customs.CustomLogcatTextView;
import com.atilim.uni.unipath.customs.CustomThread;
import com.atilim.uni.unipath.interfaces.ThreadRunInterface;

import java.util.List;

import es.munix.logcat.LogcatListener;
import es.munix.logcat.LogcatTextView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private SharedPreferences sharedPreferences;
    private static final String HEADER_CLICKED = "HEADER_CLICKED";
    private boolean isHeaderClicked = false;
    private static Handler mHandler, mHandler2;
    private static boolean isHandlerActive = false, isHandler2Active = false;
    private static int clickCount = 0;
    private static boolean isDialogActive = false;
    private static AlertDialog.Builder builder;
    private static AlertDialog alertDialog;
    private static SettingsActivity instance;

    private static Context getContext(){
        return instance;
    }

    private static SettingsActivity getInstance(){
        return instance;
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        instance = this;
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            mHandler = new Handler();
            mHandler2 = new Handler();

            final String logcatTextViewText = getActivity().getSharedPreferences("LOGCAT_TEXTVIEW_TEXT", Context.MODE_PRIVATE).getString("LOGCAT_TEXTVIEW_TEXT", "");
            final CustomLogcatTextView logcatTextView = new CustomLogcatTextView(getActivity());
            logcatTextView.setVerticalScrollBarEnabled(true);
            logcatTextView.refreshLogcat();
            logcatTextView.getLogcat(new LogcatListener() {
                @Override
                public void onLogcatCaptured(String s) {
                    logcatTextView.getTextView().setText(Html.fromHtml(Globals.customLogcatTextViewLog.toString() + s));
                }
            });

            final SwitchPreference autostartSwitchPreference = (SwitchPreference) getPreferenceManager().findPreference("gps_autostart_switch");
            autostartSwitchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SharedPreferences sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    try{
                        editor.putBoolean("NAVIGATION_AUTOSTART", (boolean) newValue);
                    }
                    catch(Exception ex){
                        ex.printStackTrace();
                    }
                    editor.apply();

                    return true;
                }
            });

            final Preference aboutPreference = getPreferenceManager().findPreference("about_button");
            aboutPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(builder == null){
                        builder = new AlertDialog.Builder(getActivity());
                        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                isHandlerActive = false;
                                isHandler2Active = false;
                                builder = null;
                            }
                        });
                        alertDialog = builder.create();
                    }

                    final TextView aboutTextView = new TextView(getActivity());
                    aboutTextView.setMaxLines(30);
                    aboutTextView.setMovementMethod(new ScrollingMovementMethod());
                    aboutTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                    aboutTextView.setVerticalScrollBarEnabled(true);
                    aboutTextView.setText(R.string.about_dialog_text);

                    Log.i("CLICK_COUNT", Integer.toString(clickCount));

                    if(clickCount >= 1){
                        mHandler.removeCallbacksAndMessages(null);

                        if(clickCount >= 5){
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mHandler2.removeCallbacksAndMessages(null);
                                    if(logcatTextView.getParent() != null){
                                        ((ViewGroup) logcatTextView.getParent()).removeView(logcatTextView);
                                    }
                                    logcatTextView.refreshLogcat();
                                    builder.setTitle("Debug Application");
                                    builder.setView(logcatTextView);
                                    builder.setCancelable(false);
                                    builder.show();
                                    clickCount = 0;
                                }
                            });
                        }
                    }

                    if(!isHandlerActive){
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                               getActivity().runOnUiThread(new Runnable() {
                                   @Override
                                   public void run() {
                                       if(clickCount < 5){
                                           builder.setTitle("About");
                                           builder.setView(aboutTextView);
                                           builder.setCancelable(false);
                                           builder.show();
                                           isHandlerActive = false;
                                           clickCount = 0;
                                       }
                                   }
                               });
                            }
                        }, 300);
                        isHandlerActive = true;
                    }
                    if(!isHandler2Active){
                        mHandler2.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                clickCount = 0;
                                isHandler2Active = false;
                            }
                        }, 1500);
                        isHandler2Active = true;
                    }

                    clickCount++;

                    return true;
                }
            });

            final Preference copyrightPreference = getPreferenceManager().findPreference("license_button");
            copyrightPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    builder = new AlertDialog.Builder(getActivity());
                    builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    final TextView copyrightTextView = new TextView(getActivity());
                    copyrightTextView.setMaxLines(30);
                    copyrightTextView.setMovementMethod(new ScrollingMovementMethod());
                    copyrightTextView.setGravity(Gravity.CENTER_HORIZONTAL);
                    copyrightTextView.setVerticalScrollBarEnabled(true);
                    copyrightTextView.setText(R.string.copyright_dialog_text);

                    builder.setTitle("Licenses");
                    builder.setView(copyrightTextView);
                    builder.setCancelable(false);
                    builder.show();

                    return true;
                }
            });

            final Preference calibratePreference = getPreferenceManager().findPreference("calibrate_button");
            calibratePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent calibrateIntent = new Intent(getActivity(), CalibrateActivity.class);
                    startActivity(calibrateIntent);

                    return true;
                }
            });


        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        super.onHeaderClick(header, position);
        isHeaderClicked = true;
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(HEADER_CLICKED, isHeaderClicked);
        editor.apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            sharedPreferences = getPreferences(Context.MODE_PRIVATE);
            isHeaderClicked = sharedPreferences.getBoolean(HEADER_CLICKED, false);

            if(!isHeaderClicked) {
                finish();
            }
            if(isHeaderClicked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(HEADER_CLICKED, !isHeaderClicked);
                editor.apply();
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        isHeaderClicked = sharedPreferences.getBoolean(HEADER_CLICKED, false);

        if(isHeaderClicked) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(HEADER_CLICKED, !isHeaderClicked);
            editor.apply();
            finish();
        }
    }
}
