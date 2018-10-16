package com.example.timo.locationtest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.time.Instant;
import java.time.ZonedDateTime;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient locationProviderClient;

    int interval        = 3600000;
    int fastestInterval = 3600000;
    int priority        = LocationRequest.PRIORITY_LOW_POWER;

    TextView textViewOutput;
    MapView mapView;
    //CameraUpdateFactory cameraUpdateFactory;

    LocationRequest locationRequest;
    LocationCallback locationCallback;

    RadioGroup radioGroupPriority;
    RadioButton radioButtonLowPower;

    EditText editTextInterval;
    EditText editTextFastestInterval;
    Button buttonIntervalUpdate;


    private final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 0;
    private final int MY_PERMISSIONS_REQUEST_FINE_LOCATION  = 1;

    boolean accessCoarseLocation = false;
    boolean accessFineLocation = false;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init
        locationRequest = new LocationRequest().setInterval(interval).setFastestInterval(fastestInterval).setPriority(priority);
        locationCallback = getLocationCallbackLowPower();

        // Text Output
        textViewOutput = findViewById(R.id.textViewOutput);

        // Interval Components
        editTextInterval        = findViewById(R.id.editTextInterval);
        editTextFastestInterval = findViewById(R.id.editTextFastestInterval);
        buttonIntervalUpdate    = findViewById(R.id.buttonIntervalUpdate);

        editTextInterval.setText(String.valueOf(interval));
        editTextFastestInterval.setText(String.valueOf(fastestInterval));

        buttonIntervalUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                interval = Integer.parseInt(editTextInterval.getText().toString());
                fastestInterval = Integer.parseInt(editTextFastestInterval.getText().toString());
                updateLocationRequest(locationCallback, interval, fastestInterval, priority);
            }
        });

        // Map Component
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        // Priority Components
        radioButtonLowPower = findViewById(R.id.radioButtonLowPower);
        radioGroupPriority = findViewById(R.id.radioGroupPriority);

        radioButtonLowPower.setChecked(true);

        radioGroupPriority.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioButtonNoPower:
                        Toast.makeText(getApplicationContext(), "No Power", Toast.LENGTH_SHORT).show();
                        updateLocationRequest(getLocationCallbackNoPower(), interval, fastestInterval, LocationRequest.PRIORITY_NO_POWER);
                        break;
                    case R.id.radioButtonLowPower:
                        updateLocationRequest(getLocationCallbackLowPower(), interval, fastestInterval, LocationRequest.PRIORITY_LOW_POWER);
                        break;
                    case R.id.radioButtonHighAcc:
                        updateLocationRequest(getLocationCallbackHighAccuracy(), interval, fastestInterval, LocationRequest.PRIORITY_HIGH_ACCURACY);
                        break;
                }
            }
        });


        // Ask User for permissions if not granted
        if (!coarse_location_permitted()) {
            permit_coarse_location(MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
        } else if (!fine_location_permitted()) {
            permit_fine_location(MY_PERMISSIONS_REQUEST_FINE_LOCATION);
        } else {
            locationProviderClient = LocationServices.getFusedLocationProviderClient(this);                      // Location Provider
            updateLocationRequest(getLocationCallbackLowPower(), interval, fastestInterval, LocationRequest.PRIORITY_LOW_POWER);
        }
    }

    // Update location requests if user changes priority or interval
    @SuppressLint("MissingPermission")
    private void updateLocationRequest(LocationCallback pLocationCallback, int pInterval, int pFastestInterval, int priority) {
        locationProviderClient.removeLocationUpdates(locationCallback);
        locationCallback = pLocationCallback;
        locationRequest = new LocationRequest().setInterval(pInterval).setFastestInterval(pFastestInterval).setPriority(priority);
        locationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        Toast.makeText(getApplicationContext(), "LocationRequest updated", Toast.LENGTH_SHORT).show();
    }

    // Show given location on GoogleMap
    private void showLocationOnMap(Location pLocation) {
        // Log.d("LOCATION", location.toString());
        final Location location = pLocation;
        textViewOutput.setText(location.getLatitude() + ", " + location.getLongitude() + "\n" + ZonedDateTime.now());

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.clear();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(
                                location.getLatitude(),
                                location.getLongitude()
                        ),
                        15
                ));
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng( location.getLatitude(), location.getLongitude()))
                        .title("You are here"));
                mapView.onResume();
            }
        });
    }

    // ==== Callback Generator Functions ====

    private LocationCallback getLocationCallbackNoPower() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Toast.makeText(getApplicationContext(), "No Power Callback", Toast.LENGTH_SHORT).show();
                final Location location = locationResult.getLastLocation();
                showLocationOnMap(location);
            }
        };
    }

    private LocationCallback getLocationCallbackLowPower() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Toast.makeText(getApplicationContext(), "Low Power Callback", Toast.LENGTH_SHORT).show();
                final Location location = locationResult.getLastLocation();
                showLocationOnMap(location);
            }
        };
    }

    private LocationCallback getLocationCallbackHighAccuracy() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Toast.makeText(getApplicationContext(), "High Accuracy Callback", Toast.LENGTH_SHORT).show();
                final Location location = locationResult.getLastLocation();
                showLocationOnMap(location);
            }
        };
    }

    private LocationRequest getLocationRequest(int priority, int interval, int fastestInterval) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(interval);
        locationRequest.setFastestInterval(fastestInterval);
        locationRequest.setPriority(priority);

        return locationRequest;
    }

    // ==== Location Permissions ====

    // Coarse Location Permission
    private boolean coarse_location_permitted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void permit_coarse_location(int callback) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, callback);
    }

    // Fine Location Permission
    private boolean fine_location_permitted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void permit_fine_location(int callback) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, callback);
    }

    // Permission Callback Handler
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Coarse Location granted!", Toast.LENGTH_SHORT).show();
                    this.accessCoarseLocation = true;
                } else {
                    Toast.makeText(getApplicationContext(), "Coarse Location denied!", Toast.LENGTH_SHORT).show();
                    this.accessCoarseLocation = false;
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission granted!", Toast.LENGTH_SHORT).show();
                    this.accessFineLocation = true;
                } else {
                    Toast.makeText(getApplicationContext(), "Permission denied!", Toast.LENGTH_SHORT).show();
                    this.accessFineLocation = false;
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
}
