package com.example.activityrecognition;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class CustomResultReceiver extends ResultReceiver {

    /*
     * Step 1: The AppReceiver is just a custom interface class we created.
     * This interface is implemented by the activity
     */
    private AppReceiver appReceiver;

    public CustomResultReceiver(Handler handler,
                                AppReceiver receiver) {
        super(handler);
        appReceiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (appReceiver != null) {
            /*
             * Step 2: We pass the resulting data from the service to the activity
             * using the AppReceiver interface
             */
            appReceiver.onReceiveResult(resultCode, resultData);
        }
    }


    public interface AppReceiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }
}
