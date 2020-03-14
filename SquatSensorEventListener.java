import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import static android.content.Context.SENSOR_SERVICE;

public class SquatSensorEventListener implements SensorEventListener {
    private final String TAG = "SquatSensorEventListener";
    private static int squatCount;
    private static long squatTime;
    private static int squatOkTime;
    private static boolean isSquat;
    private static long lastTime;
    private static SensorManager sensorManager;
    public Runnable runnable;
    private boolean hasSensor = false;

    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public SquatSensorEventListener(Context context, Runnable runnable) {
        isSquat = true;
        squatCount = 0;
        squatTime = 0;
        this.runnable = runnable;
        lastTime = 0;
        sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            Log.v("sensor..", "Sensors not supported");
        }
    }

    public void reset() {
        isSquat = true;
        squatCount = 0;
        squatTime = 0;
        lastTime = 0;
    }

    public int getSquatCount() {
        return squatCount;
    }

    public void setSquatCount(int count) {
        squatCount = count;
    }

    public void registerListener() {
        this.hasSensor = sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
    }

    public void unregisterListener() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            this.hasSensor = false;
        }
    }

    public boolean hasSensor() {
        return this.hasSensor;
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        long currentTimeMillis = System.currentTimeMillis();
        long diffTime = currentTimeMillis - lastTime;
        if (diffTime > 100) {
            diffTime = 0;
        }
        lastTime = currentTimeMillis;
        double sqrt = Math.sqrt((double) ((sensorEvent.values[0] * sensorEvent.values[0]) + (sensorEvent.values[1] * sensorEvent.values[1])));
        sqrt = Math.sqrt((sqrt * sqrt) + ((double) (sensorEvent.values[2] * sensorEvent.values[2]))) - 9.706650161743164d;
        if (squatTime == 0) {
            if (sqrt < -0.6d) {
                squatTime = diffTime;
                squatOkTime = 0;
            }
        } else if (sqrt < -0.2d) {
            squatTime += diffTime;
            squatOkTime = 0;
        } else if (squatTime > 0) {
            squatTime -= diffTime;
        }
        if (sqrt > 0.0d) {
            if (squatTime > 225) {
                squatOkTime = (int) (((long) squatOkTime) + diffTime);
                squatTime += diffTime;
                if (sqrt > 3.0d) {
                    squatOkTime = (int) (((long) squatOkTime) + diffTime);
                }
                if (isSquat && ((long) squatOkTime) > 450) {
                    isSquat = false;
                    squatCount += 1;
                    squatTime = 0;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("count: ");
                    stringBuilder.append(squatCount);
                    Log.v("HSquatSensor", stringBuilder.toString());
                    this.runnable.run();
                    return;
                }
                return;
            }
            squatTime = 0;
            squatOkTime = 0;
            isSquat = true;
        } else if (squatOkTime > 450) {
            diffTime *= 2;
            squatTime -= diffTime;
            squatOkTime = (int) (((long) squatOkTime) - diffTime);
            isSquat = true;
        }
    }
}
