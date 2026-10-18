package com.zybooks.chasesearseventtracker;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SmsSettingsActivity extends AppCompatActivity {

    EditText edtPhoneNumber;
    CheckBox chkEnableSms;
    Button btnSaveSmsSettings;
    Button btnRequestPermission;

    SharedPreferences sharedPreferences;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(SmsSettingsActivity.this, "SMS permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SmsSettingsActivity.this, "SMS permission denied. App will still work without SMS.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_settings);

        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        chkEnableSms = findViewById(R.id.chkEnableSms);
        btnSaveSmsSettings = findViewById(R.id.btnSaveSmsSettings);
        btnRequestPermission = findViewById(R.id.btnRequestSmsPermission);

        sharedPreferences = getSharedPreferences("EventTrackerPrefs", MODE_PRIVATE);

        loadSmsSettings();

        btnRequestPermission.setOnClickListener(v -> checkAndRequestSmsPermission());

        btnSaveSmsSettings.setOnClickListener(v -> {
            String phoneNumber = edtPhoneNumber.getText().toString().trim();
            boolean smsEnabled = chkEnableSms.isChecked();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("phoneNumber", phoneNumber);
            editor.putBoolean("smsEnabled", smsEnabled);
            editor.apply();

            Toast.makeText(SmsSettingsActivity.this, "SMS settings saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void loadSmsSettings() {
        String savedPhoneNumber = sharedPreferences.getString("phoneNumber", "");
        boolean smsEnabled = sharedPreferences.getBoolean("smsEnabled", false);

        edtPhoneNumber.setText(savedPhoneNumber);
        chkEnableSms.setChecked(smsEnabled);
    }

    private void checkAndRequestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission already granted", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }
    }
}