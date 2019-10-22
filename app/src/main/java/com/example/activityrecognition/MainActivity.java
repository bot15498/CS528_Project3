package com.example.activityrecognition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.activityrecognition.StepCounter.StepCounterContainer;
import com.example.activityrecognition.StepCounter.StepDetector;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
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
		ResultCallback<Status>,
		SensorEventListener,
		StepCounterContainer
{


	private GoogleApiClient mApiClient;
	private static final String TAG = MainActivity.class.getSimpleName();
	private GoogleMap map;
	private LatLng latlng;

	private static ImageView detectedActivityImageView;
	private static TextView activityTextView;

	private DatabaseLab dbLab;
	private SensorManager mSensorManager;
	private Sensor accel;
	private StepDetector mStepDetector;
	private int steps = 0;
	private TextView stepCounterTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		detectedActivityImageView = findViewById(R.id.detectedActivityImageView);
		activityTextView = findViewById(R.id.activity);

		createGoogleApi();

		mapFragment.getMapAsync(this);

		// Lab's Lat & lang
		latlng = new LatLng(42.281367, -71.805810);
		markerForGeofence(latlng);
		startGeofence();

		mApiClient = new GoogleApiClient.Builder(this)
				.addApi(ActivityRecognition.API)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.build();

		mApiClient.connect();

		// Sensor init stuff for step counting
		dbLab = DatabaseLab.get(getApplicationContext());
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
		mStepDetector = new StepDetector();
		mStepDetector.addListener(this);
		stepCounterTextView = findViewById(R.id.steps);
		updateStepCount();
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
					mApiClient,
					request,
					createGeofencePendingIntent()
			).setResultCallback(this);
	}


	// +++++++++++++++++++ GeoFence end +++++++++++++++++++++++




	//+++++++++++++++++++ google apiClient location +++++++++++


	// Create GoogleApiClient instance
	private void createGoogleApi () {
		Log.d(TAG, "createGoogleApi()");
		if (mApiClient == null) {
			mApiClient = new GoogleApiClient.Builder(this)
					.addConnectionCallbacks(this) // this
					.addOnConnectionFailedListener(this)
					.addApi(LocationServices.API)
					.build();
		}
	}


	@Override
	public void onConnected (@Nullable Bundle bundle){
		Intent intent = new Intent(this, ActivityRecognizedService.class);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 3000, pendingIntent);
	}

	@Override
	public void onConnectionSuspended (int i){

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
	private void drawGeofence() {
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

	static void updateImage(String activity) {
		switch (activity) {
			case "in_car": {
				if (detectedActivityImageView != null) {
					detectedActivityImageView.setImageResource(R.mipmap.ic_in_car);
				}
				if (activityTextView != null) {
					activityTextView.setText("You are in a Car");
				}
				break;
			}
			case "running": {
				if (detectedActivityImageView != null) {
					detectedActivityImageView.setImageResource(R.mipmap.ic_running);
				}
				if (activityTextView != null) {
					activityTextView.setText("You are Running");
				}
				break;
			}
			case "still": {
				if (detectedActivityImageView != null) {
					detectedActivityImageView.setImageResource(R.mipmap.ic_still);
				}
				if (activityTextView != null) {
					activityTextView.setText("You are Still");
				}
				break;
			}
			case "walking": {
				if (detectedActivityImageView != null) {
					detectedActivityImageView.setImageResource(R.mipmap.ic_walking);
				}
				if (activityTextView != null) {
					activityTextView.setText("You are Walking");
				}
				break;
			}
		}
	}

	public static class ActivityBroadcastReceiver extends BroadcastReceiver {
//		private final Handler handler; // Handler used to execute code on the UI thread
//
//		public ActivityBroadcastReceiver(Handler handler) {
//			this.handler = handler;
//		}

		@Override
		public void onReceive(final Context context, Intent intent) {
			// Post the UI updating code to our Handler
//			handler.post(new Runnable() {
//				@Override
//				public void run() {
//					Toast.makeText(context, "Toast from broadcast receiver", Toast.LENGTH_SHORT).show();
//				}
//			});

			Bundle extras = intent.getExtras();
			if (extras != null) {
				String activity = (String) extras.get("activity");
				updateImage(activity);
			}
		}
	}

	//+++++++++++++++++++ Sensor event listener for step counting +++++++++++

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			mStepDetector.updateAccel(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
		// cry
	}

	@Override
	public void step() {
		steps++;
		updateStepCount();
	}

	private void updateStepCount() {
		String stepText = getText(R.string.steps) + " " + Integer.toString(steps);
		stepCounterTextView.setText(stepText);
	}

}
