package com.example.activityrecognition;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.activityrecognition.StepCounter.StepCounterContainer;
import com.example.activityrecognition.StepCounter.StepDetector;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		OnMapReadyCallback,
		ResultCallback<Status>,
		SensorEventListener,
		StepCounterContainer,
		LocationListener,
		CustomResultReceiver.AppReceiver {


	private GoogleApiClient mApiClient;
	private static final String TAG = MainActivity.class.getSimpleName();
	private GoogleMap map;
	private LatLng latlng;
	private GeofencingClient geoClient;
	private HashMap<String, LatLng> geofenceInfo;
	private ArrayList<Geofence> geofences;
	private PendingIntent geoFencePendingIntent;
	private Intent geofenceIntent;
	private static final int MAP_CAMERA_ZOOM = 17;
	private final int GEOFENCE_REQ_CODE = 0;
	private static final float GEOFENCE_RADIUS = 25.0f; // in meters
	public static final int loiteringDelayMs = 15000;
	private String currGeofenceID = "";
	private long lastEnterTime = 0;
	private String lastActivity = "";
	private static final int LOCATION_REQUEST_CODE = 11111;

	private BroadcastReceiver broadcastReceiver;
	private BroadcastReceiver activityBroadcastReceiver;
	private CustomResultReceiver resultReceiver;

	private TextView libraryTextView;
	private TextView fullerTextView;
	private int libraryVisits;
	private int fullerVisits;
	private static final String FULLER = "Fuller Labs";
	private static final String LIBRARY = "Gordon Library";

	private ImageView detectedActivityImageView;
	private TextView activityTextView;
	private long lastActivityTimestamp;

	private DatabaseLab dbLab;
	private SensorManager mSensorManager;
	private Sensor accel;
	private StepDetector mStepDetector;
	private int steps = 0;
	private TextView stepCounterTextView;

	private MediaPlayer player;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		lastActivityTimestamp = System.currentTimeMillis();
		geofenceInfo = new HashMap<String, LatLng>();
		geofenceInfo.put(FULLER, new LatLng(42.274852, -71.806690));
		geofenceInfo.put(LIBRARY, new LatLng(42.274292, -71.806641));

		requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);

		// Register receiver
		registerReceiver();

		// Geofencing pre-init. Real init is after map and location services are loaded.
