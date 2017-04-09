package com.trumplist.boycott.geofencing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;


import com.trumplist.boycott.DrillDownActivty;
import com.trumplist.boycott.MainActivity;
import com.trumplist.boycott.R;
import com.trumplist.boycott.geofencing.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;


/**
 * Created by lodoss on 16/05/16.
 */
public class GeofenceService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        GeofencingManager.PendingIntentProvider, GeofencingManager.Feedback{

    private static final String LOG_TAG = "GeofenceService";
    private static final String GEOFENCE_ACTION = "com.trumplist.boycott.action.GEOFENCE_EVENT";
    private GoogleApiClient googleApiClient;
    boolean isAPIReady;
    private GeofenceStatusUpdateReceiver receiver;

    PendingIntent pendingGeofenceIntent;
    FencesWebFetcher webFenceFetcher;
    GeofencingManager geofencingManager;

    /**
     * Receives geofence state updates
     */
    private class GeofenceStatusUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            /**
            if (intent.getAction().equals(GEOFENCE_ACTION)){
                Toast.makeText(getApplicationContext(), "Intent received", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Wrong intent received", Toast.LENGTH_SHORT)
                        .show();
            }
             */
            if (null == geofencingManager){
                Log.e(LOG_TAG, "Notification received before geofencing manager is initizlied-" +
                        " perhaps geofences were not removed last time");
                return;
            }
            geofencingManager.acceptGeofenceNotification(intent);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        deploy();
    }

    @Override
    public void onDestroy() {
        suspend();
        super.onDestroy();
    }

    /**
     * Even thought we use FusedLocationApi, we can just check if GooglePlayServices is installed and
     * if Location Providers is enabled. GoogleApi library is shipped with all devices, this may
     * cause issues only while working with emulator
     * @return
     */
    private boolean isDeviceSupportGeofencing(){
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            Log.e(LOG_TAG, "Google play services isn't available");
            return false;
        }
        /** Google play services is installed, now check location managers */
        LocationManager lm = null;
        boolean gps_enabled = false;
        boolean network_enabled = false;
        if(lm==null)
            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try{
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){}
        try{
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch(Exception ex){}

        return gps_enabled || network_enabled;
    }


    /**
     * If Google Api'd fail to initialize, service will be stopped and suspend() will be
     * called anyways
     */
    protected void deploy(){
        Log.i(LOG_TAG, "Starting geofence service");
        isAPIReady = false;

        geofencingManager = new GeofencingManager();
        if (!isDeviceSupportGeofencing()){
            Log.e(LOG_TAG, "Device doesn't support geofencing, those features will not be " +
                    "available");
            stopSelf();
            return;
        }
        configureFencesWebFetcher();

        buildApiClient();
        receiver = new GeofenceStatusUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(GEOFENCE_ACTION);
        registerReceiver(receiver, intentFilter);

        Intent wrappedIntent = new Intent(GEOFENCE_ACTION);
        pendingGeofenceIntent = PendingIntent.getBroadcast(this, 0, wrappedIntent, 0);
    }

    protected void suspend(){
        Log.i(LOG_TAG, "Stopping geofence service");
        isAPIReady = false;
        if (isAPIReady) {
            googleApiClient.disconnect();

            unregisterReceiver(receiver);
            geofencingManager.stop(new Action0() {
                @Override
                public void call() {
                    Log.i(LOG_TAG, "All geofences were removed");
                }
            });
        }
    }

    /**
     * start work only after Api is ready
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(LOG_TAG, "Google Api is connected");
        isAPIReady = true;
        configureGeofencingManager();
        startProcessingGeofences();
    }

    /**
     * try to reconnect
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.w(LOG_TAG, "Google Api connection suspended, trying to reconnect");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Connection to Google Api has failed, stopping geofencing service");
        this.stopSelf();
    }

    @Override
    public PendingIntent provideGeofencePendingIntent() {
        return pendingGeofenceIntent;
    }

    /**
     * this callback is called on background thread
     * @param triggeredAreas
     */
    @Override
    public void onNotification(final List<GeofenceData> triggeredAreas) {
        if (triggeredAreas.isEmpty()) return;
        final int companyId = triggeredAreas.get(0).getCompanyId();
        Utils.executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                updateNotification(companyId);
            }
        });
    }

    public void updateNotification(int companyId){
        //TODO: Needs new incoming intent
        Intent compGEOIntent = new Intent(this, DrillDownActivty.class);
        Log.i(LOG_TAG, "QuizID: " + companyId);
        compGEOIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        compGEOIntent.putExtra("company", companyId);
        compGEOIntent.putExtra("source", "GEO");


        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(),
                compGEOIntent, 0);

        Notification n = new Notification.Builder(this)
                .setContentTitle(getString(R.string.notification_content_title))
                .setContentText(getString(R.string.notification_content_text))
                .setContentIntent(pIntent)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_speaker_dark)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, n);
    }
    /**
     * Geofencing manager call that callback when user leaves validity area (created on startup from
     * current location). stop and then start again GeofencingManager. It will be forced to reload
     * list of geofenes as well as new border.
     * After removing all geofences from system we request webFenceFetcher to get new geofence list
     * from server. that list will be delivered to geofencingManager and will cause adding new
     * geofences and a new border.
     */
    @Override
    public void onUserLeavingActualArea() {
        Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
               // Toast.makeText(GeofenceService.this, "User leaving area", Toast.LENGTH_SHORT).show();
            }
        });
        geofencingManager.removeAllGeofencesFromSystem(new Action0() {
            @Override
            public void call() {
                Log.i(LOG_TAG, "All geofences including border were removed");
                webFenceFetcher.performWebRequest();
            }
        });
    }

    protected void buildApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    protected void startProcessingGeofences(){
        Log.i(LOG_TAG, "Api is ready");
        webFenceFetcher.performWebRequest();
    }

    private void configureFencesWebFetcher(){
        webFenceFetcher = new FencesWebFetcher(this);

        webFenceFetcher.getWebResponceOutput()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<GeofenceData>>() {
                    @Override
                    public void call(List<GeofenceData> geofencesData) {
                        int n = geofencesData.size();
                        Log.i(LOG_TAG,  "geofences: " + n);
                    }
                });
    }

    /**
     * Geofencing manager requires GoogleApi client so it has to be configured when that client is
     * ready - in connection callback, not in deploy() method.
     * At a time when geofencing manager is started, all other parts is initialized
     */
    private void configureGeofencingManager(){
        Log.i(LOG_TAG, "Configuring geofencing manager");
        geofencingManager.setApiClient(googleApiClient);
        geofencingManager.setIntentProvider(this);
        geofencingManager.setFeedback(this);
        geofencingManager.setFencesSource(webFenceFetcher.getWebResponceOutput());
        geofencingManager.start();
    }


}
