package com.trumplist.boycott.geofencing;

import com.trumplist.boycott.geofencing.web_responce.Post_;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by lodoss on 17/05/16.
 */
public class GeofenceData {
    public static GeofenceData fromWebResponse(Post_ geoFence){
        GeofenceData d = new GeofenceData();
        d.setRadius(geoFence.getRADIUS());
        LatLng loc = new LatLng(geoFence.getLAT(), geoFence.getLNG());
        d.setCoords(loc);
        d.setPlaceName(geoFence.getLOCNAME());
        d.setQuizOnTapQuizId(geoFence.getQuizontapQuizId());
        String zip = "";
        try {
            zip = (String) geoFence.getZIP();
        } catch (ClassCastException e){}
        d.setZip(zip);
        d.setNotificationType(geoFence.getGEOFENCENOTIFICATION());
        return d;
    }

    public GeofenceData(){}

    private int radius;
    private LatLng coords;
    String placeName;
    String zip;
    String notificationType;
    int quizOnTapQuizId;
    String uuid;

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public LatLng getCoords() {
        return coords;
    }

    public void setCoords(LatLng coords) {
        this.coords = coords;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public int getQuizOnTapQuizId() {
        return quizOnTapQuizId;
    }

    public void setQuizOnTapQuizId(int quizOnTapQuizId) {
        this.quizOnTapQuizId = quizOnTapQuizId;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
