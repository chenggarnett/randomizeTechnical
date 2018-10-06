package com.example.randomaptesting;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Random;

public class ShowResult extends AppCompatActivity {

    private String latAndLong;
    private String placeUrl;
    private ArrayList<Destination> matchUserReqList;
    private ArrayList<Destination> suggestions;
    private int randNo;
    String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_result);
        // get the array lists of restaurants from the previous activity (API)
        matchUserReqList = (ArrayList<Destination>)getIntent().getSerializableExtra("matchUserReqList");
        suggestions = (ArrayList<Destination>)getIntent().getSerializableExtra("suggestions");

        getTheOneRandomRestaurant();
    }
    /* use Google Places API to get the details of a restaurant e.g. telephone number, website
    *  Google Places API is needed instead of the json file we get in the previous activity
    *  in order to get specific details such as telephone number and website of the restaurant
    * */
    private void getTheOneRandomRestaurant() {
        System.out.println("MatchUserReqList:");
        debugPrint(matchUserReqList); // debug purpose
        System.out.println("Suggestions:");
        debugPrint(suggestions); // debug purpose
        // if there is not restaurant, return
        if (matchUserReqList.size() == 0) {
            Toast.makeText(this,"0 results", Toast.LENGTH_LONG).show();
            return;
        }
        // get a random number that is smaller than the total number
        randNo = randomize(matchUserReqList.size());
        // create GeoDataClient object to get access to Google Places API
        // try to get one specific restaurant
        GeoDataClient mGeoDataClient = Places
                .getGeoDataClient(getApplicationContext(), null);
        // get the restaurant based on the place Id retrieved from json in the previous activity
        mGeoDataClient.getPlaceById(matchUserReqList
                .get(randNo).getId())
                .addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<PlaceBufferResponse> task) { // check if the task is complete
                        if (task.isSuccessful()) { // if it is successful
                            // put the places with the place Id (usually only one) into buffer
                            // because place Id is unique
                            PlaceBufferResponse places = task.getResult();
                            // assign the first place into a Place variable
                            Place myPlace = places.get(0);
                            System.out.println("Place found: " + myPlace.getName()); // debug purpose
//                            latAndLong = myPlace.getLatLng().toString();
//                            latAndLong = latAndLong.substring(10, latAndLong.length() - 1);
//                            System.out.println("Lat and Long: " + latAndLong); // debug purpose
//                            showName.setClickable(false);
//                            if (myPlace.getWebsiteUri() != null) {
//                                placeUrl = myPlace.getWebsiteUri().toString();
////                                System.out.println("Place URL is " + placeUrl); // debug purpose
//                                showName.setClickable(true);
//                            } else {
//                                Toast.makeText(getApplicationContext(), "No website for this place", Toast.LENGTH_SHORT).show();
//                            }
//                            showTextViews(myPlace); // for debug purpose
                            places.release(); // release the place from buffer (mandatory)

                        } else {
                            System.out.println("Place not found.");
                        }
                    }
                });
    }

//    private void showTextViews(Place myPlace) { // debug purpose
//        TextView showAddress = findViewById(R.id.showAddressTxt);
//        TextView showNoOfResults = findViewById(R.id.showNoOfResultsTxt);
//        TextView showRating = findViewById(R.id.showRatingTxt);
//        if (myPlace.getWebsiteUri() != null) {
//            showAddress.setText(myPlace.getWebsiteUri().toString());
//        }
//        if (myPlace.getPhoneNumber() != null) {
//            showRating.setText("Phone number: " + myPlace.getPhoneNumber());
//            phoneNumber = myPlace.getPhoneNumber().toString();
//        }
//
//        showNoOfResults.setText("No of results: " + matchUserReqList.size());
//    }

//    public void onGoClicked(View v) {
//        System.out.println("Inside onGoClicked function"); // debug purpose
//        goToDestination(latAndLong);
//    }
//
//    public void onShuffleClicked(View v) {
//        System.out.println("Should print out the list"); // debug purpose
//        Collections.sort(suggestions, Destination.DistanceComparator);
//        System.out.println("Sorted according to distance"); // debug purpose
//        for (Destination d: suggestions) {
//            System.out.println(d);
//        }
//        Collections.sort(suggestions, Destination.DistanceComparator);
//        System.out.println("Sorted according to distance"); // debug purpose
//        for (Destination d: suggestions) {
//            System.out.println(d);
//        }
//    }
//
//    public void onHomeClicked(View v) {
//        System.out.println("Inside onHomeClicked function"); // debug purpose
//        Intent mainActivity =  new Intent(ShowResult.this, MainActivity.class);
//        startActivity(mainActivity);
//    }
//    public void onNameClicked(View v) {
//        System.out.println("Inside onNameClicked function"); // debug purpose
//        Uri uri = Uri.parse(placeUrl);
//        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        startActivity(intent);
//    }
//
//    public void onAddressClicked(View v) {
//        String basicUrl = "https://www.google.com/search?biw=1440&bih=803&tbm=isch&sa=1&ei=YHwoW4igCueO0gKS57r4Bg&q=";
//        String placeName = matchUserReqList.get(randNo).getName();
//        String placeAddress = matchUserReqList.get(randNo).getAddress();
//        placeName = placeName.replace(' ', '+');
//        String completeUrl = basicUrl + placeName + "+restaurant+" + placeAddress;
//        Uri uri = Uri.parse(completeUrl);
//        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        startActivity(intent);
//    }
//
//    public void onPhoneClicked(View v) {
//        System.out.println("Inside onPhoneClicked function"); // debug purpose
//        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
//        startActivity(intent);
//    }

//    private void goToDestination(String latAndLong) {
//        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latAndLong);
//        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//        mapIntent.setPackage("com.google.android.apps.maps");
//        startActivity(mapIntent);
//    }

    /* get a random number
    * */
    private int randomize(int destinationListSize) {
        Random r = new Random();
        int a = r.nextInt(destinationListSize);
        return a;
    }

    private void debugPrint(ArrayList<Destination> destinationList) {
        for (int i = 0; i < destinationList.size(); i++) { // debug purpose
            System.out.println(i+1 + ". " + "Name: " + destinationList.get(i).getName());
            System.out.println(i+1 + ". " + "Address: " + destinationList.get(i).getAddress());
            System.out.println(i+1 + ". " + "Rating: " + destinationList.get(i).getRating());
            System.out.println(i+1 + ". " + "Website: " + destinationList.get(i).getWebsite());
            System.out.println(i+1 + ". " + "Phone: " + destinationList.get(i).getTelNo());
        }
    }
}
