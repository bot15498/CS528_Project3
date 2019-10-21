package com.example.activityrecognition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		OnMapReadyCallback,
		ResultCallback<Status> {


	private GoogleApiClient googleApiClient;
	private static final String TAG = MainActivity.class.getSimpleName();
	private GoogleMap map;
	private LatLng latlng;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

		createGoogleApi();

		mapFragment.getMapAsync(this);

		// Lab's Lat & lang
		latlng = new LatLng(42.281367, -71.805810);
		markerForGeofence(latlng);
		startGeofence();


	}

	private static final String NOTIFICATION_MSG = "NOTIFICATION MSG";

	public static Intent makeNotificationIntent(Context context, String msg) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra(NOTIFICATION_MSG, msg);
		return intent;
	}


	private Marker geoFenceMarker;

	private void markerForGeofence(LatLng latLng) {
		Log.i(TAG, "markerForGeofence(" + latLng + ")");
		String title = latLng.latitude + ", " + latLng.longitude;
		// Define marker options
		MarkerOptions markerOptions = new MarkerOptions()
				.position(latLng)
				.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
				.title(title);

		if (map != null) {
			// Remove last geoFenceMarker
			if (geoFenceMarker != null)
				geoFenceMarker.remove();

			geoFenceMarker = map.addMarker(markerOptions);
		}
	}


	//private static final long GEO_DURATION = 900*1000;
	private static final String GEOFENCE_REQ_ID = "My Geofence";
	private static final float GEOFENCE_RADIUS = 500.0f; // in meters
	private static final int loiteringDelayMs = 15000;

	// Check for permission to access Location
	private boolean checkPermission () {
		Log.d(TAG, "checkPermission()");
		// Ask for permission if it wasn't granted yet
		return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED);
	}


	/* +++++++++++++++++++   GeoFence start +++++++++++++++++++++++ */


	// Create a Geofence --step1--
	private Geofence createGeofence (LatLng latLng,float radius ){
		Log.d(TAG, "createGeofence");
		return new Geofence.Builder()
				.setRequestId(GEOFENCE_REQ_ID)
				.setCircularRegion(latLng.latitude, latLng.longitude, radius)
				.setExpirationDuration(Geofence.NEVER_EXPIRE) // I added it ++
				//.setExpirationDuration(GEO_DURATION)
				.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
						| Geofence.GEOFENCE_TRANSITION_EXIT
						|Geofence.GEOFENCE_TRANSITION_DWELL)
				.setLoiteringDelay (loiteringDelayMs)
				.build();
	}


	// Create a Geofence Request  -- step 2 --
	private GeofencingRequest createGeofenceRequest (Geofence geofence ){
		Log.d(TAG, "createGeofenceRequest");
		return new GeofencingRequest.Builder()
				.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
				.addGeofence(geofence)
				.build();
	}


	private PendingIntent geoFencePendingIntent;
	private final int GEOFENCE_REQ_CODE = 0;
	private PendingIntent createGeofencePendingIntent () {
		Log.d(TAG, "createGeofencePendingIntent");
		if (geoFencePendingIntent != null)
			return geoFencePendingIntent;

		Intent intent = new Intent(this, GeofenceTrasitionService.class);
		return PendingIntent.getService(
				this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}


	// Add the created GeofenceRequest to the device's monitoring list
	private void addGeofence (GeofencingRequest request){
		Log.d(TAG, "addGeofence");
		if (checkPermission())
			LocationServices.GeofencingApi.addGeofences(
					googleApiClient,
					request,
					createGeofencePendingIntent()
			).setResultCallback(this);
	}


	// +++++++++++++++++++ GeoFence end +++++++++++++++++++++++




	//+++++++++++++++++++ google apiClient location +++++++++++


	// Create GoogleApiClient instance
	private void createGoogleApi () {
		Log.d(TAG, "createGoogleApi()");
		if (googleApiClient == null) {
			googleApiClient = new GoogleApiClient.Builder(this)
					.addConnectionCallbacks(this) // this
					.addOnConnectionFailedListener(this)
					.addApi(LocationServices.API)
					.build();
		}
	}


	@Override
	public void onConnected (@Nullable Bundle bundle){

	}

	@Override
	public void onConnectionSuspended ( int i){

	}


	@Override
	public void onResult (@NonNull Status status){
		Log.i(TAG, "onResult: " + status);
		if (status.isSuccess()) {
			drawGeofence();
		} else {
			// inform about fail
		}
	}

	@Override
	public void onConnectionFailed (@NonNull ConnectionResult connectionResult){

	}

	@Override
	public void onMapReady (GoogleMap googleMap){
		Log.d(TAG, "onMapReady()");
		map = googleMap;
	}

	// Draw Geofence circle on GoogleMap
	private Circle geoFenceLimits;
	private void drawGeofence () {
		Log.d(TAG, "drawGeofence()");

		if (geoFenceLimits != null)
			geoFenceLimits.remove();

		CircleOptions circleOptions = new CircleOptions()
				.center(geoFenceMarker.getPosition())
				.strokeColor(Color.argb(50, 70, 70, 70))
				.fillColor(Color.argb(100, 150, 150, 150))
				.radius(GEOFENCE_RADIUS);
		geoFenceLimits = map.addCircle(circleOptions);
	}


	// Start Geofence creation process
	private void startGeofence () {
		Log.i(TAG, "startGeofence()");
		if (geoFenceMarker != null) {
			Geofence geofence = createGeofence(geoFenceMarker.getPosition(), GEOFENCE_RADIUS);
			GeofencingRequest geofenceRequest = createGeofenceRequest(geofence);
			addGeofence(geofenceRequest);
		} else {
			Log.e(TAG, "Geofence marker is null");
		}
	}

}
