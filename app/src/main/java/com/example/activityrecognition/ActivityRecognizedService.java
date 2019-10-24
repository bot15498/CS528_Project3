package com.example.activityrecognition;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

public class ActivityRecognizedService extends IntentService {

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
                    if (confidence > 10) {
                        notifyActivity("in_vehicle");
                    }
                    Log.e("ActivityRecognition", "In vehicle: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.RUNNING: {
                    if (confidence > 10) {
                        notifyActivity("running");
                    }
                    Log.e("ActivityRecognition", "Running: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
                    if (confidence > 10) {
                        notifyActivity("still");
                    }
                    Log.e("ActivityRecognition", "Still: " + activity.getConfidence());
                    notifyActivity("still");
                    break;
                }
                case DetectedActivity.WALKING: {
                    if (confidence > 10) {
                        notifyActivity("walking");
                    }
                    Log.e("ActivityRecognition", "Walking: " + activity.getConfidence());
                    break;
                }
            }
        }
    }

    private void notifyActivity(String activity) {
        Intent activityIntent = new Intent();
        activityIntent.setAction("com.example.activityrecognition.ActivityRecognizedService");
        activityIntent.putExtra("activity", activity);
        sendBroadcast(activityIntent);
    }
}
