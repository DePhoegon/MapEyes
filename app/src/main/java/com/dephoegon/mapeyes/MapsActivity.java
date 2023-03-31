package com.dephoegon.mapeyes;

import static com.dephoegon.mapeyes.BuildConfig.OPEN_WEATHER_API_KEY;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.dephoegon.mapeyes.aid.GPSLocator;
import com.dephoegon.mapeyes.aid.Maths;
import com.dephoegon.mapeyes.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static GoogleMap theMap;
    private static LatLng currentLatLong;
    private static String pinText;
    private static final String updatePinTextLocation = "Current Location";
    static ArrayList<MarkerOptions> markerArray = new ArrayList<>();
    public static JSONObject weatherJSON;
    private static final float defaultPinCarColor = BitmapDescriptorFactory.HUE_VIOLET;
    private static final float thirdTripPinWeatherColor = 345f; //#FF0040
    private static final float secondTripPinWeatherColor = 35f; //#FF9500
    private static final float firstTripPinWeatherColor = 60f; //#FFFF00
    private static final float zeroTripPinWeatherColor = 135f; //#00FF40
    private static GoogleMap getMap() { return theMap; }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.dephoegon.mapeyes.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else { ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1); }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 2);
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) { mapFragment.getMapAsync(this); }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    theMap.setMyLocationEnabled(true);
                }
            } else { Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show(); }
            initialMapPoke(theMap);
        } else if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) { Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show(); }
            } else { Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();  }
        }
    }
    public static void findMyLocation(@NonNull Location arg0) {
        theMap = getMap();
        theMap.clear();
        setCurrentLatLong(arg0.getLatitude(), arg0.getLongitude());
        pinText = updatePinTextLocation;
        if (markerArray.isEmpty()) { currentLocationPin(currentLatLong.latitude, currentLatLong.longitude, pinText, theMap, null, true); }
        else { currentLocationPin(currentLatLong.latitude, currentLatLong.longitude, pinText, theMap, markerArray, true); }
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        theMap = googleMap;
        theMap.clear();
        theMap.setTrafficEnabled(true);
        theMap.setOnMapClickListener((listener)-> getAsyncWeatherPin(listener.latitude, listener.longitude, theMap));
        initialMapPoke(theMap);
    }
    private static void setCurrentLatLong(double lat, double lon) { currentLatLong = new LatLng(lat, lon); }
    private void initialMapPoke(GoogleMap map) {
        theMap = map;
        GPSLocator gpsLocator = new GPSLocator(getApplicationContext());
        Location location = gpsLocator.GetLocation();
        setCurrentLatLong(location.getLatitude(), location.getLongitude());
        currentLocationPin(currentLatLong.latitude, currentLatLong.longitude, updatePinTextLocation, theMap, null, true);
    }
    public static void getAsyncWeatherPin(double lat, double lon, GoogleMap map) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() { super.onPreExecute(); }
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    URL url = new URL("https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + OPEN_WEATHER_API_KEY);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder json = new StringBuilder(4096);
                    String tmp;
                    while ((tmp = reader.readLine()) != null) { json.append(tmp).append("\n"); }
                    reader.close();

                    weatherJSON = new JSONObject(json.toString());
                    if (weatherJSON.getInt("cod") != 200) { return null; }
                } catch (Exception e) { return null; }
                return null;
            }
            @Override
            protected void onPostExecute(Void Void) {
                if (weatherJSON != null) {
                    JSONObject weatherHolder = weatherJSON;
                    JSONObject mainGroup;
                    JSONObject windGroup;
                    int visibility;
                    double wTemp;
                    double wSpeed;
                    double wGusts;
                    int wDirection;
                    String wTitle;
                    int trippedWarnings = 0;

                    try { visibility = weatherHolder.getInt("visibility"); }
                    catch (JSONException e) { return; }
                    try {
                        mainGroup = weatherHolder.getJSONObject("main");
                        wTemp = mainGroup.getDouble("temp");
                    } catch (JSONException e) { return; } // Mains/temp
                    try {
                        windGroup = weatherHolder.getJSONObject("wind");
                        wSpeed = windGroup.getDouble("speed");
                        wGusts = windGroup.getDouble("gust");
                        wDirection = windGroup.getInt("deg");
                    } catch (JSONException e) { return; } // Winds
                    try { wTitle = "| "+weatherHolder.getString("name")+" |"; }
                    catch (JSONException e) { return; }
                    if (weatherHolder.has("main")) {
                        if (weatherHolder.has("visibility") && visibility < 500) {
                            wTitle = wTitle +" - Low Vis -> "+ visibility+"Meters";
                            trippedWarnings += 1;
                        }
                        if (mainGroup.has("temp")) {
                            double hold = Maths.Fahrenheit(wTemp);
                            wTitle = wTitle + " - Temp -> "+ String.format(Locale.US, "%.2f", hold)+"F°";
                        }
                        if (windGroup.has("speed")) {
                            if (wSpeed > 32) {
                                wTitle = " Winds -> "+ String.format(Locale.US, "%.2f", wSpeed) + " - " + wTitle;
                                trippedWarnings +=1;
                            }
                            if (wGusts > 45) {
                                wTitle = " Gusts -> " + String.format(Locale.US, "%.2f", wGusts) + " - " + wTitle;
                                trippedWarnings +=1;
                            }
                            String Dir = Maths.Direction(wDirection);
                            wTitle = wTitle + " - Wind Dir -> " + Dir;
                        }
                    }
                    weatherLocationPins(lat, lon, wTitle, map, trippedWarnings);
                }
            }
        }.execute();
    }
    private static void currentLocationPin(double lat, double lon, String markerTitle, @NonNull GoogleMap map, ArrayList<MarkerOptions> arrayList, boolean moveMap) {
        map.clear();
        if (lat != 200 && lon != 200) { setCurrentLatLong(lat, lon); }
        pinText = markerTitle;
        MarkerOptions options = new MarkerOptions().position(currentLatLong).title(markerTitle).icon(BitmapDescriptorFactory.defaultMarker(defaultPinCarColor));
        if (moveMap) { map.moveCamera(CameraUpdateFactory.newLatLng(currentLatLong)); }
        map.addMarker(options);
        if (arrayList != null){ markerArray.forEach(map::addMarker); }
    }
    private static void weatherLocationPins(double lat, double lon, String markerTitle, GoogleMap map, int warningTrips) {
        MarkerOptions options = new MarkerOptions().position(new LatLng(lat, lon)).title(markerTitle).icon(BitmapDescriptorFactory.defaultMarker(hueColor(warningTrips)));
        ArrayList<MarkerOptions> temp = new ArrayList<>();
        final int countCut = 1;
        final int maxArraySize = 6; // Max Size = +1
        if (markerArray.size() > maxArraySize) {
            for (int i = 0; i < markerArray.size(); i++) {
                if (i-countCut >= 0) { temp.add(markerArray.get(i)); }
                if (i == markerArray.size()-countCut) { temp.add(options); }
            }
            markerArray = temp;
        } else { markerArray.add(options); }
        String textOnPin = pinText;

        map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));
        currentLocationPin(200, 200, textOnPin, map, markerArray, false);
    }
    private static float hueColor(int trips) {
        if (trips == 0) { return zeroTripPinWeatherColor; }
        if (trips == 1) { return firstTripPinWeatherColor; }
        if (trips == 2) { return secondTripPinWeatherColor; }
        return thirdTripPinWeatherColor;
    }
}