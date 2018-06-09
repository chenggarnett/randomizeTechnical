package com.example.randomaptesting;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by chengchinlim on 5/29/18.
 */

public class MainActivity extends FragmentActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    protected Location mLastLocation;
    private static final int LOC_REQ_CODE = 1;
    protected ArrayList<String> addressList;
    protected ArrayList<String>  nameList;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView mTextView = findViewById(R.id.text);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();
    }

    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation()
            .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        mLastLocation = task.getResult();
                        double latitude = mLastLocation.getLatitude();
                        double longitude = mLastLocation.getLongitude();
                        String result = "Last known Location Latitude is " +
                                mLastLocation.getLatitude() + "\n" +
                                "Last known longitude Longitude is " + mLastLocation.getLongitude();
                        System.out.println(result);
                        String completeUrl = constructUrl(latitude, longitude);
                        System.out.println(completeUrl);
                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, completeUrl,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            JSONObject obj = new JSONObject(response);
                                            JSONArray results = obj.getJSONArray("results");

                                            ArrayList<String> listData = new ArrayList<String>();
                                            JSONArray jArray = results;
                                            if (jArray != null) {
                                                for (int i=0;i<jArray.length();i++){
                                                    listData.add(jArray.getString(i));
                                                }
                                            }
                                            nameList =  new ArrayList<>(getNameList(listData));
                                            addressList = new ArrayList<> (getAddressList(listData));
                                            for (String s : nameList) {
                                                System.out.println("Name: " + s);
                                            }
                                            for (String s: addressList) {
                                                System.out.println("Address: " + s);
                                            }
                                            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + addressList.get(0) + ",San+Jose,United+States");
                                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                            mapIntent.setPackage("com.google.android.apps.maps");
                                            startActivity(mapIntent);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                System.out.println("Volley Error");
                            }
                        }
                        );

                        queue.add(stringRequest);
                    } else {
                        System.out.println("No Last known location found. Try current location..!");
                    }
                }
            });
    }

    private String constructUrl(double latitude, double longitude) {
        String basicUrl ="https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
        String latAndLong = "location=" + Double.toString(latitude) + "," + Double.toString(longitude);
        String radiusFrCurrentLocation = "&radius=3000";
        String placeType = "&type=restaurant";
        String keyword = "&keyword=food&opennow=1";
        String apiKey = "&key=AIzaSyDpKpQ2S8lvUK7xfHGgSoJXy0HG9tFU-7s";
        String completeUrl = basicUrl + latAndLong + radiusFrCurrentLocation
                + placeType + keyword + apiKey;
        return completeUrl;
    }

    private HashSet<String> getAddressList(ArrayList<String> listData) {
        HashSet<String> addressList = new HashSet<>();
        for (String s: listData) {
            System.out.println(s.toString());
            int a = s.indexOf("vicinity");
            int b = s.length();
            String s1 = s.substring(a + 11, b - 2);
            s1 = s1.replace(' ', '+');
            addressList.add(s1);
        }
        return addressList;
    }

    private HashSet<String> getNameList(ArrayList<String> listData) {
        HashSet<String> nameList = new HashSet<>();
        for (String s: listData) {
            //System.out.println(s.toString());
            int a = s.indexOf("name");
            int b = s.indexOf("opening");
            String s2 = s.substring(a + 7, b - 3);
            s2 = s2.replace(' ', '+');
            nameList.add(s2);
        }
        return nameList;
    }

    private boolean isLocationAccessPermitted() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    private void requestLocationAccessPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOC_REQ_CODE);
    }
}
