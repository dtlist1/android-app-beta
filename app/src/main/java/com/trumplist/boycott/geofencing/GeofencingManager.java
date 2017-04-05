package com.trumplist.boycott.geofencing;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.trumplist.boycott.R;
import com.trumplist.boycott.geofencing.utils.Utils;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

/**
 * Created by lodoss on 18/05/16.
 */

/**
 * Geofencing manager is responsible for registering intents, cancelling intents and
 * This class get intent for managing geofences from outside and it doesn't have any receivers
 * for managing incoming geofence events. That is because those events can be intercepted either
 * by BroadcastReceiver or by IntentService and it's up to other program to decide, this manager
 * just register and unregister geofences.
 */
public class GeofencingManager {
    private static final String LOG_TAG = "GeofencingManager";
    /** 10 miles radius circle around current location (at a time of creation) in which
     * geofences are considered to be valid
     */
    private static final int validityGeofenceRadius = 20125;

    public interface PendingIntentProvider{
        PendingIntent provideGeofencePendingIntent();
    }
    public interface Feedback{
        void onNotification(List<GeofenceData> triggeredAreas);
        void onUserLeavingActualArea();
    }

    GoogleApiClient apiClient;
    private PendingIntentProvider intentProvider;
    private Observable<List<GeofenceData>> fencesSource;
    private Subscription sourceSubscription;
    private List<GeofenceData> knownFences;
    /** large circle surrounding current location (at the moment of creation) */
    private String validityBorderGeofenceUUID;
    private Feedback feedback;
    /**
     * We should process notification asynchronously and avoid notification floud in case
     * few areas overlap. For that we can accept only one notification on feew hundreds
     * milliseconds.
     */
    private PublishSubject<Intent> geofencingNotificationReceiver;
    /** sever that connection to stop receiving notifications */
    private Subscription notificationConnection;

    private String transitionTypeEnter;
    private String transitionTypeExit;

    public GeofencingManager(){
        knownFences = new ArrayList<>();
        validityBorderGeofenceUUID = "";
    }

    public void setIntentProvider(PendingIntentProvider intentProvider) {
        this.intentProvider = intentProvider;
    }

    public void setFencesSource(Observable<List<GeofenceData>> fencesSource) {
        this.fencesSource = fencesSource;
    }

    public void setApiClient(GoogleApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public void acceptGeofenceNotification(Intent intent){
        if (null == geofencingNotificationReceiver){
            Log.e(LOG_TAG, "Error: receiver isn't initialized but notification received," +
                    "aborting");
            return;
        }
        geofencingNotificationReceiver.onNext(intent);
    }

    /**
     * Subscribe to fences source and register geofences when list is available
     */
    public void start(){
        if (null == fencesSource){
            Log.e(LOG_TAG, "geofences source is null");
            return;
        }
        /** perform async */
        sourceSubscription = fencesSource
                .observeOn(Schedulers.computation())
                .subscribe(new Action1<List<GeofenceData>>() {
                    @Override
                    public void call(final List<GeofenceData> geofenceDatas) {
                        /** remove old geofences and register new ones */
                        removeAllGeofencesFromSystem(new Action0() {
                            @Override
                            public void call() {
                                knownFences = new ArrayList<GeofenceData>(geofenceDatas);
                                registerGeofencesInSystem(new Action0() {
                                    @Override
                                    public void call() {
                                        setupNotificationReception();
                                    }
                                }, formListOfGeofences());
                            }
                        });
                    }
                });
        Context context = apiClient.getContext();
        transitionTypeEnter =
                context.getResources().getString(R.string.geofence_transition_type_enter);
        transitionTypeExit =
                context.getResources().getString(R.string.geofence_transition_type_exit);
    }

    public void stop(Action0 callback){
        severConnection(sourceSubscription);
        severConnection(notificationConnection);
        removeAllGeofencesFromSystem(callback);
    }

    /**
     * setup pipeline for receiving notification. This method is called when all geofences are
     * successfully registered (from callback)
     */
    public void setupNotificationReception(){
        geofencingNotificationReceiver = PublishSubject.create();
        notificationConnection =
                geofencingNotificationReceiver.asObservable()
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.computation())
                        //   .debounce(200, TimeUnit.MILLISECONDS)
                        .subscribe(new Action1<Intent>() {
                            @Override
                            public void call(Intent intent) {
                                handleGeofenceNotification(intent);
                            }
                        });
    }

