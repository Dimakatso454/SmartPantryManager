package com.example.smartpantrymanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "SmartPantry.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pantry (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "quantity TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS pantry");
        onCreate(db);
    }

    // Add a food item
    public boolean addItem(String name, String quantity) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("quantity", quantity);

        long result = db.insert("pantry", null, values);

        return result != -1;
    }

    // Get all saved food items
    public Cursor getAllItems() {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT id AS _id, name, quantity FROM pantry",
                null
        );
    }

    // Delete a food item
    public boolean deleteItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        int result = db.delete(
                "pantry",
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        return result > 0;
    }

    // Update an existing food item
    public boolean updateItem(int id, String name, String quantity) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("quantity", quantity);

        int result = db.update(
                "pantry",
                values,
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        return result > 0;
    }
}