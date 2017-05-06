package com.atilim.uni.unipath;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.atilim.uni.unipath.cons.Constants;
import com.atilim.uni.unipath.cons.Globals;
import com.atilim.uni.unipath.converters.DMS;
import com.atilim.uni.unipath.customs.CustomGyroscopeObserver;
import com.atilim.uni.unipath.customs.CustomPanoramaImageView;
import com.atilim.uni.unipath.customs.CustomSpinner;
import com.atilim.uni.unipath.customs.CustomThread;
import com.atilim.uni.unipath.extralib.TouchImageView;
import com.atilim.uni.unipath.interfaces.AfterAsyncTaskFinishInterface;
import com.atilim.uni.unipath.interfaces.ReceiveResultInterface;
import com.atilim.uni.unipath.interfaces.ThreadRunInterface;
import com.atilim.uni.unipath.receivers.AddressResultReceiver;
import com.atilim.uni.unipath.services.FetchAddressIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Places;
import com.google.api.client.http.GenericUrl;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class NavigationActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private Location mLocationOfUser;
    private GoogleApiClient mGoogleApiClient;
    private String mAddressOutput = "";
    private LocationRequest mLocationRequest;
    private boolean checkingPermission = false;
    private boolean isUserInUniversityArea = false;
    private boolean isGPSOn;
    private boolean isLastTimeGPSOn;
    private boolean isWifiOn;
    private boolean isLastTimeWifiOn;
    private CustomThread customThreadCheckLocation, customThreadCheckGPSState, customThreadCheckWifiState;
    private LocationManager locationManager;
    private TextView notInCampusAlertTextView;
    private TouchImageView floorPlanNavigationImageImageView;
    private long dateTimeMillis = 0L;
    private boolean containsAP = false;
    private boolean matchDBM = false;
    private Integer[] weightsFloor_2;
    private Integer[] weightsFloor_1;
    private Integer[] weightsFloor0;
    private Integer[] weightsFloor1;
    private Integer[] weightsFloor2;
    private Integer[] weightsFloor3;
    private Integer[] weightsFloor4;
    private SparseArray<Integer[]> weightsFloor;
    private int navToss = 0;
    private boolean modifyMapAgain = true;
    private CustomPanoramaImageView atilimOverViewPanoramaImageView;
    private CustomGyroscopeObserver gyroscopeObserver;
    private View contentView;
    private ImageButton drawerButtonNavigation;
    private DisplayMetrics displayMetrics;
    private int deviceWidth = 0, deviceHeight = 0;
    private Bitmap atilimOverview;
    private View contentDecorView;
    private RecyclerView recyclerView;
    private Bitmap atilimOverviewOutput;
    private WifiManager wifiManager;
    private boolean isLocationFound;
    private ImageButton refreshButtonNavigation;
    private CustomSpinner floorPlanSelectorSpinner;
    private int refPointIDReturn = 1;
    private boolean printAlertOnce = true;
    private CustomThread locationDetThread;
    private ProgressBar circularLoadingProgressBarNavigation;
    private PointF refPointPosBestMatch;
    private String globalFloorPlanImageName = "engfloorminus2";
    private Bitmap roundCornersBitmap;
    private boolean externalStoragePermissionGranted = false;

    private static final String LOCATION_ADDRESS_KEY = "LOCATION_ADDRESS";
    private static final int EXT_READ_REQUEST_PERMISSION_REQ = 2;
    private static final int LOCATION_ACCESS_PERMISSIONS_REQ = 1;
    private static final int LOCATION_RESOLUTION = 100;
    private static final float floorHeight = 3.0f;
    private static final double baseAltitude = 700d;

    private BroadcastReceiver receiverMaps = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkingPermission = true;
            if (intent.getAction().equals("TURN_ON_GPS_BY_SWITCH") && !intent.getBooleanExtra("FIRST_TIME", true)) {
                if (ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(NavigationActivity.this, new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_ACCESS_PERMISSIONS_REQ);
                } else {
                    turnOnGPS();
                }
            }
            checkingPermission = false;
        }
    };

    private AddressResultReceiver mResultReceiver = new AddressResultReceiver(new ReceiveResultInterface() {
        @Override
        public void whenReceivedResults(int resultCode, Bundle resultData) {
            if (resultCode == Constants.SUCCESS_RESULT) {
                isUserInUniversityArea = resultData.getBoolean(Constants.RESULT_AREA_DATA_KEY);
                mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            } else {
                Toast.makeText(getApplicationContext(), "Location cannot be found", Toast.LENGTH_SHORT).show();
            }

            Globals.isUserInUniversityArea = isUserInUniversityArea;

            if (wifiManager.isWifiEnabled()) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    if (isUserInUniversityArea && mLocationOfUser != null) {
                        String currentBSSID = wifiManager.getConnectionInfo().getBSSID().toLowerCase();
                        final Integer floorNumber = Globals.routerMacFloorNumberMatches.get(currentBSSID);
                        if (mLocationOfUser.getAccuracy() >= 10 && (floorNumber != null && floorNumber > -3 && floorNumber < 5)) {
                            //FIND FLOOR NUMBER
                            final double altitude = mLocationOfUser.getAltitude();
                            final DMS toBeConvertedLatLng = new DMS(mLocationOfUser.getLatitude(), mLocationOfUser.getLongitude());
                            final DMS convertedLatLng = toBeConvertedLatLng.convertIntoDMS();

                            gyroscopeObserver.unregister();
                            recyclerView.setVisibility(View.GONE);
                            notInCampusAlertTextView.setVisibility(View.GONE);
                            floorPlanSelectorSpinner.setVisibility(View.VISIBLE);
                            floorPlanNavigationImageImageView.setVisibility(View.VISIBLE);

                            refreshButtonNavigation.setBackground(getResources().getDrawable(R.drawable.ic_sync_blue));
                            drawerButtonNavigation.setBackground(getResources().getDrawable(R.drawable.drawer_blue));

                            floorPlanSelectorSpinner.setSelection(floorNumber + 2);

                            if(printAlertOnce) {
                                Toast.makeText(getApplicationContext(), "Location is estimating.. Please wait..", Toast.LENGTH_LONG).show();
                                printAlertOnce = false;
                            }

                            circularLoadingProgressBarNavigation.setVisibility(View.VISIBLE);

                            externalStoragePermissionGranted = false;

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (ContextCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(NavigationActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXT_READ_REQUEST_PERMISSION_REQ);
                                    externalStoragePermissionGranted = false;
                                }
                                else {
                                    externalStoragePermissionGranted = true;
                                }
                            } else {
                                externalStoragePermissionGranted = true;
                            }

                            if(locationDetThread == null) {
                                locationDetThread = new CustomThread(new ThreadRunInterface() {
                                    @Override
                                    public void whenThreadRun() {
                                        SharedPreferences sharedPreferences = getSharedPreferences("SWITCH_PREFS", Context.MODE_PRIVATE);
                                        boolean useAssets = sharedPreferences.getBoolean("READ_EMBEDDED", false);

                                        //FIND POSITION AT CORRESPONDED FLOOR
                                        setWeightsAtFloor(floorNumber, useAssets);
                                        final int refPointIDBestMatch = getBestMatchRefPointID(floorNumber, useAssets);
                                        refPointPosBestMatch = getBestMatchRefPointPos(refPointIDBestMatch, floorNumber, useAssets);

                                        //DRAW BLUE CIRCLE ON CORRESPONDED FLOOR PLAN IMAGE W.R.T COORDINATES
                                        if (refPointPosBestMatch.x != -1 && refPointPosBestMatch.y != -1) {
                                            String floorPlanImageName = floorNumber >= 0 ? ("engfloor" + floorNumber) : ("engfloorminus" + Math.abs(floorNumber));

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(getApplicationContext(), "Location is found", Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                            BitmapFactory.Options myOptions = new BitmapFactory.Options();
                                            myOptions.inDither = true;
                                            myOptions.inScaled = false;
                                            myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                            myOptions.inPurgeable = true;

                                            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(floorPlanImageName, "drawable", getPackageName()), myOptions);
                                            Paint paint = new Paint();
                                            paint.setAntiAlias(true);
                                            paint.setColor(Color.BLUE);

                                            Matrix rotateMatrix = new Matrix();
                                            rotateMatrix.postRotate(90.0f);

                                            Bitmap workingBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, true);
                                            final Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);

                                            float oldX = refPointPosBestMatch.x;
                                            refPointPosBestMatch.x = Math.abs(mutableBitmap.getWidth() - refPointPosBestMatch.y);
                                            refPointPosBestMatch.y = oldX;

                                            Canvas canvas = new Canvas(mutableBitmap);
                                            //canvas.drawCircle(refPointPosBestMatch.x, refPointPosBestMatch.y, 7, paint);

                                            Bitmap locationMarker = BitmapFactory.decodeResource(getResources(), R.drawable.location_marker);
                                            Bitmap scaledLocationMarker = Bitmap.createScaledBitmap(locationMarker, locationMarker.getWidth() / 2, locationMarker.getHeight() / 2, false);
                                            canvas.drawBitmap(scaledLocationMarker, refPointPosBestMatch.x, refPointPosBestMatch.y, null);

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    circularLoadingProgressBarNavigation.setVisibility(View.GONE);
                                                    floorPlanNavigationImageImageView.setImageBitmap(mutableBitmap);
                                                }
                                            });

                                            workingBitmap.recycle();
                                            bitmap.recycle();
                                        } else {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    circularLoadingProgressBarNavigation.setVisibility(View.GONE);
                                                    notInCampusAlertTextView.setText(R.string.location_notfound_text);
                                                    notInCampusAlertTextView.setVisibility(View.VISIBLE);
                                                }
                                            });
                                        }
                                    }
                                });
                                locationDetThread.start();
                            }
                            else{
                                if(!locationDetThread.isAlive()){
                                    locationDetThread = new CustomThread(new ThreadRunInterface() {
                                        @Override
                                        public void whenThreadRun() {
                                            //FIND POSITION AT CORRESPONDED FLOOR

                                            SharedPreferences sharedPreferences = getSharedPreferences("SWITCH_PREFS", Context.MODE_PRIVATE);
                                            boolean useAssets = sharedPreferences.getBoolean("READ_EMBEDDED", false);
                                            setWeightsAtFloor(floorNumber, useAssets);
                                            final int refPointIDBestMatch = getBestMatchRefPointID(floorNumber, useAssets);
                                            refPointPosBestMatch = getBestMatchRefPointPos(refPointIDBestMatch, floorNumber, useAssets);

                                            //DRAW BLUE CIRCLE ON CORRESPONDED FLOOR PLAN IMAGE W.R.T COORDINATES
                                            if (refPointPosBestMatch.x != -1 && refPointPosBestMatch.y != -1) {
                                                String floorPlanImageName = floorNumber >= 0 ? ("engfloor" + floorNumber) : ("engfloorminus" + Math.abs(floorNumber));

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(getApplicationContext(), refPointPosBestMatch.toString(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                                                BitmapFactory.Options myOptions = new BitmapFactory.Options();
                                                myOptions.inDither = true;
                                                myOptions.inScaled = false;
                                                myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                                myOptions.inPurgeable = true;

                                                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(floorPlanImageName, "drawable", getPackageName()), myOptions);
                                                Paint paint = new Paint();
                                                paint.setAntiAlias(true);
                                                paint.setColor(Color.BLUE);

                                                Matrix rotateMatrix = new Matrix();
                                                rotateMatrix.postRotate(90.0f);

                                                Bitmap workingBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, true);
                                                final Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);

                                                float oldX = refPointPosBestMatch.x;
                                                refPointPosBestMatch.x = Math.abs(mutableBitmap.getWidth() - refPointPosBestMatch.y);
                                                refPointPosBestMatch.y = oldX;

                                                Canvas canvas = new Canvas(mutableBitmap);
                                                //canvas.drawCircle(refPointPosBestMatch.x, refPointPosBestMatch.y, 7, paint);

                                                Bitmap locationMarker = BitmapFactory.decodeResource(getResources(), R.drawable.location_marker);
                                                Bitmap scaledLocationMarker = Bitmap.createScaledBitmap(locationMarker, locationMarker.getWidth() / 2, locationMarker.getHeight() / 2, false);
                                                canvas.drawBitmap(scaledLocationMarker, refPointPosBestMatch.x, refPointPosBestMatch.y, null);

                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        circularLoadingProgressBarNavigation.setVisibility(View.GONE);
                                                        floorPlanNavigationImageImageView.setImageBitmap(mutableBitmap);
                                                    }
                                                });

                                                workingBitmap.recycle();
                                                bitmap.recycle();
                                            } else {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        circularLoadingProgressBarNavigation.setVisibility(View.GONE);
                                                        notInCampusAlertTextView.setText(R.string.location_notfound_text);
                                                        notInCampusAlertTextView.setVisibility(View.VISIBLE);
                                                    }
                                                });
                                            }
                                        }
                                    });
                                    locationDetThread.start();
                                }
                            }

                        } else {
                            gyroscopeObserver.register(NavigationActivity.this);
                            recyclerView.setVisibility(View.VISIBLE);
                            floorPlanNavigationImageImageView.setVisibility(View.GONE);
                            notInCampusAlertTextView.setText(R.string.user_is_not_in_department_text);
                            notInCampusAlertTextView.setVisibility(View.VISIBLE);
                            Toast.makeText(getApplicationContext(), "You are not in any department", Toast.LENGTH_SHORT).show();
                        }
                    } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        gyroscopeObserver.register(NavigationActivity.this);
                        recyclerView.setVisibility(View.VISIBLE);
                        floorPlanNavigationImageImageView.setVisibility(View.GONE);
                        notInCampusAlertTextView.setText(R.string.user_is_not_in_university_text);
                        notInCampusAlertTextView.setVisibility(View.VISIBLE);
                        Toast.makeText(getApplicationContext(), "You are not in Atılım University Campus", Toast.LENGTH_SHORT).show();
                    } else {
                        gyroscopeObserver.register(NavigationActivity.this);
                        recyclerView.setVisibility(View.VISIBLE);
                        floorPlanNavigationImageImageView.setVisibility(View.GONE);
                        notInCampusAlertTextView.setText(R.string.gps_not_active_alert);
                        notInCampusAlertTextView.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                gyroscopeObserver.register(NavigationActivity.this);
                recyclerView.setVisibility(View.VISIBLE);
                floorPlanNavigationImageImageView.setVisibility(View.GONE);
                notInCampusAlertTextView.setText(R.string.wifi_not_active_alert);
                notInCampusAlertTextView.setVisibility(View.VISIBLE);
            }
        }
    });

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Bitmap oldImage = ((BitmapDrawable) floorPlanNavigationImageImageView.getDrawable()).getBitmap();
        Matrix rotateMatrix = new Matrix();

        BitmapFactory.Options myOptions = new BitmapFactory.Options();
        myOptions.inDither = true;
        myOptions.inScaled = false;
        myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        myOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(globalFloorPlanImageName, "drawable", getPackageName()), myOptions);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLUE);
        Bitmap mutableBitmap = oldImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && oldImage.getHeight() > oldImage.getWidth()) {
            rotateMatrix.postRotate(-90.0f);
            oldImage = Bitmap.createBitmap(oldImage, 0, 0, oldImage.getWidth(), oldImage.getHeight(), rotateMatrix, true);
            canvas.drawCircle(refPointPosBestMatch.x, refPointPosBestMatch.y, 7, paint);
            floorPlanNavigationImageImageView.setImageDrawable(new BitmapDrawable(getResources(), mutableBitmap));
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && oldImage.getWidth() > oldImage.getHeight()) {
            rotateMatrix.postRotate(90.0f);
            oldImage = Bitmap.createBitmap(oldImage, 0, 0, oldImage.getWidth(), oldImage.getHeight(), rotateMatrix, true);
            canvas.drawCircle(refPointPosBestMatch.y, refPointPosBestMatch.x, 7, paint);
            floorPlanNavigationImageImageView.setImageDrawable(new BitmapDrawable(getResources(), mutableBitmap));
        }

        oldImage.recycle();
        bitmap.recycle();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_bar_navigation);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        deviceWidth = displayMetrics.widthPixels;
        deviceHeight = displayMetrics.heightPixels;

        gyroscopeObserver = new CustomGyroscopeObserver();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerViewPanorama);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new PanoramaAdapter(getApplicationContext()));

        weightsFloor_2 = new Integer[100];
        weightsFloor_1 = new Integer[100];
        weightsFloor0 = new Integer[100];
        weightsFloor1 = new Integer[100];
        weightsFloor2 = new Integer[100];
        weightsFloor3 = new Integer[100];
        weightsFloor4 = new Integer[100];
        for (int i = 0; i < 100; i++) {
            weightsFloor_2[i] = 0;
            weightsFloor_1[i] = 0;
            weightsFloor0[i] = 0;
            weightsFloor1[i] = 0;
            weightsFloor2[i] = 0;
            weightsFloor3[i] = 0;
            weightsFloor4[i] = 0;
        }
        weightsFloor = new SparseArray<>();
        weightsFloor.put(-2, weightsFloor_2);
        weightsFloor.put(-1, weightsFloor_1);
        weightsFloor.put(0, weightsFloor0);
        weightsFloor.put(1, weightsFloor1);
        weightsFloor.put(2, weightsFloor2);
        weightsFloor.put(3, weightsFloor3);
        weightsFloor.put(4, weightsFloor4);

        if (savedInstanceState != null && savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
            mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
        }

        buildGoogleApiClient();

        isLocationFound = false;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        IntentFilter inIntentFilter = new IntentFilter("TURN_ON_GPS_BY_SWITCH");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverMaps, inIntentFilter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkingPermission = true;
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_ACCESS_PERMISSIONS_REQ);
            } else {
                turnOnGPS();
            }
            checkingPermission = false;
        }

        notInCampusAlertTextView = (TextView) findViewById(R.id.notInCampusAlertTextView);
        notInCampusAlertTextView.setVisibility(View.GONE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            notInCampusAlertTextView.setText(R.string.gps_not_active_alert);
            notInCampusAlertTextView.setVisibility(View.VISIBLE);
            if (!wifiManager.isWifiEnabled()) {
                notInCampusAlertTextView.setText(R.string.gps_wifi_not_active_alert);
                notInCampusAlertTextView.setVisibility(View.VISIBLE);
            }
        } else if (!wifiManager.isWifiEnabled()) {
            notInCampusAlertTextView.setText(R.string.wifi_not_active_alert);
            notInCampusAlertTextView.setVisibility(View.VISIBLE);
        }

        floorPlanNavigationImageImageView = (TouchImageView) findViewById(R.id.floorPlanImageNavigation);
        floorPlanNavigationImageImageView.setMinZoom(1.5f);

        refreshButtonNavigation = (ImageButton) findViewById(R.id.refreshButtonNavigation);
        refreshButtonNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && wifiManager.isWifiEnabled()) {
                    if (checkAllConditions()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (ContextCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(NavigationActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXT_READ_REQUEST_PERMISSION_REQ);
                                externalStoragePermissionGranted = false;
                            }
                            else {
                                externalStoragePermissionGranted = true;
                                startLocationIntentService();
                            }
                        } else {
                            externalStoragePermissionGranted = true;
                            startLocationIntentService();
                        }
                    }
                } else {
                    gyroscopeObserver.register(NavigationActivity.this);
                    recyclerView.setVisibility(View.VISIBLE);
                    floorPlanNavigationImageImageView.setVisibility(View.GONE);
                    notInCampusAlertTextView.setText(R.string.gps_not_active_alert);
                    notInCampusAlertTextView.setVisibility(View.VISIBLE);
                }
            }
        });

        final DrawerLayout drawerLayout = super.getDrawerLayout();
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                drawerButtonNavigation.setRotation(slideOffset * 135 * -1);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                navToss = 1;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                navToss = 0;
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        drawerButtonNavigation = (ImageButton) findViewById(R.id.drawerButtonNavigation);
        drawerButtonNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navToss == 0)
                    drawerLayout.openDrawer(Gravity.START);
                else if (drawerLayout.isDrawerOpen(Gravity.START))
                    drawerLayout.closeDrawer(Gravity.START);

                navToss = (navToss + 1) % 2;
            }
        });

        floorPlanSelectorSpinner = (CustomSpinner) findViewById(R.id.floorPlanSelectorSpinnerNavigation);
        ArrayAdapter<CharSequence> aa = ArrayAdapter.createFromResource(this, R.array.floor_numbers_array, R.layout.simple_list_item_customized_2);
        floorPlanSelectorSpinner.setAdapter(aa);
        floorPlanSelectorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String floorPlanImageName = position >= 2 ? ("engfloor" + (position - 2)) : ("engfloorminus" + Math.abs(position - 2));
                Bitmap floorPlanImage = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(floorPlanImageName, "drawable", getPackageName()));
                Matrix rotateMatrix = new Matrix();
                rotateMatrix.postRotate(90.0f);
                floorPlanImage = Bitmap.createBitmap(floorPlanImage, 0, 0, floorPlanImage.getWidth(), floorPlanImage.getHeight(), rotateMatrix, true);
                floorPlanNavigationImageImageView.setImageDrawable(new BitmapDrawable(getResources(), floorPlanImage));
                floorPlanNavigationImageImageView.setZoom(1.5f);

                globalFloorPlanImageName = floorPlanImageName;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        floorPlanSelectorSpinner.setVisibility(View.GONE);

        circularLoadingProgressBarNavigation = (ProgressBar) findViewById(R.id.circularLoadingProgressBarNavigation);
        circularLoadingProgressBarNavigation.setVisibility(View.GONE);

        contentView = findViewById(R.id.recyclerViewPanorama).getRootView();
        contentDecorView = getWindow().getDecorView();
    }

    private void roundCorners() {
        CustomThread customThread = new CustomThread(new ThreadRunInterface() {
            @Override
            public void whenThreadRun() {
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                final ImageView roundCornersImageView = (ImageView) findViewById(R.id.roundCornersImageViewNavigation);
                Drawable drawable = getResources().getDrawable(R.drawable.layout_rounded_corner);
                Canvas canvas = new Canvas();
                roundCornersBitmap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888);
                canvas.setBitmap(roundCornersBitmap);
                drawable.setBounds(0, 0, size.x, size.y);
                drawable.draw(canvas);
                int[] roundCornersBitmapMatrix = new int[roundCornersBitmap.getHeight() * roundCornersBitmap.getWidth()];
                roundCornersBitmap.getPixels(roundCornersBitmapMatrix, 0, roundCornersBitmap.getWidth(), 0, 0, roundCornersBitmap.getWidth(), roundCornersBitmap.getHeight());
                boolean cont = true;
                for (int r = 0; r < roundCornersBitmapMatrix.length; r++) {
                    if (roundCornersBitmapMatrix[r] == Color.argb(0, 0, 0, 0) && cont) {
                        roundCornersBitmapMatrix[r] = Color.argb(255, 0, 0, 0);
                    } else {
                        cont = false;
                    }

                    if (r % size.x == 0 || r % size.x == size.x - 1) {
                        cont = true;
                    }
                }
                for (int r = roundCornersBitmapMatrix.length - 1; r > 0; r--) {
                    if (roundCornersBitmapMatrix[r] == Color.argb(0, 0, 0, 0) && cont) {
                        roundCornersBitmapMatrix[r] = Color.argb(255, 0, 0, 0);
                    } else {
                        cont = false;
                    }

                    if (r % size.x == 0 || r % size.x == size.x - 1) {
                        cont = true;
                    }
                }

                final int[] finalRoundCornersBitmapMatrix = roundCornersBitmapMatrix;
                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        roundCornersBitmap.setPixels(finalRoundCornersBitmapMatrix, 0, roundCornersBitmap.getWidth(), 0, 0, roundCornersBitmap.getWidth(), roundCornersBitmap.getHeight());
                        roundCornersImageView.setImageBitmap(roundCornersBitmap);
                    }
                };
                mainHandler.post(runnable);
            }
        });
        customThread.start();
    }

    class PanoramaAdapter extends RecyclerView.Adapter<PanoramaAdapter.PanoramaViewHolder> {
        private Context context;

        class PanoramaViewHolder extends RecyclerView.ViewHolder {
            private int count = 0;

            PanoramaViewHolder(View itemView) {
                super(itemView);
                atilimOverViewPanoramaImageView = (CustomPanoramaImageView) itemView.findViewById(R.id.atilimOverViewPanoramaImageView);
                gyroscopeObserver.setMaxRotateRadian(Math.PI / 2);
                atilimOverViewPanoramaImageView.setGyroscopeObserver(gyroscopeObserver);
                atilimOverViewPanoramaImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                atilimOverViewPanoramaImageView.setOnPanoramaScrollListener(new CustomPanoramaImageView.OnPanoramaScrollListener() {
                    @Override
                    public void onScrolled(CustomPanoramaImageView view, float offsetProgress) {
                        /*Drawable newDrawable = new BitmapDrawable(getResources(), atilimOverview);
                        Rect bounds = newDrawable.copyBounds();
                        bounds.left = (int)view.getCurrentOffset();
                        bounds.right = (int)view.getCurrentOffset();
                        bounds.top = 0;
                        bounds.bottom = 0;
                        newDrawable.setBounds(bounds);
                        newDrawable.invalidateSelf();
                        contentDecorView.setBackground(newDrawable);*/
                    }
                });
            }
        }

        PanoramaAdapter(Context c) {
            super();
            this.context = c;
        }

        @Override
        public PanoramaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View contentView = inflater.inflate(R.layout.simple_panorama_image, parent, false);
            return new PanoramaViewHolder(contentView);
        }

        @Override
        public void onBindViewHolder(PanoramaViewHolder holder, int position) {
            atilimOverViewPanoramaImageView.setImageResource(R.drawable.atilim_overview);
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    private void setWeightsAtFloor(int floorNumber, boolean useAssets) {
        try {
            String rootFolder;
            File txtDir = null;
            String[] allFilesInFloorFolder;

            if(useAssets){
                allFilesInFloorFolder = getAssets().list("floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/");
            }
            else{
                if (isExternalStorageAvailable()) {
                    rootFolder = Environment.getExternalStorageDirectory().toString();
                    txtDir = new File(rootFolder + "/AccessPointsInfo/floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/");

                    if(!txtDir.exists()){
                        allFilesInFloorFolder = getAssets().list("floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/");
                    }
                    else{
                        allFilesInFloorFolder = txtDir.list();
                    }

                } else {
                    rootFolder = getApplicationContext().getFilesDir().getAbsolutePath();
                    txtDir = new File(rootFolder + "/AccessPointsInfo/floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/");

                    if(!txtDir.exists()){
                        allFilesInFloorFolder = getAssets().list("floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/");
                    }
                    else{
                        allFilesInFloorFolder = txtDir.list();
                    }
                }
            }

            if (((!useAssets && txtDir.canRead() && txtDir.exists()) || useAssets) && allFilesInFloorFolder != null) {
                int iterationCount = allFilesInFloorFolder.length;
                int iterationCountAP;
                int iterationCountCountAP = 0;
                int referencePointNumber;
                int matchCount = 0;

                if (ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        Toast.makeText(getApplicationContext(), "GPS isn't active. Please activate GPS.", Toast.LENGTH_SHORT).show();
                    } else {
                        List<ScanResult> scanResults = wifiManager.getScanResults();

                        if (scanResults != null && scanResults.size() > 0) {
                            while (iterationCount > 0) {
                                referencePointNumber = Integer.parseInt(allFilesInFloorFolder[iterationCount - 1].replace("referencePoint", "").replace(".json", ""));
                                iterationCountAP = getArraySizeOfAPsJSON(referencePointNumber, txtDir, floorNumber, useAssets);
                                iterationCountCountAP = 0;
                                matchCount = 0;
                                while (iterationCountCountAP < iterationCountAP) {
                                    containsAP = false;
                                    matchDBM = false;
                                    String bssid = getValueFromAccessPointJSON(referencePointNumber, iterationCountCountAP, "BSSID", txtDir, floorNumber, useAssets);

                                    for (ScanResult s : scanResults) {
                                        if (s.BSSID.equals(bssid))
                                            containsAP = true;
                                    }

                                    if (containsAP) {
                                        String DBM = getValueFromAccessPointJSON(referencePointNumber, iterationCountCountAP, "STRENGTH", txtDir, floorNumber, useAssets);
                                        int dbm = Integer.parseInt(DBM);

                                        for (ScanResult s : scanResults) {
                                            if (s.BSSID.equals(bssid)) {
                                                if (Math.abs(Math.abs(s.level) - Math.abs(dbm)) <= 2)
                                                    matchDBM = true;
                                            }
                                        }

                                        if (matchDBM) {
                                            matchCount++;
                                        }
                                    }

                                    iterationCountCountAP++;
                                }
                                if (matchCount > 0) {
                                    Integer[] weightsFloorNum = weightsFloor.get(floorNumber);
                                    weightsFloorNum[referencePointNumber - 1] = matchCount;
                                    weightsFloor.remove(floorNumber);
                                    weightsFloor.put(floorNumber, weightsFloorNum);
                                }

                                iterationCount--;
                            }
                        }
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    circularLoadingProgressBarNavigation.setVisibility(View.GONE);
                    notInCampusAlertTextView.setText(R.string.location_notfound_text);
                    notInCampusAlertTextView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private PointF getBestMatchRefPointPos(int referencePointID, int floorNumber, boolean useAssets) {
        try {
            String rootFolder;
            File txtDir = null;
            String txtFileName = "referencePoint" + referencePointID + ".json";
            JSONObject fileJSONObject = null;
            File txtFile = null;
            BufferedReader fileBufferedReader = null;
            FileReader fileReader = null;

            if(useAssets){
                fileBufferedReader = new BufferedReader(new InputStreamReader(getAssets().open("floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/" + txtFileName)));
            }
            else{
                if (isExternalStorageAvailable()) {
                    rootFolder = Environment.getExternalStorageDirectory().toString();
                    txtDir = new File(rootFolder + "/AccessPointsInfo/floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/");

                } else {
                    rootFolder = getApplicationContext().getFilesDir().getAbsolutePath();
                    txtDir = new File(rootFolder + "/AccessPointsInfo/floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/");
                }
                txtFile = new File(txtDir, txtFileName);
                fileReader = new FileReader(txtFile);
                fileBufferedReader = new BufferedReader(fileReader);
            }

            if ((!useAssets && txtDir.exists() && txtFile.exists()) || useAssets) {
                String fileContent;
                StringBuilder stringBuilder = new StringBuilder();
                while ((fileContent = fileBufferedReader.readLine()) != null) {
                    stringBuilder.append(fileContent);
                    stringBuilder.append(System.getProperty("line.separator"));
                }

                try {
                    fileJSONObject = new JSONObject(stringBuilder.toString());
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                fileBufferedReader.close();
                if(fileReader != null)
                    fileReader.close();
            }

            return fileJSONObject != null ? new PointF(Float.parseFloat(fileJSONObject.getString("XPos")),
                    Float.parseFloat(fileJSONObject.getString("YPos"))) : new PointF(-1, -1);
        } catch (Exception ex) {
            ex.printStackTrace();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    circularLoadingProgressBarNavigation.setVisibility(View.GONE);
                    notInCampusAlertTextView.setText(R.string.location_notfound_text);
                    notInCampusAlertTextView.setVisibility(View.VISIBLE);
                }
            });

            return new PointF(-1, -1);
        }
    }

    private int getBestMatchRefPointID(final int floorNumber, boolean useAssets) {
        int maxWeight = 0;
        int maxStrength = Integer.MIN_VALUE;
        int standardCount = 0;
        int refPointID = 1;
        int refPointIDIndex = 1;

        String rootFolder;
        File txtDir = null;

        if(!useAssets){
            if (isExternalStorageAvailable()) {
                rootFolder = Environment.getExternalStorageDirectory().toString();
                txtDir = new File(rootFolder + "/AccessPointsInfo/floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/");

            } else {
                rootFolder = getApplicationContext().getFilesDir().getAbsolutePath();
                txtDir = new File(rootFolder + "/AccessPointsInfo/floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/");
            }
        }

        switch (floorNumber) {
            case -2:
                for (int refPointWeight : weightsFloor.get(-2)) {
                    if (refPointWeight > maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;

                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();
                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, -2, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, -2, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(-2)[standardCount] == maxWeight)
                                        maxStrength = strength;
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    } else if (refPointWeight == maxWeight) {
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();

                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, -2, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, -2, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(-2)[standardCount] == maxWeight) {
                                        maxStrength = strength;
                                        refPointID = refPointIDIndex;
                                    }
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    }
                    refPointIDIndex++;
                }
                refPointIDReturn = refPointID;
                break;
            case -1:
                for (int refPointWeight : weightsFloor.get(-1)) {
                    if (refPointWeight > maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;

                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();
                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, -1, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, -1, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(-1)[standardCount] == maxWeight)
                                        maxStrength = strength;
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    } else if (refPointWeight == maxWeight) {
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();

                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, -1, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, -1, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(-1)[standardCount] == maxWeight) {
                                        maxStrength = strength;
                                        refPointID = refPointIDIndex;
                                    }
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    }
                    refPointIDIndex++;
                }
                refPointIDReturn = refPointID;
                break;
            case 0:
                for (int refPointWeight : weightsFloor.get(0)) {
                    if (refPointWeight > maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;

                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();
                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, 0, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, 0, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(0)[standardCount] == maxWeight)
                                        maxStrength = strength;
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    } else if (refPointWeight == maxWeight) {
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();

                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, 0, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, 0, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(0)[standardCount] == maxWeight) {
                                        maxStrength = strength;
                                        refPointID = refPointIDIndex;
                                    }
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    }
                    refPointIDIndex++;
                }
                refPointIDReturn = refPointID;
                break;
            case 1:
                for (int refPointWeight : weightsFloor.get(1)) {
                    if (refPointWeight > maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;

                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();
                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, 1, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, 1, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(1)[standardCount] == maxWeight)
                                        maxStrength = strength;
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    } else if (refPointWeight == maxWeight) {
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();

                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, 1, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, 1, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(1)[standardCount] == maxWeight) {
                                        maxStrength = strength;
                                        refPointID = refPointIDIndex;
                                    }
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    }
                    refPointIDIndex++;
                }
                refPointIDReturn = refPointID;
                break;
            case 2:
                for (int refPointWeight : weightsFloor.get(2)) {
                    if (refPointWeight > maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;

                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();
                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, 2, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, 2, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(2)[standardCount] == maxWeight)
                                        maxStrength = strength;
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    } else if (refPointWeight == maxWeight) {
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();

                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, 2, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, 2, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(2)[standardCount] == maxWeight) {
                                        maxStrength = strength;
                                        refPointID = refPointIDIndex;
                                    }
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    }
                    refPointIDIndex++;
                }
                refPointIDReturn = refPointID;
                break;
            case 3:
                for (int refPointWeight : weightsFloor.get(3)) {
                    if (refPointWeight > maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;

                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();
                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, 3, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, 3, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(3)[standardCount] == maxWeight)
                                        maxStrength = strength;
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    } else if (refPointWeight == maxWeight) {
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();

                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, 3, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, 3, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(3)[standardCount] == maxWeight) {
                                        maxStrength = strength;
                                        refPointID = refPointIDIndex;
                                    }
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    }
                    refPointIDIndex++;
                }
                refPointIDReturn = refPointID;
                break;
            case 4:
                for (int refPointWeight : weightsFloor.get(4)) {
                    if (refPointWeight > maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;

                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();
                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, 4, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, 4, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(4)[standardCount] == maxWeight)
                                        maxStrength = strength;
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    } else if (refPointWeight == maxWeight) {
                        List<ScanResult> scanResults = wifiManager.getScanResults();
                        String bssid = wifiManager.getConnectionInfo().getBSSID();

                        int arraySize = getArraySizeOfAPsJSON(refPointIDIndex, txtDir, 4, useAssets);
                        while (standardCount < arraySize) {
                            for (ScanResult s : scanResults) {
                                if (s.BSSID.equals(bssid)) {
                                    int strength = Integer.parseInt(getValueFromAccessPointJSON(refPointIDIndex, standardCount, "STRENGTH", txtDir, 4, useAssets));
                                    if (strength > maxStrength && weightsFloor.get(4)[standardCount] == maxWeight) {
                                        maxStrength = strength;
                                        refPointID = refPointIDIndex;
                                    }
                                }
                            }
                            standardCount++;
                        }
                        standardCount = 0;
                    }
                    refPointIDIndex++;
                }
                refPointIDReturn = refPointID;
                break;
            default:
                break;
        }

        return refPointIDReturn;
    }

    private int getArraySizeOfAPsJSON(int referencePointID, File txtDir, int floorNumber, boolean useAssets) {
        try {
            JSONObject fileJSONObject = null;
            String txtFileName = "referencePoint" + referencePointID + ".json";
            File txtFile = null;
            FileReader fileReader = null;
            BufferedReader fileBufferedReader = null;

            if(useAssets){
                fileBufferedReader = new BufferedReader(new InputStreamReader(getAssets().open("floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/" + txtFileName)));
            }
            else if(txtDir != null){
                txtFile = new File(txtDir, txtFileName);
                fileReader = new FileReader(txtFile);
                fileBufferedReader = new BufferedReader(fileReader);
            }

            if ((!useAssets && txtDir != null && txtDir.exists() && txtFile.exists()) || useAssets) {
                String fileContent;
                StringBuilder stringBuilder = new StringBuilder();
                while ((fileContent = fileBufferedReader.readLine()) != null) {
                    stringBuilder.append(fileContent);
                    stringBuilder.append(System.getProperty("line.separator"));
                }

                try {
                    fileJSONObject = new JSONObject(stringBuilder.toString());
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                fileBufferedReader.close();
                if(fileReader != null)
                    fileReader.close();
            }

            return fileJSONObject != null ? (fileJSONObject.getJSONObject("ReferencePoint" + referencePointID).getJSONArray("AccessPoints").length()) : 0;
        } catch (Exception ex) {
            ex.printStackTrace();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    circularLoadingProgressBarNavigation.setVisibility(View.GONE);
                    notInCampusAlertTextView.setText(R.string.location_notfound_text);
                    notInCampusAlertTextView.setVisibility(View.VISIBLE);
                }
            });

            return 0;
        }
    }

    private String getValueFromAccessPointJSON(int referencePointID, int arrayIndex, String key, File txtDir, int floorNumber, boolean useAssets) {
        JSONObject fileJSONObject = null;
        try {
            String txtFileName = "referencePoint" + referencePointID + ".json";
            File txtFile = null;
            FileReader fileReader = null;
            BufferedReader fileBufferedReader = null;

            if(useAssets){
                fileBufferedReader = new BufferedReader(new InputStreamReader(getAssets().open("floorNumber" + Integer.toString(floorNumber).replace("-", "_") + "/" + txtFileName)));
            }
            else if(txtDir != null){
                txtFile = new File(txtDir, txtFileName);
                fileReader = new FileReader(txtFile);
                fileBufferedReader = new BufferedReader(fileReader);
            }

            if ((!useAssets && txtDir != null && txtDir.exists() && txtFile.exists()) || useAssets) {
                String fileContent;
                StringBuilder stringBuilder = new StringBuilder();
                while ((fileContent = fileBufferedReader.readLine()) != null) {
                    stringBuilder.append(fileContent);
                    stringBuilder.append(System.getProperty("line.separator"));
                }

                try {
                    fileJSONObject = new JSONObject(stringBuilder.toString());
                    //Toast.makeText(getApplicationContext(), fileJSONObject.getJSONObject("ReferencePoint" + referencePointID).getJSONArray("AccessPoints").getJSONObject(arrayIndex).getString(key), Toast.LENGTH_SHORT).show();
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                fileBufferedReader.close();
                if(fileReader != null)
                    fileReader.close();
            }

            return fileJSONObject != null ? (fileJSONObject.getJSONObject("ReferencePoint" + referencePointID).getJSONArray("AccessPoints").getJSONObject(arrayIndex).getString(key)) : null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
    }

    public boolean isGPSActive() {
        return isGPSOn;
    }

    private void startLocationIntentService() {
        Intent locationIntent = new Intent(this, FetchAddressIntentService.class);
        locationIntent.putExtra(Constants.RECEIVER, mResultReceiver);
        locationIntent.putExtra(Constants.LOCATION_DATA_EXTRA, mLocationOfUser);
        startService(locationIntent);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (!checkingPermission) {
            checkingPermission = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mLocationOfUser = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                } else {
                    requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_ACCESS_PERMISSIONS_REQ);
                }
            }
            checkingPermission = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case LOCATION_ACCESS_PERMISSIONS_REQ:
                checkingPermission = false;
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    turnOnGPS();
                } else if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    turnOnGPS();
                }
                break;
            case EXT_READ_REQUEST_PERMISSION_REQ:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    externalStoragePermissionGranted = true;
                    startLocationIntentService();
                }
                break;
            default:
                break;
        }
    }

    private boolean checkAllConditions() {
        return mLocationOfUser != null && Geocoder.isPresent() && mGoogleApiClient.isConnected();
    }

    private void turnOnGPS() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(30 * 1000)
                .setFastestInterval(5 * 1000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                int statusCode = locationSettingsResult.getStatus().getStatusCode();

                switch (statusCode) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            locationSettingsResult.getStatus().startResolutionForResult(NavigationActivity.this, LOCATION_RESOLUTION);
                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOCATION_RESOLUTION:
                if (resultCode == RESULT_OK) {

                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        isLocationFound = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mGoogleApiClient == null) {
            buildGoogleApiClient();
        }

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mGoogleApiClient == null) {
            buildGoogleApiClient();
        }

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        isLastTimeGPSOn = false;
        isLastTimeWifiOn = false;

        initializeCheckLocationThread();
        initializeCheckGPSStatusThread();
        initializeCheckWifiStatusThread();
        startCheckWifiStatusThread();
        dateTimeMillis = System.currentTimeMillis();
        gyroscopeObserver.register(this);

        roundCorners();

        try{
            atilimOverview = BitmapFactory.decodeResource(getResources(), R.drawable.atilim_overview);

            if (!Globals.isUserInUniversityArea) {
                if (atilimOverview.getHeight() >= atilimOverview.getWidth()) {
                    atilimOverviewOutput = Bitmap.createBitmap(atilimOverview, 0, atilimOverview.getHeight() / 2 - atilimOverview.getWidth() / 2, atilimOverview.getWidth(), atilimOverview.getWidth());
                } else {
                    atilimOverviewOutput = Bitmap.createBitmap(atilimOverview, atilimOverview.getWidth() / 2 - atilimOverview.getHeight() / 2, 0, atilimOverview.getHeight(), atilimOverview.getHeight());
                }
                getWindow().getDecorView().setBackground(new BitmapDrawable(getResources(), atilimOverviewOutput));
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        gyroscopeObserver.unregister();
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
            }
            if (customThreadCheckLocation != null)
                customThreadCheckLocation.stopRunning(false);
            if (customThreadCheckGPSState != null)
                customThreadCheckGPSState.stopRunning(false);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            mGoogleApiClient = null;
            if (customThreadCheckLocation != null)
                customThreadCheckLocation.stopRunning(false);
            if (customThreadCheckGPSState != null)
                customThreadCheckGPSState.stopRunning(false);

            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverMaps);
        }
    }

    private void initializeCheckGPSStatusThread() {
        customThreadCheckGPSState = new CustomThread(new ThreadRunInterface() {
            @Override
            public void whenThreadRun() {
                while (customThreadCheckGPSState.getFlag()) {
                    isGPSOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    if ((isGPSOn && isGPSOn != isLastTimeGPSOn) || ((System.currentTimeMillis() - dateTimeMillis >= (20 * 1000)) && isGPSOn && isUserInUniversityArea) || (isGPSOn && !isLocationFound)) {
                        dateTimeMillis = System.currentTimeMillis();

                        if (customThreadCheckLocation != null) {
                            customThreadCheckLocation.stopRunning(false);
                        }

                        if (customThreadCheckLocation != null && !customThreadCheckLocation.isAlive()) {
                            isLocationFound = true;
                            initializeCheckLocationThread();
                            startCheckLocationThread();
                        }

                        modifyMapAgain = true;
                    } else if (!isGPSOn && modifyMapAgain) {
                        modifyMapAgain = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                    gyroscopeObserver.register(NavigationActivity.this);
                                    recyclerView.setVisibility(View.VISIBLE);
                                    floorPlanNavigationImageImageView.setVisibility(View.GONE);
                                    notInCampusAlertTextView.setText(R.string.gps_not_active_alert);
                                    notInCampusAlertTextView.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    }

                    isLastTimeGPSOn = isGPSOn;
                }
            }
        });
    }

    private void startCheckGPSStatusThread() {
        customThreadCheckGPSState.start();
    }

    private void initializeCheckWifiStatusThread() {
        customThreadCheckWifiState = new CustomThread(new ThreadRunInterface() {
            @Override
            public void whenThreadRun() {
                while (customThreadCheckWifiState.getFlag()) {
                    isWifiOn = wifiManager.isWifiEnabled();
                    if ((isWifiOn && isWifiOn != isLastTimeWifiOn)) {
                        if (customThreadCheckGPSState != null) {
                            customThreadCheckGPSState.stopRunning(false);
                        }

                        if (customThreadCheckGPSState != null && !customThreadCheckGPSState.isAlive()) {
                            initializeCheckGPSStatusThread();
                            startCheckGPSStatusThread();
                        }

                        modifyMapAgain = true;
                    } else if (!isGPSOn && modifyMapAgain) {
                        modifyMapAgain = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                    gyroscopeObserver.register(NavigationActivity.this);
                                    recyclerView.setVisibility(View.VISIBLE);
                                    floorPlanNavigationImageImageView.setVisibility(View.GONE);
                                    notInCampusAlertTextView.setText(R.string.wifi_not_active_alert);
                                    notInCampusAlertTextView.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    }

                    isLastTimeWifiOn = isWifiOn;
                }
            }
        });
    }

    private void startCheckWifiStatusThread() {
        customThreadCheckWifiState.start();
    }

    private void initializeCheckLocationThread() {
        customThreadCheckLocation = new CustomThread(new ThreadRunInterface() {
            @Override
            public void whenThreadRun() {
                while (mLocationOfUser == null && customThreadCheckLocation.getFlag()) {
                    if (mGoogleApiClient == null) {
                        buildGoogleApiClient();
                    }

                    if (ContextCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mLocationOfUser = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    }
                }
                while (!checkAllConditions() && customThreadCheckLocation.getFlag()) ;
                if (checkAllConditions()) {
                    customThreadCheckLocation.stopRunning(false);
                    startLocationIntentService();
                }
            }
        });
    }

    private void startCheckLocationThread() {
        customThreadCheckLocation.start();
    }

    private class RetrieveResponseFromURL extends AsyncTask<URL, Integer, String> {
        private DMS dms;
        private AfterAsyncTaskFinishInterface delegate = null;

        RetrieveResponseFromURL(DMS dms) {
            super();
            this.dms = dms;
        }

        @Override
        protected String doInBackground(URL... params) {
            String fullResponse = null;
            String urlParameters = "";

            if (params[0].getHost().equals("veloroutes.org")) {
                urlParameters = "location=" + mLocationOfUser.getLatitude() + "," + mLocationOfUser.getLongitude() +
                        "&units=m";
            } else if (params[0].getHost().equals("jules.unavco.org")) {
                urlParameters = "lat=" + dms.getLatDegrees() +
                        "&lat_m=" + dms.getLatMinutes() +
                        "&lat_s=" + dms.getLatSeconds() +
                        "&lon=" + dms.getLonDegrees() +
                        "&lon_m=" + dms.getLonMinutes() +
                        "&lon_s=" + dms.getLonSeconds() +
                        "&gpsheight=" + dms.getElevationHeight();
            }

            try {
                byte[] postDataByte = urlParameters.getBytes(StandardCharsets.UTF_8);
                int postDataLength = postDataByte.length;
                GenericUrl geoidUrl = new GenericUrl(params[0]);
                URL geoidURL = geoidUrl.toURL();
                HttpURLConnection connection = (HttpURLConnection) geoidURL.openConnection();
                connection.setDoOutput(true);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("charset", "utf-8");
                connection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                connection.setUseCaches(false);
                try (DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream())) {
                    dataOutputStream.write(postDataByte);

                    dataOutputStream.flush();
                    dataOutputStream.close();
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    StringBuilder sBuilder = new StringBuilder();
                    String resp;
                    while ((resp = reader.readLine()) != null) {
                        sBuilder.append(resp);
                        sBuilder.append(System.getProperty("line.separator"));
                    }

                    fullResponse = sBuilder.toString();
                    reader.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return fullResponse;
        }

        public void setDelegate(AfterAsyncTaskFinishInterface d) {
            this.delegate = d;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            delegate.afterAsyncTaskFinish(s);
        }
    }
}
