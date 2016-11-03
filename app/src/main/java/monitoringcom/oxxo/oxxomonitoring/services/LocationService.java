package monitoringcom.oxxo.oxxomonitoring.services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import monitoringcom.oxxo.oxxomonitoring.AppController;
import monitoringcom.oxxo.oxxomonitoring.Utils.Constants;
import monitoringcom.oxxo.oxxomonitoring.LocationJSONRequestPOST;

/**
 * Created by 3104729 on 22/02/2016.
 */
public class LocationService extends Service{

    private final String TAG = LocationService.class.getSimpleName();
    PowerManager.WakeLock wakeLock;
    private LocationManager mLocationManager;
    final private int REQUEST_LOCATION = 123;
    private final IBinder mBinder = new LocalBinder();
    private boolean mIsPanic = false;
    public static final String MY_PREFS_NAME = "MyPrefsFile";





    public LocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub

        Log.e(TAG, "Service Started onBind");

        Bundle b = intent.getExtras();
        if(b == null){
            Log.wtf(TAG, "Bundle NULL");
        }else{
            mIsPanic = b.getBoolean("panico", false);
        }
        mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);


        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting Location Updates");

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.LOCATION_UPDATES_MIN_TIME_IN_MILLISECONDS, Constants.LOCATION_UPDATES_MIN_DISTANCE_IN_METERS, listener);
        }else{
            Toast.makeText(getApplicationContext(), "Se necesitan permisos de GPS", Toast.LENGTH_SHORT).show();
        }



        return mBinder ;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "ServiceOnUnBind");
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(listener);
        }
        return true;
    }



    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager pm = (PowerManager) getSystemService(this.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotSleep");
        wakeLock.acquire();
        Log.d(TAG, "Service Created");

    }

    private LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {

            Log.d(TAG, "Location Changed");

            if (location == null)
                return;

            if (isConnectingToInternet(getApplicationContext())) {
                JSONObject jsonObject = new JSONObject();

                SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);

                String nombrePref  = prefs.getString("nombre", "No name defined");
                String plazaPref =  prefs.getString("plaza", "No plaza defined");
                String numeroDeEmpleado =  prefs.getString("clave", "No clave defined");



                try {

                    if(mIsPanic){
                        jsonObject.put("tipo", "panico");
                    }else{
                        jsonObject.put("tipo", "monitoreo");
                    }

                    Calendar c = Calendar.getInstance();
                    jsonObject.put("latitude", location.getLatitude());
                    jsonObject.put("longitude", location.getLongitude());
                    jsonObject.put("date", c.getTimeInMillis());
                    jsonObject.put("empleado", numeroDeEmpleado);

                    Log.d("request", jsonObject.toString());

                    LocationJSONRequestPOST jsObjRequest = new LocationJSONRequestPOST(Request.Method.POST, Constants.LOCATION_SERVLET_URL_DEMO, jsonObject, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.i(TAG, "Response: "+response.toString());
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.wtf(TAG, "Error: " +error.toString());
                        }
                    });

                    AppController.getInstance().addToRequestQueue(jsObjRequest);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }

    public static boolean isConnectingToInternet(Context _context) {
        ConnectivityManager connectivity = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
        }
        return false;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
        }
    }
}
