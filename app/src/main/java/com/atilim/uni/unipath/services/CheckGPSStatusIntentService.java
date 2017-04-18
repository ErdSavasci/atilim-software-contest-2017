package com.atilim.uni.unipath.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.atilim.uni.unipath.interfaces.ThreadRunInterface;

/**
 * Created by erd_9 on 10.04.2017.
 */

public class CheckGPSStatusIntentService extends IntentService {
    private ThreadRunInterface tri;

    public CheckGPSStatusIntentService(ThreadRunInterface tri){
        super("CHECK_GPS_SERVICE");
        this.tri = tri;
    }

    public CheckGPSStatusIntentService(String name) {
        super(name);
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        tri.whenThreadRun();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
}
