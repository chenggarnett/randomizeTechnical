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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by chengchinlim on 5/29/18.
 */

public class MainActivity extends FragmentActivity {

    private static final int LOC_REQ_CODE = 1;
    private AutoCompleteTextView keyWordInput;
    EditText radiusInput;
    EditText priceInput;
    EditText ratingInput;
    String userKeyword;
    double userRadius;
    double userPrice;
    double userRating;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        keyWordInput = findViewById(R.id.searchKeyTxt);
        ArrayList<String> keywords = getKeywordsForAutocomplete();
//        for (String keyword: keywords) { // for debug purpose
//            System.out.println("Keyword: " +keyword);
//        }
        ArrayAdapter adapter = new
                ArrayAdapter(this,android.R.layout.simple_list_item_1, keywords);
        keyWordInput.setAdapter(adapter);
        keyWordInput.setThreshold(1);
    }

    public void onSubmitClicked(View v) {
        mainFunc();
    }

    @SuppressWarnings("MissingPermission")
    private int mainFunc() {
        int a = returnInputsForURL();
        if(a != 0)
            return a;
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation()
            .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Location mLastLocation = task.getResult();
                        final double myLatitude = mLastLocation.getLatitude();
                        final double myLongitude = mLastLocation.getLongitude();
//                        System.out.println("Last known Location Latitude is " +
//                                mLastLocation.getLatitude()); // debugPrint purpose
//                        System.out.println("Last known Location Longitude is " +
//                                mLastLocation.getLongitude()); // debugPrint purpose
                        String completeUrl = constructNearbySearchUrl(myLatitude, myLongitude, "restaurant", userRadius * 1.5, userKeyword);
                        System.out.println(completeUrl); // debugPrint purpose
                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, completeUrl,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    ArrayList<Destination> destinationList = new ArrayList<>();
                                    try {
                                        JSONObject obj = new JSONObject(response);
                                        JSONArray results = obj.getJSONArray("results");
                                        for (int i = 0; i < results.length(); i++) {
                                            String name = results.getJSONObject(i).getString("name");
                                            String address = results.getJSONObject(i).getString("vicinity");
                                            String placeId = results.getJSONObject(i).getString("place_id");
                                            String ratingStr = results.getJSONObject(i).getString("rating");
                                            String priceStr = results.getJSONObject(i).getString("price_level");
                                            String latitude = results.getJSONObject(i).getJSONObject("geometry")
                                                    .getJSONObject("location").getString("lat");
                                            String longitude = results.getJSONObject(i).getJSONObject("geometry")
                                                    .getJSONObject("location").getString("lng");
                                            double placeLatitude = Double.parseDouble(latitude);
                                            double placeLongitude = Double.parseDouble(longitude);
//                                            System.out.println("placeLatitude: " + placeLatitude); // for debug purpose
//                                            System.out.println("placeLongitude: " + placeLongitude); // for debug purpose
                                            double distance = calculateDistance(myLatitude, myLongitude, placeLatitude, placeLongitude) * 1000;
//                                            System.out.println("Distance:" + distance); // for debug purpose
                                            if (ratingStr == null) {
                                                Destination d = new Destination(name, address, placeId, distance);
                                                destinationList.add(d);
                                            }
                                            double rating = Double.parseDouble(ratingStr);
                                            if (priceStr == null) {
                                                Destination d = new Destination(name, address, placeId, distance, rating);
                                                destinationList.add(d);
                                            }
                                            int price = Integer.parseInt(priceStr);
                                            Destination d = new Destination(name, address, placeId, distance, rating, price);
                                            destinationList.add(d);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("Destinations:"); // for debug purpose
                                    for (int i = 0; i < destinationList.size(); i++) {
                                        System.out.println(i+1 + ". " + destinationList.get(i));
                                    }

                                    ArrayList<Destination> matchUserReqList = new ArrayList<>();
                                    ArrayList<Destination> suggestions = new ArrayList<>();

                                    for (Destination d: destinationList) {
                                        if (matchUserReq(d)) {
                                            matchUserReqList.add(d);
                                        } else {
                                            suggestions.add(d);
                                        }
                                    }
                                    System.out.println("MatchUserReqList: "); // for debug purpose
                                    for (Destination d : matchUserReqList) {
                                        System.out.println(d);
                                    }
                                    System.out.println("Suggestions: "); // for debug purpose
                                    for (Destination d : suggestions) {
                                        System.out.println(d);
                                    }
                                    Intent showResultActivity =  new Intent(MainActivity.this, ShowResult.class);
                                    showResultActivity.putExtra("matchUserReqList", matchUserReqList);
                                    showResultActivity.putExtra("suggestions", suggestions);
                                    startActivity(showResultActivity);
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
        return 0;
    }

    private boolean matchUserReq(Destination d) {
        if (d.getDistance() > userRadius) {
            return false;
        }
        if (d.getRating() != 0) {
            if (d.getRating() < userRating) {
                return false;
            }
        } else {
            return false;
        }
        if (d.getPrice() != 0) {
            if (d.getPrice() > userPrice) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    private double calculateDistance(double myLatitude, double myLongitude, double placeLatitude, double placeLongitude) {
        double earthRadius = 6371;
        double latDiff = degreeToRadians(placeLatitude - myLatitude);
        double longDiff = degreeToRadians(placeLongitude - myLongitude);
        double a = Math.pow(Math.sin(latDiff/2), 2)
                + Math.cos(degreeToRadians(myLatitude)) * Math.cos(degreeToRadians(placeLatitude))
                * Math.pow(Math.sin(longDiff/2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = earthRadius * c;
        return d;
    }

    private double degreeToRadians(double degree) {
        return degree * (Math.PI/180);
    }


    private int returnInputsForURL() {
        radiusInput = findViewById(R.id.radiusTxt);
        String radiusString = radiusInput.getText().toString();
        userRadius = Double.parseDouble(radiusString) * 1000;
        if (userRadius < 1000 || userRadius > 10000) {
            radiusInput.setText("");
            Toast.makeText(getApplicationContext(), "Radius range: 1-10", Toast.LENGTH_LONG).show();
            return 2;
        }
        userKeyword = keyWordInput.getText().toString();
        userKeyword = userKeyword.replace(' ', '+');
        priceInput = findViewById(R.id.priceTxt);
        String price = priceInput.getText().toString();
        userPrice = Double.parseDouble(price);
        ratingInput = findViewById(R.id.ratingTxt);
        String ratingString = ratingInput.getText().toString();
        userRating = Double.parseDouble(ratingString);
        if (userRating < 0 || userRating > 5) {
            ratingInput.setText("");
            Toast.makeText(getApplicationContext(), "Rating range: 1-5", Toast.LENGTH_LONG).show();
            return 3;
        }
        return 0;
    }

    private ArrayList<String> getKeywordsForAutocomplete() {
        System.out.println("Inside getKeywordsForAutoComplete function"); // for debug purpose
        ArrayList<String> keywords = new ArrayList<>();
        InputStream is = getResources().openRawResource(R.raw.keywords);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                keywords.add(line);
            }
            br.close();
        }
        catch (IOException e) {
            System.out.println("Failed to load keywords from text file");
        }
        return keywords;
    }

    private String constructNearbySearchUrl(double latitude, double longitude,
                                            String type, double radius, String key) {
        String basicUrl ="https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
        String latAndLong = "location=" + Double.toString(latitude) + "," + Double.toString(longitude);
        String radiusFrCurrentLocation = "&radius=" + radius;
        String placeType = "&type=" + type;
        String keyword = "&keyword=" + key + "&opennow=1";
        String apiKey = "&key=AIzaSyDpKpQ2S8lvUK7xfHGgSoJXy0HG9tFU-7s";
        String completeUrl = basicUrl + latAndLong + radiusFrCurrentLocation
                + placeType + keyword + apiKey;
        return completeUrl;
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
