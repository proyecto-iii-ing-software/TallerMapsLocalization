package com.example.mapslocalization;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback{
    private static final String TAG = "MapActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    //widgets
    private AutoCompleteTextView mSearchText;
    private ImageView mGPS;
    private CardView pCardView;

    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private AutocompleteSupportFragment autocompleteFragment;
    private GoogleApi mGoogleApi;
    private PlacesClient placesClient;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "onMapReady: Map is Ready");
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();

        if (mLocationPermissionsGranted){
            getDeviceLocation();
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            //mMap.setMapType(2);
        }
        init();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mSearchText = findViewById(R.id.input_search);
        mGPS = findViewById(R.id.ic_gps);
        pCardView = findViewById(R.id.idCardView);

        getLocationPermission();
        init_PlacesClient();
    }

    private void init(){
        Log.d(TAG, "init: initializing");
        init_AutocompleteSupportFragment();

        /*
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_DONE
                || event.getAction() == KeyEvent.ACTION_DOWN
                || event.getAction() == KeyEvent.KEYCODE_ENTER){
                    //execute our method for searching
                    geoLocate();
                }
                return false;
            }
        });
        */
        mGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceLocation();
                Log.d(TAG, "onClick: clicked gps icon");
            }
        });

        autocomplete_SetOnPlaceSelectedListener();
        hideSoftKeyboard();
    }

    private void init_PlacesClient(){
        String apiKey = getString(R.string.google_api_key);

        Places.initialize(getApplicationContext(), getString(R.string.google_api_key));

        placesClient = Places.createClient(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_api_key));
        }

    }

    private void init_AutocompleteSupportFragment(){
        // Initialize the AutocompleteSupportFragment.
        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        //Autocomplete Filter, these are the attributes that are going to be fetched from the user's selection.
        autocompleteFragment.setPlaceFields(Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG));

        autocompleteFragment.setCountry("CR");
        autocompleteFragment.setTypeFilter(TypeFilter.CITIES);
        autocompleteFragment.setTypeFilter(TypeFilter.ESTABLISHMENT);
        autocompleteFragment.setHint("Where are you going?");
    }

    private void autocomplete_SetOnPlaceSelectedListener(){
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                //
                mSearchText.setText(place.getName());
                geoLocate(place);
                //moveCamera(place.getLatLng(), DEFAULT_ZOOM, place.getName());
                Log.i("HP", "Place: " + place.getName() + ", " + place.getId());
            }
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("HP", "An error occurred: " + status);
            }
        });
    }

    /**
     *
     * //This method gets a list of Predictions
     *     private Task<FindAutocompletePredictionsResponse> getListOfPredictions(){
     *         // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
     *         // and once again when the user makes a selection (for example when calling fetchPlace()).
     *         AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();
     *
     *         // Create a RectangularBounds object.
     *         RectangularBounds bounds = RectangularBounds.newInstance(
     *                 new LatLng(-33.880490, 151.184363),
     *                 new LatLng(-33.858754, 151.229596));
     *         // Use the builder to create a FindAutocompletePredictionsRequest.
     *         FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
     *         // Call either setLocationBias() OR setLocationRestriction().
     *                 .setLocationBias(bounds)
     *                 //.setLocationRestriction(bounds)
     *                 .setCountry("CR")
     *                 .setTypeFilter(TypeFilter.ADDRESS)
     *                 .setSessionToken(token)
     *                 .setQuery("") //It shouldn't contain a blank query
     *                 .build();
     *
     *         return placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
     *             for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
     *                 Log.i(TAG, prediction.getPlaceId());
     *                 Log.i(TAG, prediction.getPrimaryText(null).toString());
     *             }
     *         }).addOnFailureListener((exception) -> {
     *             if (exception instanceof ApiException) {
     *                 ApiException apiException = (ApiException) exception;
     *                 Log.e(TAG, "Place not found: " + apiException.getStatusCode());
     *             }
     *         });
     *     }
     *
     *     //This method is for the creation of an adapter, that takes the list of predictions and displays them in mSearchText
     *     private void autoComplete_Adapter(){
     *         ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
     *                 android.R.layout.simple_dropdown_item_1line, Collections.singletonList(getListOfPredictions().toString()));
     *         mSearchText = (AutoCompleteTextView)
     *                 findViewById(R.id.input_search);
     *         mSearchText.setAdapter(adapter);
     *     }
     * @return
     */


    private void geoLocate(Place place){
        Log.d(TAG, "geoLocate: geoLocating");
        String message = "Lugar: " + place.getName() + "\nDirección: " + place.getAddress() + "\nCoordenadas: " + place.getLatLng();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        moveCamera(place.getLatLng(), DEFAULT_ZOOM, place.getName());
        Log.d(TAG, "geoLocate: found a location: " + "\nName: " + place.getName() + "\nAddres: " + place.getAddress());
        /*
        String searchString = mSearchText.getText().toString();
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();

        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }
        if (list.size() > 0){
            Address address = list.get(0);
            Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();

            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM, address.getAddressLine(0));
            Log.d(TAG, "geoLocate: found a location: " + address.toString());
        }
        */
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the device location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
           if (mLocationPermissionsGranted){
               Task location = mFusedLocationProviderClient.getLastLocation();
               location.addOnCompleteListener(new OnCompleteListener() {
                   @Override
                   public void onComplete(@NonNull Task task) {
                       if (task.isSuccessful()){
                           Location currentLocation = (Location) task.getResult();
                           moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My Location");
                           Log.d(TAG, "onComplete: found location!");
                       }else{
                           Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                           Log.d(TAG, "onComplete: current location is null");
                       }
                   }
               });
           }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        mMap.animateCamera(CameraUpdateFactory.zoomTo(17),2000,null);
        if (!title.equals("My Location")){
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(markerOptions);
        }
        hideSoftKeyboard();
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
    }

    private void initMap(){
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(MapActivity.this);
        Log.d(TAG, "initMap: Map is initialized");

    }

    private void getLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                   COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                Log.d(TAG, "getLocationPermission: Permissions were granted");
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
                Log.d(TAG, "getLocationPermission: Permissions were not granted");
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
            Log.d(TAG, "getLocationPermission: Permissions were not granted");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;

        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if (grantResults.length > 0){
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: Permissions were not granted");
                            return;
                        }
                    }
                    mLocationPermissionsGranted = true;
                    Log.d(TAG, "getLocationPermission: Permissions were granted");
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    private void hideSoftKeyboard(){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(pCardView.getWindowToken(), 0);
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

}
