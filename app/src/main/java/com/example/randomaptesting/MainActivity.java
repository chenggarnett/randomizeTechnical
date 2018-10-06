package com.example.randomaptesting;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by chengchinlim on 5/29/18.
 */

public class MainActivity extends FragmentActivity {

    String userKeyword = "burrito";
    double userRadius = 1000;
    int userPrice = 2;
    double userRating = 3;
    boolean includePrice = false;
//    SharedPreferences sharedPref = getSharedPreferences("previousDestination", Context.MODE_PRIVATE);

    // main function of this activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!isNetworkConnected()) { // if there is not internet connection
            showDialog(); // prompt user to enable internet connection
        }
        if (!checkLocationAccessPermitted())  { // if location service is not enabled
            requestLocationAccessPermission(); // prompt user to open location service
        }
        getUserLocationThenCallAPI(); // get user location then callGoogleMapsAPI()
    }

    // get user's location and call Google Maps API
    @SuppressWarnings("MissingPermission")
    private void getUserLocationThenCallAPI() {
        // create a fused location client, it is needed to get user's location
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation()
                // it needs a listener to check if this task is completed
            .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) { // if it is completed
                    // if the user's location retrieval is successful
                    if (task.isSuccessful() && task.getResult() != null) {
                        Location mLastLocation = task.getResult(); // put user's location into a Location variable
                        final double myLatitude = mLastLocation.getLatitude(); // get the latitude
                        final double myLongitude = mLastLocation.getLongitude(); // get the longitude
//                        System.out.println("Last known Location Latitude is " +
//                                mLastLocation.getLatitude()); // debug purpose
//                        System.out.println("Last known Location Longitude is " +
//                                mLastLocation.getLongitude()); // debug purpose

                        // this function has to call in this "if" statement because the value
                        // of the Location variable is local, it would be null if accessed outside
                        callGoogleMapsApiToRetrieveData(myLatitude, myLongitude);
                    } else {
                        System.out.println("No Last known location found. Try current location..!");
                    }
                }
            });
    }

    /* call Google Maps API through a URL address
    *  Google Maps API would return a json file
    *  Json functions are used to split the data to respective variables, e.g. name, address, rating
    *  @param myLatitude: latitude of user's location
    *  @param myLongitude: longitude of user's location
    * */
    public void callGoogleMapsApiToRetrieveData(final double myLatitude, final double myLongitude) {
        String completeUrl = constructNearbySearchUrl(myLatitude, myLongitude, "restaurant", userRadius * 1.3, userKeyword);
        System.out.println(completeUrl); // debug purpose
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, completeUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        ArrayList<Destination> destinationList = new ArrayList<>();
                        try {
                            JSONObject obj = new JSONObject(response);
                            JSONArray results = obj.getJSONArray("results");
                            if (results.length() == 0) {
                                Toast.makeText(getApplicationContext(), "There is no nearby" + " \"" + userKeyword + "\" " +  "restaurants", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            for (int i = 0; i < results.length(); i++) {
                                String latitude = results.getJSONObject(i).getJSONObject("geometry")
                                        .getJSONObject("location").getString("lat");
                                String longitude = results.getJSONObject(i).getJSONObject("geometry")
                                        .getJSONObject("location").getString("lng");
                                double placeLatitude = Double.parseDouble(latitude);
                                double placeLongitude = Double.parseDouble(longitude);
//                                System.out.println("placeLatitude: " + placeLatitude); // debug purpose
//                                System.out.println("placeLongitude: " + placeLongitude); // debug purpose
                                double distance = calculateDisplacement(myLatitude, myLongitude, placeLatitude, placeLongitude) * 1000;
//                                System.out.println("Distance:" + distance); // debug purpose
                                String name = results.getJSONObject(i).getString("name");
                                String placeId = results.getJSONObject(i).getString("place_id");
                                String address = results.getJSONObject(i).getString("vicinity");
                                int price = 0;
                                if (results.getJSONObject(i).has("price_level")) {
                                    includePrice = true;
                                    String price_level = results.getJSONObject(i).getString("price_level");
                                    price = Integer.parseInt(price_level);
                                }
                                double rating = 0;
                                if (results.getJSONObject(i).has("rating")) {
                                    String r = results.getJSONObject(i).getString("rating");
                                    rating = Double.parseDouble(r);
                                }
                                Destination d = new Destination(name, address, placeId, distance);
                                d.setPrice(price);
                                d.setRating(rating);
                                destinationList.add(d);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (!includePrice) {
                            Toast.makeText(getApplicationContext(), "All nearby restaurants do not have price", Toast.LENGTH_SHORT).show();
                        }
//                        System.out.println("Destinations:"); // debug purpose
//                        for (int i = 0; i < destinationList.size(); i++) {
//                            System.out.println(i+1 + ". " + destinationList.get(i));
//                        }

                        // create two array lists to separate all the restaurants retrieved from the API
                        ArrayList<Destination> matchUserReqList = new ArrayList<>();
                        ArrayList<Destination> suggestions = new ArrayList<>();

                        for (Destination d: destinationList) {
                            if (matchUserReq(d, includePrice)) { // if it matches user requirement
                                matchUserReqList.add(d); // add into this list
                            } else { // if not
                                suggestions.add(d); // add into this list
                            }
                        }
//                        System.out.println("MatchUserReqList: "); // debug purpose
//                        for (int i = 0; i < matchUserReqList.size(); i++) {
//                            System.out.println(Integer.toString(i+1) + ". " + matchUserReqList.get(i));
//                        }
//                        System.out.println("Suggestions: "); // debug purpose
//                        for (int i = 0; i < suggestions.size(); i++) {
//                            System.out.println(Integer.toString(i+1) + ". " + suggestions.get(i));
//                        }

                        // create a new intent to switch to the next activity called: "Show result"
                        Intent showResultActivity =  new Intent(MainActivity.this, ShowResult.class);
                        // pass both array list to the next activity
                        showResultActivity.putExtra("matchUserReqList", matchUserReqList);
                        showResultActivity.putExtra("suggestions", suggestions);
                        // start the next activity
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
    }
    /* check if the destination matches the user's criteria
    * @return true if it matches or vice versa
    * @param d: the restaurant that is going to checked if it matches user's criteria
    * @param includePrice: if it is true include price then vice versa
    * this happens because sometimes all restaurants in both array lists do not have price
    * but we still want to show some restaurants in the list instead of nothing
    * */
    private boolean matchUserReq(Destination d, boolean includePrice) {
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
        if (includePrice) {
            if (d.getPrice() > userPrice) {
                return false;
            }
        }

        return true;
    }

    /* the below two functions are related to shared preferences
    *  it is not yet working, would make it work soon
    *
    * */
//    private void savePreviousDestination(ArrayList<Destination> prevDestination) {
//
//        SharedPreferences.Editor editor = sharedPref.edit();
//        Gson gson = new Gson();
//        String json = gson.toJson(prevDestination);
//        editor.putString("prevDestination", json);
//        editor.commit();
//    }
//
//    private ArrayList<Destination> retrievePreviousDestinations() {
//        Gson gson = new Gson();
//        String json = sharedPref.getString("prevDestination", "");
//        Type type = new TypeToken<ArrayList<Destination>>() {}.getType();
//        ArrayList<Destination> destinations = gson.fromJson(json, type);
//        return destinations;
//    }

    /* calculates the displacement between two coordinates (but not distance!!)
    *  distance is only accurate using Distance Matrix API
    *  @return the displacement between two coordinates
    *  the parameters are the coordinates of user's location and restaurant's location
    * */
    private double calculateDisplacement(double myLatitude, double myLongitude, double placeLatitude, double placeLongitude) {
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

    /* convert degree to radians to perform calculations in the above function
    * */
    private double degreeToRadians(double degree) {
        return degree * (Math.PI/180);
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

    /*
    * The two functions below are used to check if there is Wifi or data connection
    * If not, prompt user to open Wifi
    * */

    private boolean isNetworkConnected() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    private void showDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Connect to wifi or quit")
                .setCancelable(false)
                .setPositiveButton("Connect to WIFI", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishActivity(0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /*
    * The below two functions are used to check if the user has enabled location services
    * and prompt them to open it if it is not enabled
    * */

    private boolean checkLocationAccessPermitted() {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    private void requestLocationAccessPermission() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        System.out.println("All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        System.out.println("Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, 1);
                        } catch (IntentSender.SendIntentException e) {
                            System.out.println("PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        System.out.println("Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    /*
     * The code below are UI elements, it would be changed to the newest design accordingly
     * So it is not very important, but the way those bars work would be applied later
     *
     * */

//    public void listenerForRatingBar() {
//        ratingBar = findViewById(R.id.ratingBar);
//        ratingTxt = findViewById(R.id.ratingTxt);
//        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
//            @Override
//            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
//                ratingTxt.setText(Float.toString(rating));
//                if (rating < 1.0f) {
//                    ratingBar.setRating(1.0f);
//                }
//            }
//        });
//    }

//    public void listenerForRadiusBar() {
//        metricsSwitch = findViewById(R.id.metricsSwitch);
//        radiusBar = findViewById(R.id.radiusBar);
//        radiusTxt = findViewById(R.id.radiusTxt);
//        radiusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                userRadius = ((double)progress/10);
//                radiusTxt.setText(Double.toString(userRadius));
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//                if (metricsSwitch.isChecked()) {
//                    radiusBar.setMax(160);
//                    radiusBar.setProgress(50);
//                } else {
//                    radiusBar.setMax(100);
//                    radiusBar.setProgress(30);
//                }
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                if (userRadius < 1) {
//                    Toast.makeText(getApplicationContext(), "Minimum is 1, radius will be set to 1", Toast.LENGTH_SHORT).show();
//                    userRadius = 1;
//                }
//            }
//        });
//    }

    /*
     * The function below is used for auto complete loading from text file
     * But we changed plans so it is not used, but it is useful for future
     * */

//    private ArrayList<String> getKeywordsForAutocomplete() {
//        System.out.println("Inside getKeywordsForAutoComplete function"); // for debug purpose
//        ArrayList<String> keywords = new ArrayList<>();
//        InputStream is = getResources().openRawResource(R.raw.keywords);
//        try {
//            BufferedReader br = new BufferedReader(new InputStreamReader(is));
//            String line;
//            while ((line = br.readLine()) != null) {
//                keywords.add(line);
//            }
//            br.close();
//        }
//        catch (IOException e) {
//            System.out.println("Failed to load keywords from text file");
//        }
//        return keywords;
//    }
}
