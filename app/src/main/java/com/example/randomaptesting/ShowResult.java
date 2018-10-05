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
import android.widget.Button;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

public class ShowResult extends AppCompatActivity {

    private String latAndLong;
    private String placeUrl;
    private ArrayList<Destination> matchUserReqList;
    private ArrayList<Destination> suggestions;
    Button shuffleBtn;
    private int randNo;
    private int shuffleCount = 0;
    TextView showName;
    String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_result);
        shuffleBtn = findViewById(R.id.shuffleBtn);
        shuffleBtn.setText("Shuffle");
        showName = findViewById(R.id.showNameTxt);
        matchUserReqList = (ArrayList<Destination>)getIntent().getSerializableExtra("matchUserReqList");
        suggestions = (ArrayList<Destination>)getIntent().getSerializableExtra("suggestions");
        getTheOne();
    }

    private void getTheOne() {
//        debugPrint(destinationList); // debug purpose
        if (matchUserReqList.size() == 0) {
            Toast.makeText(this,"0 results", Toast.LENGTH_LONG).show();
            shuffleBtn.setText("list");
            return;
        }
        shuffleCount++;
        if (shuffleCount > 4) {
            Toast.makeText(getApplicationContext(), "I think a list is better for you", Toast.LENGTH_SHORT).show();
            shuffleBtn.setText("list");
            return;
        } else {
            Toast.makeText(getApplicationContext(), "shuffle count:" + shuffleCount, Toast.LENGTH_SHORT).show(); // debug purpose
        }
        randNo = randomize(matchUserReqList.size());
        GeoDataClient mGeoDataClient = Places
                .getGeoDataClient(getApplicationContext(), null);
        mGeoDataClient.getPlaceById(matchUserReqList
                .get(randNo).getId())
                .addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
                        if (task.isSuccessful()) {
                            PlaceBufferResponse places = task.getResult();
                            Place myPlace = places.get(0);
                            System.out.println("Place found: " + myPlace.getName()); // debugPrint purpose
//                            latAndLong = myPlace.getLatLng().toString();
//                            latAndLong = latAndLong.substring(10, latAndLong.length() - 1);
//                            System.out.println("Lat and Long: " + latAndLong); // debug purpose
//                            showName.setClickable(false);
                            if (myPlace.getWebsiteUri() != null) {
//                                placeUrl = myPlace.getWebsiteUri().toString();
////                                System.out.println("Place URL is " + placeUrl); // debug purpose
//                                showName.setClickable(true);
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

    private void showTextViews(Place myPlace) { // debug purpose
        TextView showAddress = findViewById(R.id.showAddressTxt);
        TextView showNoOfResults = findViewById(R.id.showNoOfResultsTxt);
        TextView showRating = findViewById(R.id.showRatingTxt);
        showName.setText(myPlace.getName());
        if (myPlace.getWebsiteUri() != null) {
            showAddress.setText(myPlace.getWebsiteUri().toString());
        }
        if (myPlace.getPhoneNumber() != null) {
            showRating.setText("Phone number: " + myPlace.getPhoneNumber());
            phoneNumber = myPlace.getPhoneNumber().toString();
        }

        showNoOfResults.setText("No of results: " + matchUserReqList.size());
    }

    public void onGoClicked(View v) {
        System.out.println("Inside onGoClicked function"); // debug purpose
        goToDestination(latAndLong);
    }

    public void onShuffleClicked(View v) {
        System.out.println("Inside onShuffleClicked function"); // debug purpose
        if (shuffleCount <= 4) {
            matchUserReqList.remove(randNo);
            getTheOne();
        } else {
            System.out.println("Should print out the list"); // debug purpose
            Collections.sort(suggestions, Destination.DistanceComparator);
            System.out.println("Sorted according to distance"); // debug purpose
            for (Destination d: suggestions) {
                System.out.println(d);
            }
            Collections.sort(suggestions, Destination.DistanceComparator);
            System.out.println("Sorted according to distance"); // debug purpose
            for (Destination d: suggestions) {
                System.out.println(d);
            }
        }

    }

    public void onHomeClicked(View v) {
        System.out.println("Inside onHomeClicked function"); // debug purpose
        Intent mainActivity =  new Intent(ShowResult.this, MainActivity.class);
        startActivity(mainActivity);
    }
    public void onNameClicked(View v) {
        System.out.println("Inside onNameClicked function"); // debug purpose
        Uri uri = Uri.parse(placeUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void onAddressClicked(View v) {
        String basicUrl = "https://www.google.com/search?biw=1440&bih=803&tbm=isch&sa=1&ei=YHwoW4igCueO0gKS57r4Bg&q=";
        String placeName = matchUserReqList.get(randNo).getName();
        String placeAddress = matchUserReqList.get(randNo).getAddress();
        placeName = placeName.replace(' ', '+');
        String completeUrl = basicUrl + placeName + "+restaurant+" + placeAddress;
        Uri uri = Uri.parse(completeUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void onPhoneClicked(View v) {
        System.out.println("Inside onPhoneClicked function"); // debug purpose
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    private void goToDestination(String latAndLong) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latAndLong);
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
