package com.atilim.uni.unipath;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.atilim.uni.unipath.customs.CustomSpinner;
import com.atilim.uni.unipath.extralib.TouchImageView;
import com.atilim.uni.unipath.interfaces.OnSpinnerEventsListenerInterface;
import com.atilim.uni.unipath.utils.AccessPointInfo;
import com.gc.materialdesign.views.ButtonRectangle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Places;
import com.tistory.dwfox.dwrulerviewlibrary.utils.DWUtils;
import com.tistory.dwfox.dwrulerviewlibrary.view.ObservableHorizontalScrollView;
import com.tistory.dwfox.dwrulerviewlibrary.view.ScrollingValuePicker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CalibrateActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int EXT_READ_WRITE_REQUEST_PERMISSION = 1;
    private static final int LOCATION_ACCESS_PERMISSIONS_REQ = 2;
    private int referencePointID = 1;
    private LocationRequest mLocationRequest;
    private boolean isGPSDialogActive = false;
    private static final int LOCATION_RESOLUTION = 1;
    private static final int LOCATION_RESOLUTION_2 = 2;
    private GoogleApiClient mGoogleApiClient;
    private String globalFloorPlanImageName = "engfloorminus2";
    private boolean safeToDrawDot = false;
    private float dotXPos = -1f, dotYPos = -1f;
    private PointF oldTouchValue = new PointF(-1f, -1f);
    private TouchImageView floorPlanCalibrateImage;
    private Bitmap atilimOverview, atilimOverviewOutput;
    private int floorNumber = -2;
    private ScrollingValuePicker scrollingValuePicker;
    private TextView refPointPositionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);

        buildGoogleApiClient();
        mGoogleApiClient.connect();

        floorPlanCalibrateImage = (TouchImageView) findViewById(R.id.floorPlanCalibrateImageView);

        ButtonRectangle startCalibrateButton = (ButtonRectangle) findViewById(R.id.startCalibrateButton);
        startCalibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(CalibrateActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(CalibrateActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        ActivityCompat.requestPermissions(CalibrateActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, EXT_READ_WRITE_REQUEST_PERMISSION);
                    else {
                        saveInformation(referencePointID);
                    }
                } else {
                    saveInformation(referencePointID);
                }
            }
        });

        ButtonRectangle clearDotButton = (ButtonRectangle) findViewById(R.id.clearDotButton);
        clearDotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap floorPlanImage = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(globalFloorPlanImageName, "drawable", getPackageName()));
                floorPlanCalibrateImage.setImageDrawable(new BitmapDrawable(getResources(), floorPlanImage));
                dotXPos = -1f;
                dotYPos = -1f;
            }
        });

        refPointPositionTextView = (TextView) findViewById(R.id.referencePointPositionTextView);

        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        scrollingValuePicker = (ScrollingValuePicker) findViewById(R.id.dwScrollingValuePicker);
        scrollingValuePicker.setMaxValue(1, 100);
        scrollingValuePicker.getScrollView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP)
                    scrollingValuePicker.getScrollView().startScrollerTask();

                return false;
            }
        });
        scrollingValuePicker.setOnScrollChangedListener(new ObservableHorizontalScrollView.OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ObservableHorizontalScrollView observableHorizontalScrollView, int i, int i1) {
                vibrator.vibrate(3);
            }

            @Override
            public void onScrollStopped(int i, int i1) {
                referencePointID = DWUtils.getValueAndScrollItemToCenter(scrollingValuePicker.getScrollView(), i, i1,
                        100, 1, scrollingValuePicker.getViewMultipleSize());
                refPointPositionTextView.setText(String.valueOf(referencePointID));
            }
        });

        final CustomSpinner floorPlanSelectorSpinner = (CustomSpinner) findViewById(R.id.floorPlanSelectorSpinner);
        ArrayAdapter<CharSequence> aa = ArrayAdapter.createFromResource(this, R.array.floor_numbers_array, R.layout.simple_list_item_customized_2);
        floorPlanSelectorSpinner.setAdapter(aa);
        floorPlanSelectorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String floorPlanImageName;
                floorNumber = position - 2;
                if (position >= 2) {
                    floorPlanImageName = "engfloor" + (position - 2);
                } else {
                    floorPlanImageName = "engfloorminus" + Math.abs((position + 1) - 3);
                }

                globalFloorPlanImageName = floorPlanImageName;

                Bitmap floorPlanImage = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(floorPlanImageName, "drawable", getPackageName()));
                floorPlanCalibrateImage.setImageDrawable(new BitmapDrawable(getResources(), floorPlanImage));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Bitmap floorPlanImage = BitmapFactory.decodeResource(getResources(), R.drawable.engfloorminus2);
                floorPlanCalibrateImage.setImageDrawable(new BitmapDrawable(getResources(), floorPlanImage));
            }
        });
        floorPlanSelectorSpinner.setOnSpinnerEventsListenerInterface(new OnSpinnerEventsListenerInterface() {
            @Override
            public void whenSpinnerOpened(Spinner spinner) {

            }

            @Override
            public void whenSpinnerClosed(Spinner spinner) {

            }
        });

        floorPlanCalibrateImage.setOnTouchListener(new View.OnTouchListener() {
            private Matrix inverseMatrix = new Matrix();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.i("Clicked X Pos: ", Float.toString(event.getX()));
                Log.i("Clicked Y Pos: ", Float.toString(event.getY()));

                DisplayMetrics dm = getResources().getDisplayMetrics();
                float x1Raw = event.getX() * (160f / dm.densityDpi);
                float y1Raw = event.getY() * (160f / dm.densityDpi);

                float dpi = dm.density;

                floorPlanCalibrateImage.getImageMatrix().invert(inverseMatrix);
                float[] points = new float[]{event.getX(), event.getY()};
                inverseMatrix.mapPoints(points);
                int actualX = (int) (Math.floor(points[0]) / dpi);
                int actualY = (int) (Math.floor(points[1]) / dpi);

                Log.i("Clicked X O Pos: ", Integer.toString(actualX));
                Log.i("Clicked Y O Pos: ", Integer.toString(actualY));

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    oldTouchValue.set(event.getX(), event.getY());
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    safeToDrawDot = !(Math.abs(event.getX() - oldTouchValue.x) >= 3 ||
                            Math.abs(event.getY() - oldTouchValue.y) >= 3);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (safeToDrawDot) {
                        BitmapFactory.Options myOptions = new BitmapFactory.Options();
                        myOptions.inDither = true;
                        myOptions.inScaled = false;
                        myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        myOptions.inPurgeable = true;

                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(globalFloorPlanImageName, "drawable", getPackageName()), myOptions);
                        Paint paint = new Paint();
                        paint.setAntiAlias(true);
                        paint.setColor(Color.BLUE);

                        Bitmap workingBitmap = Bitmap.createBitmap(bitmap);
                        Bitmap mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);

                        Canvas canvas = new Canvas(mutableBitmap);
                        canvas.drawCircle(actualX, actualY, 7, paint);

                        floorPlanCalibrateImage.setImageBitmap(mutableBitmap);

                        dotXPos = actualX;
                        dotYPos = actualY;

                        workingBitmap.recycle();
                        bitmap.recycle();
                    }

                }

                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        atilimOverview = BitmapFactory.decodeResource(getResources(), R.drawable.atilim_overview);
        if (atilimOverview.getHeight() >= atilimOverview.getWidth()) {
            atilimOverviewOutput = Bitmap.createBitmap(atilimOverview, 0, atilimOverview.getHeight() / 2 - atilimOverview.getWidth() / 2, atilimOverview.getWidth(), atilimOverview.getWidth());
        } else {
            atilimOverviewOutput = Bitmap.createBitmap(atilimOverview, atilimOverview.getWidth() / 2 - atilimOverview.getHeight() / 2, 0, atilimOverview.getHeight(), atilimOverview.getHeight());
        }
        getWindow().getDecorView().setBackground(new BitmapDrawable(getResources(), atilimOverviewOutput));
    }

    private boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case EXT_READ_WRITE_REQUEST_PERMISSION:
                if (grantResults.length > 0 && grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    saveInformation(referencePointID);
                }
                break;
            case LOCATION_ACCESS_PERMISSIONS_REQ:
                if (grantResults.length > 0 && grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    saveInformation(referencePointID);
                }
            default:
                break;
        }
    }

    private void saveInformation(int referencePointID) {
        if (ActivityCompat.checkSelfPermission(CalibrateActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(CalibrateActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> scanResults = wifiManager.getScanResults();
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                turnOnGPS(true);
                Toast.makeText(getApplicationContext(), "GPS isn't active. Please activate GPS.", Toast.LENGTH_SHORT).show();
            } else {
                String rootFolder = null;
                File txtDir = null;
                String txtFileName = "";

                if (scanResults != null && scanResults.size() > 0) {
                    if (isExternalStorageAvailable()) {
                        rootFolder = Environment.getExternalStorageDirectory().toString();
                        txtDir = new File(rootFolder + "/AccessPointsInfo/floorNumber" + Integer.toString(floorNumber).replace("-", "_"));
                        txtFileName = "referencePoint" + referencePointID + ".json";

                    } else {
                        rootFolder = getApplicationContext().getFilesDir().getAbsolutePath();
                        txtDir = new File(rootFolder + "/AccessPointsInfo/floorNumber" + Integer.toString(floorNumber).replace("-", "_"));
                        txtFileName = "referencePoint" + referencePointID + ".json";
                    }
                }

                boolean isSuccess;
                boolean isAPFound = false;
                try {
                    if (txtDir != null && !txtDir.exists()) {
                        isSuccess = txtDir.mkdir();
                        txtDir.setExecutable(true);
                        txtDir.setReadable(true);
                        txtDir.setWritable(true);
                    }
                    else
                        isSuccess = true;

                    if (!Objects.equals(txtFileName, "") && isSuccess) {
                        File txtFile = new File(txtDir, txtFileName);
                        if (!txtFile.exists())
                            isSuccess = txtFile.createNewFile();
                        else
                            isSuccess = txtFile.delete();

                        if (isSuccess && scanResults != null) {
                            FileWriter fileWriter = new FileWriter(txtFile);
                            JSONArray referencePointInfoArray = new JSONArray();

                            for (ScanResult scanResult : scanResults) {
                                if (scanResult.SSID.contains("eduroam") || scanResult.SSID.contains("ATILIM_WIFI") ||
                                        scanResult.SSID.contains("eduroam_kurulum")) {
                                    isAPFound = true;

                                    //Create JSON Content
                                    try {
                                        JSONObject referencePointInfo = new JSONObject();
                                        referencePointInfo.put("SSID", scanResult.SSID);
                                        referencePointInfo.put("BSSID", scanResult.BSSID);
                                        referencePointInfo.put("STRENGTH", scanResult.level);
                                        referencePointInfoArray.put(referencePointInfo);
                                    } catch (JSONException ex) {
                                        ex.printStackTrace();
                                        Toast.makeText(getApplicationContext(), "Cannot save", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            if (!isAPFound) {
                                Toast.makeText(getApplicationContext(), "Cannot Find Eduroam or ATILIM_WIFI network", Toast.LENGTH_SHORT).show();
                            }

                            try {
                                JSONObject referencePointAP = new JSONObject();
                                referencePointAP.put("AccessPoints", referencePointInfoArray);
                                JSONObject referencePoint= new JSONObject();
                                referencePoint.put("ReferencePoint" + referencePointID, referencePointAP);
                                referencePoint.put("FloorNumber", floorNumber);
                                referencePoint.put("XPos", Float.toString(dotXPos));
                                referencePoint.put("YPos", Float.toString(dotYPos));
                                fileWriter.append(referencePoint.toString());
                                Toast.makeText(getApplicationContext(), "Reference point " + referencePointID + " save successful", Toast.LENGTH_SHORT).show();

                                this.referencePointID++;
                                DWUtils.scrollToValue(scrollingValuePicker.getScrollView(), this.referencePointID, 100, 1, scrollingValuePicker.getViewMultipleSize());
                                refPointPositionTextView.setText(String.valueOf(this.referencePointID));

                                Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                Uri fileContentUri = Uri.fromFile(txtDir);
                                mediaScannerIntent.setData(fileContentUri);
                                this.sendBroadcast(mediaScannerIntent);
                                MediaScannerConnection.scanFile(getApplicationContext(), new String[]{ txtDir.toString() }, null, null);

                            } catch (JSONException ex) {
                                Toast.makeText(getApplicationContext(), "Cannot save", Toast.LENGTH_SHORT).show();
                                ex.printStackTrace();
                            }

                            fileWriter.flush();
                            fileWriter.close();
                        } else {
                            Toast.makeText(getApplicationContext(), "Cannot save", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Cannot save", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Cannot save", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            ActivityCompat.requestPermissions(CalibrateActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_ACCESS_PERMISSIONS_REQ);
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

    public synchronized void turnOnGPS(final boolean saveAfter) {
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
                        saveInformation(referencePointID);
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            isGPSDialogActive = true;
                            if (!saveAfter)
                                locationSettingsResult.getStatus().startResolutionForResult(CalibrateActivity.this, LOCATION_RESOLUTION);
                            else
                                locationSettingsResult.getStatus().startResolutionForResult(CalibrateActivity.this, LOCATION_RESOLUTION_2);
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
            case LOCATION_RESOLUTION_2:
                if (resultCode == RESULT_OK) {
                    saveInformation(referencePointID);
                }
                isGPSDialogActive = false;
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(atilimOverview != null)
            atilimOverview.recycle();
        if(atilimOverviewOutput != null)
            atilimOverviewOutput.recycle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(isFinishing()){
            floorPlanCalibrateImage.setImageDrawable(null);
        }
    }
}
