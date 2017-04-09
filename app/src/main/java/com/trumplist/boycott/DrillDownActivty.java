package com.trumplist.boycott;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class DrillDownActivty extends AppCompatActivity {
    int compNum;
    RequestQueue requestQueue;
    Resources res;
    String LOG_TAG = "Drill Down";
    static ArrayList<String> webContact= new ArrayList<>();
    static ArrayList<String> emails = new ArrayList<>();
    static ArrayList<String> phone = new ArrayList<>();
    static ArrayList<String> name = new ArrayList<>();
    static ArrayList<String> title = new ArrayList<>();
    String compName ;
    String companyWhy;

    TextView compNameTextView;
    TextView compWhyTextView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drill_down_activty);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        res = getResources();

        compNameTextView = (TextView) findViewById(R.id.companyname);
        compWhyTextView = (TextView) findViewById(R.id.companywhy);


        Intent incomingIntent = getIntent();
        Bundle incomingBundle = incomingIntent.getExtras();

        if (incomingBundle != null){
            if (incomingBundle.containsKey("company")){
                compNum = incomingIntent.getIntExtra("company", 1);
                Log.i(LOG_TAG, compNum + " contains");
                companyInfo();

                compNameTextView.setText(compName);
                compWhyTextView.setText(companyWhy);
            }
            else {
                Log.i(LOG_TAG, "no company");
            }

        }


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }



    public void companyInfo(){
        String requestURL = res.getString(R.string.server)+"notificationResponse.php?format=json&COMP_NUM="+compNum;

        requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jor = new JsonObjectRequest(Request.Method.GET, requestURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try{

                            Log.i(LOG_TAG, response.toString() + " Company Load");
                            JSONArray ja = response.getJSONArray("posts");



                            for(int i=0; i < ja.length(); i++) {

                                JSONObject jsonObject = ja.getJSONObject(i);


                                JSONObject interior = jsonObject.getJSONObject("post");
                                compName = interior.getString("Company");
                                companyWhy = interior.getString("WHY");

                                webContact.add(i, interior.getString("WEB_CONTACT"));
                                emails.add(i, interior.getString("CONTACT_EMAIL"));
                                phone.add(i, interior.getString("CONTACT_PHONE"));
                                name.add(i, interior.getString("CONTACT_NAME"));
                                title.add(i, interior.getString("CONTACT_TITLE"));

                               // Log.i(LOG_TAG, compName + "   " + companyWhy);
                                compNameTextView.setText(compName);
                                compWhyTextView.setText(companyWhy);

                            }




                        }catch(JSONException e){
                            e.printStackTrace();
                        }







                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(LOG_TAG, "Volley Error "+error);

                    }
                }
        );


        requestQueue.add(jor);






    }

}
