
package com.trumplist.boycott.geofencing.utils.zip_gmap_api_response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class Viewport {

    @SerializedName("northeast")
    @Expose
    private Northeast_ northeast;
    @SerializedName("southwest")
    @Expose
    private Southwest_ southwest;

    /**
     * 
     * @return
     *     The northeast
     */
    public Northeast_ getNortheast() {
        return northeast;
    }

    /**
     * 
     * @param northeast
     *     The northeast
     */
    public void setNortheast(Northeast_ northeast) {
        this.northeast = northeast;
    }

    /**
     * 
     * @return
     *     The southwest
     */
    public Southwest_ getSouthwest() {
        return southwest;
    }

    /**
     * 
     * @param southwest
     *     The southwest
     */
    public void setSouthwest(Southwest_ southwest) {
        this.southwest = southwest;
    }

}
