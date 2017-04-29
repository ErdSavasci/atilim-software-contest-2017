package com.atilim.uni.unipath;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.atilim.uni.unipath.cons.Constants;
import com.atilim.uni.unipath.cons.Globals;
import com.atilim.uni.unipath.customs.CustomThread;
import com.atilim.uni.unipath.interfaces.DirectionsAPIServiceInterface;
import com.atilim.uni.unipath.interfaces.DirectionsAPITrafficServiceInterface;
import com.atilim.uni.unipath.interfaces.ReceiveResultInterface;
import com.atilim.uni.unipath.interfaces.ThreadRunInterface;
import com.atilim.uni.unipath.json.DirectionsResult;
import com.atilim.uni.unipath.json.Route;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Maps;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsActivity extends BaseFragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private GoogleMap mMap;
    private Location mLocationOfUser = null;
    private GoogleApiClient mGoogleApiClient;
    private String mAddressOutput = "";
    private LocationRequest mLocationRequest;
    private boolean checkingPermission = false;
    private boolean isUserInUniversityArea = false;
    private CameraPosition mCameraPosition;
    private int routeOption = 0;
    private boolean isMapReady;
    private boolean isGPSDialogActive = false;
    private ArrayList<Polyline> polylinesList;
    private LocationManager locationManager;
    private CustomThread customThreadCheckLocation, customThreadCheckGPSState;
    private boolean isGPSOn = false;
    private boolean isLastTimeGPSOn = false;
    private int navToss = 0;
    private boolean modifyMapAgain = true;
    private ImageButton drawerButton;
    private boolean isLocationFound;
    private SupportMapFragment mapFragment;
    private long dateTimeMillis = 0L;
    private boolean printAddressOnce = true;
    private boolean isRefreshButtonClicked = true;

    private static final String LOCATION_ADDRESS_KEY = "LOCATION_ADDRESS";
    private static final String KEY_CAMERA_POSITION = "CAMERA_POSITION";
    private static final String KEY_LOCATION = "LOCATION";
    private static final int LOCATION_ACCESS_PERMISSIONS_REQ = 1;
    private static final int LOCATION_RESOLUTION = 100;
    private static float DEFAULT_ZOOM = 10.0f;

    private BroadcastReceiver receiverMaps = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkingPermission = true;
            if (intent.getAction().equals("TURN_ON_GPS_BY_SWITCH") && intent.getBooleanExtra("USER_CLICKED", false)) {
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MapsActivity.this, new String[]{
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
                if (!isUserInUniversityArea && printAddressOnce) {
                    Toast.makeText(getApplicationContext(), mAddressOutput, Toast.LENGTH_SHORT).show();
                    printAddressOnce = false;
                }
            } else if(printAddressOnce) {
                Toast.makeText(getApplicationContext(), "Location cannot be found", Toast.LENGTH_SHORT).show();
            }

            Globals.isUserInUniversityArea = isUserInUniversityArea;

            SharedPreferences sharedPreferences = getSharedPreferences("SWITCH_PREFS", Context.MODE_PRIVATE);
            boolean autoStart = sharedPreferences.getBoolean("NAVIGATION_AUTOSTART", true);

            if (isUserInUniversityArea && (Globals.openNavigationActivityOnce || autoStart)) {
                Globals.openNavigationActivityOnce = false;
                Intent navigateIntent = new Intent(MapsActivity.this, NavigationActivity.class);
                navigateIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(navigateIntent);
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isMapReady = false;
        polylinesList = new ArrayList<>();
        setContentView(R.layout.activity_maps);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buildGoogleApiClient();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        IntentFilter inIntentFilter = new IntentFilter("TURN_ON_GPS_BY_SWITCH");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiverMaps, inIntentFilter);

        //getWindow().getDecorView().setBackgroundColor(Color.parseColor("#F3F3F3"));
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        if (savedInstanceState != null && savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
            mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
            mLocationOfUser = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

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

        isLocationFound = false;

        final DrawerLayout drawerLayout = super.getDrawerLayout();
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                drawerButton.setRotation(slideOffset * 135 * -1);
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

        drawerButton = (ImageButton) findViewById(R.id.drawerButton);
        drawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navToss == 0)
                    drawerLayout.openDrawer(Gravity.START);
                else if (drawerLayout.isDrawerOpen(Gravity.START))
                    drawerLayout.closeDrawer(Gravity.START);

                navToss = (navToss + 1) % 2;
            }
        });

        ImageButton refreshButton = (ImageButton) findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkAllConditions()) {
                    isRefreshButtonClicked = true;
                    printAddressOnce = true;
                    startLocationIntentService();
                    planRouteOrPutMarker();
                }
                else if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    turnOnGPS();
                }
            }
        });

        Spinner routeOptionsSpinner = (Spinner) findViewById(R.id.routeOptionSpinner);
        ArrayAdapter<CharSequence> aa = ArrayAdapter.createFromResource(this, R.array.route_options_array, R.layout.simple_list_item_customized);
        routeOptionsSpinner.setAdapter(aa);

        routeOptionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                routeOption = position;

                if (mLocationOfUser != null) {
                    if (position == 0) { //Shortest
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLocationOfUser.getLatitude(), mLocationOfUser.getLongitude()), DEFAULT_ZOOM));
                        //LocationAsyncTask locationAsyncTask = new LocationAsyncTask();
                        //locationAsyncTask.execute(new LatLng(mLocationOfUser.getLatitude(), mLocationOfUser.getLongitude()));

                        planRouteOrPutMarker();
                    } else { //Fastest
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLocationOfUser.getLatitude(), mLocationOfUser.getLongitude()), DEFAULT_ZOOM));
                        //LocationAsyncTask locationAsyncTask = new LocationAsyncTask();
                        //locationAsyncTask.execute(new LatLng(mLocationOfUser.getLatitude(), mLocationOfUser.getLongitude()));

                        planRouteOrPutMarker();
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (mLocationOfUser != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLocationOfUser.getLatitude(), mLocationOfUser.getLongitude()), DEFAULT_ZOOM));
                    //LocationAsyncTask locationAsyncTask = new LocationAsyncTask();
                    //locationAsyncTask.execute(new LatLng(mLocationOfUser.getLatitude(), mLocationOfUser.getLongitude()));

                    planRouteOrPutMarker();
                }
            }
        });
    }

    private void roundCorners(){
        CustomThread customThread = new CustomThread(new ThreadRunInterface() {
            @Override
            public void whenThreadRun() {
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);

                final ImageView roundCornersImageView = (ImageView) findViewById(R.id.roundCornersImageView);
                Drawable drawable = getResources().getDrawable(R.drawable.layout_rounded_corner);
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
                for (int r = roundCornersBitmapMatrix.length - 1; r > 0; r--){
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
                        roundCornersImageView.setImageBitmap(roundCornersBitmap);
                    }
                };
                mainHandler.post(runnable);
            }
        });
        customThread.start();
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

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
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

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        isLastTimeGPSOn = false;

        initializeCheckLocationThread();
        initializeCheckGPSStatusThread();
        startCheckGPSStatusThread();

        dateTimeMillis = System.currentTimeMillis();

        roundCorners();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!isGPSDialogActive) {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                mGoogleApiClient.disconnect();
                if (customThreadCheckLocation != null)
                    customThreadCheckLocation.stopRunning(false);
                if (customThreadCheckGPSState != null)
                    customThreadCheckGPSState.stopRunning(false);
            }
        }
    }

    private void initializeCheckGPSStatusThread() {
        customThreadCheckGPSState = new CustomThread(new ThreadRunInterface() {
            @Override
            public void whenThreadRun() {
                while (customThreadCheckGPSState.getFlag()) {
                    isGPSOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    if ((isGPSOn && isGPSOn != isLastTimeGPSOn) || ((System.currentTimeMillis() - dateTimeMillis >= (10 * 1000)) && isGPSOn) || (isGPSOn && !isLocationFound)) {
                        dateTimeMillis = System.currentTimeMillis();

                        if (customThreadCheckLocation != null) {
                            customThreadCheckLocation.stopRunning(false);
                        }

                        if (customThreadCheckLocation != null && !customThreadCheckLocation.isAlive()) {
                            initializeCheckLocationThread();
                            startCheckLocationThread();
                            isLocationFound = true;
                        }

                        modifyMapAgain = true;
                    } else if (!isGPSOn && modifyMapAgain) {
                        modifyMapAgain = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                        && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    mMap.setMyLocationEnabled(false);
                                }
                                mMap.getUiSettings().setMyLocationButtonEnabled(false);
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

    private void initializeCheckLocationThread() {
        customThreadCheckLocation = new CustomThread(new ThreadRunInterface() {
            @Override
            public void whenThreadRun() {
                while (mLocationOfUser == null && customThreadCheckLocation.getFlag()) {
                    if (mGoogleApiClient == null) {
                        buildGoogleApiClient();
                    }

                    if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mLocationOfUser = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    }
                }
                while (!checkAllConditions() && customThreadCheckLocation.getFlag()) ;
                if (checkAllConditions()) {
                    customThreadCheckLocation.stopRunning(false);
                    startLocationIntentService();
                    Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
                    Runnable mainRunnable = new Runnable() {
                        @Override
                        public void run() {
                            planRouteOrPutMarker();
                            if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                mMap.setMyLocationEnabled(true);
                            }
                            mMap.getUiSettings().setMyLocationButtonEnabled(true);
                        }
                    };
                    mainHandler.post(mainRunnable);
                }
            }
        });
    }

    private void startCheckLocationThread() {
        customThreadCheckLocation.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing() && !isGPSDialogActive) {
            mGoogleApiClient = null;
            if (customThreadCheckLocation != null)
                customThreadCheckLocation.stopRunning(false);
            if (customThreadCheckGPSState != null)
                customThreadCheckGPSState.stopRunning(false);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverMaps);
        }
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
                }
                else if(grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    turnOnGPS();
                }
                break;
            default:
                break;
        }
    }

    private boolean checkAllConditions() {
        return isMapReady && mLocationOfUser != null && Geocoder.isPresent() && mGoogleApiClient.isConnected();
    }

    public synchronized void turnOnGPS() {
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
                        if (mMap != null) {
                            if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                mMap.setMyLocationEnabled(true);
                                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                            }
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            isGPSDialogActive = true;
                            locationSettingsResult.getStatus().startResolutionForResult(MapsActivity.this, LOCATION_RESOLUTION);
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
                isGPSDialogActive = false;
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        if (mMap != null)
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
        outState.putParcelable(KEY_LOCATION, mLocationOfUser);
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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
        mMap.setPadding(0, 80, 0, 0);
        isMapReady = true;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.setMyLocationEnabled(true);
        } else {
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.setMyLocationEnabled(false);
            mLocationOfUser = null;
        }

        getWindow().getDecorView().setBackgroundColor(Color.parseColor("#000000"));
        planRouteOrPutMarker();
    }

    private void planRouteOrPutMarker() {
        try {
            if (mLocationOfUser != null) {
                if(isRefreshButtonClicked) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLocationOfUser.getLatitude(), mLocationOfUser.getLongitude()), DEFAULT_ZOOM));
                    isRefreshButtonClicked = false;
                }
                //LocationAsyncTask locationAsyncTask = new LocationAsyncTask();
                //locationAsyncTask.execute(new LatLng(mLocationOfUser.getLatitude(), mLocationOfUser.getLongitude()));

                Retrofit retrofit = new Retrofit.Builder().baseUrl("https://maps.googleapis.com/").addConverterFactory(GsonConverterFactory.create()).build();
                Call<DirectionsResult> directionsResultCall;
                DirectionsAPITrafficServiceInterface directionsAPITrafficServiceInterface;
                DirectionsAPIServiceInterface directionsAPIServiceInterface;
                if (routeOption == 0) {
                    directionsAPIServiceInterface = retrofit.create(DirectionsAPIServiceInterface.class);
                    directionsResultCall = directionsAPIServiceInterface.getDirections(Double.toString(mLocationOfUser.getLatitude()) + "," + Double.toString(mLocationOfUser.getLongitude()),
                            "39.815929,32.723868",
                            getResources().getString(R.string.google_maps_not_restricted_key),
                            "true");
                } else {
                    directionsAPITrafficServiceInterface = retrofit.create(DirectionsAPITrafficServiceInterface.class);
                    directionsResultCall = directionsAPITrafficServiceInterface.getDirections(Double.toString(mLocationOfUser.getLatitude()) + "," + Double.toString(mLocationOfUser.getLongitude()),
                            "39.815929,32.723868",
                            getResources().getString(R.string.google_maps_not_restricted_key),
                            "true");
                }

                directionsResultCall.enqueue(new Callback<DirectionsResult>() {
                    @Override
                    public void onResponse(Call<DirectionsResult> call, Response<DirectionsResult> response) {
                        try{
                            DirectionsResult directionsResult = response.body();
                            long shortest = Long.MAX_VALUE, fastest = Long.MAX_VALUE;
                            int shortestIndex = -1, fastestIndex = -1;

                            for (Route r : directionsResult.routes) {
                                if (r.legs.get(0).distance.value < shortest) {
                                    shortest = r.legs.get(0).distance.value;
                                    shortestIndex++;
                                }
                                if (r.legs.get(0).duration.value < fastest) {
                                    fastest = r.legs.get(0).duration.value;
                                    fastestIndex++;
                                }
                            }

                            String encodedPoints = "";
                            if (directionsResult.routes.size() > 0) {
                                if (routeOption == 0) { //Shortest
                                    encodedPoints = directionsResult.routes.get(shortestIndex).overviewPolyLine.points;
                                } else { //Fastest
                                    encodedPoints = directionsResult.routes.get(fastestIndex).overviewPolyLine.points;
                                }
                            }

                            List<LatLng> latLngs = PolyUtil.decode(encodedPoints);
                            if (polylinesList.size() > 0) {
                                polylinesList.get(0).remove();
                                polylinesList.clear();
                            }

                            PolylineOptions polylineOptions = new PolylineOptions();
                            polylineOptions.width(7);
                            polylineOptions.color(Color.RED);
                            for (LatLng latlng : latLngs) {
                                polylineOptions.add(latlng);
                            }

                            polylinesList.add(mMap.addPolyline(polylineOptions));
                        }
                        catch(Exception ex){
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<DirectionsResult> call, Throwable t) {

                    }
                });

                Log.i("ROUTE_PLAN: ", "STARTED");
            } else if (mCameraPosition != null) {
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
            } else {
                // Add a marker in Atilim University and move the camera
                LatLng atilimLocation = new LatLng(39.815929, 32.723868);
                mMap.addMarker(new MarkerOptions().position(atilimLocation).title("Atılım University"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(atilimLocation, DEFAULT_ZOOM));
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private class LocationAsyncTask extends AsyncTask<LatLng, Integer, List<LatLng>> {
        @Override
        protected List<LatLng> doInBackground(LatLng... params) {
            try {
                HttpRequestFactory httpRequestFactory = AndroidHttp.newCompatibleTransport().createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest httpRequest) throws IOException {
                        httpRequest.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });

                GenericUrl directionUrl = new GenericUrl("https://maps.googleapis.com/maps/api/directions/json");
                directionUrl.put("origin", params[0].latitude + "," + params[0].longitude);
                directionUrl.put("destination", "39.815929,32.723868");
                directionUrl.put("key", getResources().getString(R.string.google_maps_not_restricted_key));
                directionUrl.put("alternatives", "true");

                if (routeOption == 1) {
                    directionUrl.put("traffic_model", "best_guess");
                    directionUrl.put("departure_time", "now");
                }

                HttpRequest httpRequest = httpRequestFactory.buildGetRequest(directionUrl);
                URL url = directionUrl.toURL();
                HttpResponse httpResponse = httpRequest.execute();
                DirectionsResult directionsResult = httpResponse.parseAs(DirectionsResult.class);

                long shortest = Long.MAX_VALUE, fastest = Long.MAX_VALUE;
                int shortestIndex = -1, fastestIndex = -1;

                for (Route r : directionsResult.routes) {
                    if (r.legs.get(0).distance.value < shortest) {
                        shortest = r.legs.get(0).distance.value;
                        shortestIndex++;
                    }
                    if (r.legs.get(0).duration.value < fastest) {
                        fastest = r.legs.get(0).duration.value;
                        fastestIndex++;
                    }
                }

                String encodedPoints = "";
                if (directionsResult.routes.size() > 0) {
                    if (routeOption == 0) { //Shortest
                        encodedPoints = directionsResult.routes.get(shortestIndex).overviewPolyLine.points;
                    } else { //Fastest
                        encodedPoints = directionsResult.routes.get(fastestIndex).overviewPolyLine.points;
                    }
                }

                return PolyUtil.decode(encodedPoints);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(List<LatLng> latLngs) {
            try {
                if (polylinesList.size() > 0) {
                    polylinesList.get(0).remove();
                    polylinesList.clear();
                }
                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.width(7);
                polylineOptions.color(Color.RED);
                for (LatLng latlng : latLngs) {
                    polylineOptions.add(latlng);
                }
                polylinesList.add(mMap.addPolyline(polylineOptions));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
