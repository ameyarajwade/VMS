package com.example.mayur.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;

import static com.example.mayur.myapplication.Constants.HOSTNAME;
import static com.example.mayur.myapplication.Constants.SERVERPROJECT;

public class ImprintSendService extends Service {
    public ImprintSendService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("ImprintSendService","service started");

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(mWifi.isConnected()) {
            ImprintSendTask imprintSendTask = new ImprintSendTask();
            imprintSendTask.execute();
        }

        Intent intentservice = new Intent(ImprintSendService.this, ImprintSendService.class);
        PendingIntent restartService = PendingIntent.getService(ImprintSendService.this, 0, intentservice, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarm.cancel(restartService);
        alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (1000 * 60), restartService);
        return super.onStartCommand(intent, flags, startId);

    }

    class ImprintSendTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            long folder1Imprint = MainActivity.folder1Imprint;
            long folder2Imprint = MainActivity.folder2Imprint;
            long folder3Imprint = MainActivity.folder3Imprint;
            long folder4Imprint = MainActivity.folder4Imprint;
            long folder5Imprint = MainActivity.folder5Imprint;

            String strURL = String.format(HOSTNAME + "/" + SERVERPROJECT + "/GetImprints?Folder1Imprint=%d&Folder2Imprint=%d&Folder3Imprint=%d&Folder4Imprint=%d&Folder5Imprint=%d", folder1Imprint,folder2Imprint,folder3Imprint,folder4Imprint,folder5Imprint);
            try {
                URL url = new URL(strURL);
                URLConnection connection = url.openConnection();
                InputStream stream = connection.getInputStream();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


            MainActivity.folder1Imprint=0;
            MainActivity.folder2Imprint=0;
            MainActivity.folder3Imprint=0;
            MainActivity.folder4Imprint=0;
            MainActivity.folder5Imprint=0;

            try {
                String strURL2 = HOSTNAME + "/" + SERVERPROJECT + "/GetSlideTimeOut";
                URL url2 = new URL(strURL2);
                URLConnection connection2 = url2.openConnection();
                InputStream stream2 = connection2.getInputStream();
                InputStreamReader reader2 = new InputStreamReader(stream2);
                StringBuilder builder2 = new StringBuilder();
                int next = 0;
                try {
                    while ((next = reader2.read()) != -1) {
                        builder2.append((char) next);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String SlideTimeout = builder2.toString();
                int temp = Integer.parseInt(SlideTimeout)*1000;
                if(temp < 1000) {
                    temp = 1000;
                } else if (temp > 30000) {
                    temp = 30000;
                }

                if(temp != MainActivity.SlideInterval) {
                    MainActivity.SlideInterval = temp;
                    MainActivity.timechanged = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}
