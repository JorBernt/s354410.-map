package apputvikling.jorber.s354410_map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import apputvikling.jorber.s354410_map.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback, IOnClick {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    public static final String TAG = MapsActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private Geocoder geocoder;
    private BottomSheetFragment bottomSheetFragment;
    private AttractionViewModel attractionViewModel;
    private int lastMarkerIndex = -1;
    private int markerIndex = 0;
    private Map<Integer, String> addressMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        addressMap = new HashMap<>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(1 * 1000);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(getApplicationContext(), Locale.ENGLISH);
        attractionViewModel = new ViewModelProvider(this).get(AttractionViewModel.class);
        final Observer<List<Attraction>> attractionObserver = attractions -> {
            for (Attraction attraction : attractions) {
                Marker marker = mMap.addMarker(new MarkerOptions().title(attraction.getTitle()).position(attraction.getLatLng()).snippet(attraction.getDescription()));
                addressMap.put(markerIndex, attraction.getAddress());
                marker.setTag(markerIndex++);
            }
        };
        attractionViewModel.getCurrentAttractions().observe(this, attractionObserver);
    }

    public void getAddressFromLocation(LatLng latLng, int mIndex) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                JSONObject jsonObject;
                String query = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latLng.latitude + "," + latLng.longitude + "&key=" + getResources().getString(R.string.googleApi);
                String location;
                try {
                    URL urlen = new URL(query);
                    HttpURLConnection conn = (HttpURLConnection) urlen.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setReadTimeout(15000);
                    conn.setConnectTimeout(1500);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Accept", "application/json");
                    if (conn.getResponseCode() != 200) {
                        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                    String s;
                    StringBuilder output = new StringBuilder();
                    while ((s = br.readLine()) != null) {
                        output.append(s);
                    }
                    jsonObject = new JSONObject(output.toString());
                    conn.disconnect();
                    System.out.println(jsonObject.toString());
                    location = jsonObject.getJSONArray("results").getJSONObject(1).getString("formatted_address");
                    bottomSheetFragment.setAddress(location);
                    addressMap.put(mIndex, location);
                } catch (IOException | JSONException ex) {
                    ex.printStackTrace();
                }
            }
        };
        thread.start();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnMapClickListener(latLng -> {
            Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title("New Marker"));
            marker.setTag(markerIndex++);
            bottomSheetFragment = new BottomSheetFragment(latLng, marker, this);
            bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());
            getAddressFromLocation(latLng, markerIndex - 1);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        });

        mMap.setOnMarkerClickListener(marker -> {
            //Triks for å åpne "edit" menyen når en marker trykkes på for andre gang.
            int index = (int) marker.getTag();
            if (lastMarkerIndex == index) {
                bottomSheetFragment = new BottomSheetFragment(marker.getPosition(), marker, this);
                Bundle bundle = new Bundle();
                bundle.putString("title", marker.getTitle());
                System.out.println(marker.getTitle());
                bundle.putString("description", marker.getSnippet());
                bundle.putString("latlng", String.format(Locale.ENGLISH, "%s,%s", marker.getPosition().latitude, marker.getPosition().longitude));
                bundle.putString("address", addressMap.get((int)marker.getTag()));
                bundle.putInt("id", (int)marker.getTag());
                bottomSheetFragment.setArguments(bundle);
                bottomSheetFragment.show(getSupportFragmentManager(), bottomSheetFragment.getTag());
                lastMarkerIndex = -1;
                return true;
            }
            lastMarkerIndex = index;
            marker.showInfoWindow();
            mMap.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
            return true;
        });

        getAttractionFromDb();

        // Add a marker in Sydney and move the camera
        /*LatLng oslo = new LatLng(59.911, 10.75);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(oslo));*/
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            handleNewLocation(mLastLocation);
        }
    };

    public void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("Jeg er her!");
        mMap.addMarker(options);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location services connected.");

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        /*fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            Location location = task.getResult();
            if (location == null) {
                requestNewLocationData();
            } else {
                handleNewLocation(location);
            }
        });*/

    }

    private void requestNewLocationData() {
        // Initializing LocationRequest
        // object with appropriate methods
        com.google.android.gms.location.LocationRequest mLocationRequest = new com.google.android.gms.location.LocationRequest();
        mLocationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        handleNewLocation(location);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void saveAttractionInDb(String latLng, String title, String description, String address) {
        class SendJSON extends AsyncTask<String, Void, Void> {
            @Override
            protected Void doInBackground(String... urls) {
                try {
                    URL url = new URL(urls[0]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    if (conn.getResponseCode() != 200) {
                        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    System.out.println("Output from Server .... \n");
                    StringBuilder output = new StringBuilder();
                    while (true) {
                        String s = br.readLine();
                        if (s == null)
                            break;
                        output.append(s);
                    }
                    conn.disconnect();
                    System.out.println("Request complete");
                } catch (Exception e) {
                    System.out.println("Failed to save to db");
                    e.printStackTrace();
                }
                return null;
            }
        }
        SendJSON sendJSON = new SendJSON();
        sendJSON.execute("http://data1500.cs.oslomet.no/~s354410/jsonin.php?title=" + title + "&description=" + description + "&address=" + address + "&latlng=" + latLng);
    }

    public void updateAttractionInDb(int id, String latLng, String title, String description, String address) {
        class SendJSON extends AsyncTask<String, Void, Void> {
            @Override
            protected Void doInBackground(String... urls) {
                try {
                    URL url = new URL(urls[0]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    if (conn.getResponseCode() != 200) {
                        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    System.out.println("Output from Server .... \n");
                    StringBuilder output = new StringBuilder();
                    while (true) {
                        String s = br.readLine();
                        if (s == null)
                            break;
                        output.append(s);
                    }
                    conn.disconnect();
                    System.out.println("Request complete");
                } catch (Exception e) {

                }
                return null;
            }
        }
        SendJSON sendJSON = new SendJSON();
        sendJSON.execute("http://data1500.cs.oslomet.no/~s354410/jsonupdate.php?id=" + id + "title=" + title + "&description=" + description + "&address=" + address + "&latlng=" + latLng);
    }

    public void deleteAttractionInDb(int id) {
        class SendJSON extends AsyncTask<String, Void, Void> {
            @Override
            protected Void doInBackground(String... urls) {
                try {
                    URL url = new URL(urls[0]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    if (conn.getResponseCode() != 200) {
                        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    System.out.println("Output from Server .... \n");
                    StringBuilder output = new StringBuilder();
                    while (true) {
                        String s = br.readLine();
                        if (s == null)
                            break;
                        output.append(s);
                    }
                    conn.disconnect();
                    System.out.println("Request complete");
                } catch (Exception e) {

                }
                return null;
            }
        }
        SendJSON sendJSON = new SendJSON();
        sendJSON.execute("http://data1500.cs.oslomet.no/~s354410/jsondelete.php?id=" + id);
    }

    public void getAttractionFromDb() {
        class GetJSON extends AsyncTask<String, Void, List<JSONObject>> {
            JSONObject jsonObject = null;

            @Override
            protected List<JSONObject> doInBackground(String... urls) {
                try {
                    URL url = new URL(urls[0]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");
                    if (conn.getResponseCode() != 200) {
                        throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                    System.out.println("Output from Server .... \n");
                    StringBuilder output = new StringBuilder();
                    while (true) {
                        String s = br.readLine();
                        if (s == null)
                            break;
                        output.append(s);
                    }
                    conn.disconnect();
                    try {
                        JSONArray mat = new JSONArray(output.toString());
                        List<JSONObject> attractions = new ArrayList<>();
                        for (int i = 0; i < mat.length(); i++) {
                            attractions.add(mat.getJSONObject(i));
                        }
                        return attractions;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {

                }
                return null;
            }

            @Override
            protected void onPostExecute(List<JSONObject> attractions) {
                if (attractions == null)
                    return;
                try {
                    List<Attraction> attractionsList = new ArrayList<>();
                    for (JSONObject attraction : attractions) {
                        String title = attraction.getString("title");
                        String description = attraction.getString("description");
                        String address = attraction.getString("address");
                        String[] latlngString = attraction.getString("latlng").split(",");
                        LatLng latLng;
                        try {
                            latLng = new LatLng(Double.parseDouble(latlngString[0]), Double.parseDouble(latlngString[1]));
                        } catch (Exception e) {
                            Log.e("Map", "Could not parse position");
                            continue;
                        }
                        attractionsList.add(new Attraction(title, description, address, latLng));
                    }
                    attractionViewModel.getCurrentAttractions().postValue(attractionsList);

                } catch (Exception e) {

                }
            }
        }
        GetJSON getJSON = new GetJSON();
        getJSON.execute("http://data1500.cs.oslomet.no/~s354410/jsonout.php");
    }
}