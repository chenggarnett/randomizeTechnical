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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by chengchinlim on 5/29/18.
 */

public class MainActivity extends FragmentActivity {

    private static final int LOC_REQ_CODE = 1;
    private String[] userInput = new String[4];
    private AutoCompleteTextView keyWordInput;
    EditText radiusInput;
    EditText priceInput;
    EditText ratingInput;

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
        int a = returnUsersInputsForURL();
        if(a != 0)
            return a;
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation()
            .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Location mLastLocation = task.getResult();
                        double latitude = mLastLocation.getLatitude();
                        double longitude = mLastLocation.getLongitude();
//                        System.out.println("Last known Location Latitude is " +
//                                mLastLocation.getLatitude()); // debugPrint purpose
//                        System.out.println("Last known Location Longitude is " +
//                                mLastLocation.getLongitude()); // debugPrint purpose
                        String completeUrl = constructNearbySearchUrl(latitude, longitude, "restaurant", userInput[0], userInput[1], userInput[2]);
                        System.out.println(completeUrl); // debugPrint purpose
                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, completeUrl,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    ArrayList<Destination> destinations = getDestinationList(response);
                                    Intent showResultActivity =  new Intent(MainActivity.this, ShowResult.class);
                                    showResultActivity.putExtra("DESTINATIONS", destinations);
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


    private int returnUsersInputsForURL() {
        radiusInput = findViewById(R.id.radiusTxt);
        String radiusString = radiusInput.getText().toString();
        double radius = Double.parseDouble(radiusString) * 1000;
        if (radius < 1000 || radius > 50000) {
            radiusInput.setText("");
            Toast.makeText(getApplicationContext(), "Radius range: 1-50", Toast.LENGTH_LONG).show();
            return 2;
        }
        radiusString  = Double.toString(radius);
        String keyword = keyWordInput.getText().toString();
        keyword = keyword.replace(' ', '+');
        priceInput = findViewById(R.id.priceTxt);
        String maxPrice = priceInput.getText().toString();
        ratingInput = findViewById(R.id.ratingTxt);
        String ratingString = ratingInput.getText().toString();
        double userRating = Double.parseDouble(ratingString);
        if (userRating < 0 || userRating > 5) {
            ratingInput.setText("");
            Toast.makeText(getApplicationContext(), "Rating range: 1-5", Toast.LENGTH_LONG).show();
            return 3;
        }
        userInput[0] = radiusString;
        userInput[1] = keyword;
        userInput[2] = maxPrice;
        userInput[3] = Double.toString(userRating);
        for (int i = 0; i < userInput.length; i++) {
            if (userInput[i] == null)
                return 1;
        }

        return 0;
    }

    private ArrayList<Destination> getDestinationList(String response) {
        try {
            JSONObject obj = new JSONObject(response);
            JSONArray results = obj.getJSONArray("results");
            HashSet<Destination> destinationList = new HashSet<>();
            for (int i = 0; i < results.length(); i++) {
                String name = results.getJSONObject(i).getString("name");
                String address = results.getJSONObject(i).getString("vicinity");
                String placeId = results.getJSONObject(i).getString("place_id");
                String ratingStr = results.getJSONObject(i).getString("rating");
                if (ratingStr == null)
                    continue;
                double rating = Double.parseDouble(ratingStr);
                Destination d = new Destination(name, address, placeId, rating);
                if (rating >= Double.parseDouble(userInput[3]))
                    destinationList.add(d);
            }
//            for (Destination d: destinationList) { // for debug purpose
//                System.out.println(d);
//            }
            return new ArrayList<>(destinationList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
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
