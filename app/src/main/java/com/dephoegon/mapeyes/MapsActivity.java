package com.dephoegon.mapeyes;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.dephoegon.mapeyes.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static GoogleMap mMap;
    private ActivityMapsBinding binding;
    private static LatLng current;
    private static String pinText;
    private static LatLng fallBack = new LatLng(40.758701, -111.876183);
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
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) { mapFragment.getMapAsync(this); }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    mMap.setMyLocationEnabled(true);
                }
            } else { Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show(); }
            pokeMap(mMap);
        }
    }
    static ArrayList<MarkerOptions> markers = new ArrayList<>();
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
            MarkerOptions options = new MarkerOptions().position(mark).title("clicked Marker");
            if ((long) markers.size() > 4) {
                MarkerOptions t1 = markers.get(1);
                MarkerOptions t2 = markers.get(2);
                MarkerOptions t3 = markers.get(3);
                MarkerOptions t4 = markers.get(4);
                markers.clear();
                markers.add(t1);
                markers.add(t2);
                markers.add(t3);
                markers.add(t4);
                markers.add(options);
            } else { markers.add(options); }
            mMap.clear();
            mMap.moveCamera(CameraUpdateFactory.newLatLng(mark));
            pokeMapMarkers(mMap, markers);
        });
        mMap.clear();
        mMap.moveCamera(CameraUpdateFactory.newLatLng(find));
        pokeMapMarkers(mMap, markers);
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) { pokeMap(googleMap); }
    private static void pokeMapMarkers(@NonNull GoogleMap map, @NonNull ArrayList<MarkerOptions> arrayList) {
        arrayList.forEach(map::addMarker);
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

        mMap.addMarker(new MarkerOptions().position(SLC).title(pinText));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(SLC));
        pinText = null;
    }
}