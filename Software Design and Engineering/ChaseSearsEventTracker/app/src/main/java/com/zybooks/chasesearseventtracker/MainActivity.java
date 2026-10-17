package com.zybooks.chasesearseventtracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin;
    private Button btnCreateAccount;
    private Button btnGuest;

    private EditText edtUsername;
    private EditText edtPassword;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect Java variables to the XML controls.
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnGuest = findViewById(R.id.btnGuest);

        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);

        dbHelper = new DatabaseHelper(this);

        // LOGIN
        btnLogin.setOnClickListener(v -> {

            String username =
                    edtUsername.getText().toString().trim();

            String password =
                    edtPassword.getText().toString();

            // NEW:
            // Validate the fields before querying the database.
            if (username.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please enter username and password",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (dbHelper.checkUser(username, password)) {

                // Pass the username to the event screen.
                // The username becomes the event owner.
                openEvents(username);

            } else {

                Toast.makeText(
                        this,
                        "Invalid username or password",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // CREATE ACCOUNT
        btnCreateAccount.setOnClickListener(v -> {

            String username =
                    edtUsername.getText().toString().trim();

            String password =
                    edtPassword.getText().toString();

            // NEW:
            // Username validation.
            if (username.isEmpty()) {

                edtUsername.setError(
                        "Username is required"
                );

                return;
            }

            // NEW:
            // Require a minimum password length.
            if (password.length() < 6) {

                edtPassword.setError(
                        "Password must be at least 6 characters"
                );

                return;
            }

            if (dbHelper.createUser(username, password)) {

                Toast.makeText(
                        this,
                        "Account created!",
                        Toast.LENGTH_SHORT
                ).show();

                // Clear the form after account creation.
                edtUsername.setText("");
                edtPassword.setText("");

            } else {

                Toast.makeText(
                        this,
                        "Account creation failed. Username may already exist.",
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        // GUEST MODE
        btnGuest.setOnClickListener(v -> {

            // Guest events are stored under a special owner.
            openEvents("__guest__");
        });
    }

    // Opens the event grid and passes the active user.
    private void openEvents(String owner) {

        Intent intent =
                new Intent(this, EventGridActivity.class);

        intent.putExtra("owner", owner);

        startActivity(intent);
    }
}