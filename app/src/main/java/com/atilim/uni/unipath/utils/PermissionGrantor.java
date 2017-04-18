package com.atilim.uni.unipath.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by erd_9 on 8.04.2017.
 */

public class PermissionGrantor {
    public PermissionGrantor(){

    }

    public static void hasPermission(Activity activity, String[] permissions, int requestCode){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if(ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED){
                    grantPermission(activity, permissions, requestCode);
                }
            }
        }
    }

    public static void grantPermission(Activity activity, String[] permissions, int requestCode){
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }
}
