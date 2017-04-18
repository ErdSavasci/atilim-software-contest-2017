package com.atilim.uni.unipath;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.atilim.uni.unipath.cons.Constants;
import com.atilim.uni.unipath.cons.Globals;
import com.atilim.uni.unipath.converters.DMS;
import com.atilim.uni.unipath.customs.CustomGyroscopeObserver;
import com.atilim.uni.unipath.customs.CustomPanoramaImageView;
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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import im.delight.android.location.SimpleLocation;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesWithFallbackProvider;

public class NavigationActivity extends BaseActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private Location mLocationOfUser;
    private GoogleApiClient mGoogleApiClient;
    private String mAddressOutput = "";
    private LocationRequest mLocationRequest;
    private boolean checkingPermission = false;
    private boolean isUserInUniversityArea;
    private boolean isGPSOn;
    private boolean isLastTimeGPSOn;
    private CustomThread customThreadCheckLocation, customThreadCheckGPSState;
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

    private static final String LOCATION_ADDRESS_KEY = "LOCATION_ADDRESS";
    private static final int LOCATION_ACCESS_PERMISSIONS_REQ = 1;
    private static final int LOCATION_RESOLUTION = 100;
    private static final float floorHeight = 3.0f;
    private static final double baseAltitude = 700d;

    private BroadcastReceiver receiverMaps = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkingPermission = true;
            if (intent.getAction().equals("TURN_ON_GPS_BY_SWITCH") && !intent.getBooleanExtra("FIRST_TIME", true)) {
                if (ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(NavigationActivity.this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
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

            if (isUserInUniversityArea && mLocationOfUser != null) {
                if(mLocationOfUser.getAccuracy() > 20) {
                    //FIND FLOOR NUMBER
                    final double altitude = mLocationOfUser.getAltitude();

                    final DMS toBeConvertedLatLng = new DMS(mLocationOfUser.getLatitude(), mLocationOfUser.getLongitude());
                    final DMS convertedLatLng = toBeConvertedLatLng.convertIntoDMS();

                    if (altitude == 0) {
                        RetrieveResponseFromURL retrieveResponseFromURL = new RetrieveResponseFromURL(convertedLatLng);
                        retrieveResponseFromURL.setDelegate(new AfterAsyncTaskFinishInterface() {
                            @Override
                            public void afterAsyncTaskFinish(String result) {
                                try {
                                    String res = result;
                                    res = res.substring(res.lastIndexOf("<span style=\"font-size:20px\">") + 29);
                                    res = res.substring(0, res.indexOf("</span> meters<br/>"));
                                    final double elevationHeight = Double.parseDouble(res);
                                    convertedLatLng.setElevationHeight(elevationHeight);
                                    final RetrieveResponseFromURL retrieveResponseFromURL = new RetrieveResponseFromURL(convertedLatLng);
                                    retrieveResponseFromURL.setDelegate(new AfterAsyncTaskFinishInterface() {
                                        @Override
                                        public void afterAsyncTaskFinish(String result) {
                                            String res = result;
                                            res = res.substring(res.lastIndexOf("Geoid height = ") + 15);
                                            res = res.substring(0, res.indexOf("(meters)") - 1);
                                            res = res.replace("\n", "");
                                            double ellipsoidalHeight = Double.parseDouble(res);
                                            double topographicHeight = elevationHeight - ellipsoidalHeight;
                                            Toast.makeText(getApplicationContext(), Double.toString(topographicHeight), Toast.LENGTH_LONG).show();

                                            int floorNumber = ((int) Math.ceil((altitude - topographicHeight) / floorHeight));
                                            if (floorNumber > -3 && floorNumber < 5) {
                                                String floorPlanImageName = floorNumber >= 0 ? ("engfloor" + floorNumber) : ("engfloorminus" + floorNumber);
                                                Bitmap floorPlanImage = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(floorPlanImageName, "drawable", getPackageName()));

                                                //FIND POSITION AT CORRESPONDED FLOOR
                                                setWeightsAtFloor(floorNumber);
                                                int refPointIDBestMatch = getBestMatchRefPointID(floorNumber);
                                                PointF refPointPosBestMatch = getBestMatchRefPointPos(refPointIDBestMatch, floorNumber);
                                                Toast.makeText(getApplicationContext(), refPointPosBestMatch.toString(), Toast.LENGTH_SHORT).show();

                                                gyroscopeObserver.unregister();
                                                recyclerView.setVisibility(View.INVISIBLE);
                                                notInCampusAlertTextView.setVisibility(View.GONE);
                                                floorPlanNavigationImageImageView.setVisibility(View.VISIBLE);
                                                floorPlanNavigationImageImageView.setImageDrawable(new BitmapDrawable(getResources(), floorPlanImage));
                                            }
                                        }
                                    });
                                    GenericUrl geoidUrl = new GenericUrl("http://jules.unavco.org/Geoid/Geoid/");
                                    retrieveResponseFromURL.execute(geoidUrl.toURL());
                                } catch (Exception ex) {
                                    gyroscopeObserver.register(getApplicationContext());
                                    recyclerView.setVisibility(View.VISIBLE);
                                    notInCampusAlertTextView.setText(R.string.gps_error_text);
                                    notInCampusAlertTextView.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                        GenericUrl elevationUrl = new GenericUrl("http://veloroutes.org/elevation/");
                        retrieveResponseFromURL.execute(elevationUrl.toURL());
                    } else {
                        final RetrieveResponseFromURL retrieveResponseFromURL = new RetrieveResponseFromURL(convertedLatLng);
                        retrieveResponseFromURL.setDelegate(new AfterAsyncTaskFinishInterface() {
                            @Override
                            public void afterAsyncTaskFinish(String result) {
                                int floorNumber = ((int) Math.ceil((altitude - baseAltitude) / floorHeight));
                                String floorPlanImageName = floorNumber >= 0 ? ("engfloor" + floorNumber) : ("engfloorminus" + floorNumber);
                                Bitmap floorPlanImage = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(floorPlanImageName, "drawable", getPackageName()));

                                floorPlanNavigationImageImageView.setVisibility(View.VISIBLE);
                                floorPlanNavigationImageImageView.setImageDrawable(new BitmapDrawable(getResources(), floorPlanImage));

                                //FIND POSITION AT CORRESPONDED FLOOR
                                setWeightsAtFloor(floorNumber);
                                int refPointIDBestMatch = getBestMatchRefPointID(floorNumber);
                                PointF refPointPosBestMatch = getBestMatchRefPointPos(refPointIDBestMatch, floorNumber);
                                Toast.makeText(getApplicationContext(), refPointPosBestMatch.toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        GenericUrl geoidUrl = new GenericUrl("http://jules.unavco.org/Geoid/Geoid");
                        retrieveResponseFromURL.execute(geoidUrl.toURL());
                    }
                }
                else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    gyroscopeObserver.register(NavigationActivity.this);
                    recyclerView.setVisibility(View.VISIBLE);
                    floorPlanNavigationImageImageView.setVisibility(View.GONE);
                    notInCampusAlertTextView.setText(R.string.user_is_not_in_university_text);
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
            }
        }
    });

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

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        IntentFilter inIntentFilter = new IntentFilter("TURN_ON_GPS_BY_SWITCH");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverMaps, inIntentFilter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkingPermission = true;
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_ACCESS_PERMISSIONS_REQ);
            } else {
                turnOnGPS();
            }
            checkingPermission = false;
        }

        atilimOverview = BitmapFactory.decodeResource(getResources(), R.drawable.atilim_overview);

        if (!Globals.isUserInUniversityArea) {
            Bitmap atilimOverviewOutput;
            if (atilimOverview.getHeight() >= atilimOverview.getWidth()) {
                atilimOverviewOutput = Bitmap.createBitmap(atilimOverview, 0, atilimOverview.getHeight() / 2 - atilimOverview.getWidth() / 2, atilimOverview.getWidth(), atilimOverview.getWidth());
            } else {
                atilimOverviewOutput = Bitmap.createBitmap(atilimOverview, atilimOverview.getWidth() / 2 - atilimOverview.getHeight() / 2, 0, atilimOverview.getHeight(), atilimOverview.getHeight());
            }
            //getWindow().getDecorView().setBackground(new BitmapDrawable(getResources(), atilimOverviewOutput));
        }

        notInCampusAlertTextView = (TextView) findViewById(R.id.notInCampusAlertTextView);
        notInCampusAlertTextView.setVisibility(View.GONE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            notInCampusAlertTextView.setText(R.string.gps_not_active_alert);
            notInCampusAlertTextView.setVisibility(View.VISIBLE);
        }

        floorPlanNavigationImageImageView = (TouchImageView) findViewById(R.id.floorPlanImageNavigation);

        ImageButton refreshButtonNavigation = (ImageButton) findViewById(R.id.refreshButtonNavigation);
        refreshButtonNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    if (checkAllConditions()) {
                        startLocationIntentService();
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
                Log.i("SlideOffset", Float.toString(slideOffset));
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

        contentView = findViewById(R.id.recyclerViewPanorama).getRootView();
        contentDecorView = getWindow().getDecorView();
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

    private void setWeightsAtFloor(int floorNumber) {
        try {
            String rootFolder = Environment.getExternalStorageDirectory().toString();
            File txtDir = new File(rootFolder + "/AccessPointsInfo/floorNumber" + Integer.toString(floorNumber).replace("-", "_"));
            File[] allFilesInFloorFolder = txtDir.listFiles();
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
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    List<ScanResult> scanResults = wifiManager.getScanResults();

                    if (scanResults != null && scanResults.size() > 0) {
                        while (iterationCount > 0) {
                            referencePointNumber = Integer.parseInt(allFilesInFloorFolder[iterationCount].getName().replace("referencePoint", ""));
                            iterationCountAP = getArraySizeOfAPsJSON(referencePointNumber, txtDir);
                            while (iterationCountCountAP < iterationCountAP) {
                                containsAP = false;
                                matchDBM = false;
                                String bssid = getValueFromAccessPointJSON(referencePointNumber, iterationCountCountAP, "BSSID", txtDir);

                                for (ScanResult s : scanResults) {
                                    if (s.BSSID.equals(bssid))
                                        containsAP = true;
                                }

                                if (containsAP) {
                                    String DBM = getValueFromAccessPointJSON(referencePointNumber, iterationCountCountAP, "DBM", txtDir);
                                    int dbm = Integer.parseInt(DBM);

                                    for (ScanResult s : scanResults) {
                                        if (s.BSSID.equals(bssid)) {
                                            if (Math.abs(Math.abs(s.level) - Math.abs(dbm)) <= 3)
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
        } catch (Exception ex) {
            ex.printStackTrace();
            gyroscopeObserver.register(getApplicationContext());
            recyclerView.setVisibility(View.VISIBLE);
            floorPlanNavigationImageImageView.setVisibility(View.GONE);
            notInCampusAlertTextView.setText(R.string.gps_error_text);
            notInCampusAlertTextView.setVisibility(View.VISIBLE);
        }
    }

    private PointF getBestMatchRefPointPos(int referencePointID, int floorNumber) {
        try {
            String rootFolder = Environment.getExternalStorageDirectory().toString();
            File txtDir = new File(rootFolder + "/AccessPointsInfo/floorNumber" + Integer.toString(floorNumber).replace("-", "_"));
            String txtFileName = "referencePoint" + referencePointID + ".json";
            JSONObject fileJSONObject = null;

            File txtFile = new File(txtDir, txtFileName);
            if (txtDir.exists() && txtFile.exists()) {
                FileReader fileReader = new FileReader(txtFile);
                BufferedReader fileBufferedReader = new BufferedReader(fileReader);
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
                fileReader.close();
            }

            return fileJSONObject != null ? new PointF(Float.parseFloat(fileJSONObject.getJSONObject("ReferencePoint" + referencePointID).getString("XPos")),
                    Float.parseFloat(fileJSONObject.getJSONObject("ReferencePoint" + referencePointID).getString("YPos"))) : new PointF(-1, -1);
        } catch (Exception ex) {
            ex.printStackTrace();

            gyroscopeObserver.register(getApplicationContext());
            recyclerView.setVisibility(View.VISIBLE);
            floorPlanNavigationImageImageView.setVisibility(View.GONE);
            notInCampusAlertTextView.setText(R.string.gps_error_text);
            notInCampusAlertTextView.setVisibility(View.VISIBLE);

            return new PointF(-1, -1);
        }
    }

    private int getBestMatchRefPointID(int floorNumber) {
        int maxWeight = Integer.MAX_VALUE;
        int refPointID = 0;
        int refPointIDIndex = 0;

        switch (floorNumber) {
            case -2:
                for (int refPointWeight : weightsFloor_2) {
                    if (refPointWeight < maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;
                    }
                    refPointIDIndex++;
                }
                break;
            case -1:
                for (int refPointWeight : weightsFloor_1) {
                    if (refPointWeight < maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;
                    }
                    refPointIDIndex++;
                }
                break;
            case 0:
                for (int refPointWeight : weightsFloor0) {
                    if (refPointWeight < maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;
                    }
                    refPointIDIndex++;
                }
                break;
            case 1:
                for (int refPointWeight : weightsFloor1) {
                    if (refPointWeight < maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;
                    }
                    refPointIDIndex++;
                }
                break;
            case 2:
                for (int refPointWeight : weightsFloor2) {
                    if (refPointWeight < maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;
                    }
                    refPointIDIndex++;
                }
                break;
            case 3:
                for (int refPointWeight : weightsFloor3) {
                    if (refPointWeight < maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;
                    }
                    refPointIDIndex++;
                }
                break;
            case 4:
                for (int refPointWeight : weightsFloor4) {
                    if (refPointWeight < maxWeight) {
                        maxWeight = refPointWeight;
                        refPointID = refPointIDIndex;
                    }
                    refPointIDIndex++;
                }
                break;
            default:
                break;
        }

        return refPointID;
    }

    private int getArraySizeOfAPsJSON(int referencePointID, File txtDir) {
        try {
            JSONObject fileJSONObject = null;
            String txtFileName = "referencePoint" + referencePointID + ".json";

            File txtFile = new File(txtDir, txtFileName);
            if (txtDir.exists() && txtFile.exists()) {
                FileReader fileReader = new FileReader(txtFile);
                BufferedReader fileBufferedReader = new BufferedReader(fileReader);
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
                fileReader.close();
            }

            return fileJSONObject != null ? (fileJSONObject.getJSONObject("ReferencePoint" + referencePointID).getJSONArray("AccessPoints").length()) : 0;
        } catch (Exception ex) {
            ex.printStackTrace();

            gyroscopeObserver.register(getApplicationContext());
            recyclerView.setVisibility(View.VISIBLE);
            floorPlanNavigationImageImageView.setVisibility(View.GONE);
            notInCampusAlertTextView.setText(R.string.gps_error_text);
            notInCampusAlertTextView.setVisibility(View.VISIBLE);

            return 0;
        }
    }

    private String getValueFromAccessPointJSON(int referencePointID, int arrayIndex, String key, File txtDir) {
        JSONObject fileJSONObject = null;
        try {
            String txtFileName = "referencePoint" + referencePointID + ".json";
            File txtFile = new File(txtDir, txtFileName);
            FileReader fileReader = new FileReader(txtFile);
            BufferedReader fileBufferedReader = new BufferedReader(fileReader);
            String fileContent;
            StringBuilder stringBuilder = new StringBuilder();
            while ((fileContent = fileBufferedReader.readLine()) != null) {
                stringBuilder.append(fileContent);
                stringBuilder.append(System.getProperty("line.separator"));
            }

            try {
                fileJSONObject = new JSONObject(stringBuilder.toString());
                Toast.makeText(getApplicationContext(), fileJSONObject.getJSONObject("ReferencePoint" + referencePointID).getJSONArray("AccessPoints").getJSONObject(arrayIndex).getString(key), Toast.LENGTH_SHORT).show();
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            fileBufferedReader.close();
            fileReader.close();

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
                            Manifest.permission.ACCESS_COARSE_LOCATION,
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
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    turnOnGPS();
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

        initializeCheckLocationThread();
        initializeCheckGPSStatusThread();
        startCheckGPSStatusThread();
        dateTimeMillis = System.currentTimeMillis();
        gyroscopeObserver.register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        gyroscopeObserver.unregister();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        if (customThreadCheckLocation != null)
            customThreadCheckLocation.stopRunning(false);
        if (customThreadCheckGPSState != null)
            customThreadCheckGPSState.stopRunning(false);
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
                    if ((isGPSOn && isGPSOn != isLastTimeGPSOn) || (System.currentTimeMillis() - dateTimeMillis >= (60 * 1000))) {
                        if (customThreadCheckLocation != null) {
                            customThreadCheckLocation.stopRunning(false);
                        }

                        if (customThreadCheckLocation != null && !customThreadCheckLocation.isAlive()) {
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

                    dateTimeMillis = System.currentTimeMillis();
                    isLastTimeGPSOn = isGPSOn;
                }
            }
        });
    }

    private void startCheckGPSStatusThread() {
        customThreadCheckGPSState.start();
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
