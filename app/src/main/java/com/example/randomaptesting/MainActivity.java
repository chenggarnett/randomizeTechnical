package com.example.randomaptesting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by chengchinlim on 5/29/18.
 */

public class MainActivity extends FragmentActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    protected Location mLastLocation;
    private static final int LOC_REQ_CODE = 1;

    protected ArrayList<String> placeIdList;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Button submitButton = findViewById(R.id.submitBtn);
        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                mainFunc();
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void mainFunc() {
        mFusedLocationClient.getLastLocation()
            .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        mLastLocation = task.getResult();
                        double latitude = mLastLocation.getLatitude();
                        double longitude = mLastLocation.getLongitude();
//                        String result = "Last known Location Latitude is " +
//                                mLastLocation.getLatitude() + "\n" +
//                                "Last known longitude Longitude is " + mLastLocation.getLongitude();
//                        System.out.println(result); // debug purpose
                        String[] userInput = returnUsersInputsForURL();
                        String completeUrl = constructUrl(latitude, longitude, "restaurant", userInput[0], userInput[1], userInput[2]);
                        System.out.println(completeUrl); // debug purpose
                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, completeUrl,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        JSONObject obj = new JSONObject(response);
                                        JSONArray results = obj.getJSONArray("results");

                                        ArrayList<String> listData = new ArrayList<>();
                                        JSONArray jArray = results;
                                        if (jArray != null) {
                                            for (int i=0;i<jArray.length();i++){
                                                listData.add(jArray.getString(i));
                                            }
                                        }
                                        placeIdList = getPlaceIdList(listData);
                                        debug(listData);
                                        getTheOne();
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

    private void debug(ArrayList<String> listData) {
        ArrayList<String> addressList =  getNameList(listData);
        ArrayList<String>  nameList = getAddressList(listData);
        ArrayList<Double> ratingList = getRatingList(listData);
        for (int i = 0; i < nameList.size(); i++) { // debug purpose
            System.out.println("Name: " + nameList.get(i) +
                    " Rating: " + ratingList.get(i) + " Address: " + addressList.get(i));
        }
        System.out.println("Random number: " + randomize(placeIdList.size())); // debug purpose
    }

    private String[] returnUsersInputsForURL() {
        String[] userInput = new String[3];
        EditText keyInput = findViewById(R.id.keyTxt);
        EditText radiusInput = findViewById(R.id.radiusTxt);
        EditText priceInput = findViewById(R.id.priceTxt);
        String keyword = keyInput.getText().toString();
        String radiusString = radiusInput.getText().toString();
        String maxPrice = priceInput.getText().toString();
        double tempRadius = Double.parseDouble(radiusString) * 1000;
        radiusString  = Double.toString(tempRadius);
        userInput[0] = radiusString;
        userInput[1] = keyword;
        userInput[2] = maxPrice;
        return userInput;
    }

    private void getTheOne() {
        GeoDataClient mGeoDataClient = Places
                .getGeoDataClient(getApplicationContext(), null);
        mGeoDataClient.getPlaceById(placeIdList.get(randomize(placeIdList.size())))
                .addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                if (task.isSuccessful()) {
                    PlaceBufferResponse places = task.getResult();
                    Place myPlace = places.get(0);
                    System.out.println("Place found: " + myPlace.getName()); // debug purpose
                    String editedAddress = myPlace.getAddress()
                            .toString().replace(' ', '+');
                    goToDestination(editedAddress);
                    places.release();
                } else {
                    System.out.println("Place not found.");
                }
            }
        });
    }

    private void goToDestination(String editedAddress) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + editedAddress);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private String constructUrl(double latitude, double longitude,
                                String type, String radius, String key, String maxPrice) {
        String basicUrl ="https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
        String latAndLong = "location=" + Double.toString(latitude) + "," + Double.toString(longitude);
        String radiusFrCurrentLocation = "&radius=" + radius;
        String placeType = "&type=" + type;
        String keyword = "&keyword=" + key + "&opennow=1";
        String maxP = "&maxprice=" + maxPrice;
        String apiKey = "&key=AIzaSyDpKpQ2S8lvUK7xfHGgSoJXy0HG9tFU-7s";
        String completeUrl = basicUrl + latAndLong + radiusFrCurrentLocation
                + placeType + keyword + maxP + apiKey;
        return completeUrl;
    }

    private ArrayList<String> getAddressList(ArrayList<String> listData) {
        ArrayList<String> addressList = new ArrayList<>();
        for (String s: listData) {
            System.out.println(s.toString()); // debug purpose
            if (s.toLowerCase().contains("price_level") && s.toLowerCase().contains("rating")){
                int a = s.indexOf("vicinity");
                int b = s.length();
                String s1 = s.substring(a + 11, b - 2);
                s1 = s1.replace(' ', '+');
                addressList.add(s1);
            }
        }
        return addressList;
    }

    private ArrayList<String> getNameList(ArrayList<String> listData) {
        ArrayList<String> nameList = new ArrayList<>();
        for (String s: listData) {
            if (s.toLowerCase().contains("price_level") && s.toLowerCase().contains("rating")) {
                int a = s.indexOf("name");
                int b = s.indexOf("opening");
                String s2 = s.substring(a + 7, b - 3);
                s2 = s2.replace(' ', '+');
                nameList.add(s2);
            }
        }
        return nameList;
    }

    private ArrayList<Double> getRatingList(ArrayList<String> listData) {
        ArrayList<Double> ratingList = new ArrayList<>();
        for (String s: listData) {
            //System.out.println(s.toString()); // debug purpose
            if (s.toLowerCase().contains("price_level") && s.toLowerCase().contains("rating")){
                int a = s.indexOf("rating");
                int b = s.indexOf("\"reference\"");
                System.out.println("a:" + a + " b: " + b);
                String s1 = s.substring(a + 8, b - 1);
                System.out.println(s1);
                double rating = Double.parseDouble(s1);
                ratingList.add(rating);
            }
        }
        return ratingList;
    }

    private ArrayList<String> getPlaceIdList(ArrayList<String> listData) {
        ArrayList<String> placeIdList = new ArrayList<>();
        for (String s: listData) {
            int a = s.indexOf("place_id");
            int b = 0;
            String placeId = "";
            if (s.toLowerCase().contains("price_level")) {
                b = s.indexOf("price_level");
                placeId = s.substring(a + 11, b - 3);
                placeIdList.add(placeId);
            } else if (s.toLowerCase().contains("rating")) {
                b = s.indexOf("rating");
                placeId = s.substring(a + 11, b - 3);
                placeIdList.add(placeId);
            } else {
                System.out.println("fml no rating nor price level");
            }
        }
        return placeIdList;
    }

    private int randomize(int placeIdListSize) {
        Random r = new Random();
        int a = r.nextInt(placeIdListSize);
        return a;
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
