package com.example.randomaptesting;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
    private ArrayList<Destination> destinationList;
    private int randNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_result);
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
                            editedAddress = myPlace.getAddress()
                                    .toString().replace(' ', '+');
                            final String photoUrl = constructPhotoUrl(destinationList.get(randNo));
                            System.out.println(photoUrl);
                            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                            StringRequest stringRequest = new StringRequest(Request.Method.GET, photoUrl,
                                    new Response.Listener<String>() {
                                        @Override
                                        public void onResponse(String response) {
                                            ImageView showPhoto = findViewById(R.id.showPhoto);
                                            new GetBitmap().execute(photoUrl);
//                                            System.out.println("Succeed! Response is " + response); // for debug purpose
                                        }
                                    }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    System.out.println("Volley Error");
                                }
                            }
                            );
                            queue.add(stringRequest);

                            showTextViews(myPlace); // for debug purpose
                            places.release();
                        } else {
                            System.out.println("Place not found.");
                        }
                    }
                });
    }

    private String constructPhotoUrl(Destination d) {
        String basicUrl = "https://maps.googleapis.com/maps/api/place/photo?";
        String photoRef = "photoreference=" + d.getPhotoRef();
        String heightWidth = "&maxheight=200&maxwidth=320&key=AIzaSyDpKpQ2S8lvUK7xfHGgSoJXy0HG9tFU-7s";
        String completeUrl = basicUrl + photoRef + heightWidth;
        return completeUrl;
    }


    private void showTextViews(Place myPlace) { // for debug purpose
        TextView showName = findViewById(R.id.showNameTxt);
        TextView showAddress = findViewById(R.id.showAddressTxt);
        TextView showNoOfResults = findViewById(R.id.showNoOfResultsTxt);
        TextView showRating = findViewById(R.id.showRatingTxt);
        showName.setText("Restaurant name: " + myPlace.getName());
        showAddress.setText("Address: " + myPlace.getAddress());
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
