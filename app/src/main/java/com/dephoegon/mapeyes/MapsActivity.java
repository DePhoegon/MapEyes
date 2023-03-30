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

    private static GoogleMap mMap;
    private ActivityMapsBinding binding;
    private static LatLng current;
    private static String pinText;
    private static final LatLng fallBack = new LatLng(40.758701, -111.876183);
    static ArrayList<MarkerOptions> markers = new ArrayList<>();
    public static JSONObject weatherJSON;
    private static GoogleMap getMap() { return mMap; }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
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
                    mMap.setMyLocationEnabled(true);
                }
            } else { Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show(); }
            pokeMap(mMap);
        } else if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) { Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show(); }
            } else { Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();  }
        }
    }
    public static void findMyLocation(@NonNull Location arg0) {
        mMap = getMap();
        mMap.clear();

        LatLng find = new LatLng(arg0.getLatitude(), arg0.getLongitude());
        current = find;
        pinText = "Find Me via Location Change";
        //load the traffic now
        mMap.setTrafficEnabled(true);
        mMap.setOnMapClickListener((listener)-> {
            LatLng mark = new LatLng(listener.latitude, listener.longitude);
            getJSON(mark.latitude, mark.longitude, mMap);
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLng(mark));
        });
        mMap.clear();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(find));
        if (markers.size() > 0) { pokeMapMarkers(mMap, markers); } else { pokeMapMarkers(mMap, null); }
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) { pokeMap(googleMap); }
    private static void pokeMapMarkers(@NonNull GoogleMap map, ArrayList<MarkerOptions> arrayList) {
        if (arrayList != null){ arrayList.forEach(map::addMarker); }
        LatLng test = current == null ? fallBack : current;
        String text = pinText == null ? "Default Fallback" : pinText;
        map.addMarker(new MarkerOptions().position(test).title(text));
        pinText = null;
    }
    private void pokeMap(GoogleMap map) {
        GPSLocator gpsLocator = new GPSLocator(getApplicationContext());
        Location location = gpsLocator.GetLocation();
        LatLng SLC;

        mMap = map;
        current = location == null ? null : new LatLng(location.getLatitude(), location.getLongitude());
        SLC = location == null ? fallBack : current;
        pinText = location == null ? "Default Fallback" : pinText == null ? "Marker on Current Location" : pinText;
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(SLC).title(pinText));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(SLC));
        pinText = null;
    }
    public static void getJSON(double lat, double lon, GoogleMap map) {
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
                    String tmp = "";

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
                    JSONObject weatherJSON1 = weatherJSON;
                    JSONObject mainGroup;
                    JSONObject windGroup;
                    int visibility;
                    double temp;
                    double wSpeed;
                    double wGusts;
                    int wDirection;
                    String wTitle = "WeatherTag";

                    try { visibility = weatherJSON1.getInt("visibility"); }
                    catch (JSONException e) { return; }
                    try {
                        mainGroup = weatherJSON1.getJSONObject("main");
                        temp = mainGroup.getDouble("temp");
                    } catch (JSONException e) { return; } // Mains/temp
                    try {
                        windGroup = weatherJSON1.getJSONObject("wind");
                        wSpeed = windGroup.getDouble("speed");
                        wGusts = windGroup.getDouble("gust");
                        wDirection = windGroup.getInt("deg");
                    } catch (JSONException e) { return; } // Winds
                    try { wTitle = weatherJSON1.getString("name"); }
                    catch (JSONException e) { return; }
                    if (weatherJSON1.has("main")) {
                        if (weatherJSON1.has("visibility") && visibility < 500) {
                            wTitle = wTitle +" - Low Vis -> "+ visibility+"Meters";
                        }
                        if (mainGroup.has("temp")) {
                            double hold = Maths.Fahrenheit(temp);
                            wTitle = wTitle + " - Temp -> F'"+ String.format(Locale.US, "%.2f", hold);
                        }
                        if (windGroup.has("speed")) {
                            if (wSpeed > 32) { wTitle = " Winds -> "+ String.format(Locale.US, "%.2f", wSpeed) + " - " + wTitle; }
                            if (wGusts > 45) { wTitle = " Gusts -> " + String.format(Locale.US, "%.2f", wGusts) + " - " + wTitle; }
                            String Dir = Maths.Direction(wDirection);
                            wTitle = wTitle + " - Wind Dir -> " + Dir;
                        }
                    }
                    addWeatherMarkers(lat, lon, wTitle, map);
                }
            }
        }.execute();
    }
    private static void addWeatherMarkers(double lat, double lon, String markerTitle, GoogleMap map) {
        LatLng mark = new LatLng(lat, lon);
        MarkerOptions options = new MarkerOptions().position(mark).title(markerTitle);
        ArrayList<MarkerOptions> temp = new ArrayList<>();
        // > 6 -- Max Size 7 ( #+1)
        if (markers.size() > 6) {
            for (int i = 0; i < markers.size(); i++) {
                if (i-1 > -1) { temp.add(markers.get(i)); }
                if (i == markers.size()-1) { temp.add(options); }
            }
            markers = temp;
        } else { markers.add(options); }
        map.clear();
        if (markers.size() > 0){ pokeMapMarkers(map, markers); } else { pokeMapMarkers(map, null); }
    }
}