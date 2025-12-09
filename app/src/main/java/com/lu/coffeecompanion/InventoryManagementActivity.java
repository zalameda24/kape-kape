package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryManagementActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<InventoryItem> inventoryList;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_management);

        firestore = FirebaseFirestore.getInstance();
        inventoryList = new ArrayList<>();

        initializeViews();
        setupClickListeners();
        loadInventory();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewInventory);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InventoryAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Add Item button
        findViewById(R.id.btnAddItem).setOnClickListener(v -> showAddItemDialog());
    }

    private void showAddItemDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Add New Inventory Item");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_inventory_item, null);
        builder.setView(dialogView);

        EditText etProductName = dialogView.findViewById(R.id.etProductName);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String productName = etProductName.getText().toString().trim();
            String quantityStr = etQuantity.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();

            if (productName.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantity = Integer.parseInt(quantityStr);
                double price = Double.parseDouble(priceStr);

                addInventoryItem(productName, quantity, price);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addInventoryItem(String productName, int quantity, double price) {
        Map<String, Object> item = new HashMap<>();
        item.put("productName", productName);
        item.put("quantity", quantity);
        item.put("price", price);
        item.put("timestamp", System.currentTimeMillis());

        firestore.collection("inventory")
                .add(item)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
                    loadInventory();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error adding item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadInventory() {
        firestore.collection("inventory")
                .orderBy("productName")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    inventoryList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String id = document.getId();
                        String productName = document.getString("productName");
                        Long quantity = document.getLong("quantity");
                        Double price = document.getDouble("price");

                        if (productName != null && quantity != null && price != null) {
                            InventoryItem item = new InventoryItem(id, productName, quantity.intValue(), price);
                            inventoryList.add(item);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (inventoryList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading inventory: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteInventoryItem(String itemId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firestore.collection("inventory").document(itemId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                inventoryList.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();

                                if (inventoryList.isEmpty()) {
                                    tvEmptyState.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error deleting item", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateInventoryItem(String itemId, String productName, int quantity, double price) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("productName", productName);
        updates.put("quantity", quantity);
        updates.put("price", price);

        firestore.collection("inventory").document(itemId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show();
                    loadInventory();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Inventory Item Model Class
    private static class InventoryItem {
        String id;
        String productName;
        int quantity;
        double price;

        InventoryItem(String id, String productName, int quantity, double price) {
            this.id = id;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }
    }

    // RecyclerView Adapter
    private class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_inventory, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            InventoryItem item = inventoryList.get(position);

            holder.tvProductName.setText(item.productName);
            holder.tvQuantity.setText(String.valueOf(item.quantity));
            holder.tvPrice.setText(String.format("₱%.2f", item.price));

            holder.btnEdit.setOnClickListener(v -> showEditDialog(item, position));
            holder.btnDelete.setOnClickListener(v -> deleteInventoryItem(item.id, position));
        }

        @Override
        public int getItemCount() {
            return inventoryList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvProductName, tvQuantity, tvPrice;
            ImageButton btnEdit, btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvQuantity = itemView.findViewById(R.id.tvQuantity);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }

    private void showEditDialog(InventoryItem item, int position) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Edit Inventory Item");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_inventory_item, null);
        builder.setView(dialogView);

        EditText etProductName = dialogView.findViewById(R.id.etProductName);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);

        // Set current values
        etProductName.setText(item.productName);
        etQuantity.setText(String.valueOf(item.quantity));
        etPrice.setText(String.valueOf(item.price));

        builder.setPositiveButton("Update", (dialog, which) -> {
            String productName = etProductName.getText().toString().trim();
            String quantityStr = etQuantity.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();

            if (productName.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantity = Integer.parseInt(quantityStr);
                double price = Double.parseDouble(priceStr);
                updateInventoryItem(item.id, productName, quantity, price);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}