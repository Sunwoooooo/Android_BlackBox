package com.example.sunwoo.blackbox;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int PERMISSION_REQUEST_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_REQUEST_LOCATION = 2;

    private Button call_CameraPreview;
    private Button call_VideoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        int permissionCamera =
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.CAMERA);

        if(permissionCamera == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        }

        int permissionExternalStorage =
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permissionExternalStorage == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_EXTERNAL_STORAGE);
        }

        int permissionLocation =
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION);

        if(permissionLocation == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        }

        call_CameraPreview = (Button) findViewById(R.id.button_playcamera);
        call_CameraPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraPreview.class);
                startActivity(intent);
            }
        });

        call_VideoList = (Button) findViewById(R.id.button_selectdirectory);
        call_VideoList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SelectDirectory.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                for (int i = 0; i < permissions.length; i++) {

                    String permission = permissions[i];
                    int grantResult = grantResults[i];

                    if (permission.equals(Manifest.permission.CAMERA)) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "storage permission authorized", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "storage permission denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                break;

            case PERMISSION_REQUEST_EXTERNAL_STORAGE:
                for (int i = 0; i < permissions.length; i++) {

                    String permission = permissions[i];
                    int grantResult = grantResults[i];

                    if (permission.equals(Manifest.permission.CAMERA)) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "storage permission authorized", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "storage permission denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                break;

            case PERMISSION_REQUEST_LOCATION:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];

                    if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(MainActivity.this, "location permission authorized", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "location permission denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                break;
        }
    }
}