//		if(checkPermission()) {
//			geofenceInfo = new HashMap<String, LatLng>();
//			geofenceInfo.put(FULLER, new LatLng(42.274852, -71.806690));
//			geofenceInfo.put(LIBRARY, new LatLng(42.274292, -71.806641));
//			libraryTextView = findViewById(R.id.library);
//			fullerTextView = findViewById(R.id.fuller);
//			String fullerTxt = getText(R.string.fuller) + Integer.toString(fullerVisits);
//			fullerTextView.setText(fullerTxt);
//			String libraryTxt = getText(R.string.library) + Integer.toString(libraryVisits);
//			libraryTextView.setText(libraryTxt);
//			startGeofence();
//		}

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		detectedActivityImageView = findViewById(R.id.detectedActivityImageView);
		activityTextView = findViewById(R.id.activity);

		createGoogleApi();

		mapFragment.getMapAsync(this);

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

	@Override
	protected void onResume() {
		super.onResume();
	}

	private static final String NOTIFICATION_MSG = "NOTIFICATION MSG";

	public static Intent makeNotificationIntent(Context context, String msg) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra(NOTIFICATION_MSG, msg);
		return intent;
	}

	// Check for permission to access Location
	private boolean checkPermission() {
		Log.d(TAG, "checkPermission()");
		// Ask for permission if it wasn't granted yet
		return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case LOCATION_REQUEST_CODE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
					map.setMyLocationEnabled(true);
					drawGeofences();
					latlng = new LatLng(42.2742608, -71.8066728);
					map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, MAP_CAMERA_ZOOM));

					mApiClient = new GoogleApiClient.Builder(this)
							.addApi(ActivityRecognition.API)
							.addConnectionCallbacks(this)
							.addOnConnectionFailedListener(this)
							.build();

					mApiClient.connect();

					libraryTextView = findViewById(R.id.library);
					fullerTextView = findViewById(R.id.fuller);
					String fullerTxt = getText(R.string.fuller) + Integer.toString(fullerVisits);
					fullerTextView.setText(fullerTxt);
					String libraryTxt = getText(R.string.library) + Integer.toString(libraryVisits);
					libraryTextView.setText(libraryTxt);

					startGeofence();
				} else {
					Toast.makeText(getApplicationContext(), "Why did you press deny for location?", Toast.LENGTH_LONG).show();
				}
				return;
			}
			default:
				return;

			// other 'case' lines to check for other
			// permissions this app might request.
		}
	}


	/* +++++++++++++++++++   GeoFence start +++++++++++++++++++++++ */

	// Start Geofence creation process
	private void startGeofence() {
		Log.i(TAG, "startGeofence()");
		geoClient = LocationServices.getGeofencingClient(this);
		geofences = new ArrayList<Geofence>();
		for(String place : geofenceInfo.keySet()) {
			geofences.add(createGeofence(place, geofenceInfo.get(place), GEOFENCE_RADIUS));
		}
		geoClient.addGeofences(createGeofenceRequest(), getGeofencePendingIntent())
			.addOnSuccessListener(this, new OnSuccessListener<Void>() {
				@Override
				public void onSuccess(Void aVoid) {
					// moved drawing geofences to onMapReady
					Log.i(TAG, "Added geofences successfully.");
				}
			})
			.addOnFailureListener(this, new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception e) {
					Log.i(TAG, "Failed to add geofences.");
					Log.i(TAG, e.getMessage());
				}
			});

		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String broadcast = intent.getAction();
				if(broadcast.equalsIgnoreCase("com.example.activityrecognition.GeofenceTrasitionService")) {
					String place = intent.getStringExtra("place");
					int type = intent.getIntExtra("type", -1);
					switch(type) {
						case Geofence.GEOFENCE_TRANSITION_DWELL:
							long enterTime = intent.getLongExtra("enterTime",-1);
							if (enterTime > 0) {
								currGeofenceID = place;
								lastEnterTime = enterTime;
							}
							break;
						case Geofence.GEOFENCE_TRANSITION_EXIT:
							long exitTime = intent.getLongExtra("exitTime",-1);
							if (exitTime > 0 && place.equals(currGeofenceID)) {
								float duration = (exitTime - lastEnterTime) / 1000f;
								String msg = String.format(Locale.US, "You have been in %s geofence for %.2f seconds", place, duration);
								Toast toast = Toast.makeText(getApplicationContext(), msg,Toast.LENGTH_LONG);
								toast.show();
								updateGeofenceText(place);
							}
							currGeofenceID = "";
							break;
					}
				}
			}
		};
		IntentFilter filter = new IntentFilter("com.example.activityrecognition.GeofenceTrasitionService");
		registerReceiver(broadcastReceiver, filter);
	}

	private void updateGeofenceText(String place) {
		if(place.equals(FULLER)) {
			fullerVisits++;
			String txt = getText(R.string.fuller) + Integer.toString(fullerVisits);
			fullerTextView.setText(txt);
		} else if (place.equals(LIBRARY)) {
			libraryVisits++;
			String txt = getText(R.string.library) + Integer.toString(libraryVisits);
			libraryTextView.setText(txt);
		}
	}

	// Create a Geofence --step1--
	private Geofence createGeofence(String name, LatLng latLng, float radius) {
		Log.d(TAG, "createGeofence");
		return new Geofence.Builder()
				.setRequestId(name)
				.setCircularRegion(latLng.latitude, latLng.longitude, radius)
				.setExpirationDuration(Geofence.NEVER_EXPIRE) // I added it ++
				.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER
						| Geofence.GEOFENCE_TRANSITION_EXIT
						| Geofence.GEOFENCE_TRANSITION_DWELL)
				.setLoiteringDelay(loiteringDelayMs)
				.build();
	}


	// Create a Geofence Request  -- step 2 --
	private GeofencingRequest createGeofenceRequest() {
		return new GeofencingRequest.Builder()
				.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
				.addGeofences(geofences)
				.build();
	}

	private PendingIntent getGeofencePendingIntent() {
		Log.d(TAG, "createGeofencePendingIntent");
		if (geoFencePendingIntent != null)
			return geoFencePendingIntent;

		geofenceIntent = new Intent(this, GeofenceTrasitionService.class);
		geoFencePendingIntent = PendingIntent.getService(
				this, GEOFENCE_REQ_CODE, geofenceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		return geoFencePendingIntent;
	}

	// +++++++++++++++++++ GeoFence end +++++++++++++++++++++++


	//+++++++++++++++++++ google apiClient location +++++++++++


	// Create GoogleApiClient instance
	private void createGoogleApi() {
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
	public void onLocationChanged(Location location) {
		// really only care about this.
	}

	@Override
	public void onStatusChanged(String s, int i, Bundle bundle) {

	}

	@Override
	public void onProviderEnabled(String s) {

	}

	@Override
	public void onProviderDisabled(String s) {

	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		Intent intent = new Intent(this, ActivityRecognizedService.class);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mApiClient, 3000, pendingIntent);

		// Now set up map fragment to update based on location services.
		LocationRequest request = LocationRequest.create();
		request.setInterval(5000); // update every 5 seconds
		request.setFastestInterval(3000); // update every 3 seconds
		request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
		FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
		LocationCallback locationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				if (locationResult == null) {
					return;
				}
				Location currLocation = locationResult.getLastLocation();
				latlng = new LatLng(currLocation.getLatitude(), currLocation.getLongitude());
				map.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, MAP_CAMERA_ZOOM));
			}
		};

		client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
	}

	@Override
	public void onConnectionSuspended(int i) {

	}


	@Override
	public void onResult(@NonNull Status status) {
		Log.i(TAG, "onResult: " + status);
		if (status.isSuccess()) {
//			drawGeofence();
			//
		} else {
			// inform about fail
			Toast toast = Toast.makeText(getApplicationContext(), "Failed to connect to google services", Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		Log.d(TAG, "onMapReady()");
		MapsInitializer.initialize(getApplicationContext());
		map = googleMap;
//		map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
//		map.setMyLocationEnabled(true);
	}

	// Draw Geofence circle on GoogleMap

	private void drawGeofences() {
		Log.d(TAG, "drawGeofence()");
		for(LatLng center : geofenceInfo.values()) {
			CircleOptions circleOptions = new CircleOptions()
					.center(center)
					.strokeColor(Color.argb(50, 70, 70, 70))
					.fillColor(Color.argb(100, 150, 150, 150))
					.radius(GEOFENCE_RADIUS);
			map.addCircle(circleOptions);
		}
	}

	//+++++++++++++++++++ Activity recognition +++++++++++

	private void registerReceiver() {
		prepareMusicFile();
		activityBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String activity = intent.getStringExtra("activity");
				long currTime = System.currentTimeMillis();

				/*
				 * Step 3: We can update the UI of the activity here
				 * */
				if (activity != null) {
					if(!activity.equals(lastActivity)) {
						long duration = currTime - lastActivityTimestamp;
						if(!lastActivity.equals("")) {
							dbLab.saveActivity(lastActivity, duration);
						}
						Toast toast = Toast.makeText(getApplicationContext(), getActivityToastText(lastActivity, duration), Toast.LENGTH_LONG);
						toast.show();
						lastActivity = activity;
						lastActivityTimestamp = System.currentTimeMillis();
						switch (activity) {
							case "in_car": {
								if (detectedActivityImageView != null) {
									detectedActivityImageView.setImageResource(R.mipmap.ic_in_car);
								}
								if (activityTextView != null) {
									activityTextView.setText("You are in a Car");
								}
								if(player != null) {
									player.stop();
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
								if(player != null) {
									player.start();
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
								if(player != null) {
									player.stop();
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
								if(player != null) {
									player.stop();
								}
								break;
							}
						}
					}

				}
				Log.e(getClass().getName(), "Got back activity: " + activity);
			}
		};
		IntentFilter intentFilter = new IntentFilter("com.example.activityrecognition.ActivityRecognizedService");
		registerReceiver(activityBroadcastReceiver, intentFilter);
	}

	private String getActivityToastText(String activity, long duration) {
		switch(activity) {
			case "in_car":
				return String.format(Locale.US, "You were in a moving car for %.2f seconds", duration / 1000f);
			case "walking":
				return String.format(Locale.US, "You were walking for %.2f seconds", duration / 1000f);
			case "running":
				return String.format(Locale.US, "You were running for %.2f seconds", duration / 1000f);
			case "still":
				return String.format(Locale.US, "You were still for %.2f seconds", duration / 1000f);
			default:
				return String.format(Locale.US, "Your last activity took %.2f seconds", duration / 1000f);
		}
	}

	private void prepareMusicFile() {
		try {
			AssetFileDescriptor afd;
			afd = getAssets().openFd("beat_02.mp3");
			player = new MediaPlayer();
			player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			player.prepare();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		Log.e(getClass().getName(), resultData.toString());
	}

	//+++++++++++++++++++ Sensor event listener for step counting +++++++++++

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
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

	@Override
	protected void onStop() {
		super.onStop();
	}
}
