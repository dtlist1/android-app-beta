package com.trumplist.boycott.geofencing;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.trumplist.boycott.R;
import com.trumplist.boycott.geofencing.web_responce.Post;
import com.trumplist.boycott.geofencing.web_responce.WebResponse;
import com.trumplist.boycott.geofencing.utils.Utils;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by lodoss on 17/05/16.
 */
public class FencesWebFetcher {
    private static final String LOG_TAG =  "FencesWebFetcher";
    private Context context;

    /**
     * This pipe receives raw web responce
     */
    private Observable<WebResponse> rawWebResponseOutput;
    /** relay used from volley's adapter. */
    private PublishSubject<String> webResponceReceiver;

    /** gives processed web responce - only what we need  */
    private Observable<List<GeofenceData>> webResponceOutput;

    public FencesWebFetcher(Context context){
        this.context = context;
        configureWebResponseHandling();
    }

    /**
     *  setup rx pipeline
     */
    private void configureWebResponseHandling(){
        webResponceReceiver = PublishSubject.create();
        rawWebResponseOutput =
        webResponceReceiver.asObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.computation())
                .map(new Func1<String, WebResponse>() {
                    @Override
                    public WebResponse call(String s) {
                        return parseWebResponse(s);
                    }
                });
        webResponceOutput = rawWebResponseOutput
                .map(new Func1<WebResponse, List<GeofenceData>>() {
                    @Override
                    public List<GeofenceData> call(WebResponse webResponse) {
                        return adaptWebResponse(webResponse);
                    }
                });
    }

    public Observable<List<GeofenceData>> getWebResponceOutput() {
        return webResponceOutput;
    }

    /**
     * Creates GET request string
     * @param latitude
     * @param longitude
     * @param zip
     * @return
     */
    private String createGetRequestString(double latitude, double longitude, String zip){
        String baseUrl = context.getResources().getString(R.string.server);
        String url = baseUrl + "geoFenceAndroid.php";

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        //String userID = sp.getString("USERID", "");
        String firebaseID = sp.getString("Firebase", "");
        /** values from a doc - not sure web server work with my current coordinates */
        String lat = String.valueOf(latitude);
        String lon = String.valueOf(longitude);

        StringBuilder sb = new StringBuilder(url);
        sb.append("?device_id=" + firebaseID + '&');
        sb.append("format=json&");
        sb.append("lng="+ lon + "&");
        sb.append("lat=" + lat + "&");
        sb.append("zip=" + zip);
        return sb.toString();
    }

    /**
     * Find current location and postal code and then perform web request for those
     * values to get a list of geofences
     */
    public void performWebRequest(){
        Utils.getCurrentLocationAndZipCode(context)
                .observeOn(Schedulers.computation())
                .subscribe(new Action1<Pair<LatLng, String>>() {
                    @Override
                    public void call(Pair<LatLng, String> arg) {
                        LatLng loc = arg.first;
                        performWebRequest(loc.latitude, loc.longitude, arg.second);
                    }
                });
    }


    /**
     * instantiates Volley and asks it to make a request
     * @param latitude
     * @param longitude
     * @param zip
     */
    private void performWebRequest(double latitude, double longitude, String zip){
        //String requestUrl = createGetRequestString(30.915208816528, -88.277397155762, "36521");
        String requestUrl = createGetRequestString(latitude, longitude, zip);
        RequestQueue queueUser = Volley.newRequestQueue(context);
        StringRequest getRequest = new StringRequest(Request.Method.GET, requestUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i(LOG_TAG, response);
                        webResponceReceiver.onNext(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(LOG_TAG, "error: " + error.getMessage());
            }
        });
        queueUser.add(getRequest);
    }

    /**
     * Uses Gson for parsing web response
     * @param jsonString
     * @return
     */
    private WebResponse parseWebResponse(String jsonString){
        WebResponse model = null;
        Gson gson = new GsonBuilder().create();
        try {
            int firstPos = jsonString.indexOf("{");
            String response = jsonString.substring(firstPos);
            model = gson.fromJson(response, WebResponse.class);
        } catch (Exception e){
            Log.e(LOG_TAG, "web response parsing error, use empty values instead: " + e.getMessage());
            model = new WebResponse();
            model.setPosts(new ArrayList<Post>());
        }
        return model;
    }

    /**
     * Geofences coming from web server carry a lot of redundant information. We take only what we
     * need - coordinates, radius, place name
     * @param webResponse
     * @return
     */
    private List<GeofenceData> adaptWebResponse(WebResponse webResponse){
        List<GeofenceData> transformedResult = new ArrayList<>();
        for (Post post : webResponse.getPosts()){
            GeofenceData d = GeofenceData.fromWebResponse(post.getPost());
            transformedResult.add(d);
        }
        return transformedResult;
    }

}
