package com.zybooks.chasesearseventtracker;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnLogin;
    Button btnCreateAccount;
    Button btnGuest;

    EditText edtUsername;
    EditText edtPassword;

    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnGuest = findViewById(R.id.btnGuest);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);

        dbHelper = new DatabaseHelper(this);

        btnLogin.setOnClickListener(v -> {
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean validUser = dbHelper.checkUser(username, password);

            if (validUser) {
                Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, EventGridActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });

        btnCreateAccount.setOnClickListener(v -> {
            String username = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_USERNAME, username);
            values.put(DatabaseHelper.COLUMN_PASSWORD, password);

            long result = db.insert(DatabaseHelper.TABLE_USERS, null, values);

            if (result == -1) {
                Toast.makeText(MainActivity.this, "Account creation failed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                edtUsername.setText("");
                edtPassword.setText("");
            }
        });

        btnGuest.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EventGridActivity.class);
            startActivity(intent);
        });
    }
}