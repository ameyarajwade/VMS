package com.example.mayur.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.FileObserver;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements VideoFragment.OnFragmentInteractionListener {

    //static LruCache<String, Bitmap> mMemoryCache;

    static int SlideInterval = 8 * 1000;                //time is in millis
    public static boolean timechanged = false;
    boolean stopthread = false;
    static ArrayList<String>[] filepath = new ArrayList[5];
    static String[] folders = new String[] {"BreakingNews","Priority1","Priority2","Priority3","Priority4"};
    ViewPager viewPager;
    SlideApapter mAdapter;
    public int current_page = 0;
    public static Timer timer;
    View mDecorView;
    static int width,height;
//    PowerManager.WakeLock wl;
    FileObserver[] mFileObserver = new FileObserver[5];
    static long folder1Imprint = 0;
    static long folder2Imprint = 0;
    static long folder3Imprint = 0;
    static long folder4Imprint = 0;
    static long folder5Imprint = 0;

    Intent updateService = null,imprintService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDecorView = getWindow().getDecorView();
        // Hide the status bar.
        hideSystemUI();
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SlideItTag");
//        wl.acquire();


        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        Log.e("Display","Width : "+ width + " Height : " + height);

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
//        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
//        Log.e("Memory","Max Memory : " + maxMemory + " kB");
//        // Use 1/8th of the available memory for this memory cache.
//        final int cacheSize = maxMemory / 4;
//
//        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
//            @Override
//            protected int sizeOf(String key, Bitmap bitmap) {
//                // The cache size will be measured in kilobytes rather than
//                // number of items.
//                return bitmap.getByteCount() / 1024;
//            }
//        };

        File hostfile = new File("/sdcard/www/hostname.txt");
        StringBuilder text = new StringBuilder();

            BufferedReader br = null;
            String line;
            try {
                br = new BufferedReader(new FileReader(hostfile));
                while ((line = br.readLine()) != null) {
                    text.append(line);
                }
                br.close();
                Constants.HOSTNAME = text.toString();
            } catch (IOException e) {
            }
        Log.e("Hostname is ",Constants.HOSTNAME);
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert").setMessage("Please Connect to WiFi").setCancelable(false);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                finish();
            }
        });

        builder.create();

        updateService = new Intent(MainActivity.this, UpdateFilesService.class);
        imprintService = new Intent(MainActivity.this,ImprintSendService.class);
        startService(updateService);
        startService(imprintService);

        if (mWifi.isConnected()) {
            updateService = new Intent(MainActivity.this, UpdateFilesService.class);
            imprintService = new Intent(MainActivity.this,ImprintSendService.class);
            startService(updateService);
            startService(imprintService);
        } else {
            builder.show();
        }

        stopthread = false;

        for (int i = 0; i < folders.length; i++) {
            filepath[i] = new ArrayList<>();
        }

        for (int j = 0; j < folders.length; j++) {
            File path = new File("/sdcard/www/" + folders[j] + "/");
            if (path.isDirectory()) {
                File[] files = path.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        String name = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
                        if (name.equals("jpg") || name.equals("jpeg") || name.equals("png") || name.equals("mp4") || name.equals("3gp") || name.equals("3gpp")) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

                for (int i = 0; i < files.length; i++) {
                    String abspath = files[i].getAbsolutePath();
                    filepath[j].add(abspath);
                }
            }
        }

        mAdapter = new SlideApapter(getSupportFragmentManager());

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Boolean isvideo = mAdapter.getisvideo(position);
                if (isvideo == null) {
                    isvideo = false;
                }
                if (isvideo) {
                    VideoFragment videoFragment = (VideoFragment) mAdapter.getRegisteredFragment(position);
                    videoFragment.playvideo();
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        viewPager.setAdapter(mAdapter);

        for (int i = 0; i < 5; i++) {
            final String j = folders[i];
            final int index = i;
            mFileObserver[i] = new FileObserver("/sdcard/www/" + j + "/", FileObserver.CLOSE_WRITE | FileObserver.DELETE) {
                @Override
                public void onEvent(int event, String path) {
                    final String path_f = path;
                    if (event == FileObserver.CLOSE_WRITE) {
                        Log.e("FileObserver", "added: /sdcard/www/" + j + "/" + path);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update_directory(index, true, "/sdcard/www/" + j + "/" + path_f);
                            }
                        });
                    } else {
                        Log.e("FileObserver", "deleted: /sdcard/www/" + j + "/" + path);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update_directory(index, false, "/sdcard/www/" + j + "/" + path_f);
                            }
                        });
                    }
                }
            };
            mFileObserver[i].startWatching();
        }

        /*viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });*/

        //viewPager.setPageTransformer(true,new FadePageTransformer());
        onTimerStartStop(true);

        Thread slidechanged = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!stopthread) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(timechanged) {
                        Log.e("SlideTimeout","SlideTimeout changed to "+MainActivity.SlideInterval);
                        onTimerStartStop(false);
                        onTimerStartStop(true);
                        timechanged = false;
                    }


                }

            }
        });
        slidechanged.start();

    }

    void update_directory(int whichFolder, boolean added, String path) {
        File file = new File(path);
        String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
        if(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("mp4") ||  ext.equals("3gp") || ext.equals("3gpp")) {
            onTimerStartStop(false);
            if (added) {
                filepath[whichFolder].add(path);
            } else {
                filepath[whichFolder].remove(path);
            }
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
            onTimerStartStop(true);
        }
    }

