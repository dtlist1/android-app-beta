
package com.trumplist.boycott.geofencing.web_responce;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class Post_ {

    @SerializedName("LOCATION_ID")
    @Expose
    private Integer locationID;

    /*@SerializedName("quizontap_quiz_id")
    @Expose
    private Integer quizontapQuizId;*/
    @SerializedName("COMP_NUM")
    @Expose
    private Integer companyID;

    @SerializedName("COMP_NAME")
    @Expose
    private String COMPNAME;
    /*
    @SerializedName("FULL_ADDRESS")
    @Expose
    private String FULLADDRESS;
    */

    @SerializedName("ADDRESS_1")
    @Expose
    private Object ADDRESS_1;

    @SerializedName("ADDRESS_2")
    @Expose
    private Object ADDRESS_2;

    @SerializedName("CITY")
    @Expose
    private String CITY;

    @SerializedName("STATE")
    @Expose
    private String STATE;
    @SerializedName("ZIP")
    @Expose
    private Object ZIP;
    /*
    @SerializedName("PHONE")
    @Expose
    private Object PHONE;
  */

    @SerializedName("LAT")
    @Expose
    private Double LAT;

    @SerializedName("LONG")
    @Expose
    private Double LNG;

    @SerializedName("RADIUS")
    @Expose
    private Integer RADIUS;

    /*@SerializedName("GEO_FENCE_NOTIFICATION")
    @Expose
    private String GEOFENCENOTIFICATION;

    @SerializedName("DWELL_TIME_MIN")
    @Expose
    private Object DWELLTIMEMIN;
    @SerializedName("CREATE_DATETIME")
    @Expose
    private String CREATEDATETIME;
    @SerializedName("UPDATE_DATETIME")
    @Expose
    private String UPDATEDATETIME;*/

    @SerializedName("ACTIVE_FLAG")
    @Expose
    private Integer ACTIVEFLAG;

    /**
     * 
     * @return
     *     The idquizontapGeoFencedquiz
     */
    public Integer getId() {
        return locationID;
    }

    /**
     * 
     * @param locationID
     *     The locationID
     */
    public void setId(Integer locationID) {
        this.locationID = locationID;
    }

    /**
     * 
     * @return
     *     The companyID
     */
    public Integer getCompanyID() {
        return companyID;
    }

    /**
     * 
     * @param companyID
     *     The companyID
     */
    public void setCompanyID(Integer companyID) {
        this.companyID = companyID;
    }

    /**
     * 
     * @return
     *     The LOCNAME
     */
    public String getCOMPNAME() {
        return COMPNAME;
    }

    /**
     * 
     * @param LOCNAME
     *     The LOC_NAME
     */
    public void setCOMPNAME(String LOCNAME) {
        this.COMPNAME = COMPNAME;
    }

    /**
     *
     * @return
     *     The ADDRESS_1
     */
    public Object getADDRESS() {
        return ADDRESS_1;
    }

    /**
     * 
     * @param ADDRESS_1
     *     The ADDRESS_1
     */
    public void setADDRESS_1 (Object ADDRESS) {
        this.ADDRESS_1 = ADDRESS_1;
    }

    /**
     *
     * @return
     *     The ADDRESS_1
     */
    public Object getADDRESS_2() {
        return ADDRESS_2;
    }

    /**
     *
     * @param ADDRESS_2
     *     The ADDRESS_2
     */
    public void setADDRESS_2 (Object ADDRESS) {
        this.ADDRESS_2 = ADDRESS_2;
    }

    /**
     * 
     * @return
     *     The CITY
     */
    public String getCITY() {
        return CITY;
    }

    /**
     * 
     * @param CITY
     *     The CITY
     */
    public void setCITY(String CITY) {
        this.CITY = CITY;
    }

    /**
     * 
     * @return
     *     The STATE
     */
    public String getSTATE() {
        return STATE;
    }

    /**
     * 
     * @param STATE
     *     The STATE
     */
    public void setSTATE(String STATE) {
        this.STATE = STATE;
    }

    /**
     * 
     * @return
     *     The ZIP
     */
    public Object getZIP() {
        return ZIP;
    }

    /**
     * 
     * @param ZIP
     *     The ZIP
     */
    public void setZIP(Object ZIP) {
        this.ZIP = ZIP;
    }

    /**
     * 
     * @return
     *     The LAT
     */
    public Double getLAT() {
        return LAT;
    }

    /**
     * 
     * @param LAT
     *     The LAT
     */
    public void setLAT(Double LAT) {
        this.LAT = LAT;
    }

    /**
     * 
     * @return
     *     The LNG
     */
    public Double getLNG() {
        return LNG;
    }

    /**
     * 
     * @param LNG
     *     The LNG
     */
    public void setLNG(Double LNG) {
        this.LNG = LNG;
    }

    /**
     * 
     * @return
     *     The RADIUS
     */
    public Integer getRADIUS() {
        return RADIUS;
    }

    /**
     * 
     * @param RADIUS
     *     The RADIUS
     */
    public void setRADIUS(Integer RADIUS) {
        this.RADIUS = RADIUS;
    }


    /**
     * 
     * @return
     *     The ACTIVEFLAG
     */
    public Integer getACTIVEFLAG() {
        return ACTIVEFLAG;
    }

    /**
     * 
     * @param ACTIVEFLAG
     *     The ACTIVE_FLAG
     */
    public void setACTIVEFLAG(Integer ACTIVEFLAG) {
        this.ACTIVEFLAG = ACTIVEFLAG;
    }

}
