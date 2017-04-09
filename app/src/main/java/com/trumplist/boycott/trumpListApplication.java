package com.trumplist.boycott;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.trumplist.boycott.geofencing.GeofenceService;

/**
 * Created by Andrew on 4/8/2017.
 */

public class trumpListApplication extends android.app.Application {

    private static trumpListApplication mInstance;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        Log.i("trumpListApplication", "in onCreate() of trumpListApplication");
        startGeofencingService();
    }

    @Override
    public void onTerminate() {
        stopGeofencingService();
        super.onTerminate();
    }

    private trumpListApplication getInstance() {
        if (mInstance == null) {
            mInstance = new trumpListApplication();
        }
        return mInstance;
    }


    private void startGeofencingService(){
        Intent intent = new Intent(this, GeofenceService.class);
        startService(intent);
    }

    private void stopGeofencingService(){
        Intent intent = new Intent(this, GeofenceService.class);
        stopService(intent);
    }
}
