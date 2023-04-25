package com.dephoegon.mapeyes;

import static com.dephoegon.mapeyes.BuildConfig.OPEN_WEATHER_API_KEY;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements GoogleMap.OnInfoWindowClickListener, OnMapReadyCallback {
    public static GoogleMap theMap;
    private static LatLng currentLatLong;
    private static String pinText;
    private static final String updatePinTextLocation = "Current Location";
    static ArrayList<MarkerOptions> markerArray = new ArrayList<>();
    public static JSONObject weatherJSON;
    private static Context thisAppContext;
    Location gps_loc;
    Location network_loc;
    Location final_loc;

    private static Context getThisAppContext() { return thisAppContext; }
    private static GoogleMap getMap() { return theMap; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        thisAppContext = this.getApplicationContext();
        com.dephoegon.mapeyes.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
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
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                }
            } else { Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show(); }
        }
    }

    public static void findMyLocation(@NonNull Location arg0) {
        theMap = getMap();
        theMap.clear();
        setCurrentLatLong(arg0.getLatitude(), arg0.getLongitude());
        pinText = updatePinTextLocation;
        if (markerArray.isEmpty()) {
            currentLocationPin(currentLatLong.latitude, currentLatLong.longitude, pinText, theMap, null, true);
        } else {
            currentLocationPin(currentLatLong.latitude, currentLatLong.longitude, pinText, theMap, markerArray, true);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        theMap = googleMap;
        theMap.clear();
        theMap.setTrafficEnabled(true);
        theMap.setOnMapClickListener((listener) -> getAsyncWeatherPin(listener.latitude, listener.longitude, theMap));
        theMap.setOnInfoWindowClickListener(this);
        theMap.setOnMarkerClickListener((marker) -> {
            if (marker.isInfoWindowShown()) { marker.hideInfoWindow(); }
            else { marker.showInfoWindow(); }
            return false;
        });
        initialMapPoke(theMap);
    }

    private static void setCurrentLatLong(double lat, double lon) { currentLatLong = new LatLng(lat, lon); }

    private void initialMapPoke(GoogleMap map) {
        theMap = map;
        GPSLocator gpsLocator = new GPSLocator(getApplicationContext());
        Location location = gpsLocator.GetLocation();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (location != null) { setCurrentLatLong(location.getLatitude(), location.getLongitude()); }
        else try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { return; }
            gps_loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            network_loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) { return; }
        if (gps_loc != null) {
            final_loc = gps_loc;
            setCurrentLatLong(final_loc.getLatitude(), final_loc.getLongitude());
        } else if (network_loc != null) {
            final_loc = network_loc;
            setCurrentLatLong(final_loc.getLatitude(), final_loc.getLongitude());
        }
        if (currentLatLong == null) { setCurrentLatLong(0,0); }
        currentLocationPin(currentLatLong.latitude, currentLatLong.longitude, updatePinTextLocation, theMap, null, true);
    }
    private static void getAsyncWeatherPin(double lat, double lon, GoogleMap map) {
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
                    JSONObject mainGroup = null;
                    JSONObject windGroup = null;
                    JSONArray weatherType;
                    JSONObject typeHolder;
                    int visibility = 999999999;
                    double initialValue = 9999999;
                    double wTemp = initialValue;
                    double wSpeed = initialValue;
                    double wGusts = initialValue;
                    int wDirection = 0;
                    double wID = 0;
                    String wMain = "";
                    String wDesc = "";
                    String wTitle = "";
                    int trippedWarnings = 0;

                    try { visibility = weatherHolder.getInt("visibility"); }
                    catch (JSONException ignored) {  }
                    try {
                        mainGroup = weatherHolder.getJSONObject("main");
                        wTemp = mainGroup.getDouble("temp");
                    } catch (JSONException ignored) {  } // Mains/temp
                    try {
                        windGroup = weatherHolder.getJSONObject("wind");
                        wSpeed = windGroup.getDouble("speed");
                        wGusts = windGroup.getDouble("gust");
                        wDirection = windGroup.getInt("deg");
                    } catch (JSONException ignored) {  } // Winds
                    try { wTitle = "| "+weatherHolder.getString("name")+" |"; }
                    catch (JSONException ignored) { }
                    try {
                        weatherType = weatherHolder.getJSONArray("weather");
                        typeHolder = weatherType.getJSONObject(0);
                        if (typeHolder.has("id")) { wID = typeHolder.getInt("id"); }
                        if (typeHolder.has("main")) { wMain = typeHolder.getString("main"); }
                        if (typeHolder.has("description")) { wDesc = typeHolder.getString("description"); }
                    }  catch (JSONException ignored) {  }
                    String snipping = "";
                    boolean safeEnoughTrip = false;
                    if (wID > 199 && wID < 800) {
                        snipping = snipping + wMain + " | ";
                        if (wID < 211 || (wID > 299 && wID <310) || (wID > 499 && wID < 503) || wID == 600 || wID == 701) {
                            trippedWarnings += 1;
                            safeEnoughTrip = true;
                        }
                        else if (wID > 750) { trippedWarnings += 3; }
                        else { trippedWarnings += 2; }
                        if (!safeEnoughTrip) { snipping = snipping + wDesc + " | "; }
                    }
                    if (weatherHolder.has("main")) {
                        if (weatherHolder.has("visibility") && visibility < 500) {
                            snipping = snipping + "Low Vis-> " + visibility + "Meters | ";
                            trippedWarnings += 1;
                        }
                        assert mainGroup != null;
                        if (mainGroup.has("temp") && wTemp != initialValue) {
                            double hold = Maths.Fahrenheit(wTemp);
                            wTitle = wTitle + " - Temp -> "+ String.format(Locale.US, "%.2f", hold)+"F°";
                        }
                        if (windGroup != null && windGroup.has("speed")) {
                            if (wSpeed > 25 && wSpeed != initialValue) {
                                snipping = snipping + "Wind-> " + String.format(Locale.US, "%.2f", wSpeed) + " | ";
                                trippedWarnings += 1;
                            }
                            if (wGusts > 35 && wGusts != initialValue) {
                                snipping = snipping + "Gusts-> " + String.format(Locale.US, "%.2f", wGusts) + " | ";
                                trippedWarnings += 1;
                            }
                            String Dir = Maths.Direction(wDirection);
                            if (!Dir.equals("invalid")) { wTitle = wTitle + " - Wind Dir -> " + Dir; }
                        }
                    }
                    if (safeEnoughTrip && trippedWarnings == 1) { snipping = snipping + "Friendly Enough Driving weather"; }
                    else if (!safeEnoughTrip && trippedWarnings == 1) { snipping = snipping + "Driving weather"; }
                    if (trippedWarnings == 2)  { snipping = snipping + "Use Caution"; }
                    if (snipping.equals("")) { snipping = "Friendly Enough Driving weather"; }
                    weatherLocationPins(lat, lon, wTitle, map, trippedWarnings, snipping);
                }
            }
        }.execute();
    }
    public static void currentLocationPin(double lat, double lon, String markerTitle, @NonNull GoogleMap map, ArrayList<MarkerOptions> arrayList, boolean moveMap) {
        map.clear();
        if (lat != 200 && lon != 200) { setCurrentLatLong(lat, lon); }
        pinText = markerTitle;
        String snips = lat == 0 && lon == 0 ? "Location Services Not Ready" : "You are here | Drive Safely";
        MarkerOptions options = new MarkerOptions().icon(BitmapFromVector(getThisAppContext(), R.drawable.vechicle)).position(currentLatLong).title(markerTitle).snippet(snips);
        if (moveMap) { map.moveCamera(CameraUpdateFactory.newLatLng(currentLatLong)); }
        map.addMarker(options);
        if (arrayList != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { arrayList.forEach(map::addMarker); }
            else { for (int i = 0; i < arrayList.size(); i++) { map.addMarker(arrayList.get(i)); } }
        }
    }
    private static void weatherLocationPins(double lat, double lon, String markerTitle, GoogleMap map, int warningTrips, String snips) {
        MarkerOptions options = getTripPin(warningTrips).position(new LatLng(lat, lon)).title(markerTitle).snippet(snips);
        final int countCut = 1;
        final int maxArraySize = 7; // 2+ size
        ArrayList<MarkerOptions> temp = new ArrayList<>();
        if (markerArray.size() > maxArraySize-1) {
            for (int i = 0; i < markerArray.size(); i++) {
                if (i-countCut > -1) { temp.add(markerArray.get(i)); }
                if (i == markerArray.size()-countCut) { temp.add(options); }
            }
            markerArray = temp;
        } else { markerArray.add(options); }
        String textOnPin = pinText;

        map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));
        currentLocationPin(200, 200, textOnPin, map, markerArray, false);
    }
    @NonNull
    private static MarkerOptions getTripPin(int tripped) {
        if (tripped == 0) { return new MarkerOptions().icon(BitmapFromVector(getThisAppContext(), R.drawable.zerotrippin)); }
        if (tripped == 1) { return new MarkerOptions().icon(BitmapFromVector(getThisAppContext(), R.drawable.firsttrippin)); }
        if (tripped == 2) { return new MarkerOptions().icon(BitmapFromVector(getThisAppContext(), R.drawable.sendtrippin)); }
        return new MarkerOptions().icon(BitmapFromVector(getThisAppContext(), R.drawable.thirdtrippin));
    }
    @NonNull
    private static BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        // below line is use to generate a drawable.
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);

        // below line is use to set bounds to our vector drawable.
        assert vectorDrawable != null;
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());

        // below line is use to create a bitmap for our
        // drawable which we have added.
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        // below line is use to add bitmap in our canvas.
        Canvas canvas = new Canvas(bitmap);

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas);

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) { marker.hideInfoWindow(); }
}