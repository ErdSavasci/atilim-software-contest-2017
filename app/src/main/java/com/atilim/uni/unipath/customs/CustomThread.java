package com.atilim.uni.unipath.customs;

import com.atilim.uni.unipath.interfaces.ThreadRunInterface;

/**
 * Created by erd_9 on 8.04.2017.
 */

public class CustomThread extends Thread {
    ThreadRunInterface tri;
    boolean continueRequest = true;

    CustomThread(String name) {
        super(name);
    }

    public CustomThread(ThreadRunInterface tri){
        this.tri = tri;
    }

    @Override
    public void run() {
        tri.whenThreadRun();
    }

    public void stopRunning(boolean continueRequest){
        this.continueRequest = continueRequest;
    }

    public boolean getFlag() { return continueRequest; }
}
