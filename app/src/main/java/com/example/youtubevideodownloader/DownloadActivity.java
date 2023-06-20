package com.example.youtubevideodownloader;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class DownloadActivity extends AppCompatActivity {

    private int STORAGE_PERMISSION_CODE = 1;
    private static String youtubeLink;
    private LinearLayout mainLayout;
    private ProgressBar mainProgressBar;
    private EditText linkInput;
    private Button downloadBtn;

    private ImageView imageView;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        loadBannerAd();

        mainLayout = findViewById(R.id.main_layout);
        mainProgressBar = findViewById(R.id.prgrBar);
        mainProgressBar.setVisibility(View.INVISIBLE);
        linkInput = findViewById(R.id.linkInput);
        downloadBtn = findViewById(R.id.downloadBtn);
        imageView = findViewById(R.id.imagePreview);

        // get extras
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            String link = extras.getString("YOUTUBE_LINK");
            if(link != null && !link.isEmpty()){
                linkInput.setText(link);
                getYoutubeDownloadUrl(link);
            }
        }

        // Admob Initializing
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                Log.d("ADMOB", "OnInitializationCompleteListener");
            }
        });

        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchLink();
            }
        });


    }

    private void searchLink(){
        // String ytLink = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        String ytLink = linkInput.getText().toString();

        if (ytLink != null
                && (ytLink.contains("://youtu.be/") || ytLink.contains("youtube.com/watch?v="))) {
            youtubeLink = ytLink;
            // We have a valid link
            getYoutubeDownloadUrl(youtubeLink);
        } else {
            Toast.makeText(this, R.string.error_no_yt_link, Toast.LENGTH_LONG).show();
            mainProgressBar.setVisibility(View.GONE);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void getYoutubeDownloadUrl(String youtubeLink) {
        mainProgressBar.setVisibility(View.VISIBLE);
        new YouTubeExtractor(this) {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                mainProgressBar.setVisibility(View.GONE);

                if (ytFiles == null) {
                    // Something went wrong we got no urls. Always check this.
                    finish();
                    return;
                }

                // return thumbnail
                String thumbnailLink = "https://img.youtube.com/vi/"+getYouTubeId(youtubeLink)+"/0.jpg";
                Glide.with(DownloadActivity.this).load(thumbnailLink).into(imageView);

                // Iterate over itags
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    // ytFile represents one file with its url and meta data
                    YtFile ytFile = ytFiles.get(itag);

                    // Just add videos in a decent format => height -1 = audio
                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                        addButtonToMainLayout(vMeta.getTitle(), ytFile);
                    }
                }
            }
        }.extract(youtubeLink);
    }

    private void addButtonToMainLayout(final String videoTitle, final YtFile ytfile) {
        // Display some buttons and let the user choose the format
        String btnText = (ytfile.getFormat().getHeight() == -1) ? "Audio " +
                ytfile.getFormat().getAudioBitrate() + " kbit/s" :
                ytfile.getFormat().getHeight() + "p";
        btnText += (ytfile.getFormat().isDashContainer()) ? " dash" : "";
        Button btn = new Button(this);
        btn.setText(btnText);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(DownloadActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    Log.d("STORAGE_PERMISSION", "Already Granted");
                    String filename;
                    if (videoTitle.length() > 55) {
                        filename = videoTitle.substring(0, 55) + "." + ytfile.getFormat().getExt();
                    } else {
                        filename = videoTitle + "." + ytfile.getFormat().getExt();
                    }
                    filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
                    downloadFromUrl(ytfile.getUrl(), videoTitle, filename);
                }else{
                    requestStoragePermission();
                }
            }
        });
        mainLayout.addView(btn);
    }

    private void downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName) {
        Uri uri = Uri.parse(youtubeDlUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(downloadTitle);

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        Toast.makeText(this, "Downloading...", Toast.LENGTH_LONG).show();
        finish();
        getIntent().removeExtra("YOUTUBE_LINK");
        startActivity(getIntent());
    }

    private String getYouTubeId (String youTubeUrl) {
        String pattern = "(?<=youtu.be/|watch\\?v=|/videos/|embed\\/)[^#\\&\\?]*";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(youTubeUrl);
        if(matcher.find()){
            return matcher.group();
        } else {
            return "error";
        }
    }

    private void loadBannerAd(){
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.d("ADMOB", "Banner failed to load");
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                Log.d("ADMOB", "Banner loaded");
            }
        });

    }

    private void requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            new AlertDialog.Builder(this)
                    .setTitle("Storage permission needed")
                    .setMessage("This permission is needed to allow application to save files on your storage")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(DownloadActivity.this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create().show();
        }else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }
}