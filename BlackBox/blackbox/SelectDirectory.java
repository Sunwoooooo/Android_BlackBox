package com.example.sunwoo.blackbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class SelectDirectory extends AppCompatActivity {

    private Button type_Normal;
    private Button type_Collision;
    private Button type_Record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selectdirectory);

        type_Normal = (Button) findViewById(R.id.button_type_normal);
        type_Collision = (Button) findViewById(R.id.button_type_collision);
        type_Record = (Button) findViewById(R.id.button_type_record);

        type_Normal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectDirectory.this, NormalList.class);
                startActivity(intent);
            }
        });

        type_Collision.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectDirectory.this, CollisionList.class);
                startActivity(intent);
            }
        });

        type_Record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectDirectory.this, RecordList.class);
                startActivity(intent);
            }
        });
    }
}
