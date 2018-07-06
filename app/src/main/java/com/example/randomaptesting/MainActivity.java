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
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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
    SeekBar radiusBar;
    TextView radiusTxt;
    ToggleButton cheap;
    ToggleButton normal;
    ToggleButton expensive;
    ToggleButton extreme;
    RatingBar ratingBar;
    TextView ratingTxt;
    String userKeyword;
    double userRadius;
    int userPrice;
    double userRating;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listenerForRadiusBar();
        cheap = findViewById(R.id.cheap);
        normal = findViewById(R.id.normal);
        expensive = findViewById(R.id.expensive);
        extreme = findViewById(R.id.extreme);
        listenerForRatingBar();
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

    public void onCheapClicked(View v) {
        cheap.setChecked(true);
        normal.setChecked(false);
        expensive.setChecked(false);
        extreme.setChecked(false);
    }

    public void onNormalClicked(View v) {
        cheap.setChecked(false);
        normal.setChecked(true);
        expensive.setChecked(false);
        extreme.setChecked(false);
    }

    public void onExpensiveClicked(View v) {
        cheap.setChecked(false);
        normal.setChecked(false);
        expensive.setChecked(true);
        extreme.setChecked(false);
    }

    public void onExtremeClicked(View v) {
        cheap.setChecked(false);
        normal.setChecked(false);
        expensive.setChecked(false);
        extreme.setChecked(true);
    }

    public void listenerForRatingBar() {
        ratingBar = findViewById(R.id.ratingBar);
        ratingTxt = findViewById(R.id.ratingTxt);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                ratingTxt.setText(Float.toString(rating));
                if (rating < 1.0f) {
                    ratingBar.setRating(1.0f);
                }
            }
        });
    }

    public void listenerForRadiusBar() {
        radiusBar = findViewById(R.id.radiusBar);
        radiusTxt = findViewById(R.id.radiusTxt);
        radiusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                userRadius = ((double)progress/10);
                radiusTxt.setText(Double.toString(userRadius));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (userRadius < 1) {
                    Toast.makeText(getApplicationContext(), "Minimum is 1, radius will be set to 1", Toast.LENGTH_SHORT).show();
                    userRadius = 1;
                }
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void mainFunc() {
        if (!returnInputsForURL()) {
            Toast.makeText(getApplicationContext(), "Please key in a keyword",Toast.LENGTH_SHORT).show();
            return;
        }
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
                                    for (int i = 0; i < matchUserReqList.size(); i++) {
                                        System.out.println(Integer.toString(i+1) + ". " + matchUserReqList.get(i));
                                    }
                                    System.out.println("Suggestions: "); // for debug purpose
                                    for (int i = 0; i < suggestions.size(); i++) {
                                        System.out.println(Integer.toString(i+1) + ". " + suggestions.get(i));
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


    private boolean returnInputsForURL() {
        userKeyword = keyWordInput.getText().toString();
        if (userKeyword.length() == 0) {
            return false;
        }
        userKeyword = userKeyword.replace(' ', '+');
        userRadius *= 1000;
//        System.out.println("userRadius: " + userRadius); // debug purpose
        if (cheap.isChecked()) {
            userPrice = 1;
        } else if (normal.isChecked()) {
            userPrice = 2;
        } else if (expensive.isChecked()) {
            userPrice = 3;
        } else {
            userPrice = 4;
        }
//        System.out.println("userPrice: " + userPrice); // for debug purpose
        userRating = ratingBar.getRating();
        return true;
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
