package com.dephoegon.mapeyes.aid;

import static com.dephoegon.mapeyes.MapsActivity.findMyLocation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.dephoegon.mapeyes.MapsActivity;

public class GPSLocator implements LocationListener {
    private final Context context;
    private boolean providerDisabled = false;

    public GPSLocator(Context c) { context = c; }
    public Location GetLocation() {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(context, "Fine Permission not granted", Toast.LENGTH_SHORT).show();
            return null;
        }

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(isGPSEnabled){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 10, this);
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else { Toast.makeText(context, "No GPS Detected", Toast.LENGTH_SHORT).show(); }
        return null;
    }
    @Override
    public void onLocationChanged(Location location) { if (!providerDisabled) { findMyLocation(location); } }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override
    public void onProviderEnabled(String provider) {
        providerDisabled = false;
        Toast.makeText(context, "Provider Enabled", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onProviderDisabled(String provider) {
        MapsActivity.currentLocationPin(0,0, "GPS Disabled", MapsActivity.theMap, null, true);
        providerDisabled = true;
        Toast.makeText(context, "Provider Disabled", Toast.LENGTH_SHORT).show();
    }
}