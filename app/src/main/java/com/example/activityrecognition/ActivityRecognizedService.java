package com.example.activityrecognition;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

public class ActivityRecognizedService extends IntentService {

    private static int CONFIDENCE_LEVEL = 50;

    MainActivity.ActivityBroadcastReceiver activityBroadcastReceiver;

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getProbableActivities());
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probablyActivities) {
        for (DetectedActivity activity : probablyActivities) {
            int confidence = activity.getConfidence();
            switch (activity.getType()) {
                case DetectedActivity.IN_VEHICLE: {
                    if (confidence > CONFIDENCE_LEVEL) {
                        notifyActivity("in_vehicle");
                    }
                    Log.e("ActivityRecognition", "In vehicle: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.RUNNING: {
                    if (confidence > CONFIDENCE_LEVEL) {
                        notifyActivity("running");
                    }
                    Log.e("ActivityRecognition", "Running: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
                    if (confidence > CONFIDENCE_LEVEL) {
                        notifyActivity("still");
                    }
                    Log.e("ActivityRecognition", "Still: " + activity.getConfidence());
                    notifyActivity("still");
                    break;
                }
                case DetectedActivity.WALKING: {
                    if (confidence > CONFIDENCE_LEVEL) {
                        notifyActivity("walking");
                    }
                    Log.e("ActivityRecognition", "Walking: " + activity.getConfidence());
                    break;
                }
            }
        }
    }

    private void notifyActivity(String activity) {
        Intent activityIntent = new Intent(this, MainActivity.ActivityBroadcastReceiver.class);
        activityIntent.putExtra("activity", activity);
        sendBroadcast(activityIntent);
    }
}
