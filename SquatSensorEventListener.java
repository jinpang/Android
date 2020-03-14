import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import static android.content.Context.SENSOR_SERVICE;

public class SquatSensorEventListener implements SensorEventListener {
    private final String TAG = SquatSensorEventListener.class.getSimpleName();
    private static int mCount;
    private static long mTotalTime;
    private static int mAdjustTime;
    private static boolean mayBeSquat;
    private static long mLastTime;
    private static SensorManager mSensorManager;
    public Runnable mRunnable;
    private boolean hasSensor = false;

    public SquatSensorEventListener(Context context, Runnable runnable) {
        mayBeSquat = true;
        mCount = 0;
        mTotalTime = 0;
        mRunnable = runnable;
        mLastTime = 0;
        mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        if (mSensorManager == null) {
            Log.v(TAG, "Sensors not supported");
        }
    }

    public void reset() {
        mayBeSquat = true;
        mCount = 0;
        mTotalTime = 0;
        mLastTime = 0;
    }

    public int getSquatCount() {
        return mCount;
    }

    public void setSquatCount(int count) {
        mCount = count;
    }

    public void registerListener() {
        hasSensor = mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
    }

    public void unregisterListener() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            hasSensor = false;
        }
    }

    public boolean hasSensor() {
        return hasSensor;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long currentTimeMillis = System.currentTimeMillis();
        long diffTime = currentTimeMillis - mLastTime;
        if (diffTime > 100) {
            diffTime = 0;
        }
        mLastTime = currentTimeMillis;
        double sqrt = Math.sqrt((sensorEvent.values[0] * sensorEvent.values[0]) + (sensorEvent.values[1] * sensorEvent.values[1]));
        sqrt = Math.sqrt((sqrt * sqrt) + (sensorEvent.values[2] * sensorEvent.values[2])) - 9.706650161743164d;
        if (mTotalTime == 0) {
            if (sqrt < -0.6d) {
                mTotalTime = diffTime;
                mAdjustTime = 0;
            }
        } else if (sqrt < -0.2d) {
            mTotalTime += diffTime;
            mAdjustTime = 0;
        } else if (mTotalTime > 0) {
            mTotalTime -= diffTime;
        }
        if (sqrt > 0.0d) {
            if (mTotalTime > 225) {
                mAdjustTime = (int) (mAdjustTime + diffTime);
                mTotalTime += diffTime;
                if (sqrt > 3.0d) {
                    mAdjustTime = (int) (mAdjustTime + diffTime);
                }
                if (mayBeSquat && mAdjustTime > 450) {
                    mayBeSquat = false;
                    mCount += 1;
                    mTotalTime = 0;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("count: ");
                    stringBuilder.append(mCount);
                    Log.v(TAG, stringBuilder.toString());
                    mRunnable.run();
                    return;
                }
                return;
            }
            mTotalTime = 0;
            mAdjustTime = 0;
            mayBeSquat = true;
        } else if (mAdjustTime > 450) {
            diffTime *= 2;
            mTotalTime -= diffTime;
            mAdjustTime = (int) (mAdjustTime - diffTime);
            mayBeSquat = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
