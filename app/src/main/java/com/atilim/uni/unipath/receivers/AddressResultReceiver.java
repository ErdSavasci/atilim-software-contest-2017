package com.atilim.uni.unipath.receivers;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;

import com.atilim.uni.unipath.interfaces.ReceiveResultInterface;

/**
 * Created by erd_9 on 8.04.2017.
 */

public class AddressResultReceiver extends ResultReceiver {
    private ReceiveResultInterface rri;

    AddressResultReceiver(Handler handler) {
        super(handler);
    }

    public AddressResultReceiver(ReceiveResultInterface rri){
        super(new Handler());
        this.rri = rri;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        rri.whenReceivedResults(resultCode, resultData);
    }
}
