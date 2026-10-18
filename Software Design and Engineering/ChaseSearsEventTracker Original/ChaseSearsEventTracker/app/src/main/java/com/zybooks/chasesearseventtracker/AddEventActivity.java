package com.zybooks.chasesearseventtracker;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    Button btnSaveEvent;
    Button btnCancelEvent;

    EditText edtEventName;
    EditText edtEventDate;
    EditText edtEventTime;
    EditText edtEventLocation;

    DatabaseHelper dbHelper;

    int eventId = -1;
    boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        btnSaveEvent = findViewById(R.id.btnSaveEvent);
        btnCancelEvent = findViewById(R.id.btnCancelEvent);

        edtEventName = findViewById(R.id.edtEventName);
        edtEventDate = findViewById(R.id.edtEventDate);
        edtEventTime = findViewById(R.id.edtEventTime);
        edtEventLocation = findViewById(R.id.edtEventLocation);

        dbHelper = new DatabaseHelper(this);

        edtEventDate.setOnClickListener(v -> showDatePicker());
        edtEventTime.setOnClickListener(v -> showTimePicker());

        Intent intent = getIntent();
        if (intent.hasExtra("eventId")) {
            isEditMode = true;
            eventId = intent.getIntExtra("eventId", -1);

            edtEventName.setText(intent.getStringExtra("eventName"));
            edtEventDate.setText(intent.getStringExtra("eventDate"));
            edtEventTime.setText(intent.getStringExtra("eventTime"));
            edtEventLocation.setText(intent.getStringExtra("eventLocation"));

            btnSaveEvent.setText("Update Event");
        }

        btnSaveEvent.setOnClickListener(v -> {
            String eventName = edtEventName.getText().toString().trim();
            String eventDate = edtEventDate.getText().toString().trim();
            String eventTime = edtEventTime.getText().toString().trim();
            String eventLocation = edtEventLocation.getText().toString().trim();

            if (eventName.isEmpty() || eventDate.isEmpty() || eventTime.isEmpty() || eventLocation.isEmpty()) {
                Toast.makeText(AddEventActivity.this, "Please fill in all event fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEditMode) {
                int result = dbHelper.updateEvent(eventId, eventName, eventDate, eventTime, eventLocation);

                if (result > 0) {
                    sendSmsIfEnabled(eventName, eventDate, eventTime, eventLocation, true);
                    Toast.makeText(AddEventActivity.this, "Event updated", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(AddEventActivity.this, EventGridActivity.class));
                    finish();
                } else {
                    Toast.makeText(AddEventActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                long result = dbHelper.insertEvent(eventName, eventDate, eventTime, eventLocation);

                if (result == -1) {
                    Toast.makeText(AddEventActivity.this, "Failed to save event", Toast.LENGTH_SHORT).show();
                } else {
                    sendSmsIfEnabled(eventName, eventDate, eventTime, eventLocation, false);
                    Toast.makeText(AddEventActivity.this, "Event saved", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(AddEventActivity.this, EventGridActivity.class));
                    finish();
                }
            }
        });

        btnCancelEvent.setOnClickListener(v -> {
            startActivity(new Intent(AddEventActivity.this, EventGridActivity.class));
            finish();
        });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = (selectedMonth + 1) + "/" + selectedDay + "/" + selectedYear;
                    edtEventDate.setText(formattedDate);
                },
                year,
                month,
                day
        );

        datePickerDialog.show();
    }

    private void showTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    String amPm;
                    int displayHour;

                    if (selectedHour == 0) {
                        displayHour = 12;
                        amPm = "AM";
                    } else if (selectedHour < 12) {
                        displayHour = selectedHour;
                        amPm = "AM";
                    } else if (selectedHour == 12) {
                        displayHour = 12;
                        amPm = "PM";
                    } else {
                        displayHour = selectedHour - 12;
                        amPm = "PM";
                    }

                    String formattedTime = String.format(Locale.getDefault(), "%d:%02d %s",
                            displayHour, selectedMinute, amPm);

                    edtEventTime.setText(formattedTime);
                },
                hour,
                minute,
                false
        );

        timePickerDialog.show();
    }

    private void sendSmsIfEnabled(String eventName, String eventDate, String eventTime, String eventLocation, boolean isUpdate) {
        SharedPreferences sharedPreferences = getSharedPreferences("EventTrackerPrefs", MODE_PRIVATE);
        boolean smsEnabled = sharedPreferences.getBoolean("smsEnabled", false);
        String phoneNumber = sharedPreferences.getString("phoneNumber", "");

        if (!smsEnabled) {
            return;
        }

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "SMS is enabled but no phone number is saved", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted. Event was still saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        String message;
        if (isUpdate) {
            message = "Event updated: " + eventName + " on " + eventDate + " at " + eventTime + " location: " + eventLocation;
        } else {
            message = "New event added: " + eventName + " on " + eventDate + " at " + eventTime + " location: " + eventLocation;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "SMS alert sent", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "SMS failed to send", Toast.LENGTH_SHORT).show();
        }
    }
}