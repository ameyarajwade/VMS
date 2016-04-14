package com.example.mayur.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static com.example.mayur.myapplication.Constants.*;

public class UpdateFilesService extends Service {
    PowerManager.WakeLock wl;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("UpdateFilesService","Service Started");
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(mWifi.isConnected()) {
            CheckAndDownloadTask checkAndDownloadTask = new CheckAndDownloadTask();
            checkAndDownloadTask.execute();
        }

        Intent intentservice = new Intent(UpdateFilesService.this, UpdateFilesService.class);
        PendingIntent restartService = PendingIntent.getService(UpdateFilesService.this, 0, intentservice, 0);
        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarm.cancel(restartService);
        alarm.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (1000 * 60), restartService);
        return super.onStartCommand(intent, flags, startId);
    }


    class CheckAndDownloadTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            boolean connected = false;
            String strURL = HOSTNAME + "/" + SERVERPROJECT + "/CheckConnection";
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"SlideItTag");
            wl.acquire();
            try{
                URL url = new URL(strURL);
                URLConnection connection = url.openConnection();
                InputStream stream = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(stream);
                StringBuilder builder = new StringBuilder();
                int next = 0;
                try {
                    while ((next = reader.read()) != -1) {
                        builder.append((char) next);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String connectionEst = builder.toString();
                if(connectionEst.equals("SUCCESS")){
                    connected = true;
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(connected){
                downloadFiles();
            }
            wl.release();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }


    public void downloadFiles() {

        ArrayList<String> serverFilePath = new ArrayList<>();
        ArrayList<String> localFilePath = new ArrayList<>();
        ArrayList<String> splittedServerFiles = new ArrayList<>();
        String fileDirectory = "/sdcard/www/";
        File fileDir;
        File dir;

        fileDir = new File(fileDirectory);
        if (!fileDir.exists())
            fileDir.mkdir();

        String strURL = HOSTNAME + "/" + SERVERPROJECT + "/GetFilePaths";
        try {
            URL url = new URL(strURL);
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();

            InputStreamReader reader = new InputStreamReader(stream);
            StringBuilder builder = new StringBuilder();
            int next = 0;
            try {
                while ((next = reader.read()) != -1) {
                    builder.append((char) next);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            String pathOfImages = builder.toString();
            try {
                JSONArray jsonArray = new JSONArray(pathOfImages);
                for (int index = 0; index < jsonArray.length(); index++) {
                    JSONObject obj = jsonArray.getJSONObject(index);
                    String imgPath = obj.getString("path");
                    serverFilePath.add(imgPath);
                }

            } catch (JSONException e) {
                e.printStackTrace();

            }

            for (int i = 0; i < serverFilePath.size(); i++) {
                File file = new File(serverFilePath.get(i));
                String str1 = file.getParent();
                String serverDirName = new File(str1).getName();
                dir = new File(fileDirectory + serverDirName + "/");
                if (!dir.exists())
                    dir.mkdir();
            }

            File directory = new File(fileDirectory);
            File[] listOfFiles = directory.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isDirectory()) {
                    String parentName = listOfFiles[i].getName();

                    File[] listfiles = listOfFiles[i].listFiles();
                    if (listfiles.length > 0) {
                        for (int j = 0; j < listfiles.length; j++) {
                            if (listfiles[j].isFile()) {
                                localFilePath.add(parentName + "/" + listfiles[j].getName());
                            }
                        }
                    } else {
                        // Log.e("MainActivity", "no file in sub directory");
                    }
                }
            }
            String str22 = new String();
            if(serverFilePath.size() != 0) {
                File file = new File(serverFilePath.get(0));
                String str1 = file.getParent();
                File file1 = new File(str1);
                str22 = file1.getAbsoluteFile().getParentFile().getAbsolutePath() + "/";
            }

            //Log.e("MainAcivity","Server File Parent path" + str22);

            for (int index = 0; index < serverFilePath.size(); index++) {
                String str = serverFilePath.get(index);
                String str2 = str.substring(str.lastIndexOf("/", str.lastIndexOf("/") - 1) + 1);
                // Log.e("MainActivity","Server Split String: " +  str2);
                splittedServerFiles.add(str2);

            }


            for (int i = 0; i < splittedServerFiles.size(); i++) {
                boolean ifAvailable = false;
                for (int j = 0; j < localFilePath.size(); j++) {
                    if (splittedServerFiles.get(i).equals(localFilePath.get(j))) {
                        ifAvailable = true;
                        break;
                    }
                }
                if (ifAvailable == false) {
                    String fileToDownload = str22 + splittedServerFiles.get(i);
                    File file2 = new File(fileToDownload);
                    String parentName = file2.getParentFile().getName();
                    String fileName = file2.getName();
                    //  Log.e("Download","Server Parent path : " + str22 );
                    String strURL1 = String.format(HOSTNAME + "/" + SERVERPROJECT + "/DownloadFiles?Path=%s", fileToDownload);
                    try {
                        Log.e("DownloadFilesTask", "URL " + strURL1);
                        URL url1 = new URL(strURL1);
                        URLConnection connection1 = url1.openConnection();
                        InputStream stream1 = connection1.getInputStream();
                        byte[] byteArray = IOUtils.toByteArray(stream1);
                        String fileLocalPath = fileDirectory + "/" + parentName;
                        FileOutputStream fileOutputStream = new FileOutputStream(fileLocalPath + "/" + fileName);
                        fileOutputStream.write(byteArray);
                        fileOutputStream.close();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }

            for (int i = 0; i < localFilePath.size(); i++) {
                boolean ifdeleted = true;
                for (int j = 0; j < splittedServerFiles.size(); j++) {
                    if (splittedServerFiles.get(j).equals(localFilePath.get(i))) {
                        ifdeleted = false;
                        break;
                    }
                }
                if (ifdeleted) {
                    File fileToDelete = new File("/sdcard/www/" + localFilePath.get(i));
                    boolean deleted = false;
                    Log.e("fileChanges","Deleted : "+fileToDelete.getPath());
                    deleted = fileToDelete.delete();
                    if (deleted) {
                    }
                }

            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
