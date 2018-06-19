package com.example.randomaptesting;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by chengchinlim on 5/29/18.
 */

public class MainActivity extends FragmentActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    private static final int LOC_REQ_CODE = 1;
    private String[] userInput = new String[3];
    EditText radiusInput;
    EditText keyInput;
    EditText priceInput;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Intent mainActivityIntent = getIntent();

        if (isLocationAccessPermitted()) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            Button submitButton = findViewById(R.id.submitBtn);
            submitButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // Code here executes on main thread after user presses button
                    mainFunc(mainActivityIntent);
                }
            });
        } else {
            Toast.makeText(this, "Location access is not permitted", Toast.LENGTH_LONG).show();
        }

    }

    @SuppressWarnings("MissingPermission")
    private void mainFunc(final Intent mainActivityIntent) {
        mFusedLocationClient.getLastLocation()
            .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Location mLastLocation = task.getResult();
                        double latitude = mLastLocation.getLatitude();
                        double longitude = mLastLocation.getLongitude();
                        System.out.println("Last known Location Latitude is " +
                                mLastLocation.getLatitude()); // debugPrint purpose
                        System.out.println("Last known Location Longitude is " +
                                mLastLocation.getLongitude()); // debugPrint purpose
                        returnUsersInputsForURL();
                        if (Double.parseDouble(userInput[0]) < 1000 || Double.parseDouble(userInput[0]) > 50000) {
                            radiusInput.setText("");
                            Toast.makeText(getApplicationContext(), "Radius range: 1-50", Toast.LENGTH_LONG).show();
                            return;
                        }
                        String completeUrl = constructNearbySearchUrl(latitude, longitude, "restaurant", userInput[0], userInput[1], userInput[2]);
                        System.out.println(completeUrl); // debugPrint purpose
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
                                        EditText ratingInput = findViewById(R.id.ratingTxt);
                                        String ratingString = ratingInput.getText().toString();
                                        double rating = Double.parseDouble(ratingString);
                                        if (rating < 0 || rating > 5) {
                                            ratingInput.setText("");
                                            Toast.makeText(getApplicationContext(), "Rating range: 1-5", Toast.LENGTH_LONG).show();
                                            return;
                                        }
                                        ArrayList<Destination> destinationList = jsonToJavaObj(listData, rating);
                                        Intent showResultActivity =  new Intent(MainActivity.this, ShowResult.class);
                                        showResultActivity.putExtra("DESTINATIONS", destinationList);
                                        startActivity(showResultActivity);
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

    private ArrayList<Destination> jsonToJavaObj(ArrayList<String> listData, double userDesireRating) {
        ArrayList<String>  nameList = getNameList(listData);
        ArrayList<String> addressList =  getAddressList(listData);
        ArrayList<String> placeIdList = getPlaceIdList(listData);
        ArrayList<Double> ratingList = getRatingList(listData);
        ArrayList<String> photoRefList = getPhotoRefList(listData);

        HashSet<Destination> destinationList = new HashSet<>();

        for (int i = 0; i < nameList.size(); i++) {
            Destination d = new Destination(nameList.get(i), addressList.get(i),
                    placeIdList.get(i), ratingList.get(i), photoRefList.get(i));
            if (d.getRating() >= userDesireRating)
                destinationList.add(d);
        }
        return new ArrayList<>(destinationList);
    }

    private void returnUsersInputsForURL() {
        radiusInput = findViewById(R.id.radiusTxt);
        keyInput = findViewById(R.id.keyTxt);
        priceInput = findViewById(R.id.priceTxt);
        String radiusString = radiusInput.getText().toString();
        double radius = Double.parseDouble(radiusString) * 1000;
        radiusString  = Double.toString(radius);
        String keyword = keyInput.getText().toString();
        String maxPrice = priceInput.getText().toString();
        userInput[0] = radiusString;
        userInput[1] = keyword;
        userInput[2] = maxPrice;
    }

    private String constructNearbySearchUrl(double latitude, double longitude,
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

    private ArrayList<String> getPhotoRefList(ArrayList<String> listData) {
        ArrayList<String> photoRefList = new ArrayList<>();
        int count = 1;
        String basicUrl = "https://maps.googleapis.com/maps/api/place/photo?";
        String heightWidth = "&maxheight=200&maxwidth=320&key=AIzaSyDpKpQ2S8lvUK7xfHGgSoJXy0HG9tFU-7s";
        for (String s: listData) {
            if (s.toLowerCase().contains("price_level") && s.toLowerCase().contains("rating")){
                int a = s.indexOf("photo_reference");
                int b = s.indexOf("width");
                String s1 = s.substring(a + 18, b - 3);
                s1 = s1.replace(' ', '+');
                String completeUrl = basicUrl + "photreference=" + s1 + heightWidth;
                photoRefList.add(completeUrl);
                System.out.println(count + ". " + completeUrl); // debugPrint purpose
            }
            count++;
        }
        return photoRefList;
    }

    private ArrayList<String> getAddressList(ArrayList<String> listData) {
        ArrayList<String> addressList = new ArrayList<>();
        int count = 1;
        for (String s: listData) {
            System.out.println(count + ". " + s.toString()); // debugPrint purpose
            if (s.toLowerCase().contains("price_level") && s.toLowerCase().contains("rating")){
                int a = s.indexOf("vicinity");
                int b = s.length();
                String s1 = s.substring(a + 11, b - 2);
                s1 = s1.replace(' ', '+');
                addressList.add(s1);
            }
            count++;
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
            if (s.toLowerCase().contains("price_level") && s.toLowerCase().contains("rating")){
                int a = s.indexOf("rating");
                int b = s.indexOf("\"reference\"");
//                System.out.println("a:" + a + " b: " + b); // debug purpose
                String s1 = s.substring(a + 8, b - 1);
//                System.out.println(s1); // debug purpose
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
