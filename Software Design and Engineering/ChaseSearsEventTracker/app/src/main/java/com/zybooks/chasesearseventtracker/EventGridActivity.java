package com.zybooks.chasesearseventtracker;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class EventGridActivity extends AppCompatActivity {

    private Button btnAddEvent;
    private Button btnSmsSettings;

    private LinearLayout eventContainer;

    // NEW:
    // Spinner allows events to be filtered by category.
    private Spinner spnFilterCategory;

    private DatabaseHelper dbHelper;

    // NEW:
    // Stores the currently active user.
    private String owner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_grid);

        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnSmsSettings = findViewById(R.id.btnSmsSettings);

        eventContainer =
                findViewById(R.id.eventContainer);

        spnFilterCategory =
                findViewById(R.id.spnFilterCategory);

        dbHelper = new DatabaseHelper(this);

        // Retrieve the user that was passed from MainActivity.
        owner = getIntent().getStringExtra("owner");

        // Default to guest mode if no owner was provided.
        if (owner == null) {
            owner = "__guest__";
        }

        // NEW:
        // Available event filters.
        String[] filters = {
                "All",
                "Work",
                "Personal",
                "School",
                "Other"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        filters
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spnFilterCategory.setAdapter(adapter);

        // Reload events whenever the user selects
        // a different category.
        spnFilterCategory.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            android.view.View view,
                            int position,
                            long id) {

                        loadEvents();
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {

                        // No action needed.
                    }
                }
        );

        // ADD EVENT
        btnAddEvent.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            this,
                            AddEventActivity.class
                    );

            // NEW:
            // Tell AddEventActivity who owns the event.
            intent.putExtra("owner", owner);

            startActivity(intent);
        });

        // SMS SETTINGS
        btnSmsSettings.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            this,
                            SmsSettingsActivity.class
                    )
            );
        });
    }

    @Override
    protected void onResume() {

        super.onResume();

        // Reload the list when returning from
        // adding or editing an event.
        loadEvents();
    }

    private void loadEvents() {

        // Clear old views before rebuilding the event list.
        eventContainer.removeAllViews();

        String category;

        if (spnFilterCategory.getSelectedItem() == null) {

            category = "All";

        } else {

            category =
                    spnFilterCategory
                            .getSelectedItem()
                            .toString();
        }

        /*
         * NEW:
         * Database query now uses both the event owner
         * and selected category.
         */
        try (Cursor cursor =
                     dbHelper.getEvents(owner, category)) {

            // Show a helpful message when no events exist.
            if (cursor.getCount() == 0) {

                TextView emptyMessage =
                        new TextView(this);

                emptyMessage.setText(
                        "No events found for this category."
                );

                eventContainer.addView(emptyMessage);

                return;
            }

            while (cursor.moveToNext()) {

                int id =
                        cursor.getInt(
                                cursor.getColumnIndexOrThrow(
                                        DatabaseHelper.COLUMN_EVENT_ID
                                )
                        );

                String name =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        DatabaseHelper.COLUMN_EVENT_NAME
                                )
                        );

                String date =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        DatabaseHelper.COLUMN_EVENT_DATE
                                )
                        );

                String time =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        DatabaseHelper.COLUMN_EVENT_TIME
                                )
                        );

                String location =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        DatabaseHelper.COLUMN_EVENT_LOCATION
                                )
                        );

                // NEW:
                // Retrieve the category.
                String eventCategory =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        DatabaseHelper.COLUMN_EVENT_CATEGORY
                                )
                        );

                // Container for one event.
                LinearLayout row =
                        new LinearLayout(this);

                row.setOrientation(
                        LinearLayout.VERTICAL
                );

                row.setPadding(
                        8,
                        12,
                        8,
                        12
                );

                // Display event information.
                TextView details =
                        new TextView(this);

                details.setText(
                        name +
                                "\n" +
                                date +
                                " at " +
                                time +
                                "\n" +
                                location +
                                " • " +
                                eventCategory
                );

                details.setTextSize(16);

                // Container for Edit and Delete buttons.
                LinearLayout actions =
                        new LinearLayout(this);

                actions.setOrientation(
                        LinearLayout.HORIZONTAL
                );

                // EDIT BUTTON
                Button edit =
                        new Button(this);

                edit.setText("Edit");

                edit.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1
                        )
                );

                // DELETE BUTTON
                Button delete =
                        new Button(this);

                delete.setText("Delete");

                delete.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1
                        )
                );

                // Edit the selected event.
                edit.setOnClickListener(v -> {

                    Intent intent =
                            new Intent(
                                    this,
                                    AddEventActivity.class
                            );

                    // Pass the owner and event information.
                    intent.putExtra("owner", owner);
                    intent.putExtra("eventId", id);
                    intent.putExtra("eventName", name);
                    intent.putExtra("eventDate", date);
                    intent.putExtra("eventTime", time);
                    intent.putExtra(
                            "eventLocation",
                            location
                    );

                    // NEW:
                    // Pass the existing category.
                    intent.putExtra(
                            "eventCategory",
                            eventCategory
                    );

                    startActivity(intent);
                });

                /*
                 * NEW:
                 * Delete confirmation.
                 *
                 * The original behavior could immediately
                 * remove an event. The enhanced version
                 * asks the user to confirm first.
                 */
                delete.setOnClickListener(v ->

                        new AlertDialog.Builder(this)

                                .setTitle(
                                        "Delete event?"
                                )

                                .setMessage(
                                        "Delete \"" +
                                                name +
                                                "\"? This cannot be undone."
                                )

                                .setNegativeButton(
                                        "Cancel",
                                        null
                                )

                                .setPositiveButton(
                                        "Delete",
                                        (dialog, which) -> {

                                            // Ownership is checked
                                            // during deletion.
                                            int result =
                                                    dbHelper.deleteEvent(
                                                            id,
                                                            owner
                                                    );

                                            Toast.makeText(
                                                    this,
                                                    result > 0
                                                            ? "Event deleted"
                                                            : "Delete failed",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            if (result > 0) {
                                                loadEvents();
                                            }
                                        }
                                )

                                .show()
                );

                actions.addView(edit);
                actions.addView(delete);

                row.addView(details);
                row.addView(actions);

                eventContainer.addView(row);
            }
        }
    }
}