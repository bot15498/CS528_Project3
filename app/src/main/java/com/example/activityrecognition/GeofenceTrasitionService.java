package com.example.activityrecognition;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;


import java.util.ArrayList;
import java.util.List;

import static com.example.activityrecognition.MainActivity.loiteringDelayMs;

public class GeofenceTrasitionService extends IntentService {

    private static final String TAG = GeofenceTrasitionService.class.getSimpleName();
    //public static final int GEOFENCE_NOTIFICATION_ID = 0;

    public GeofenceTrasitionService() {
        super(TAG);
    }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.google.android.c2dm.intent.RECEIVE");
		filter.addAction("com.google.android.c2dm.intent.REGISTRATION");
		filter.addCategory("com.example.myapp");
		return super.onStartCommand(intent,flags,startId);
	}

    @Override
    protected void onHandleIntent(Intent intent) {

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMsg = getErrorString(geofencingEvent.getErrorCode() );
            Log.e( TAG, errorMsg );
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL
                || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
                || geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                // start the timer
                // Lets assume the first geofence listed in the return list is the one you are actually in.
                Intent broadcastIntent = new Intent("com.example.activityrecognition.GeofenceTrasitionService");
                broadcastIntent.putExtra("type",Geofence.GEOFENCE_TRANSITION_DWELL);
                broadcastIntent.putExtra("place",triggeringGeofences.get(0).getRequestId());
                broadcastIntent.putExtra("enterTime", System.currentTimeMillis() - loiteringDelayMs);
                Log.i(TAG, "Dwelled in " + triggeringGeofences.get(0).getRequestId());
                sendBroadcast(broadcastIntent);
            } else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                // end the timer, save to main screen.
				Intent broadcastIntent = new Intent("com.example.activityrecognition.GeofenceTrasitionService");
				broadcastIntent.putExtra("type",Geofence.GEOFENCE_TRANSITION_EXIT);
                broadcastIntent.putExtra("place",triggeringGeofences.get(0).getRequestId());
                broadcastIntent.putExtra("exitTime", System.currentTimeMillis());
				Log.i(TAG, "Exited " + triggeringGeofences.get(0).getRequestId());
                sendBroadcast(broadcastIntent);
            }
        } else {
            // Log the error.
            Log.e(TAG, "Error in the Geofence TransitionDetails");
        }
    }



    // Create a detail message with Geofences received
    private String getGeofenceTrasitionDetails(int geoFenceTransition, List<Geofence> triggeringGeofences) {
        // get the ID of each geofence triggered
        ArrayList<String> triggeringGeofencesList = new ArrayList<>();
        for ( Geofence geofence : triggeringGeofences ) {
            triggeringGeofencesList.add( geofence.getRequestId() );

        }

        String status = null;

        if ( geoFenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER )
            status = "You have been inside the " + TextUtils.join( ", ", triggeringGeofencesList) +"for 15 seconds, incrementing counter";
        else if ( geoFenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT )
            status = "Exiting " + TextUtils.join( ", ", triggeringGeofencesList);
        return status;
    }


    // Handle errors
    private static String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "GeoFence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many GeoFences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown error.";
        }
    }



}
