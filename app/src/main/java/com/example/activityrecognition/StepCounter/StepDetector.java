package com.example.activityrecognition.StepCounter;

public class StepDetector {
	private static final int TS_LENGTH = 100;
	private static final float MIN_ACCEL_DIFF = 4f;
	private static final float MAX_OTHER_ACCEL_DIFF = 4f;
	private static final double MIN_STEP_DURATION = 1f;

	private StepCounterContainer counter;
	private float[] accelX = new float[TS_LENGTH];
	private float[] accelY = new float[TS_LENGTH];
	private float[] accelZ = new float[TS_LENGTH];
	private int seenPoints = 0;
	private float lastDominantValue = -99f;
	private long lastStepTime;

	public StepDetector() {
		lastStepTime = System.currentTimeMillis();
	}

	public void updateAccel(float x, float y, float z) {
		int arrCount = seenPoints % TS_LENGTH;
		accelX[arrCount] = x;
		accelY[arrCount] = y;
		accelZ[arrCount] = z;
		
		float[] extremaX = getExtrema(accelX);
		float[] extremaY = getExtrema(accelY);
		float[] extremaZ = getExtrema(accelZ);
		
		float[] dominantDim = new float[4];
		float[] otherDims = new float[2];
		switch(findDominantExtrema(extremaX, extremaY, extremaZ)) {
			case 0:
				dominantDim[0] = x;
				dominantDim[1] = extremaX[0];
				dominantDim[2] = extremaX[1];
				dominantDim[3] = extremaX[2];
				otherDims[0] = extremaY[2] - extremaY[0];
				otherDims[1] = extremaZ[2] - extremaZ[0];
				break;
			case 1:
				dominantDim[0] = y;
				dominantDim[1] = extremaY[0];
				dominantDim[2] = extremaY[1];
				dominantDim[3] = extremaY[2];
				otherDims[0] = extremaX[2] - extremaX[0];
				otherDims[1] = extremaZ[2] - extremaZ[0];
				break;
			case 2:
				dominantDim[0] = z;
				dominantDim[1] = extremaZ[0];
				dominantDim[2] = extremaZ[1];
				dominantDim[3] = extremaZ[2];
				otherDims[0] = extremaX[2] - extremaX[0];
				otherDims[1] = extremaY[2] - extremaY[0];
				break;
			default:
				dominantDim[0] = y;
				dominantDim[1] = extremaY[0];
				dominantDim[2] = extremaY[1];
				dominantDim[3] = extremaY[2];
				otherDims[0] = extremaX[2] - extremaX[0];
				otherDims[1] = extremaZ[2] - extremaZ[0];
				break;
		}

		// if curr point is less than mean, and last point is above mean, is a step.
		if(dominantDim[0] < dominantDim[2]
				&& dominantDim[0] < lastDominantValue
				&& lastDominantValue >= dominantDim[2]
				&& dominantDim[3] - dominantDim[1] > MIN_ACCEL_DIFF
//				&& otherDims[0] < MAX_OTHER_ACCEL_DIFF
//				&& otherDims[1] < MAX_OTHER_ACCEL_DIFF
		) {
			long currTime = System.currentTimeMillis();
			double stepTime = (currTime - lastStepTime) / 1000;
			if(stepTime > MIN_STEP_DURATION) {
				counter.step();
				lastStepTime = System.currentTimeMillis();
			}
		}
		
		lastDominantValue = dominantDim[0];
		seenPoints++;
	}

	public void addListener(StepCounterContainer counter) {
		this.counter = counter;
	}
	
	private float sum(float[] arr) {
		float toReturn = 0f;
		for(float f : arr) {
			toReturn += f;
		}
		return toReturn;
	}
	
	private float min(float[] arr) {
		float small = 999f;
		for(float f : arr) {
			if(small > f) {
				small = f;
			}
		}
		return small;
	}
	
	private float max(float[] arr) {
		float big = -999f;
		for(float f : arr) {
			if(big < f) {
				big = f;
			}
		}
		return big;
	}
	
	private float[] getExtrema(float[] vals) {
		float maxVal = max(vals);
		float minVal = min(vals);
		float[] extrema = {minVal, (maxVal - minVal) / 2 + minVal, maxVal};
		return extrema;
	}
	
	private int findDominantExtrema(float[] extremaX, float[] extremaY, float[] extremaZ) {
		float[] test = {extremaX[2] - extremaX[0], extremaY[2] - extremaY[0], extremaZ[2] - extremaZ[0]};
		float max = 0f;
		int dim = -1;
		for(int i=0; i<test.length; i++) {
			if(test[i] > max) {
				max = test[i];
				dim = i;
			}
		}
		return dim;
	}
}
