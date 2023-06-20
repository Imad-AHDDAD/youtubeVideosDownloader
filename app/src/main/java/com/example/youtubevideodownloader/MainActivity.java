package com.example.youtubevideodownloader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class MainActivity extends AppCompatActivity {

    private String youtubeLink = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null && Intent.ACTION_SEND.equals(getIntent().getAction())
                && getIntent().getType() != null && "text/plain".equals(getIntent().getType())) {

            youtubeLink = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        }

        startDownloadActivity();

    }

    private void startDownloadActivity(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this,DownloadActivity.class);
                        intent.putExtra("YOUTUBE_LINK", youtubeLink);
                        startActivity(intent);
                    }
                }).start();

            }
        }, 500);
    }
}