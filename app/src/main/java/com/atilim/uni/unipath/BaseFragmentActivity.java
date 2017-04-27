package com.atilim.uni.unipath;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.InputType;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.atilim.uni.unipath.cons.Globals;
import com.atilim.uni.unipath.customs.CustomLogcatTextView;
import com.atilim.uni.unipath.customs.CustomThread;
import com.atilim.uni.unipath.interfaces.ThreadRunInterface;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.fonts.MaterialCommunityModule;
import com.joanzapata.iconify.fonts.MaterialModule;

/**
 * Created by erd_9 on 9.04.2017.
 */

public class BaseFragmentActivity extends FragmentActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;
    private static final int LOCATION_ACCESS_PERMISSIONS_REQ = 100;
    private LocationManager locationManager;
    private boolean isGPSSwitchChecked = false;
    private SwitchCompat gpsHeaderSwitch;
    private boolean isFirstTime = true;
    private CustomThread gpsStatusThread;
    private boolean isTwiceClicked = false;
    private Handler mHandler;
    private CustomLogcatTextView logcatTextView;

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_main, null);
        FrameLayout frameLayoutMain = (FrameLayout) drawerLayout.findViewById(R.id.frameLayoutMain);
        getLayoutInflater().inflate(layoutResID, frameLayoutMain, true);

        drawerLayout.setFitsSystemWindows(false);

        super.setContentView(drawerLayout);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        drawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                (navigationView.getMenu().findItem(R.id.nav_maps)).setChecked(false);
                (navigationView.getMenu().findItem(R.id.nav_navigate)).setChecked(false);
                (navigationView.getMenu().findItem(R.id.nav_manage)).setChecked(false);
                (navigationView.getMenu().findItem(R.id.nav_send)).setChecked(false);
                (navigationView.getMenu().findItem(R.id.nav_exit)).setChecked(false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        gpsHeaderSwitch = ((SwitchCompat) (navigationView.getHeaderView(0)).findViewById(R.id.gpsSwitch));
        gpsHeaderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isGPSSwitchChecked = isChecked;

                if (ActivityCompat.checkSelfPermission(BaseFragmentActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(BaseFragmentActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(BaseFragmentActivity.this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_ACCESS_PERMISSIONS_REQ);
                } else {
                    if(isChecked){
                        Intent intent = new Intent("TURN_ON_GPS_BY_SWITCH");
                        intent.putExtra("FIRST_TIME", isFirstTime);
                        LocalBroadcastManager.getInstance(BaseFragmentActivity.this).sendBroadcast(intent);
                        isFirstTime = false;
                    }
                }
            }
        });
        gpsHeaderSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!gpsHeaderSwitch.isChecked()){
                    gpsStatusThread.stopRunning(false);
                    Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(onGPS);
                }
            }
        });

        (navigationView.getMenu().findItem(R.id.nav_maps)).setIcon(new IconDrawable(this, FontAwesomeIcons.fa_map).colorRes(R.color.colorAccent).actionBarSize());
        (navigationView.getMenu().findItem(R.id.nav_navigate)).setIcon(new IconDrawable(this, FontAwesomeIcons.fa_street_view).colorRes(R.color.colorAccent).actionBarSize());
        (navigationView.getMenu().findItem(R.id.nav_manage)).setIcon(new IconDrawable(this, FontAwesomeIcons.fa_cogs).colorRes(R.color.colorAccent).actionBarSize());
        (navigationView.getMenu().findItem(R.id.nav_send)).setIcon(new IconDrawable(this, FontAwesomeIcons.fa_paper_plane_o).colorRes(R.color.colorAccent).actionBarSize());
        (navigationView.getMenu().findItem(R.id.nav_exit)).setIcon(new IconDrawable(this, FontAwesomeIcons.fa_sign_out).colorRes(R.color.colorAccent).actionBarSize());

        navigationView.post(new Runnable() {
            @Override
            public void run() {
                roundCorner();
            }
        });
    }

    private void roundCorner(){
        CustomThread customThread = new CustomThread(new ThreadRunInterface() {
            @Override
            public void whenThreadRun() {
                final View navigationHeaderView = navigationView.getHeaderView(0);
                final RelativeLayout navigationHeaderRelativeLayout = (RelativeLayout) navigationHeaderView.findViewById(R.id.navigationHeaderRelativeLayout);

                final ImageView roundCornersImageViewDrawer = (ImageView) navigationHeaderRelativeLayout.findViewById(R.id.roundCornersImageViewDrawer);

                Point size = new Point(roundCornersImageViewDrawer.getWidth(), roundCornersImageViewDrawer.getHeight());

                Drawable drawable = getResources().getDrawable(R.drawable.drawer_layout_rounded_corner);
                Canvas canvas = new Canvas();
                final Bitmap roundCornersBitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
                canvas.setBitmap(roundCornersBitmap);
                drawable.setBounds(0, 0, size.x, size.y);
                drawable.draw(canvas);
                int[] roundCornersBitmapMatrix = new int[roundCornersBitmap.getHeight() * roundCornersBitmap.getWidth()];
                roundCornersBitmap.getPixels(roundCornersBitmapMatrix, 0, roundCornersBitmap.getWidth(), 0, 0, roundCornersBitmap.getWidth(), roundCornersBitmap.getHeight());
                boolean cont = true;
                for (int r = 0; r < roundCornersBitmapMatrix.length; r++){
                    if(roundCornersBitmapMatrix[r] == Color.argb(0, 0, 0, 0) && cont){
                        roundCornersBitmapMatrix[r] = Color.argb(255, 0, 0, 0);
                    }
                    else{
                        cont = false;
                    }

                    if(r % size.x == 0 || r % size.x == size.x - 1){
                        cont = true;
                    }
                }

                final int[] finalRoundCornersBitmapMatrix = roundCornersBitmapMatrix;
                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        roundCornersBitmap.setPixels(finalRoundCornersBitmapMatrix, 0, roundCornersBitmap.getWidth(), 0, 0, roundCornersBitmap.getWidth(), roundCornersBitmap.getHeight());
                        roundCornersImageViewDrawer.setImageBitmap(roundCornersBitmap);
                    }
                };
                mainHandler.post(runnable);
            }
        });
        customThread.start();
    }

    public DrawerLayout getDrawerLayout(){
        return drawerLayout;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION_ACCESS_PERMISSIONS_REQ:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(isGPSSwitchChecked){
                        LocalBroadcastManager.getInstance(BaseFragmentActivity.this).sendBroadcast(new Intent("TURN_ON_GPS_BY_SWITCH"));
                    }
                }
                else if(grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    if(isGPSSwitchChecked){
                        LocalBroadcastManager.getInstance(BaseFragmentActivity.this).sendBroadcast(new Intent("TURN_ON_GPS_BY_SWITCH"));
                    }
                }
                break;
            default:
                break;
        }
    }

    private void startGPSStatusThread(){
        gpsStatusThread = new CustomThread(new ThreadRunInterface() {
            @Override
            public void whenThreadRun() {
                while(gpsStatusThread.getFlag()){
                    if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                gpsHeaderSwitch.setChecked(true);
                            }
                        });
                    }
                    else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                gpsHeaderSwitch.setChecked(false);
                            }
                        });
                    }

                    try{
                        Thread.sleep(1500);
                    }
                    catch(InterruptedException ex){
                        ex.printStackTrace();
                    }
                }
            }
        });
        gpsStatusThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startGPSStatusThread();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Iconify.with(new FontAwesomeModule())
                .with(new MaterialCommunityModule())
                .with(new MaterialModule());

        mHandler = new Handler();

        logcatTextView = new CustomLogcatTextView(getApplicationContext());
        logcatTextView.setVerticalScrollBarEnabled(true);
        logcatTextView.getTextView().setText(Globals.customLogcatTextViewLog);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        item.setChecked(true);

        /*SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("LOGCAT_TEXTVIEW_TEXT", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String logcat = logcatTextView.getTextView().getText().toString();
        editor.putString("LOGCAT_TEXTVIEW_TEXT", logcat);
        editor.apply();*/

        if (id == R.id.nav_exit) {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            startActivity(homeIntent);
            finish();

        } else if (id == R.id.nav_navigate) {
            logcatTextView.refreshLogcat();
            Globals.customLogcatTextViewLog = logcatTextView.getTextView().getText();

            Intent navigateIntent = new Intent(this, NavigationActivity.class);
            navigateIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(navigateIntent);
            finish();

        }
        else if (id == R.id.nav_maps) {
            //DO NOTHING

        } else if (id == R.id.nav_manage) {
            logcatTextView.refreshLogcat();
            Globals.customLogcatTextViewLog = logcatTextView.getTextView().getText();

            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);

        } else if (id == R.id.nav_send) {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle("Report Problem");

            final EditText messageEditText = new EditText(this);
            messageEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

            alertDialog.setView(messageEditText);
            alertDialog.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(messageEditText.getText() != null && messageEditText.getText().length() != 0) {
                        Intent sendEmail = new Intent(Intent.ACTION_SENDTO);
                        sendEmail.setData(Uri.parse("mailto:"));
                        sendEmail.putExtra(Intent.EXTRA_EMAIL, new String[]{"erdsavasci06@gmail.com"});
                        sendEmail.putExtra(Intent.EXTRA_CC, "");
                        sendEmail.putExtra(Intent.EXTRA_SUBJECT, "UniPath Problem");
                        sendEmail.putExtra(Intent.EXTRA_TEXT, messageEditText.getText());

                        try {
                            if(sendEmail.resolveActivity(getPackageManager()) != null){
                                startActivity(Intent.createChooser(sendEmail, "Report Problem by Email"));
                            }
                        } catch (Exception ex) {
                            Toast.makeText(BaseFragmentActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });

            alertDialog.show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        gpsStatusThread.stopRunning(false);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            if(isTwiceClicked){
                isTwiceClicked = false;
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                startActivity(homeIntent);
            }

            isTwiceClicked = true;
            Toast.makeText(getApplicationContext(), "Press again to exit..", Toast.LENGTH_SHORT).show();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isTwiceClicked = false;
                }
            }, 2000);
        }
    }
}
