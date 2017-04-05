package com.trumplist.boycott.geofencing.utils;

import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.trumplist.boycott.R;
import com.trumplist.boycott.geofencing.utils.zip_gmap_api_response.AddressComponent;
import com.trumplist.boycott.geofencing.utils.zip_gmap_api_response.Result;
import com.trumplist.boycott.geofencing.utils.zip_gmap_api_response.ZipGmapApiResponse;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by lodoss on 13/05/16.
 */
public class Utils {
    private static final String LOG_TAG = "Utils";
    private static PublishSubject<String> zipcodeResultReceiver;
    static {
        zipcodeResultReceiver = PublishSubject.create();
    }
    public static Location getCurrentLocation(Context context) {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        Location lastKnownLocation = null;
        try {
            lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        } catch (SecurityException e) {
        }
        return lastKnownLocation;
    }



    /**
     *
     * @param context
     * @return
     */
     public static Observable<LatLng> getCurrentLocationAsync(final Context context) {
        final Observable<LatLng> pos = Observable.just("")
                .map(new Func1<String, LatLng>() {
                    @Override
                    public LatLng call(String s) {
                            Location loc = Utils.getCurrentLocation(context);
                        LatLng currLocation;
                        if (null == loc){
                            Log.i(LOG_TAG, "Location service is unavailable on your device, "
                            + " using default location instead");
                            /** use default location from resources */
                            Resources res = context.getResources();
                            String defaultLat = res.getString(R.string.location_default_lat);
                            String defaultLon = res.getString(R.string.location_default_lon);
                            try {
                                currLocation = new LatLng(Double.valueOf(defaultLat),
                                        Double.valueOf(defaultLon));
                            } catch (NumberFormatException e){
                                Log.w(LOG_TAG, "Can't parse default location value, use (0, 0)" +
                                        "instead");
                                currLocation = new LatLng(0, 0);
                            }
                        }else {
                            currLocation = new LatLng(loc.getLatitude(), loc.getLongitude());
                            Log.i(LOG_TAG, "Location detected: " + currLocation);
                        }
                        return currLocation;
                    }
                });
        return Observable.defer(new Func0<Observable<LatLng>>() {
            @Override
            public Observable<LatLng> call() {
                return pos;
            }
        }).subscribeOn(Schedulers.computation())
                .take(1);
    }

    private static String createZipGetRequest(Context context, double latitude, double longitude){
        String baseURL = context.getResources().getString(R.string.zipcode_gmap_api_address);
        StringBuilder sb = new StringBuilder(baseURL);
        sb.append("?latlng=");
        sb.append(String.valueOf(latitude));
        sb.append(",");
        sb.append(String.valueOf(longitude));
        sb.append("&sensor=true");
        return sb.toString();
    }

    private static String parseGMapApiResultAndPullOutZipCode(String rawResponse){
        String zip = "";
        Gson gson = new GsonBuilder().create();
        ZipGmapApiResponse parsedResponse = null;
        try {
            parsedResponse = gson.fromJson(rawResponse, ZipGmapApiResponse.class);
            List<Result> results = parsedResponse.getResults();
            if (results.isEmpty()) {
                return zip;
            }
            /** assume the first result is the most accurate */
            List<AddressComponent> addressParts = results.get(0).getAddressComponents();
            for (AddressComponent addressComponent : addressParts){
                if (addressComponent.getTypes().contains("postal_code")){
                    zip = addressComponent.getLongName();
                    break;
                }
            }
        } catch (Exception e){
        }
        return zip;
    }

    public static Observable<String> getZipCode(Context context, double lat, double lon){
        String requestUrl = createZipGetRequest(context, lat, lon);
        RequestQueue queueUser = Volley.newRequestQueue(context);
        StringRequest getRequest = new StringRequest(Request.Method.GET, requestUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Log.i(LOG_TAG, response);
                        zipcodeResultReceiver.onNext(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(LOG_TAG, "error: " + error.getMessage());
            }
        });
        queueUser.add(getRequest);

        return zipcodeResultReceiver.asObservable().take(1)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        String zip = parseGMapApiResultAndPullOutZipCode(s);
                        return zip;
                    }
                });
    }

    /**
     * get current location and its zip code via Google Maps Api
     * @param context
     * @return
     */
    public static Observable<String> getZipCodeForCurrentLocation(final Context context){
        return Utils.getCurrentLocationAsync(context)
        .flatMap(new Func1<LatLng, Observable<String>>() {
            @Override
            public Observable<String> call(LatLng latLng) {
                return Utils.getZipCode(context, latLng.latitude, latLng.longitude);
            }
        });
    }

    /**
     * This method make 2 calls for location, but Location Api stores current location internally
     * anyways (here network provider)
     * @param context
     * @return
     */
    public static Observable<Pair<LatLng, String>> getCurrentLocationAndZipCode(Context context){
        return Observable.zip(
                getCurrentLocationAsync(context),
                getZipCodeForCurrentLocation(context),
                new Func2<LatLng, String, Pair<LatLng, String>>() {
                    @Override
                    public Pair<LatLng, String> call(LatLng loc, String zip) {
                        return new Pair<LatLng, String>(loc, zip);
                    }
                }
        ).subscribeOn(Schedulers.computation())
                .take(1);
    }

    public static void executeOnMainThread(Runnable r){
        Handler h = new Handler(Looper.getMainLooper());
        h.post(r);
    }

}
