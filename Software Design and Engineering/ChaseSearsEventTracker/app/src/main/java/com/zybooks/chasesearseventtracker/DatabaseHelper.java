package com.zybooks.chasesearseventtracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database information
    private static final String DATABASE_NAME = "EventTracker.db";
    private static final int DATABASE_VERSION = 2;

    // User table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    // Event table
    public static final String TABLE_EVENTS = "events";
    public static final String COLUMN_EVENT_ID = "event_id";
    public static final String COLUMN_EVENT_NAME = "event_name";
    public static final String COLUMN_EVENT_DATE = "event_date";
    public static final String COLUMN_EVENT_TIME = "event_time";
    public static final String COLUMN_EVENT_LOCATION = "event_location";

    // NEW: Category for organizing events
    public static final String COLUMN_EVENT_CATEGORY = "event_category";

    // NEW: Owner keeps events separated by user
    public static final String COLUMN_EVENT_OWNER = "event_owner";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // Create users table.
        // Username is now UNIQUE so duplicate accounts cannot be created.
        db.execSQL(
                "CREATE TABLE " + TABLE_USERS + " (" +
                        COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
                        COLUMN_PASSWORD + " TEXT NOT NULL)"
        );

        // Create events table.
        // Category and owner were added as part of the enhancement.
        db.execSQL(
                "CREATE TABLE " + TABLE_EVENTS + " (" +
                        COLUMN_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_EVENT_NAME + " TEXT NOT NULL, " +
                        COLUMN_EVENT_DATE + " TEXT NOT NULL, " +
                        COLUMN_EVENT_TIME + " TEXT NOT NULL, " +
                        COLUMN_EVENT_LOCATION + " TEXT NOT NULL, " +
                        COLUMN_EVENT_CATEGORY +
                        " TEXT NOT NULL DEFAULT 'Other', " +
                        COLUMN_EVENT_OWNER +
                        " TEXT NOT NULL DEFAULT '__guest__')"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Upgrade an existing version 1 database without deleting
        // the user's old event information.
        if (oldVersion < 2) {

            // Add event category to existing events.
            db.execSQL(
                    "ALTER TABLE " + TABLE_EVENTS +
                            " ADD COLUMN " + COLUMN_EVENT_CATEGORY +
                            " TEXT NOT NULL DEFAULT 'Other'"
            );

            // Add event ownership to existing events.
            db.execSQL(
                    "ALTER TABLE " + TABLE_EVENTS +
                            " ADD COLUMN " + COLUMN_EVENT_OWNER +
                            " TEXT NOT NULL DEFAULT '__guest__'"
            );

            // Prevent duplicate usernames.
            db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_username ON " +
                            TABLE_USERS + "(" + COLUMN_USERNAME + ")"
            );
        }
    }

    // Creates a new account.
    public boolean createUser(String username, String password) {

        // Validate account information before inserting it.
        if (username == null ||
                username.trim().isEmpty() ||
                password == null ||
                password.length() < 6) {

            return false;
        }

        ContentValues values = new ContentValues();

        values.put(COLUMN_USERNAME, username.trim());

        // NEW:
        // Do not save the password directly.
        // Save the SHA-256 hash instead.
        values.put(COLUMN_PASSWORD, hashPassword(password));

        long result = getWritableDatabase().insert(
                TABLE_USERS,
                null,
                values
        );

        return result != -1;
    }

    // Checks login credentials.
    public boolean checkUser(String username, String password) {

        SQLiteDatabase db = getWritableDatabase();

        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COLUMN_PASSWORD},
                COLUMN_USERNAME + "=?",
                new String[]{username},
                null,
                null,
                null)) {

            if (!cursor.moveToFirst()) {
                return false;
            }

            String storedPassword = cursor.getString(0);

            // Hash the password entered by the user.
            String hashedPassword = hashPassword(password);

            // Normal login for enhanced accounts.
            if (hashedPassword.equals(storedPassword)) {
                return true;
            }

            /*
             * MIGRATION SUPPORT:
             *
             * Accounts created in the original application stored
             * passwords as plaintext.
             *
             * If an old plaintext password matches, allow the login
             * and immediately replace it with the hashed version.
             */
            if (password.equals(storedPassword)) {

                ContentValues values = new ContentValues();
                values.put(COLUMN_PASSWORD, hashedPassword);

                db.update(
                        TABLE_USERS,
                        values,
                        COLUMN_USERNAME + "=?",
                        new String[]{username}
                );

                return true;
            }

            return false;
        }
    }

    // NEW:
    // Converts a password into a SHA-256 hash.
    private String hashPassword(String password) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] bytes = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 unavailable",
                    e
            );
        }
    }

    // Adds a new event.
    public long insertEvent(
            String name,
            String date,
            String time,
            String location,
            String category,
            String owner) {

        ContentValues values = eventValues(
                name,
                date,
                time,
                location,
                category,
                owner
        );

        return getWritableDatabase().insert(
                TABLE_EVENTS,
                null,
                values
        );
    }

    // NEW:
    // Retrieves only events belonging to the logged-in user.
    // It can also filter events by category.
    public Cursor getEvents(String owner, String category) {

        String selection = COLUMN_EVENT_OWNER + "=?";

        String[] args;

        // Apply category filtering unless "All" is selected.
        if (category != null && !"All".equals(category)) {

            selection +=
                    " AND " + COLUMN_EVENT_CATEGORY + "=?";

            args = new String[]{
                    owner,
                    category
            };

        } else {

            args = new String[]{owner};
        }

        return getReadableDatabase().query(
                TABLE_EVENTS,
                null,
                selection,
                args,
                null,
                null,
                COLUMN_EVENT_DATE + " ASC, " +
                        COLUMN_EVENT_TIME + " ASC"
        );
    }

    // NEW:
    // Event deletion now checks the event owner.
    public int deleteEvent(int eventId, String owner) {

        return getWritableDatabase().delete(
                TABLE_EVENTS,
                COLUMN_EVENT_ID + "=? AND " +
                        COLUMN_EVENT_OWNER + "=?",
                new String[]{
                        String.valueOf(eventId),
                        owner
                }
        );
    }

    // Updates an existing event.
    // Ownership is checked before an event can be updated.
    public int updateEvent(
            int eventId,
            String name,
            String date,
            String time,
            String location,
            String category,
            String owner) {

        return getWritableDatabase().update(
                TABLE_EVENTS,
                eventValues(
                        name,
                        date,
                        time,
                        location,
                        category,
                        owner
                ),
                COLUMN_EVENT_ID + "=? AND " +
                        COLUMN_EVENT_OWNER + "=?",
                new String[]{
                        String.valueOf(eventId),
                        owner
                }
        );
    }

    // Helper method used by both INSERT and UPDATE.
    private ContentValues eventValues(
            String name,
            String date,
            String time,
            String location,
            String category,
            String owner) {

        ContentValues values = new ContentValues();

        values.put(COLUMN_EVENT_NAME, name);
        values.put(COLUMN_EVENT_DATE, date);
        values.put(COLUMN_EVENT_TIME, time);
        values.put(COLUMN_EVENT_LOCATION, location);

        // NEW fields
        values.put(COLUMN_EVENT_CATEGORY, category);
        values.put(COLUMN_EVENT_OWNER, owner);

        return values;
    }
}