package com.atilim.uni.unipath.services;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.os.ResultReceiver;
import android.text.TextUtils;
import android.widget.Toast;

import com.atilim.uni.unipath.cons.Constants;
import com.atilim.uni.unipath.cons.Globals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by erd_9 on 8.04.2017.
 */

public class FetchAddressIntentService extends IntentService {
    private String errorMessage = "";
    private ResultReceiver mResultReceiver;
    private boolean isUserInUniversityArea = false;

    public FetchAddressIntentService(){
        super("FetchAddressIntentService");
    }

    public FetchAddressIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        Location mLocation = null;

        if(intent != null)
            mLocation = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);

        if(mLocation != null){
            List<Address> addresses = null;

            mResultReceiver = intent.getParcelableExtra(Constants.RECEIVER);

            try{
                addresses = geocoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 1);
                if(mLocation.getLatitude() >= 39.810745 &&
                        mLocation.getLatitude() <= 39.818478 &&
                        mLocation.getLongitude() >= 32.721043 &&
                        mLocation.getLongitude() <= 32.729119){
                    isUserInUniversityArea = true;
                    Globals.isUserInUniversityArea = true;
                }
                else{
                    isUserInUniversityArea = false;
                    Globals.isUserInUniversityArea = false;
                }
            }
            catch(IOException ex){
                errorMessage = "Unknown Location";
                ex.printStackTrace();
            }
            catch (IllegalArgumentException ex){
                errorMessage = "Unknown Location";
                ex.printStackTrace();
            }
            catch(NullPointerException ex){
                errorMessage = "Unknown Location";
                ex.printStackTrace();
            }

            if(addresses == null || addresses.size() < 1){
                errorMessage = "Unknown Location";

                deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            }
            else{
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<>();

                for (int i = 0; i < address.getMaxAddressLineIndex(); i++){
                    addressFragments.add(address.getAddressLine(i));
                }

                deliverResultToReceiver(Constants.SUCCESS_RESULT, TextUtils.join(System.getProperty("line.separator"), addressFragments));
            }
        }
        else{
            deliverResultToReceiver(Constants.FAILURE_RESULT, "Unknown Location");
        }
    }

    private void deliverResultToReceiver(int resultCode, String address){
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, address);
        bundle.putBoolean(Constants.RESULT_AREA_DATA_KEY, isUserInUniversityArea);

        if(mResultReceiver != null){
            mResultReceiver.send(resultCode, bundle);
        }
        else{
            Toast.makeText(getApplicationContext(), "Location cannot be found", Toast.LENGTH_SHORT).show();
        }
    }
}
