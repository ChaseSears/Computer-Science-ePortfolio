package com.zybooks.chasesearseventtracker;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EventGridActivity extends AppCompatActivity {

    Button btnAddEvent;
    Button btnSmsSettings;

    LinearLayout eventContainer;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_grid);

        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnSmsSettings = findViewById(R.id.btnSmsSettings);
        eventContainer = findViewById(R.id.eventContainer);

        dbHelper = new DatabaseHelper(this);

        loadEvents();

        btnAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(EventGridActivity.this, AddEventActivity.class);
            startActivity(intent);
        });

        btnSmsSettings.setOnClickListener(v -> {
            Intent intent = new Intent(EventGridActivity.this, SmsSettingsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    private void loadEvents() {
        eventContainer.removeAllViews();

        Cursor cursor = dbHelper.getAllEvents();

        if (cursor.getCount() == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No events added yet.");
            eventContainer.addView(emptyText);
        } else {
            while (cursor.moveToNext()) {
                int eventId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_NAME));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_DATE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_TIME));
                String location = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_LOCATION));

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(8, 8, 8, 8);

                TextView txtName = new TextView(this);
                txtName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
                txtName.setText(name);

                TextView txtDate = new TextView(this);
                txtDate.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
                txtDate.setText(date);

                TextView txtTime = new TextView(this);
                txtTime.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
                txtTime.setText(time);

                TextView txtLocation = new TextView(this);
                txtLocation.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
                txtLocation.setText(location);

                Button btnEdit = new Button(this);
                btnEdit.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                btnEdit.setText("Edit");

                btnEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(EventGridActivity.this, AddEventActivity.class);
                    intent.putExtra("eventId", eventId);
                    intent.putExtra("eventName", name);
                    intent.putExtra("eventDate", date);
                    intent.putExtra("eventTime", time);
                    intent.putExtra("eventLocation", location);
                    startActivity(intent);
                });

                Button btnDelete = new Button(this);
                btnDelete.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                btnDelete.setText("X");

                btnDelete.setOnClickListener(v -> {
                    int deleted = dbHelper.deleteEvent(eventId);
                    if (deleted > 0) {
                        Toast.makeText(EventGridActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                        loadEvents();
                    } else {
                        Toast.makeText(EventGridActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                });

                row.addView(txtName);
                row.addView(txtDate);
                row.addView(txtTime);
                row.addView(txtLocation);
                row.addView(btnEdit);
                row.addView(btnDelete);

                eventContainer.addView(row);
            }
        }

        cursor.close();
    }
}