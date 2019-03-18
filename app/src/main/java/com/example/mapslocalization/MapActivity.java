package com.example.mapslocalization;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.system.ErrnoException;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, View.OnClickListener {
    private static final String TAG = "MapActivity";
    private static final String FINE_LOCATION = ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;

    //widgets
    private AutoCompleteTextView mSearchText;
    private ImageView mGPS;
    private ImageView mInfo;

    private Marker mMarker;

    //vars
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private AutocompleteSupportFragment autocompleteFragment;

    //Places API
    private PlacesClient placesClient;

    //User location
    private LatLng userLatLng;

    public LatLng getUserLatLng() {
        return userLatLng;
    }

    public void setUserLatLng(LatLng userLatLng) {
        this.userLatLng = userLatLng;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this::onInfoWindowClick);
        Log.d(TAG, "onMapReady: Map is Ready");
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();

        if (mLocationPermissionsGranted){
            getDeviceLocation();
            if(ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            //mMap.setMapType(2);
            init_OnMapReady();
        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mSearchText = findViewById(R.id.input_search);
        mGPS = findViewById(R.id.ic_gps);
        mInfo = findViewById(R.id.place_info);
        getLocationPermission();
        init_PlacesClient();
    }

    private void initMap(){
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(MapActivity.this);
        Log.d(TAG, "initMap: Map is initialized");
    }

    private void init_OnMapReady(){
        Log.d(TAG, "init: initializing");
        init_AutocompleteSupportFragment();
        mGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDeviceLocation();
                Log.d(TAG, "onClick: clicked gps icon");
            }
        });

        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked place info");
                try {
                    if(mMarker.isInfoWindowShown()){
                        mMarker.hideInfoWindow();
                    }else{
                        Log.d(TAG, "onClick: clicked place info");
                        mMarker.showInfoWindow();
                    }
                }catch (NullPointerException e){
                    Log.e(TAG, "onClick: NullPointerException: " + e.getMessage());
                }
            }
        });

        autocomplete_SetOnPlaceSelectedListener();
        hideSoftKeyboard();
    }

    private void geoLocate(Place place){
        Log.d(TAG, "geoLocate: geoLocating");
        String message = "Lugar: " + place.getName() + "\nDirección: " + place.getAddress() + "\nCoordenadas: " + place.getLatLng();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        //moveCamera(place.getLatLng(), DEFAULT_ZOOM, place.getName());
        Log.d(TAG, "geoLocate: found a location: " + "\nName: " + place.getName() + "\nAddres: " + place.getAddress());

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
                           setUserLatLng(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
                           Toast.makeText(MapActivity.this, "User Location: " + getUserLatLng(), Toast.LENGTH_LONG).show();
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

    private void moveCamera(LatLng latLng, float zoom, Place place){
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17),2000,null);
        mMap.clear();
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapActivity.this));
        if (place != null){
            try{
                String snippet = "Address: " + place.getAddress() +
                        "\nPhoneNumber: " + place.getPhoneNumber() +
                        "\nLatLng: " + place.getLatLng() +
                        "\nPrice Ratings: " + place.getRating() +
                        "\nOpening Hours: " + place.getOpeningHours();
                MarkerOptions options = new MarkerOptions().position(latLng).title(place.getName()).snippet(snippet);
                mMarker = mMap.addMarker(options);
                mMap.addMarker(options);
            }catch (NullPointerException e){
                Log.e(TAG, "moveCamer: NullpointerException: " + e.getMessage());
            }
        }else{
            mMap.addMarker(new MarkerOptions().position(latLng));
        }
        hideSoftKeyboard();
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
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
        //imm.hideSoftInputFromWindow(pCardView.getWindowToken(), 0);
        //this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }


    /*
        ********************************************************************************************
        **************************************Places API********************************************
        ********************************************************************************************
     */

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
                Place.Field.OPENING_HOURS,
                Place.Field.RATING,
                Place.Field.ADDRESS,
                Place.Field.PHONE_NUMBER,
                Place.Field.LAT_LNG));

        autocompleteFragment.setCountry("CR");
        autocompleteFragment.setTypeFilter(TypeFilter.CITIES);
        autocompleteFragment.setHint("Where are you going?");
    }

    private void autocomplete_SetOnPlaceSelectedListener(){
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // TODO: Get info about the selected place.
                //
                mSearchText.setText(place.getName());
                //geoLocate(place);
                moveCamera(place.getLatLng(), DEFAULT_ZOOM, place);
                Log.i("HP", "Place: " + place.getName() + ", " + place.getId());
            }
            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("HP", "An error occurred: " + status);
            }
        });
    }


    /*
        **************************************************************************************************************
        *************************************************Directions API***********************************************
        **************************************************************************************************************
    */

    @Override
    public void onInfoWindowClick(final Marker marker) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(marker.getSnippet())
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {

                        dialog.dismiss();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onClick(View v) {

    }


    /*
        ********************************************************************************************
        **************************************Custom Markers****************************************
        ********************************************************************************************
    */

    /*
        ********************************************************************************************
        ***************************************User Geo Location************************************
        ********************************************************************************************
    */


}
