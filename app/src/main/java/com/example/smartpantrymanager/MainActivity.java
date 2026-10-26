package com.example.smartpantrymanager;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    EditText editItemName, editQuantity;
    Button btnAddItem;
    ListView listPantry;

    ArrayList<String> pantryItems;
    ArrayList<Integer> itemIds;
    ArrayList<String> itemNames;
    ArrayList<String> itemQuantities;

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

                    v.setPadding(
                            bars.left + 20,
                            bars.top + 20,
                            bars.right + 20,
                            bars.bottom + 20
                    );

                    return insets;
                }
        );

        editItemName = findViewById(R.id.editItemName);
        editQuantity = findViewById(R.id.editQuantity);
        btnAddItem = findViewById(R.id.btnAddItem);
        listPantry = findViewById(R.id.listPantry);

        databaseHelper = new DatabaseHelper(this);

        pantryItems = new ArrayList<>();
        itemIds = new ArrayList<>();
        itemNames = new ArrayList<>();
        itemQuantities = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                pantryItems
        );

        listPantry.setAdapter(adapter);

        loadItems();

        // Add a food item
        btnAddItem.setOnClickListener(v -> {

            String name = editItemName.getText().toString().trim();
            String quantity = editQuantity.getText().toString().trim();

            if (name.isEmpty() || quantity.isEmpty()) {
                Toast.makeText(this,
                        "Please enter item and quantity",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            boolean saved = databaseHelper.addItem(name, quantity);

            if (saved) {
                loadItems();

                editItemName.setText("");
                editQuantity.setText("");

                Toast.makeText(this,
                        "Item saved successfully",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Failed to save item",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Tap an item to edit it
        listPantry.setOnItemClickListener(
                (parent, view, position, id) -> {
                    showEditDialog(position);
                }
        );

        // Long-press an item to delete it
        listPantry.setOnItemLongClickListener(
                (parent, view, position, id) -> {

                    int itemId = itemIds.get(position);
                    String itemName = pantryItems.get(position);

                    new AlertDialog.Builder(this)
                            .setTitle("Delete Item")
                            .setMessage("Delete " + itemName + "?")
                            .setPositiveButton("Delete",
                                    (dialog, which) -> {

                                        boolean deleted =
                                                databaseHelper.deleteItem(itemId);

                                        if (deleted) {
                                            loadItems();

                                            Toast.makeText(this,
                                                    "Item deleted",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    })
                            .setNegativeButton("Cancel", null)
                            .show();

                    return true;
                }
        );
    }

    // Display the edit dialog
    private void showEditDialog(int position) {

        int itemId = itemIds.get(position);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Food name");
        nameInput.setText(itemNames.get(position));

        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Quantity");
        quantityInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        quantityInput.setText(itemQuantities.get(position));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);

        layout.setPadding(padding, 0, padding, 0);
        layout.addView(nameInput);
        layout.addView(quantityInput);

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

                        if (newName.isEmpty() || newQuantity.isEmpty()) {
                            Toast.makeText(this,
                                    "Please fill in both fields",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        boolean updated = databaseHelper.updateItem(
                                itemId, newName, newQuantity
                        );

                        if (updated) {
                            loadItems();

                            Toast.makeText(this,
                                    "Item updated successfully",
                                    Toast.LENGTH_SHORT).show();

                            dialog.dismiss();
                        } else {
                            Toast.makeText(this,
                                    "Failed to update item",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        dialog.show();
    }

    // Load food items from SQLite
    private void loadItems() {

        pantryItems.clear();
        itemIds.clear();
        itemNames.clear();
        itemQuantities.clear();

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

                itemIds.add(id);
                itemNames.add(name);
                itemQuantities.add(quantity);
                pantryItems.add(name + " - " + quantity);
            }
        }

        adapter.notifyDataSetChanged();
    }
}