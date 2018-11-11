package com.example.randomaptesting;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

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
    Location mLastLocation;
//    Handler handler = new Handler();
//    SharedPreferences sharedPref = getSharedPreferences("previousDestination", Context.MODE_PRIVATE);

    // main function of this activity
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        if (!isNetworkConnected()) { // if there is not internet connection
//            Toast.makeText(this, "Please restart your app after connecting to Internet", Toast.LENGTH_LONG).show();
//            showDialog(); // prompt user to enable internet connection
//        }
//        if (!isLocationServiceEnabled())  { // if location service is not enabled and prompt user to open location service
//            Toast.makeText(this, "Please restart your app after enabling your location service", Toast.LENGTH_LONG).show();
//            Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            getApplicationContext().startActivity(myIntent);
//        }
//        checkPermission();
//    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getUserLocation(new LocationVolleyCallback() {
            @Override
            public void onSuccess(final Location userLocation) {
                Log.i("Location", "onSuccess: Latitude is " + userLocation.getLatitude());
                callGooglePlacesApiToRetrieveJSON(userLocation.getLatitude(), userLocation.getLongitude(), new NearbySearchVolleyCallback() {
                    @Override
                    public void onSuccess(JSONArray restaurantDetails) {
                        Log.i("Location", "onSuccess2: Latitude is " + userLocation.getLatitude());
                        try {
                            ArrayList<Destination> destinations = getDestinationList(restaurantDetails, userLocation.getLatitude(), userLocation.getLongitude());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        }); // get user location then callGoogleMapsAPI()
    }

    // get user's location and call Google Maps API
    private void getUserLocation(final LocationVolleyCallback callback) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {//Can add more as per requirement
            Log.d("checkPermission", "onMapReady: opening the prompt");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            // create a fused location client, it is needed to get user's location
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//            MyLocationSuccessListener listener = new MyLocationSuccessListener();
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    callback.onSuccess(location);
                }
            });
//            final MyLocationRunnable runnable = new MyLocationRunnable(task);
//            handler.postDelayed(runnable, 1000);
//            CallGoogleMapsApiRunnable apiRunnable = new CallGoogleMapsApiRunnable();
//            handler.postDelayed(apiRunnable, 2500);
        }

    }

    /* call Google Maps API through a URL address
    *  Google Maps API would return a json file
    *  Json functions are used to split the data to respective variables, e.g. name, address, rating
    *  @param myLatitude: latitude of user's location
    *  @param myLongitude: longitude of user's location
    * */

    public void callGooglePlacesApiToRetrieveJSON(final double myLatitude, final double myLongitude, final NearbySearchVolleyCallback callback) {
        String completeUrl = constructNearbySearchUrl(myLatitude, myLongitude, "restaurant", userRadius * 1.3, userKeyword);
        Log.i("API", completeUrl); // debug purpose
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
                            callback.onSuccess(results);
//                            destinationList = getDestinationList(results);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (!includePrice) {
                            Toast.makeText(getApplicationContext(), "All nearby restaurants do not have price", Toast.LENGTH_SHORT).show();
                        }
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

    public void callGoogleDistanceMatrixAPI(String destinationId, double latitude, double longitude, final DistanceMatrixVolleyCallback callback) {
        String completeUrl = constructDistanceMatrixUrls(destinationId, latitude, longitude);
        Log.i("API", "callGoogleDistanceMatrixAPI: " + completeUrl);
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, completeUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("API", "onResponse: call back success");
                callback.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(stringRequest);

    }

    private ArrayList<Destination> getDestinationList(JSONArray results, double mLatitude, double mLongitude) throws JSONException {
        ArrayList<Destination> destinationList = new ArrayList<>();

        for (int i = 0; i < results.length(); i++) {
//            String latitude = results.getJSONObject(i).getJSONObject("geometry")
//                    .getJSONObject("location").getString("lat");
//            String longitude = results.getJSONObject(i).getJSONObject("geometry")
//                    .getJSONObject("location").getString("lng");
            final String name = results.getJSONObject(i).getString("name");
            final String placeId = results.getJSONObject(i).getString("place_id");
            final String address = results.getJSONObject(i).getString("vicinity");
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
            final Destination[] d = new Destination[1];
            callGoogleDistanceMatrixAPI(placeId, mLatitude, mLongitude, new DistanceMatrixVolleyCallback() {
                @Override
                public String onSuccess(String distanceResults) {
                    String realDistance = "";
                    try {
//                        Log.d("Restaurants", "onSuccess: first restaurant: " + destinations.get(0).getName());
                        Log.d("InsideDistance", "onSuccess: " + distanceResults);
                        JSONObject obj = new JSONObject(distanceResults);
                        realDistance = obj.getJSONArray("rows").getJSONObject(0)
                                .getJSONArray("elements").getJSONObject(0)
                                .getJSONObject("distance").getString("value");
                        Log.d("realDistance", "onSuccess: " + realDistance);
                        d[0] = new Destination(name, address, placeId, Double.parseDouble(realDistance));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return realDistance;
                }
            });
//            Destination d = new Destination(name, address, placeId, 0);
            d[0].setPrice(price);
            d[0].setRating(rating);
            destinationList.add(d[0]);
        }
        return destinationList;
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

    //    class MyLocationRunnable implements Runnable {
//
//        public Task<Location> locationTask;
//
//        public MyLocationRunnable(Task<Location> task) {
//            locationTask = task;
//        }
//
//        @Override
//        public void run() {
//            mLastLocation = locationTask.getResult();
//            Log.d("mLastLocation", "Latitude: " + mLastLocation.getLatitude());
//        }
//    }
//
//    class CallGoogleMapsApiRunnable implements Runnable {
//        @Override
//        public void run() {
//            callGooglePlacesApiToRetrieveJSON(mLastLocation.getLatitude(), mLastLocation.getLongitude());
//        }
//    }

//    class MyLocationSuccessListener implements OnSuccessListener<Location> {
//        @Override
//        public void onSuccess(Location location) {
//            Log.d("onSuccess","inside onSuccess");
//            if (location != null) {
//                Log.d("onSuccess","Retrieved location successfully");
//                Log.d("onSuccess", "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude());
//            } else {
//                Log.d("onSuccess","Location is null");
//            }
//        }
//    }

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

    private String constructDistanceMatrixUrls(String destinationId, double latitude, double longitude) {
        String origin = latitude + "," + longitude;
        String distanceMatrixUrl = "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins="
                + origin + "&destinations=place_id:" + destinationId + "&key=AIzaSyDpKpQ2S8lvUK7xfHGgSoJXy0HG9tFU-7s";
        Log.i("API", "constructDistanceMatrixUrls: " + distanceMatrixUrl);
        return distanceMatrixUrl;
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
    * The below functions are used to check if the user has enabled location services
    * and prompt them to open it if it is not enabled
    * */

    protected boolean isLocationServiceEnabled(){
        LocationManager locationManager = null;
        boolean gps_enabled= false;

        if(locationManager ==null)
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        try{
            gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch(Exception ex){
            //do nothing...
        }
        return gps_enabled;
    }

    protected boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {//Can add more as per requirement
            Log.d("checkPermission", "opening the prompt");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return false;
        } else {
            Log.d("checkPermission", "Location is allowed");
        }
        return true;
    }

//    private boolean checkLocationAccessPermitted() {
//        int locationMode = 0;
//        String locationProviders;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
//            try {
//                locationMode = Settings.Secure.getInt(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_MODE);
//
//            } catch (Settings.SettingNotFoundException e) {
//                e.printStackTrace();
//                return false;
//            }
//
//            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
//
//        }else{
//            locationProviders = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
//            return !TextUtils.isEmpty(locationProviders);
//        }
//    }

//    private void requestLocationAccessPermission() {
//        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
//                .addApi(LocationServices.API).build();
//        googleApiClient.connect();
//
//        LocationRequest locationRequest = LocationRequest.create();
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        locationRequest.setInterval(10000);
//        locationRequest.setFastestInterval(10000 / 2);
//
//        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
//        builder.setAlwaysShow(true);
//
//        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
//        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
//            @Override
//            public void onResult(LocationSettingsResult result) {
//                final Status status = result.getStatus();
//                switch (status.getStatusCode()) {
//                    case LocationSettingsStatusCodes.SUCCESS:
//                        System.out.println("All location settings are satisfied.");
//                        break;
//                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                        System.out.println("Location settings are not satisfied. Show the user a dialog to upgrade location settings ");
//
//                        try {
//                            // Show the dialog by calling startResolutionForResult(), and check the result
//                            // in onActivityResult().
//                            status.startResolutionForResult(MainActivity.this, 1);
//                        } catch (IntentSender.SendIntentException e) {
//                            System.out.println("PendingIntent unable to execute request.");
//                        }
//                        break;
//                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                        System.out.println("Location settings are inadequate, and cannot be fixed here. Dialog not created.");
//                        break;
//                }
//            }
//        });
//    }

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
