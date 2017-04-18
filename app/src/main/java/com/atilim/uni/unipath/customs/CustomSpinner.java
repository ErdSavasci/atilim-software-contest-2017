package com.atilim.uni.unipath.customs;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.Spinner;

import com.atilim.uni.unipath.interfaces.OnSpinnerEventsListenerInterface;

/**
 * Created by erd_9 on 19.04.2017.
 */

public class CustomSpinner extends android.support.v7.widget.AppCompatSpinner {
    private OnSpinnerEventsListenerInterface mOnSpinnerEventsListenerInterface;
    private boolean mOpened = false;

    public CustomSpinner(Context context) {
        super(context);
    }

    public CustomSpinner(Context context, int mode) {
        super(context, mode);
    }

    public CustomSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomSpinner(Context context, AttributeSet attrs, int defStyleAttr, int mode) {
        super(context, attrs, defStyleAttr, mode);
    }

    @Override
    public boolean performClick() {
        mOpened = true;
        if(mOnSpinnerEventsListenerInterface != null){
            mOnSpinnerEventsListenerInterface.whenSpinnerOpened(this);
        }

        return super.performClick();
    }

    public void setOnSpinnerEventsListenerInterface(OnSpinnerEventsListenerInterface mOnSpinnerEventsListenerInterface){
        this.mOnSpinnerEventsListenerInterface = mOnSpinnerEventsListenerInterface;
    }

    public boolean hasBeenOpened(){
        return mOpened;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        if(hasBeenOpened() && hasWindowFocus && mOnSpinnerEventsListenerInterface != null){
            mOpened = false;
            mOnSpinnerEventsListenerInterface.whenSpinnerClosed(this);
        }
    }
}