    /**
     * remove all geofences registered for PendingIntent
     */
    public void removeAllGeofencesFromSystem(final Action0 successCallback){
        PendingIntent intent = intentProvider.provideGeofencePendingIntent();
        knownFences.clear();
        try {
            LocationServices.GeofencingApi.removeGeofences(
                    apiClient,
                    intent
            )
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.getStatusCode() == CommonStatusCodes.SUCCESS) {
                                Log.i(LOG_TAG, "All geofences were successfully removed");
                                validityBorderGeofenceUUID = "";
                                successCallback.call();
                            } else {
                                Log.e(LOG_TAG, "Problem with removing geofences: " +
                                        status.getStatusMessage());
                            }
                        }
                    });
        } catch (SecurityException se){
            Log.e(LOG_TAG, "Geofencing access denied, check permissions");
        }
    }


    /**
     * This method is called when list of geofences arrives. It registers list of geofences in
     * system. It also register border area
     * @param successCallback
     * @param geofences
     */
    private void registerGeofencesInSystem(final Action0 successCallback,
                                           List<Geofence> geofences){
        Log.i(LOG_TAG, "Registering " + knownFences.size() + " geofences in system");
        if (geofences.isEmpty()) {
            return;
        }
        PendingIntent intent = intentProvider.provideGeofencePendingIntent();
        /** now we have list of geofences, create geofencign request */
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.addGeofences(geofences);
        GeofencingRequest request = builder.build();
        try {
            LocationServices.GeofencingApi.addGeofences(
                    apiClient,
                    request,
                    intent
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.getStatusCode() == CommonStatusCodes.SUCCESS) {
                        successCallback.call();
                        /** get current location and add validity geofence */

                        Log.i(LOG_TAG, "Geofences successfully added");
                        Utils.getCurrentLocationAsync(apiClient.getContext())
                                .subscribe(new Action1<LatLng>() {
                                    @Override
                                    public void call(LatLng latLng) {
                                        scheduleBorderGeofence(latLng);
                                    }
                                });
                    } else {
                        Log.e(LOG_TAG, "Problem with adding geofences: " +
                                status.getStatusMessage());
                    }
                }
            });
        } catch (SecurityException se){
            Log.e(LOG_TAG, "Geofencing access denied, check permissions");
        }
    }

    /**
     * Add the last validity geofence only when and if other geofences added successfully
     *
     */
    private void scheduleBorderGeofence(final LatLng currentLocation){
        GeofenceData borderData = new GeofenceData();
        borderData.setPlaceName("Border");
        borderData.setCoords(currentLocation);
        borderData.setUuid(validityBorderGeofenceUUID);
        borderData.setRadius(validityGeofenceRadius);

        borderData.setNotificationType(transitionTypeExit);
        Geofence border = createGeofenceFromGeofenceData(borderData);
        validityBorderGeofenceUUID = border.getRequestId();
        Log.i(LOG_TAG, "Border centered at: " + currentLocation.toString() +
                "with uuid: " + validityBorderGeofenceUUID);

        PendingIntent intent = intentProvider.provideGeofencePendingIntent();
        /** now we have list of geofences, create geofencign request */
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.addGeofences(Arrays.asList(border));
        GeofencingRequest request = builder.build();
        try {
            LocationServices.GeofencingApi.addGeofences(
                    apiClient,
                    request,
                    intent
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.getStatusCode() == CommonStatusCodes.SUCCESS) {
                        Log.i(LOG_TAG, "Border successfully registered: " +
                                validityBorderGeofenceUUID);
                    } else {
                        Log.e(LOG_TAG, "Can't register border");
                        validityBorderGeofenceUUID = "";
                    }
                }
            });
        } catch (SecurityException se){
            Log.e(LOG_TAG, "Geofencing access denied, check permissions");
        }
    }

    private List<Geofence> formListOfGeofences(){
        List<Geofence> geofences = new ArrayList<>();
        for (int i = 0; i < knownFences.size(); ++i){
            geofences.add(createGeofenceFromGeofenceData(knownFences.get(i)));
        }
        Log.i(LOG_TAG, geofences.size() + " geofences created");
        return geofences;
    }

    private Geofence createGeofenceFromGeofenceData(GeofenceData geofenceData){
        LatLng place = geofenceData.getCoords();
        int radius = geofenceData.getRadius();

        Geofence.Builder builder = new Geofence.Builder();
        /** generate random id string and assign it to geofence so we can distinguish those
         * later */
        String id = UUID.randomUUID().toString();
        builder.setRequestId(id);
        geofenceData.setUuid(id);

        /** figure out transition type - string constants are in resources */
        int transitionType = 0;
        if (transitionTypeEnter.equals(geofenceData.getNotificationType())){
            transitionType |= Geofence.GEOFENCE_TRANSITION_ENTER;
        }
        if (transitionTypeExit.equals(geofenceData.getNotificationType())){
            transitionType |= Geofence.GEOFENCE_TRANSITION_EXIT;
        }

        builder.setRequestId(id)
                .setCircularRegion(place.latitude, place.longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitionType);
        return builder.build();
    }


    private void severConnection(Subscription connection){
        if (null != connection && !connection.isUnsubscribed()){
            connection.unsubscribe();
            connection = null;
        }
    }

    /**
     * We need to verify that this notification triggered by one of our areas
     * ( even though other scenarios very unlikely) and then call to external action -
     *
     * @param notificationIntent
     */
    private void handleGeofenceNotification(Intent notificationIntent){

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(notificationIntent);
        if (geofencingEvent.hasError()){
            String errorMessage = "Geofencing error with code: " + geofencingEvent.getErrorCode();
            Log.e(LOG_TAG, errorMessage);
            return;
        }

        int transitionType = geofencingEvent.getGeofenceTransition();
        List<GeofenceData> triggeredAreas = new ArrayList<>();
        for (Geofence gf : geofencingEvent.getTriggeringGeofences()){
            String uuid = gf.getRequestId();
            if (validityBorderGeofenceUUID.equals(uuid)){
                Log.i(LOG_TAG, "User leaving valid area");
                if (null != feedback){
                    feedback.onUserLeavingActualArea();
                }
                return;
            }
            /** could use HashMap instead, but there are few places anyways */
            for (GeofenceData data : knownFences){
                if (data.getUuid().equals(uuid)){
                    Log.i(LOG_TAG, "reached area with ID: " + data.getUuid());
                    triggeredAreas.add(data);
                }
            }
        }
        if (null == feedback){
            Log.e(LOG_TAG, " Geofencing manager were notified, buf feedback is null");
            return;
        }
        feedback.onNotification(triggeredAreas);
    }


}