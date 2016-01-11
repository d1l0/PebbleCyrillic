package com.d1l0.pebblecyrillic;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;


public class MainActivity extends AppCompatActivity {
    InterstitialAd mInterstitialAd;
    String FOLDER_MAIN = "Android/data/com.d1l0.pebble.cyrillic/files";
    String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+FOLDER_MAIN+"/cyrillic.pbl";
    String RU_LATEST = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+FOLDER_MAIN+"/ru.pbl";
    String CURRENT_FILE = "";
    String LANG_SOURCE_URL = "https://github.com/whidrasl/pebble-russian-language-pack/blob/master/Russian-ru_RU.pbl?raw=true";
    // Storage Permissions

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    boolean storage_unv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_onclick));

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                try {
                    choose_install();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        requestNewInterstitial();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Verify whether we have write to storage permissions
        verifyStoragePermissions(this);
    }

    public void Install(String path) throws IOException{
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        //Verify whether write to storage permissions was granted
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.granted);

            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            verifyStoragePermissions(this);
        } else {
            writeOnStorage();

            if (!storage_unv) {
                File file = new File(path);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/octet-stream .pbl");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                String chooserTitle = getString(R.string.install_lang);
                Intent chosenIntent = Intent.createChooser(intent, chooserTitle);
                startActivity(chosenIntent);
            }
        }
    }

    public void writeOnStorage() throws IOException{
        if (!isExternalStorageReadOnly() && isExternalStorageAvailable()) {

            File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), FOLDER_MAIN);
            if (!f.exists()) {
                f.mkdirs();
            }

            InputStream in = getResources().openRawResource(
                    getResources().getIdentifier("raw/cyrillic",
                            "raw", getPackageName()));
            OutputStream out = new FileOutputStream(FILE_PATH);

            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

        } else {
            Context context = getApplicationContext();
            CharSequence text = getString(R.string.storage_unavailable);
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            //toast.setGravity(Gravity.BOTTOM| Gravity.CENTER, 0, 10);
            toast.show();
            storage_unv = true;
        }
    }

    public void onClick(View view) throws IOException {
        //Calling interstitial google add and than launch Install method.
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            choose_install();
        }
    }

    public void choose_install() throws IOException{
        Spinner pack = (Spinner) findViewById(R.id.pack);
        String pack_selected = String.valueOf(pack.getSelectedItem());
        if (pack_selected.equals("Cyrillic fonts only")){
            CURRENT_FILE = FILE_PATH;
            Install(CURRENT_FILE);
        }
        else {
            install_latest();
        }
    }

    public void install_latest(){
        if (isNetworkAvailable(this)) {
            downloadLatest();
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(this, getString(R.string.downloading), duration);
            toast.show();
            CURRENT_FILE = RU_LATEST;
            BroadcastReceiver onComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Install(RU_LATEST);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            registerReceiver(onComplete, new IntentFilter(
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
        else{
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(this, getString(R.string.no_internet), duration);
            toast.show();
        }
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

     public void About(){
         // Create intent to start about.class activity
         Intent intent = new Intent(this, about.class);
         startActivity(intent);
     }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            About();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static boolean isExternalStorageReadOnly() {
        //Verify whether external storage is readonly
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
    }

    private static boolean isExternalStorageAvailable() {
        //Verify whether external storage is available
        String extStorageState = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(extStorageState));
    }

    private void requestNewInterstitial() {
        //Google adMob code to request new interstitial ad.
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    public void downloadLatest(){
        String url = LANG_SOURCE_URL;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(getString(R.string.download_description));
        request.setTitle(getString(R.string.download_title));
        // in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalPublicDir(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/"+FOLDER_MAIN, "ru.pbl");

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    public boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}
