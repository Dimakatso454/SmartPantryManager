
package com.example.smartpantrymanager;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    EditText editItemName, editQuantity, editExpiryDate, editSearch;
    Button btnAddItem;
    ListView listPantry;
    Spinner spinnerSort;
    TextView textPantrySummary, textEmptyPantry;

    ArrayList<String> pantryItems = new ArrayList<>();
    ArrayList<Integer> itemIds = new ArrayList<>();
    ArrayList<String> itemNames = new ArrayList<>();
    ArrayList<String> itemQuantities = new ArrayList<>();
    ArrayList<String> itemExpiryDates = new ArrayList<>();

    ArrayList<String> displayedItems = new ArrayList<>();
    ArrayList<Integer> displayedIds = new ArrayList<>();

    ArrayAdapter<String> adapter;
    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    int padding = (int) (
                            20 * getResources()
                                    .getDisplayMetrics().density
                    );

                    v.setPadding(
                            bars.left + padding,
                            bars.top + padding,
                            bars.right + padding,
                            bars.bottom + padding
                    );

                    return insets;
                }
        );

        editItemName = findViewById(R.id.editItemName);
        editQuantity = findViewById(R.id.editQuantity);
        editExpiryDate = findViewById(R.id.editExpiryDate);
        editSearch = findViewById(R.id.editSearch);
        btnAddItem = findViewById(R.id.btnAddItem);
        listPantry = findViewById(R.id.listPantry);
        spinnerSort = findViewById(R.id.spinnerSort);
        textPantrySummary = findViewById(R.id.textPantrySummary);
        textEmptyPantry = findViewById(R.id.textEmptyPantry);

        databaseHelper = new DatabaseHelper(this);

        // Coloured expiry warnings
        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                displayedItems
        ) {
            @Override
            public View getView(
                    int position,
                    View convertView,
                    ViewGroup parent
            ) {
                View view = super.getView(
                        position, convertView, parent
                );

                TextView textView = (TextView) view;
                String item = displayedItems.get(position);

                if (item.endsWith("EXPIRED")) {
                    textView.setTextColor(Color.RED);

                } else if (item.endsWith("EXPIRING SOON")) {
                    textView.setTextColor(Color.rgb(230, 120, 0));

                } else if (item.endsWith("FRESH")) {
                    textView.setTextColor(Color.rgb(0, 130, 0));

                } else {
                    textView.setTextColor(Color.DKGRAY);
                }

                return view;
            }
        };

        listPantry.setAdapter(adapter);

        // Sorting dropdown
        String[] sortOptions = {
                "Sort by Name (A-Z)",
                "Sort by Name (Z-A)",
                "Expiry Date (Earliest First)",
                "Expiry Date (Latest First)"
        };

        ArrayAdapter<String> sortAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        sortOptions
                );

        sortAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerSort.setAdapter(sortAdapter);

        spinnerSort.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        filterItems(editSearch.getText().toString());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        editExpiryDate.setOnClickListener(v ->
                showDatePicker(editExpiryDate)
        );

        loadItems();

        // Add food
        btnAddItem.setOnClickListener(v -> {
            String name = editItemName.getText()
                    .toString().trim();
            String quantity = editQuantity.getText()
                    .toString().trim();
            String expiryDate = editExpiryDate.getText()
                    .toString().trim();

            if (name.isEmpty() ||
                    quantity.isEmpty() ||
                    expiryDate.isEmpty()) {
                Toast.makeText(
                        this,
                        "Please fill in all fields",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            boolean saved = databaseHelper.addItem(
                    name, quantity, expiryDate
            );

            if (saved) {
                loadItems();

                editItemName.setText("");
                editQuantity.setText("");
                editExpiryDate.setText("");

                Toast.makeText(
                        this,
                        "Item saved successfully",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        this,
                        "Failed to save item",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Search
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count
            ) {
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Tap to edit
        listPantry.setOnItemClickListener(
                (parent, view, position, id) -> {
                    int itemId = displayedIds.get(position);
                    int originalPosition = itemIds.indexOf(itemId);

                    if (originalPosition != -1) {
                        showEditDialog(originalPosition);
                    }
                }
        );

        // Long press to delete
        listPantry.setOnItemLongClickListener(
                (parent, view, position, id) -> {
                    int itemId = displayedIds.get(position);
                    String itemName = displayedItems.get(position);

                    new AlertDialog.Builder(this)
                            .setTitle("Delete Item")
                            .setMessage("Delete " + itemName + "?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                if (databaseHelper.deleteItem(itemId)) {
                                    loadItems();
                                    Toast.makeText(
                                            this,
                                            "Item deleted",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                } else {
                                    Toast.makeText(
                                            this,
                                            "Failed to delete item",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();

                    return true;
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (databaseHelper != null && adapter != null) {
            loadItems();
        }
    }

    // Calendar
    private void showDatePicker(EditText dateField) {
        Calendar calendar = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    String date = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year, month + 1, day
                    );
                    dateField.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // Expiry status
    private String getExpiryStatus(String expiryDate) {
        if (expiryDate == null || expiryDate.trim().isEmpty()) {
            return "NO EXPIRY DATE";
        }

        try {
            SimpleDateFormat format =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            format.setLenient(false);

            Date expiry = format.parse(expiryDate);

            if (expiry == null) {
                return "INVALID DATE";
            }

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar expiryCalendar = Calendar.getInstance();
            expiryCalendar.setTime(expiry);
            expiryCalendar.set(Calendar.HOUR_OF_DAY, 0);
            expiryCalendar.set(Calendar.MINUTE, 0);
            expiryCalendar.set(Calendar.SECOND, 0);
            expiryCalendar.set(Calendar.MILLISECOND, 0);

            if (expiryCalendar.before(today)) {
                return "EXPIRED";
            }

            Calendar warningDate = (Calendar) today.clone();
            warningDate.add(Calendar.DAY_OF_YEAR, 3);

            if (!expiryCalendar.after(warningDate)) {
                return "EXPIRING SOON";
            }

            return "FRESH";

        } catch (ParseException e) {
            return "INVALID DATE";
        }
    }

    // Search and sort pantry items
    private void filterItems(String searchText) {
        displayedItems.clear();
        displayedIds.clear();

        String query = searchText.toLowerCase(Locale.ROOT).trim();

        ArrayList<Integer> matchingPositions = new ArrayList<>();

        for (int i = 0; i < pantryItems.size(); i++) {
            if (itemNames.get(i)
                    .toLowerCase(Locale.ROOT)
                    .contains(query)) {
                matchingPositions.add(i);
            }
        }

        int sortType = spinnerSort.getSelectedItemPosition();

        Collections.sort(matchingPositions, (a, b) -> {
            switch (sortType) {
                case 1:
                    return itemNames.get(b)
                            .compareToIgnoreCase(itemNames.get(a));

                case 2:
                    return compareExpiryDates(
                            itemExpiryDates.get(a),
                            itemExpiryDates.get(b)
                    );

                case 3:
                    return compareExpiryDates(
                            itemExpiryDates.get(b),
                            itemExpiryDates.get(a)
                    );

                default:
                    return itemNames.get(a)
                            .compareToIgnoreCase(itemNames.get(b));
            }
        });

        for (int position : matchingPositions) {
            displayedItems.add(pantryItems.get(position));
            displayedIds.add(itemIds.get(position));
        }

        // Empty pantry and search messages
        if (pantryItems.isEmpty()) {
            textEmptyPantry.setText(
                    "Your pantry is empty.\n" +
                            "Add your first food item to get started!"
            );
            textEmptyPantry.setVisibility(View.VISIBLE);
            listPantry.setVisibility(View.GONE);

        } else if (displayedItems.isEmpty()) {
            textEmptyPantry.setText("No matching food items found.");
            textEmptyPantry.setVisibility(View.VISIBLE);
            listPantry.setVisibility(View.GONE);

        } else {
            textEmptyPantry.setVisibility(View.GONE);
            listPantry.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }

    // Compare expiry dates
    private int compareExpiryDates(String date1, String date2) {
        boolean empty1 = date1 == null || date1.trim().isEmpty();
        boolean empty2 = date2 == null || date2.trim().isEmpty();

        if (empty1 && empty2) return 0;
        if (empty1) return 1;
        if (empty2) return -1;

        return date1.compareTo(date2);
    }

    // Edit food item
    private void showEditDialog(int position) {
        int itemId = itemIds.get(position);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Food name");
        nameInput.setText(itemNames.get(position));

        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Quantity");
        quantityInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        quantityInput.setText(itemQuantities.get(position));

        EditText expiryInput = new EditText(this);
        expiryInput.setHint("Expiry Date (YYYY-MM-DD)");
        expiryInput.setText(itemExpiryDates.get(position));
        expiryInput.setFocusable(false);
        expiryInput.setClickable(true);
        expiryInput.setOnClickListener(v ->
                showDatePicker(expiryInput)
        );

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (
                20 * getResources().getDisplayMetrics().density
        );
        layout.setPadding(padding, 0, padding, 0);

        layout.addView(nameInput);
        layout.addView(quantityInput);
        layout.addView(expiryInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Food Item")
                .setView(layout)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        String newName = nameInput.getText()
                                .toString().trim();
                        String newQuantity = quantityInput.getText()
                                .toString().trim();
                        String newExpiry = expiryInput.getText()
                                .toString().trim();

                        if (newName.isEmpty() || newQuantity.isEmpty()) {
                            Toast.makeText(
                                    this,
                                    "Please enter name and quantity",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        boolean updated = databaseHelper.updateItem(
                                itemId,
                                newName,
                                newQuantity,
                                newExpiry
                        );

                        if (updated) {
                            loadItems();
                            Toast.makeText(
                                    this,
                                    "Item updated successfully",
                                    Toast.LENGTH_SHORT
                            ).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(
                                    this,
                                    "Failed to update item",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
        });

        dialog.show();
    }

    // Load food items and update summary
    private void loadItems() {
        pantryItems.clear();
        itemIds.clear();
        itemNames.clear();
        itemQuantities.clear();
        itemExpiryDates.clear();

        int totalItems = 0;
        int expiredItems = 0;
        int expiringSoonItems = 0;
        int freshItems = 0;

        try (Cursor cursor = databaseHelper.getAllItems()) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(
                        cursor.getColumnIndexOrThrow("_id")
                );
                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow("name")
                );
                String quantity = cursor.getString(
                        cursor.getColumnIndexOrThrow("quantity")
                );
                String expiryDate = cursor.getString(
                        cursor.getColumnIndexOrThrow("expiry_date")
                );

                if (expiryDate == null) {
                    expiryDate = "";
                }

                itemIds.add(id);
                itemNames.add(name);
                itemQuantities.add(quantity);
                itemExpiryDates.add(expiryDate);

                String status = getExpiryStatus(expiryDate);

                totalItems++;

                if (status.equals("EXPIRED")) {
                    expiredItems++;
                } else if (status.equals("EXPIRING SOON")) {
                    expiringSoonItems++;
                } else if (status.equals("FRESH")) {
                    freshItems++;
                }

                String displayText = name + " - " + quantity;

                if (!expiryDate.isEmpty()) {
                    displayText += " | Expiry: " + expiryDate;
                }

                displayText += " | " + status;
                pantryItems.add(displayText);
            }
        }

        textPantrySummary.setText(
                "Total Items: " + totalItems +
                        "\nExpired: " + expiredItems +
                        " | Expiring Soon: " + expiringSoonItems +
                        " | Fresh: " + freshItems
        );

        filterItems(editSearch.getText().toString());
    }
}
