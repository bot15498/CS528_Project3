package com.example.activityrecognition;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

import static com.google.android.gms.games.multiplayer.Participant.STATUS_FINISHED;

public class ActivityRecognizedService extends IntentService {

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            if (ActivityRecognitionResult.hasResult(intent)) {
                String detActivity = handleDetectedActivities(result.getProbableActivities());
                Intent broadcastIntent = new Intent("com.example.activityrecognition.ActivityRecognizedService");
                broadcastIntent.putExtra("activity", detActivity);
                sendBroadcast(broadcastIntent);
            }
        }
    }

    private String handleDetectedActivities(List<DetectedActivity> probablyActivities) {
        String highest = null;
        int highestConfidence = 0;

        for (DetectedActivity activity : probablyActivities) {
            int confidence = activity.getConfidence();
            switch (activity.getType()) {
                case DetectedActivity.IN_VEHICLE: {
                    if (confidence > highestConfidence) {
                        highest = "in_vehicle";
                        highestConfidence = confidence;
                    }
                    Log.e("ActivityRecognition", "In vehicle: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.RUNNING: {
                    if (confidence > highestConfidence) {
                        highest = "running";
                        highestConfidence = confidence;
                    }
                    Log.e("ActivityRecognition", "Running: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
                    if (confidence > highestConfidence) {
                        highest = "still";
                        highestConfidence = confidence;
                    }
                    Log.e("ActivityRecognition", "Still: " + activity.getConfidence());
                    break;
                }
                case DetectedActivity.WALKING: {
                    if (confidence > highestConfidence) {
                        highest = "walking";
                        highestConfidence = confidence;
                    }
                    Log.e("ActivityRecognition", "Walking: " + activity.getConfidence());
                    break;
                }
            }
        }

        return highest;
    }
}
