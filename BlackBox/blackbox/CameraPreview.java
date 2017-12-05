package com.example.sunwoo.blackbox;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CameraPreview extends AppCompatActivity implements
        SurfaceHolder.Callback,
        SensorEventListener{

    private TimerTask recordTimer;
    private TextView recordingTime;
    private final Handler handler = new Handler();
    private int count = -1;

    private int count_normal = -1;
    private int count_collision = -1;
    private int count_record = -1;

    private static final String OUTPUT_FILE_PATH = "/storage/emulated/0/DCIM/Camera";
    private static final String PATH_NORMAL = "/Normal/";
    private static final String PATH_COLLISION = "/Collision/";
    private static final String PATH_RECORD = "/Record/";

    private String path;
    private String path_log;

    private Camera camera = null;
    private SurfaceView sv;
    private SurfaceHolder surfaceHolder;
    private LayoutInflater controlInflater = null;
    private Button record;
    private Button record_normal;
    private MediaRecorder mediaRecorder;

    private boolean previewing = false;

    private boolean state_normal = false;
    private boolean state_collision = false;
    private boolean state_record = false;

    private boolean collision_now = false;

    private long lastTime;
    private float speed;
    private float lastX;
    private float lastY;
    private float lastZ;
    private float x, y, z;

    private static final int SHAKE_THRESHOLD = 1600;
    private static final int DATA_X = SensorManager.DATA_X;
    private static final int DATA_Y = SensorManager.DATA_Y;
    private static final int DATA_Z = SensorManager.DATA_Z;

    private SensorManager sensorManager;
    private Sensor sensor;

    private LocationManager lm;

    private double record_longitude;
    private double record_latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFormat(PixelFormat.UNKNOWN);

        camera = Camera.open();
        sv = (SurfaceView) findViewById(R.id.surfaceview_camerapreview);
        surfaceHolder = sv.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        controlInflater = LayoutInflater.from(getApplicationContext());
        View viewControl = controlInflater.inflate(R.layout.camera_control, null);
        ViewGroup.LayoutParams layoutParamsControl =
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        this.addContentView(viewControl, layoutParamsControl);

        record = (Button) findViewById(R.id.button_recording);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state_collision == false && state_normal == false && state_record == false) {
                    buttonRecording();
                }
                else if(state_collision == false && state_normal == true && state_record == false) {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    camera.lock();
                    state_normal = false;
                    record_normal.setText("Normal Record");
                    timerStop();

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            buttonRecording();
                        }
                    }, 1000);
                }
            }
        });

        record_normal = (Button) findViewById(R.id.button_recording_normal);
        record_normal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(state_record == false && state_collision == false && state_normal == false) {
                    normalRecording();
                }
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e("sss", "created");
        try {
            if(camera == null) {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e("sss", "changed");

        refreshCamera(camera);
    }

    public void refreshCamera(Camera cam) {
        if(surfaceHolder.getSurface() == null) {
            return;
        }

        if(previewing) {
            cam.stopPreview();
            previewing = false;
        }

        setCamera(cam);

        if(camera != null) {
            try {
                cam.setPreviewDisplay(surfaceHolder);
                cam.startPreview();
                previewing = true;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setCamera(Camera cam) {
        camera = cam;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("sss", "destroyed");

        if(state_collision == true || state_record == true || state_normal == true) {
            mediaRecorder.stop();
            mediaRecorder.release();
            Toast.makeText(CameraPreview.this, "stop record, file path : " + path, Toast.LENGTH_SHORT).show();
            timerStop();
        }

        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }

    public void timerStart() {
        Log.e("sss", "timer start");
        recordingTime = (TextView)findViewById(R.id.text_recording_time);

        recordTimer = new TimerTask() {
            @Override
            public void run() {
                Update();
                count++;
                count_collision = count;
                count_normal = count;
                count_record = count;
            }
        };

        Timer timer = new Timer();
        timer.schedule(recordTimer, 0, 1000);
    }

    protected void Update() {
        Runnable updater = new Runnable() {
            @Override
            public void run() {
                recordingTime.setText("recording time : " + count + " sec");

                if(state_record == false && state_collision == false && state_normal == true && count_normal >= 60) {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    camera.lock();
                    state_normal = false;
                    Toast.makeText(CameraPreview.this, "stop record, file path : " + path, Toast.LENGTH_SHORT).show();
                    record_normal.setText("Normal Record");
                    timerStop();

                    if(state_record == false && state_collision == false && state_normal == false) {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                normalRecording();
                            }
                        }, 1000);
                    }
                }

                if(state_record == true && state_collision == false && state_normal == false && count_record >= 120) {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    camera.lock();
                    state_record = false;
                    Toast.makeText(CameraPreview.this, "stop record, file path : " + path, Toast.LENGTH_SHORT).show();
                    record.setText("Record");
                    timerStop();

                    if(state_record == false && state_collision == false && state_normal == false) {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                normalRecording();
                            }
                        }, 1000);
                    }
                }

                if(state_record == false && state_collision == true && state_normal == false && count_collision >= 120) {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    camera.lock();
                    state_collision = false;
                    Toast.makeText(CameraPreview.this, "stop record, file path : " + path, Toast.LENGTH_SHORT).show();
                    timerStop();

                    if(state_record == false && state_collision == false && state_normal == false) {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                normalRecording();
                            }
                        }, 1000);
                    }
                }

                if(count % 10 == 0) {
                    appendLog();
                }
            }
        };

        handler.post(updater);
    }

    public void timerStop() {
        Log.e("sss", "timer stop");
        recordTimer.cancel();
        count = count_collision = count_record = count_normal = -1;
        recordingTime.setText("ready");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long gab = currentTime - lastTime;

            if(gab > 100) {
                lastTime = currentTime;
                x = event.values[SensorManager.DATA_X];
                y = event.values[SensorManager.DATA_Y];
                z = event.values[SensorManager.DATA_Z];

                speed = Math.abs(x + y + z - lastX - lastY - lastZ) / gab * 10000;

                if(speed > SHAKE_THRESHOLD && state_collision == false && state_record == false && state_normal == false && collision_now == false) {
                    Log.e("sss", "shake");
                    collisionRecording();
                }
                else if(speed > SHAKE_THRESHOLD && state_collision == false && state_normal == true && state_record == false) {
                    Log.e("sss", "shake!!");
                    collision_now = true;
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    camera.lock();
                    state_normal = false;
                    Toast.makeText(CameraPreview.this, "stop record, file path : " + path, Toast.LENGTH_SHORT).show();
                    record_normal.setText("Normal Record");
                    timerStop();

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            collisionRecording();
                            collision_now = false;
                        }
                    }, 1000);
                }
                else if(speed > SHAKE_THRESHOLD && state_collision == false && state_normal == false && state_record == true) {
                    Log.e("sss", "shake!!");
                    collision_now = true;
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    camera.lock();
                    state_record = false;
                    Toast.makeText(CameraPreview.this, "stop record, file path : " + path, Toast.LENGTH_SHORT).show();
                    record.setText("Record");
                    timerStop();

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            collisionRecording();
                            collision_now = false;
                        }
                    }, 1000);
                }

                lastX = event.values[DATA_X];
                lastY = event.values[DATA_Y];
                lastZ = event.values[DATA_Z];
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onStart() {
        super.onStart();

        if(sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    public void normalRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            camera.unlock();
            mediaRecorder.setCamera(camera);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

            path = OUTPUT_FILE_PATH + PATH_NORMAL;
            File file = new File(path);
            file.mkdirs();

            path_log = path + "log_" + getDateString() + ".txt";

            makePath(path);
            mediaRecorder.setOutputFile(path);
            Log.e("path", path);

            mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
            mediaRecorder.prepare();
            mediaRecorder.start();
            state_normal = true;
            Toast.makeText(CameraPreview.this, "start normal record for 1 minutes", Toast.LENGTH_SHORT).show();
            record_normal.setText("Stop");
            timerStart();
        }
        catch (final Exception e) {
            e.printStackTrace();
            mediaRecorder.release();
            return;
        }

        MediaScannerConnection.scanFile(getApplicationContext(),
                new String[]{path},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {

            }
        });
    }

    public void buttonRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            camera.unlock();
            mediaRecorder.setCamera(camera);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
            timerStart();

            path = OUTPUT_FILE_PATH + PATH_RECORD;
            File file = new File(path);
            file.mkdirs();

            path_log = path + "log_" + getDateString() + ".txt";

            makePath(path);
            mediaRecorder.setOutputFile(path);
            Log.e("path", path);
            mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
            mediaRecorder.prepare();
            mediaRecorder.start();
            state_record = true;
            Toast.makeText(CameraPreview.this, "start button record for 2 minutes", Toast.LENGTH_SHORT).show();
            record.setText("Stop");
        } catch (final Exception e) {
            e.printStackTrace();
            mediaRecorder.release();
            return;
        }

        MediaScannerConnection.scanFile(getApplicationContext(),
                new String[]{path},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
    }

    public void collisionRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            camera.unlock();
            mediaRecorder.setCamera(camera);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

            path = OUTPUT_FILE_PATH + PATH_COLLISION;
            File file = new File(path);
            file.mkdirs();

            path_log = path + "log_" + getDateString() + ".txt";

            makePath(path);
            mediaRecorder.setOutputFile(path);
            Log.e("path", path);
            mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
            mediaRecorder.prepare();
            mediaRecorder.start();
            state_collision = true;
            Toast.makeText(CameraPreview.this, "start collision record for 2 minutes", Toast.LENGTH_SHORT).show();
            timerStart();
        }
        catch (final Exception e) {
            e.printStackTrace();
            mediaRecorder.release();
            return;
        }

        MediaScannerConnection.scanFile(getApplicationContext(),
                new String[]{path},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
    }

    public String getDateString() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
        String str_date = df.format(new Date());

        return str_date;
    }

    public void makePath(String path) {
        this.path = path + getDateString() + ".mp4";
    }

    public void appendLog() {
        File logFile = new File(path_log);

        if(!logFile.exists()) {
            try {
                logFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            registerLocationUpdates();
            buf.write(getDateString() + " longitude : " + record_longitude + " latitude : " + record_latitude + "\r\n");
            buf.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    1000, 1, mLocationListener);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000, 1, mLocationListener);
        }

    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if(location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();

                record_longitude = longitude;
                record_latitude = latitude;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
}
