package com.atilim.uni.unipath;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.fonts.MaterialCommunityModule;
import com.joanzapata.iconify.fonts.MaterialModule;

import java.io.ByteArrayOutputStream;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class SplashActivity extends AppCompatActivity {
    private GifImageView loadingImageGifView;
    private GifDrawable gifDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Iconify.with(new FontAwesomeModule())
                .with(new MaterialCommunityModule())
                .with(new MaterialModule());

        loadingImageGifView = (GifImageView) findViewById(R.id.loadingImageGifView);
        try{
            gifDrawable = new GifDrawable(getResources(), R.drawable.atilim_flip_logo);
            loadingImageGifView.setImageDrawable(gifDrawable);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                    startMapsActivity();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();

        gifDrawable.start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        gifDrawable.stop();
    }

    private void startMapsActivity() {
        Intent mapsIntent = new Intent(this, MapsActivity.class);
        mapsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mapsIntent);
        finish();
    }
}
