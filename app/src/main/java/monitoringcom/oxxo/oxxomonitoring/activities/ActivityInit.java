package monitoringcom.oxxo.oxxomonitoring.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.gesture.GestureLibrary;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Permission;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


import java.io.File;
import java.security.Permissions;
import java.util.ArrayList;

import monitoringcom.oxxo.oxxomonitoring.AppController;
import monitoringcom.oxxo.oxxomonitoring.PlaceAutoCompleteAdapter;
import monitoringcom.oxxo.oxxomonitoring.R;
import monitoringcom.oxxo.oxxomonitoring.Utils.Util;
import monitoringcom.oxxo.oxxomonitoring.services.LocationService;

public class ActivityInit extends AppCompatActivity implements OnMapReadyCallback, RoutingListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = ActivityInit.class.getSimpleName();
    private PlaceAutoCompleteAdapter mAdapter;
    protected GoogleMap map;
    protected LatLng start;
    protected LatLng end;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationManager mLocationManager;
    protected LocationListener mLocationListener;
    private ArrayList<Polyline> polylines;
    AutoCompleteTextView starting;
    AutoCompleteTextView destination;
    private ProgressDialog progressDialog;

    final private int REQUEST_LOCATION = 123;
    final private int EXTERNAL_STORAGE = 4;

    private FloatingActionButton iniciarMonitoreoBtn;
    private FloatingActionButton panicoBtn;
    private ImageView iniciarNavegacion;
    private CoordinatorLayout snackbarCoordinatorLayout;
    private TextView timerTextView;
    private CountDownTimer mPanicCountDownTimer;
    private boolean isSendingAlert = false;
    private boolean isMonitoring = false;
    private boolean isLocationDetected = false;
    public LocationService mService;
    private CheckBox mUsarMiUbicacionCheckbox;
    boolean mBound = false;

    private static final int[] COLORS = new int[]{R.color.colorPrimaryDark, R.color.colorPrimary, R.color.colorPrimary, R.color.colorAccent, R.color.primary_dark_material_light};


    private static final LatLngBounds BOUNDS_JAMAICA = new LatLngBounds(new LatLng(4.092553093275739, -116.15162841975689),
            new LatLng(43.999379059283605, -84.51100308448076));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

                /* Google Api Client*/
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        MapsInitializer.initialize(this);
        mGoogleApiClient.connect();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /* Polylines */
        polylines = new ArrayList<>();


        /*Bindings*/
        iniciarMonitoreoBtn = (FloatingActionButton) findViewById(R.id.iniciarMonitoreo);
        iniciarMonitoreoBtn.setVisibility(View.GONE);
        iniciarNavegacion = (ImageView)findViewById(R.id.send);
        panicoBtn = (FloatingActionButton) findViewById(R.id.panicButton);
        timerTextView = (TextView) findViewById(R.id.timerTextView);
        snackbarCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.snackbarCoordinatorLayout);
        starting = (AutoCompleteTextView) findViewById(R.id.start);
        destination = (AutoCompleteTextView) findViewById(R.id.destination);
        mUsarMiUbicacionCheckbox = (CheckBox)findViewById(R.id.checkboxMiUbicacion);

        /*Permisos de Storage*/
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ActivityInit.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE);
        }

        /*Permisos de GPS*/
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Check Permissions Now
            ActivityCompat.requestPermissions(ActivityInit.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }

        mAdapter = new PlaceAutoCompleteAdapter(this, android.R.layout.simple_list_item_1, mGoogleApiClient, BOUNDS_JAMAICA, null);

        /*
        * Adds auto complete adapter to both auto complete
        * text views.
        * */

        starting.setAdapter(mAdapter);
        destination.setAdapter(mAdapter);

        mUsarMiUbicacionCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Snackbar snackbar = Snackbar.make(
                            snackbarCoordinatorLayout,
                            "Obteniendo ubicación actual...",
                            Snackbar.LENGTH_LONG);

                    snackbar.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //TODO Implementar algo pa este pedo jaja
                        }
                    });

                    snackbar.show();

                    starting.setText("Mi ubicación");
                    starting.setEnabled(false);
                    starting.dismissDropDown();
                }else{
                    starting.setText("");
                    starting.setEnabled(true);
                }
            }
        });

        /*
        * Sets the start and destination points based on the values selected
        * from the autocomplete text views.
        * */

        starting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final PlaceAutoCompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                Log.i(TAG, "Autocomplete item selected: " + item.description);

                /*
                Issue a request to the Places Geo Data API to retrieve a Place object with additional
                details about the place.
                */
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);
                        start = place.getLatLng();
                    }
                });

            }
        });
        destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final PlaceAutoCompleteAdapter.PlaceAutocomplete item = mAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                Log.i(TAG, "Autocomplete item selected: " + item.description);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
              details about the place.
              */
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);

                        end = place.getLatLng();

                    }
                });

            }
        });

        /*
        These text watchers set the start and end points to null because once there's
        * a change after a value has been selected from the dropdown
        * then the value has to reselected from dropdown to get
        * the correct location.
        * */
        starting.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int startNum, int before, int count) {
                if (start != null) {
                    start = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        destination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


                if (end != null) {
                    end = null;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        iniciarMonitoreoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Bind to Location Service
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Check Permissions Now
                    ActivityCompat.requestPermissions(ActivityInit.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                    //
                } else {

                    if (mBound) {
                        Log.v("ActivityInit", "Service is bound");
                        if (mService != null) {
                            Log.v("ActivityInit", "Service is bound and not null, stopping service...");
                            mService.stopService(new Intent(ActivityInit.this, LocationService.class));
                            unbindService(mConnection);
                            mBound = false;
                            isMonitoring = false;

                            //iniciarMonitoreoBtn.setText("Iniciar Monitoreo");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                iniciarMonitoreoBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_location_on_black_24dp, getApplicationContext().getTheme()));
                            } else {
                                iniciarMonitoreoBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_location_on_black_24dp));
                            }
                        }
                    } else {
                        if (!mBound) {
                            Log.v("ActivityInit", "Service is not bound, starting service...");
                            isMonitoring = true;
                            Intent intent = new Intent(ActivityInit.this, LocationService.class);
                            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

                            Snackbar snackbar = Snackbar.make(
                                    snackbarCoordinatorLayout,
                                    "¡Monitoreo Iniciado!",
                                    Snackbar.LENGTH_LONG);

                            snackbar.setAction("OK", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //TODO Implementar algo pa este pedo jaja
                                }
                            });

                            snackbar.show();
                        }
                    }

                }

            }
        });

