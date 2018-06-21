package com.example.randomaptesting;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Random;

public class ShowResult extends AppCompatActivity {

    private String editedAddress;
    private String placeUrl;
    private ArrayList<Destination> destinationList;
    private int randNo;
    TextView showName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_result);
        showName = findViewById(R.id.showNameTxt);
        destinationList = (ArrayList<Destination>)getIntent().getSerializableExtra("DESTINATIONS");
        getTheOne(destinationList);
    }

    private void getTheOne(final ArrayList<Destination> destinationList) {
        debugPrint(destinationList); // for debug purpose
        if (destinationList.size() == 0) {
            Toast.makeText(this,"0 results", Toast.LENGTH_LONG).show();
            return;
        }
        randNo = randomize(destinationList.size());
        GeoDataClient mGeoDataClient = Places
                .getGeoDataClient(getApplicationContext(), null);
        mGeoDataClient.getPlaceById(destinationList
                .get(randNo).getId())
                .addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                        if (task.isSuccessful()) {
                            PlaceBufferResponse places = task.getResult();
                            Place myPlace = places.get(0);
                            System.out.println("Place found: " + myPlace.getName()); // debugPrint purpose
                            String completeUrl = constructReviewUrl();
                            System.out.println(completeUrl); // for debug purpose
                            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                            StringRequest stringRequest = new StringRequest(Request.Method.GET, completeUrl,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            JSONObject obj = new JSONObject(response);
                                            JSONArray reviews = obj.getJSONObject("result").getJSONArray("reviews");
                                            if (reviews != null) {
//                                                for (int i = 0; i < reviews.length(); i++) { // for debug purpose
//                                                    System.out.println(reviews.get(i));
//                                                }
                                            }
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
                            editedAddress = myPlace.getAddress()
                                    .toString().replace(' ', '+');
                            showName.setClickable(false);
                            if (myPlace.getWebsiteUri() != null) {
                                placeUrl = myPlace.getWebsiteUri().toString();
//                                System.out.println("Place URL is " + placeUrl); // for debug purpose
                                showName.setClickable(true);
                            } else {
                                Toast.makeText(getApplicationContext(), "No website for this place", Toast.LENGTH_SHORT).show();
                            }
                            showTextViews(myPlace); // for debug purpose
                            places.release();

                        } else {
                            System.out.println("Place not found.");
                        }
                    }
                });
    }

    private String constructReviewUrl() {
        String basicUrl = "https://maps.googleapis.com/maps/api/place/details/json?";
        String placeId = "placeid=" + destinationList.get(randNo).getId() + "&key=AIzaSyDpKpQ2S8lvUK7xfHGgSoJXy0HG9tFU-7s";
        String completeUrl = basicUrl + placeId;
        return completeUrl;
    }

    private void showTextViews(Place myPlace) { // for debug purpose
        TextView showAddress = findViewById(R.id.showAddressTxt);
        TextView showNoOfResults = findViewById(R.id.showNoOfResultsTxt);
        TextView showRating = findViewById(R.id.showRatingTxt);
        showName.setText(myPlace.getName());
        showAddress.setText(myPlace.getAddress());
        showRating.setText("Rating: " + myPlace.getRating());
        showNoOfResults.setText("No of results: " + destinationList.size());
    }

    public void onGoClicked(View v) {
        System.out.println("Inside onGoClicked function"); // for debug purpose
        goToDestination(editedAddress);
    }

    public void onRefreshClicked(View v) {
        System.out.println("Inside onRefreshClicked function"); // for debug purpose
        destinationList.remove(randNo);
        getTheOne(destinationList);
    }

    public void onHomeClicked(View v) {
        System.out.println("Inside onHomeClicked function"); // for debug purpose
        Intent mainActivity =  new Intent(ShowResult.this, MainActivity.class);
        startActivity(mainActivity);
    }
    public void onNameClicked(View v) {
        System.out.println("Inside onNameClicked function"); // for debug purpose
        Uri uri = Uri.parse(placeUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void onAddressClicked(View v) {
        String basicUrl = "https://www.google.com/search?biw=1440&bih=803&tbm=isch&sa=1&ei=YHwoW4igCueO0gKS57r4Bg&q=";
        String placeName = destinationList.get(randNo).getName();
        String placeAddress = destinationList.get(randNo).getAddress();
        placeName = placeName.replace(' ', '+');
        String completeUrl = basicUrl + placeName + "+restaurant+" + placeAddress;
        Uri uri = Uri.parse(completeUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void goToDestination(String editedAddress) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + editedAddress);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private int randomize(int destinationListSize) {
        Random r = new Random();
        int a = r.nextInt(destinationListSize);
        return a;
    }

    private void debugPrint(ArrayList<Destination> destinationList) {
        for (int i = 0; i < destinationList.size(); i++) { // debugPrint purpose
            System.out.println(i+1 + ". " + "Name: " + destinationList.get(i).getName());
            System.out.println(i+1 + ". " + "Address: " + destinationList.get(i).getAddress());
            System.out.println(i+1 + ". " + "Rating: " + destinationList.get(i).getRating());
        }
    }
}