//    public void callBroadCast(String path) {
//        MediaScannerConnection.scanFile(this, new String[]{path}, null, null);
//    }

    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopthread = true;
        if(updateService != null) {
            stopService(updateService);
        }
        if(imprintService != null) {
            stopService(imprintService);
        }
//        if(wl != null) {
//            wl.release();
//        }
        onTimerStartStop(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTimerStartStop(boolean start) {
        if (start) {
            if (timer == null) {
                timer = new Timer(); // At this line a new Thread will be created
            }
            timer.scheduleAtFixedRate(new RemindTask(), 0, SlideInterval); // delay in milliseconds
        } else {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }

    public class RemindTask extends TimerTask {
        @Override
        public void run() {
            // As the TimerTask run on a seprate thread from UI thread we have
            // to call runOnUiThread to do work on UI thread.
            runOnUiThread(new Runnable() {
                public void run() {
                    current_page = current_page + 1;
                    //TODO: need to change this
                    if (current_page >= 3 * filepath[0].size() + filepath[1].size() + filepath[2].size() + filepath[3].size() + filepath[4].size()) {
                        current_page = 0;
                    }
                    if (current_page == 0) {
                        viewPager.setCurrentItem(current_page, false);
                    } else {
                        viewPager.setCurrentItem(current_page, true);
                    }

                }
            });

        }
    }

    public static class SlideApapter extends FragmentStatePagerAdapter {

        private FragmentStatePagerAdapter adapter;

        SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();
        SparseArray<Boolean> isvideo = new SparseArray<Boolean>();

        public SlideApapter(FragmentManager fm) {
            super(fm);
        }

        boolean nodata = false;
        int state = 1;
        int folder1_index = 0;
        int folder2_index = 0;
        int folder3_index = 0;
        int folder4_index = 0;
        int folder5_index = 0;

        public void setAdapter(FragmentStatePagerAdapter adapter) {
            this.adapter = adapter;
        }

        /**
         * Return the Fragment associated with a specified position.
         *
         * @param position
         */
        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            String ext;
            boolean run = false;

            if (nodata) {
                return new ImageFragment().newInstance("/sdcard/www/default.jpg");
            }
            /*
            if(getCount() == 0) {
                return new ImageFragment().newInstance("/sdcard/www/nodata.jpg");
            }*/

            do {
                switch (state) {
                    case 1:
                        //display all images from folder 1
                        if (folder1_index >= filepath[0].size()) {
                            folder1_index = 0;
                            state = 2;
                        }
                        if (filepath[0].size() > 0) {
                            ext = filepath[0].get(folder1_index).substring(filepath[0].get(folder1_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[0].get(folder1_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[0].get(folder1_index));
                            } else {
                                fragment = null;
                            }
                            folder1Imprint++;
                            folder1_index++;
                            run = false;
                            break;
                        }
                    case 2:
                        //folder 2
                        if (folder2_index >= filepath[1].size()) {
                            folder2_index = 0;
                        }
                        state = 3;
                        if (filepath[1].size() > 0) {
                            ext = filepath[1].get(folder2_index).substring(filepath[1].get(folder2_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[1].get(folder2_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[1].get(folder2_index));
                            } else {
                                fragment = null;
                            }
                            folder2Imprint++;
                            folder2_index++;
                            run = false;
                            break;
                        }
                    case 3:
                        //folder 3
                        if (folder3_index >= filepath[2].size()) {
                            folder3_index = 0;
                        }
                        state = 4;
                        if (filepath[2].size() > 0) {
                            ext = filepath[2].get(folder3_index).substring(filepath[2].get(folder3_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[2].get(folder3_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[2].get(folder3_index));
                            } else {
                                fragment = null;
                            }
                            folder3Imprint++;
                            folder3_index++;
                            run = false;
                            break;
                        }

                    case 4:
                        //folder 2
                        if (folder2_index >= filepath[1].size()) {
                            folder2_index = 0;
                        }
                        state = 5;

                        if (filepath[1].size() > 0) {
                            ext = filepath[1].get(folder2_index).substring(filepath[1].get(folder2_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[1].get(folder2_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[1].get(folder2_index));
                            } else {
                                fragment = null;
                            }
                            folder2Imprint++;
                            folder2_index++;
                            run = false;
                            break;
                        }

                    case 5:
                        //folder 4
                        if (folder4_index >= filepath[3].size()) {
                            folder4_index = 0;
                        }
                        state = 6;

                        if (filepath[3].size() > 0) {
                            ext = filepath[3].get(folder4_index).substring(filepath[3].get(folder4_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[3].get(folder4_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[3].get(folder4_index));
                            } else {
                                fragment = null;
                            }
                            folder4Imprint++;
                            folder4_index++;
                            run = false;
                            break;
                        }

                    case 6:
                        //folder 2
                        if (folder2_index >= filepath[1].size()) {
                            folder2_index = 0;
                        }
                        state = 7;

                        if (filepath[1].size() > 0) {
                            ext = filepath[1].get(folder2_index).substring(filepath[1].get(folder2_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[1].get(folder2_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[1].get(folder2_index));
                            } else {
                                fragment = null;
                            }
                            folder2Imprint++;
                            folder2_index++;
                            run = false;
                            break;
                        }

                    case 7:
                        //display all images from folder 1
                        if (folder1_index >= filepath[0].size()) {
                            folder1_index = 0;
                            state = 8;
                        }
                        if (filepath[0].size() > 0) {
                            ext = filepath[0].get(folder1_index).substring(filepath[0].get(folder1_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[0].get(folder1_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[0].get(folder1_index));
                            } else {
                                fragment = null;
                            }
                            folder1Imprint++;
                            folder1_index++;
                            run = false;
                            break;
                        }

                    case 8:
                        //folder 3
                        if (folder3_index >= filepath[2].size()) {
                            folder3_index = 0;
                        }
                        state = 9;

                        if (filepath[2].size() > 0) {
                            ext = filepath[2].get(folder3_index).substring(filepath[2].get(folder3_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[2].get(folder3_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[2].get(folder3_index));
                            } else {
                                fragment = null;
                            }
                            folder3Imprint++;
                            folder3_index++;
                            run = false;
                            break;
                        }

                    case 9:
                        //folder 2
                        if (folder2_index >= filepath[1].size()) {
                            folder2_index = 0;
                        }
                        state = 10;

                        if (filepath[1].size() > 0) {
                            ext = filepath[1].get(folder2_index).substring(filepath[1].get(folder2_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[1].get(folder2_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[1].get(folder2_index));
                            } else {
                                fragment = null;
                            }
                            folder2Imprint++;
                            folder2_index++;
                            run = false;
                            break;
                        }

                    case 10:
                        //folder 5
                        if (folder5_index >= filepath[4].size()) {
                            folder5_index = 0;
                        }
                        state = 11;
                        if (filepath[4].size() > 0) {
                            ext = filepath[4].get(folder5_index).substring(filepath[4].get(folder5_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[4].get(folder5_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[4].get(folder5_index));
                            } else {
                                fragment = null;
                            }
                            folder5Imprint++;
                            folder5_index++;
                            run = false;
                            break;
                        }

                    case 11:
                        //folder 2
                        if (folder2_index >= filepath[1].size()) {
                            folder2_index = 0;
                        }
                        state = 12;

                        if (filepath[1].size() > 0) {
                            ext = filepath[1].get(folder2_index).substring(filepath[1].get(folder2_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[1].get(folder2_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[1].get(folder2_index));
                            } else {
                                fragment = null;
                            }
                            folder2Imprint++;
                            folder2_index++;
                            run = false;
                            break;
                        }

                    case 12:
                        //folder 3
                        if (folder3_index >= filepath[2].size()) {
                            folder3_index = 0;
                        }
                        state = 13;

                        if (filepath[2].size() > 0) {
                            ext = filepath[2].get(folder3_index).substring(filepath[2].get(folder3_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[2].get(folder3_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[2].get(folder3_index));
                            } else {
                                fragment = null;
                            }
                            folder3Imprint++;
                            folder3_index++;
                            run = false;
                            break;
                        }

                    case 13:
                        //display all images from folder 1
                        if (folder1_index >= filepath[0].size()) {
                            folder1_index = 0;
                            state = 14;
                        }
                        if (filepath[0].size() > 0) {
                            ext = filepath[0].get(folder1_index).substring(filepath[0].get(folder1_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[0].get(folder1_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[0].get(folder1_index));
                            } else {
                                fragment = null;
                            }
                            folder1Imprint++;
                            folder1_index++;
                            run = false;
                            break;
                        }

                    case 14:
                        //folder 2
                        if (folder2_index >= filepath[1].size()) {
                            folder2_index = 0;
                        }
                        state = 15;

                        if (filepath[1].size() > 0) {
                            ext = filepath[1].get(folder2_index).substring(filepath[1].get(folder2_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[1].get(folder2_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[1].get(folder2_index));
                            } else {
                                fragment = null;
                            }
                            folder2Imprint++;
                            folder2_index++;
                            run = false;
                            break;
                        }

                    case 15:
                        //folder 4
                        if (folder4_index >= filepath[3].size()) {
                            folder4_index = 0;
                        }
                        state = 16;

                        if (filepath[3].size() > 0) {
                            ext = filepath[3].get(folder4_index).substring(filepath[3].get(folder4_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[3].get(folder4_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[3].get(folder4_index));
                            } else {
                                fragment = null;
                            }
                            folder4Imprint++;
                            folder4_index++;
                            run = false;
                            break;
                        }

                    case 16:
                        //folder 2
                        if (folder2_index >= filepath[1].size()) {
                            folder2_index = 0;
                        }
                        state = 17;

                        if (filepath[1].size() > 0) {
                            ext = filepath[1].get(folder2_index).substring(filepath[1].get(folder2_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[1].get(folder2_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[1].get(folder2_index));
                            } else {
                                fragment = null;
                            }
                            folder2Imprint++;
                            folder2_index++;
                            run = false;
                            break;
                        }

                    case 17:
                        //folder 3
                        if (folder3_index >= filepath[2].size()) {
                            folder3_index = 0;
                        }
                        state = 18;

                        if (filepath[2].size() > 0) {
                            ext = filepath[2].get(folder3_index).substring(filepath[2].get(folder3_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[2].get(folder3_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[2].get(folder3_index));
                            } else {
                                fragment = null;
                            }
                            folder3Imprint++;
                            folder3_index++;
                            run = false;
                            break;
                        }

                    case 18:
                        //folder 2
                        if (folder2_index >= filepath[1].size()) {
                            folder2_index = 0;
                        }
                        state = 1;

                        if (filepath[1].size() > 0) {
                            ext = filepath[1].get(folder2_index).substring(filepath[1].get(folder2_index).lastIndexOf(".") + 1).toLowerCase();
                            if (ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")) {
                                isvideo.put(position, false);
                                fragment = new ImageFragment().newInstance(filepath[1].get(folder2_index));
                            } else if (ext.equals("mp4") || ext.equals("3gp") || ext.equals("3gpp")) {
                                isvideo.put(position, true);
                                fragment = new VideoFragment().newInstance(filepath[1].get(folder2_index));
                            } else {
                                fragment = null;
                            }
                            folder2Imprint++;
                            folder2_index++;
                            run = false;
                            break;
                        } else {
                            run = true;
                            break;
                        }

                    default:
                        fragment = null;
                        state = 1;
                        break;
                }
            } while (run);
            //Log.e("getitem","position : "+position+" & state : "+state);
            return fragment;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            registeredFragments.remove(position);
            isvideo.remove(position);
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }

        public Boolean getisvideo(int position) {
            return isvideo.get(position);
        }

//        @Override
//        public int getItemPosition(Object object) {
//            return POSITION_NONE;
//        }

        /**
         * Return the number of views available.
         */
        @Override
        public int getCount() {
            int count = 3 * filepath[0].size() + filepath[1].size() + filepath[2].size() + filepath[3].size() + filepath[4].size();
            if (count == 0) {
                nodata = true;
                return 1;
            } else {
                nodata = false;
                return count;
            }
        }
    }

//    public class FadePageTransformer implements ViewPager.PageTransformer {
//        public void transformPage(View view, float position) {
//            if(position <= -1.0F || position >= 1.0F) {
//                view.setTranslationX(view.getWidth() * position);
//                view.setAlpha(0.0F);
//            } else if( position == 0.0F ) {
//                view.setTranslationX(view.getWidth() * position);
//                view.setAlpha(1.0F);
//            } else {
//                // position is between -1.0F & 0.0F OR 0.0F & 1.0F
//                view.setTranslationX(view.getWidth() * -position);
//                view.setAlpha(1.0F - Math.abs(position));
//            }
//        }
//    }
//
//    public class DepthPageTransformer implements ViewPager.PageTransformer {
//        private static final float MIN_SCALE = 0.75f;
//
//        public void transformPage(View view, float position) {
//            int pageWidth = view.getWidth();
//
//            if (position < -1) { // [-Infinity,-1)
//                // This page is way off-screen to the left.
//                view.setAlpha(0);
//
//            } else if (position <= 0) { // [-1,0]
//                // Use the default slide transition when moving to the left page
//                view.setAlpha(1);
//                view.setTranslationX(0);
//                view.setScaleX(1);
//                view.setScaleY(1);
//
//            } else if (position <= 1) { // (0,1]
//                // Fade the page out.
//                view.setAlpha(1 - position);
//
//                // Counteract the default slide transition
//                view.setTranslationX(pageWidth * -position);
//
//                // Scale the page down (between MIN_SCALE and 1)
//                float scaleFactor = MIN_SCALE
//                        + (1 - MIN_SCALE) * (1 - Math.abs(position));
//                view.setScaleX(scaleFactor);
//                view.setScaleY(scaleFactor);
//
//            } else { // (1,+Infinity]
//                // This page is way off-screen to the right.
//                view.setAlpha(0);
//            }
//        }
//    }
//
//    public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
//        private static final float MIN_SCALE = 0.85f;
//        private static final float MIN_ALPHA = 0.5f;
//
//        public void transformPage(View view, float position) {
//            int pageWidth = view.getWidth();
//            int pageHeight = view.getHeight();
//
//            if (position < -1) { // [-Infinity,-1)
//                // This page is way off-screen to the left.
//                view.setAlpha(0);
//
//            } else if (position <= 1) { // [-1,1]
//                // Modify the default slide transition to shrink the page as well
//                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
//                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
//                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
//                if (position < 0) {
//                    view.setTranslationX(horzMargin - vertMargin / 2);
//                } else {
//                    view.setTranslationX(-horzMargin + vertMargin / 2);
//                }
//
//                // Scale the page down (between MIN_SCALE and 1)
//                view.setScaleX(scaleFactor);
//                view.setScaleY(scaleFactor);
//
//                // Fade the page relative to its size.
//                view.setAlpha(MIN_ALPHA +
//                        (scaleFactor - MIN_SCALE) /
//                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));
//
//            } else { // (1,+Infinity]
//                // This page is way off-screen to the right.
//                view.setAlpha(0);
//            }
//        }
//    }

}