//        eliminarBtn = (Button) findViewById(R.id.buttonElimiar);
//        eliminarBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                SharedPreferences preferences = getSharedPreferences("MyPrefsFile", 0);
//                preferences.edit().clear().commit();
//                ActivityInit.this.finish();
//            }
//        });

        panicoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //if(!isSendingAlert && mGestureOverlayView.getVisibility() != View.GONE){
                if (!isSendingAlert) {
                    //mGestureOverlayView.setVisibility(View.GONE);
                    mPanicCountDownTimer = new CountDownTimer(5000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            //panicoBtn.setText("Detener");
                            //TODO: Change icon to stop
                            isSendingAlert = true;

                            timerTextView.setText("" + millisUntilFinished / 1000);
                        }

                        @Override
                        public void onFinish() {

                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                // Check Permissions Now
                                ActivityCompat.requestPermissions(ActivityInit.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                                //
                            } else {

                                if (mBound) {
                                    Log.v("ActivityInit", "Service is bound");
                                    if (mService != null) {
                                        Log.v("ActivityInit", "Service is bound and not null, stopping service...");
                                        mService.stopService(new Intent(ActivityInit.this, LocationService.class));
                                        unbindService(mConnection);
                                        mBound = false;
                                        //iniciarMonitoreoBtn.setText("Iniciar Monitoreo");
                                    }
                                } else {
                                    if (!mBound) {
                                        Log.v("ActivityInit", "Service is not bound, starting service...");
                                        Intent intent = new Intent(ActivityInit.this, LocationService.class);
                                        intent.putExtra("panico", true);
                                        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                                    }
                                }

                            }

                            Snackbar snackbar = Snackbar.make(
                                    snackbarCoordinatorLayout,
                                    "¡Alerta Enviada!",
                                    Snackbar.LENGTH_LONG);

                            snackbar.setAction("OK", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //TODO Implementar algo pa este pedo jaja
                                }
                            });

                            snackbar.show();
                        }
                    }.start();
                } else {
                    //mGestureOverlayView.setVisibility(View.VISIBLE);
                    mPanicCountDownTimer.cancel();
                    isSendingAlert = false;
                    //TODO: Change icon
                    //panicoBtn.setText("Botón de Pánico");
                    timerTextView.setText("");

                }
            }
        });

        startTrackingOnMap();
    }

    @Override
    public void onPause(){

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(mLocationListener);
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.d("ActivityInit", "" + item.getItemId());
        switch (item.getItemId()) {
            case R.id.action_logout:
                SharedPreferences preferences = getSharedPreferences("MyPrefsFile", 0);
                preferences.edit().clear().commit();
                ActivityInit.this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!

                    Intent intent = new Intent(ActivityInit.this, LocationService.class);
                    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

                } else {

                    // permission denied, boo! Disable the functionality that depends on this permission.
                    finish();
                }
                return;

            }
            case EXTERNAL_STORAGE: {

                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.wtf("ActivityInit", "NO SE DIERON PERMISOS DE ESCRITURA");
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;

            Log.wtf("ServiceConnection", "onServiceConnected");
            mService = binder.getService();
            mBound = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                iniciarMonitoreoBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_location_off_black_24dp, getApplicationContext().getTheme()));
            } else {
                iniciarMonitoreoBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_location_off_black_24dp));
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.wtf("ServiceConnection", "onServiceDisconnected");
            mService = null;
            mBound = false;
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.wtf(TAG, "onMapReady");

        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ActivityInit.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }else{
            map.setMyLocationEnabled(true);

        }
        /*
        * Updates the bounds being used by the auto complete adapter based on the position of the
        * map.
        * */
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
                mAdapter.setBounds(bounds);
            }
        });


        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(20.683243, -99.208609));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(4);

        map.moveCamera(center);
        map.animateCamera(zoom);
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onRoutingFailure(RouteException e) {
        // The Routing request failed
        progressDialog.dismiss();
        if (e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Algo pasó, por favor intenta de nuevo", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        progressDialog.dismiss();
        CameraUpdate center = CameraUpdateFactory.newLatLng(start);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);

        map.moveCamera(center);


        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.

        //TODO: Get only the first one.
//        for (int i = 0; i <route.size(); i++) {
//
//            //In case of more than 5 alternative routes
//            int colorIndex = i % COLORS.length;
//
//            PolylineOptions polyOptions = new PolylineOptions();
//            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
//            polyOptions.width(10 + i * 3);
//            polyOptions.addAll(route.get(i).getPoints());
//            Polyline polyline = map.addPolyline(polyOptions);
//            polylines.add(polyline);
//
//            //Toast.makeText(getApplicationContext(),"Route "+ (i+1) +": distance - "+ route.get(i).getDistanceValue()+": duration - "+ route.get(i).getDurationValue(),Toast.LENGTH_SHORT).show();
//        }
        int colorIndex = COLORS.length - 1;
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(getResources().getColor(COLORS[colorIndex]));
        polyOptions.width(10 + 0 * 3);
        polyOptions.addAll(route.get(0).getPoints());
        Polyline polyline = map.addPolyline(polyOptions);
        polylines.add(polyline);

        // Start marker
        MarkerOptions options = new MarkerOptions();
        options.position(start);
        //TODO: Change icon
        //options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_media_play));
        //options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_on_black_24dp));

        map.addMarker(options);

        // End marker
        options = new MarkerOptions();
        options.position(end);
        //TODO: Change icon
        //options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_media_play));
        //options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_on_black_24dp));
        map.addMarker(options);
    }

    @Override
    public void onRoutingCancelled() {

    }

    //@OnClick(R.id.send)
    public void sendRequest(View v) {
        if (Util.Operations.isOnline(this)) {
            route();

        } else {
            Toast.makeText(this, "No hay conexión a Internet", Toast.LENGTH_SHORT).show();
        }
    }

    public void route() {
        if (start == null || end == null) {

            if(start == null && mUsarMiUbicacionCheckbox.isChecked()){
                //Si está nulo y usar mi ubicacion actual está checked, es porque aún no tiene la ubicación actual, se muestra una snackbar.
                if (mUsarMiUbicacionCheckbox.isChecked()) {
                    Snackbar snackbar = Snackbar.make(
                            snackbarCoordinatorLayout,
                            "Obteniendo ubicación actual...",
                            Snackbar.LENGTH_LONG);

                    snackbar.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //TODO Implementar algo pa este pedo jaja
                        }
                    });

                    snackbar.show();
            }
            if (start == null) {
                Log.wtf(TAG, "Texto: "+starting.getText().toString());
                    starting.setError("Elige una ubicación del menú.");
                } else {
                    Toast.makeText(this, "Por favor elige un punto de partida.", Toast.LENGTH_SHORT).show();
                }
            }
            if (end == null) {
                if (destination.getText().length() > 0) {
                    destination.setError("Elige una ubicación del menú.");
                } else {
                    Toast.makeText(this, "Por favor elige un destino.", Toast.LENGTH_SHORT).show();
                }
            }
        } else {

            if (map != null) {
                map.clear();
            }
            iniciarMonitoreoBtn.setVisibility(View.VISIBLE);
            progressDialog = ProgressDialog.show(this, "Por favor espere.",
                    "Obteniendo información de ruta.", true);
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(start, end)
                    .build();
            routing.execute();
        }
    }

    public void startTrackingOnMap() {

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i(TAG, "onLocationChanged");
                isLocationDetected = true;
                if (isMonitoring) {
                    CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude()));
                    CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
                    if (map != null) {
                        map.moveCamera(center);
                        map.animateCamera(zoom);
                    }
                }else{
                    //Si no se está monitoreando aún y el boton de usar mi ubicación está checked, el punto de partida es será mi ubicación.
                    //TODO Agregar otra validación para que no aparezca chingos de veces la snackbar
                    if (mUsarMiUbicacionCheckbox.isChecked()) {
                        Snackbar snackbar = Snackbar.make(
                                snackbarCoordinatorLayout,
                                "¡Ubicación actual obtenida!",
                                Snackbar.LENGTH_LONG);

                        snackbar.setAction("OK", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //TODO Implementar algo pa este pedo jaja
                            }
                        });

                        snackbar.show();
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        start = latLng;
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                    Snackbar snackbar = Snackbar.make(
                            snackbarCoordinatorLayout,
                            "¡El "+provider+" ha sido desactivado!",
                            Snackbar.LENGTH_LONG);

                    snackbar.setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //TODO Implementar algo pa este pedo jaja
                        }
                    });

                    snackbar.show();
            }
        };


        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && isMonitoring){
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 0, mLocationListener);
                Log.wtf(TAG, "usando GPS para monitoreo y traer la ubicación");
            }else if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !isMonitoring){
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, mLocationListener);
                Log.wtf(TAG, "usando NETWORK para traer la ubicación");
            }

        }


    }
}
