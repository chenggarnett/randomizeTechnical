package com.example.randomaptesting;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class ShowResult extends AppCompatActivity {

    private ArrayList<Destination> matchUserReqList;
    private ArrayList<Destination> suggestions;
    private int randNo;
    GeoDataClient mGeoDataClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_result);
        mGeoDataClient = Places
                .getGeoDataClient(getApplicationContext(), null);
        // get the array lists of restaurants from the previous activity (API)
        matchUserReqList = (ArrayList<Destination>)getIntent().getSerializableExtra("matchUserReqList");
        suggestions = (ArrayList<Destination>)getIntent().getSerializableExtra("suggestions");
        debugPrintList(false, false,false, false, false);
        sorting(DistanceComparator);
        debugPrintList(true, false, false, false, false);
        sorting(PriceComparator);
        debugPrintList(false, true, false, false, false);
        sorting(RatingComparator);
        debugPrintList(false, false, true, false, false);

        getTheOneRandomRestaurant();
    }
    /* use Google Places API to get the details of a restaurant e.g. telephone number, website
    *  Google Places API is needed instead of the json file we get in the previous activity
    *  in order to get specific details such as telephone number and website of the restaurant
    * */
    private void getTheOneRandomRestaurant() {
        // if there is not restaurant, return
        if (matchUserReqList.size() == 0) {
            Toast.makeText(this,"0 results", Toast.LENGTH_LONG).show();
            return;
        }
        // get a random number that is smaller than the total number
        randNo = randomize(matchUserReqList.size());
        // create GeoDataClient object to get access to Google Places API
        // try to get one specific restaurant

        // get the restaurant based on the place Id retrieved from json in the previous activity
        Destination randomRestaurant = matchUserReqList
                .get(randNo);
        Log.d("randomRestaurant", "getTheOneRandomRestaurant: Restaurant name: " + randomRestaurant.getName());
        mGeoDataClient.getPlaceById(randomRestaurant.getId())
                .addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<PlaceBufferResponse> task) { // check if the task is complete
                        if (task.isSuccessful()) { // if it is successful
                            // put the places with the place Id (usually only one) into buffer
                            // because place Id is unique
                            PlaceBufferResponse places = task.getResult();
                            // assign the first place into a Place variable
                            Place myPlace = places.get(0);
                            Log.i("Place", "onComplete: Place name: " + myPlace.getName());
                            if (myPlace.getWebsiteUri() != null) {
                                Log.i("Place", "onComplete: Place website: " + myPlace.getWebsiteUri());
                            }
                            if (myPlace.getPhoneNumber() != null) {
                                Log.i("Place", "onComplete: Place no: " + myPlace.getPhoneNumber());
                            }
                            places.release(); // release the place from buffer (mandatory)

                        } else {
                            Log.d("Place", "Place not found.");
                        }
                    }
                });
    }

    private void sorting(Comparator<Destination> comparator) {
        Collections.sort(matchUserReqList, comparator);
        Collections.sort(suggestions, comparator);
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
//        Collections.sorting(suggestions, Destination.DistanceComparator);
//        System.out.println("Sorted according to distance"); // debug purpose
//        for (Destination d: suggestions) {
//            System.out.println(d);
//        }
//        Collections.sorting(suggestions, Destination.DistanceComparator);
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

    private void debugPrintList(boolean showDistance, boolean showPrice, boolean showRating, boolean showTelNo, boolean showWebsite) {
        Log.i("debugPrint", "MatchUserReqList:");
        debugPrint(matchUserReqList, showDistance, showPrice, showRating, showTelNo, showWebsite); // debug purpose
        Log.i("debugPrint", "Suggestions:");
        debugPrint(suggestions, showDistance, showPrice, showRating, showTelNo, showWebsite); // debug purpose
    }

    public static void debugPrint(ArrayList<Destination> destinationList,
                            boolean showDistance, boolean showPrice, boolean showRating, boolean showTelNo, boolean showWebsite) {
        for (int i = 0; i < destinationList.size(); i++) { // debug purpose
            Log.i("debugPrint",i+1 + ". " + "Name: " + destinationList.get(i).getName());
            Log.i("debugPrint","   " + "Address: " + destinationList.get(i).getAddress());
            if (showDistance) {
                Log.i("debugPrint", "   " + "Distance: " +  destinationList.get(i).getDistance());
            }
            if (showPrice) {
                Log.i("debugPrint", "   " + "Price: " +  destinationList.get(i).getPrice());
            }
            if (showRating) {
                Log.i("debugPrint","   " + "Rating: " + destinationList.get(i).getRating());
            }
            if (showTelNo) {
                Log.i("debugPrint","   " + "Phone: " + destinationList.get(i).getTelNo());
            }
            if (showWebsite){
                Log.i("debugPrint","   " + "Website: " + destinationList.get(i).getWebsite());
            }
        }
    }

    public static final Comparator<Destination> DistanceComparator = new Comparator<Destination>() {
        @Override
        public int compare(Destination d1, Destination d2) {
            return (int)Math.signum(d1.getDistance() - d2.getDistance());
        }
    };

    public static final Comparator<Destination> RatingComparator = new Comparator<Destination>() {
        @Override
        public int compare(Destination d1, Destination d2) {
            return (int)Math.signum(d2.getRating() - d1.getRating());
        }
    };

    public static final Comparator<Destination> PriceComparator = new Comparator<Destination>() {
        @Override
        public int compare(Destination d1, Destination d2) {
            return (int)Math.signum(d1.getPrice() - d2.getPrice());
        }
    };

}
