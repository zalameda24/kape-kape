package com.lu.coffeecompanion;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lu.coffeecompanion.adapters.InventoryAdapter;
import com.lu.coffeecompanion.models.InventoryItem;

import java.util.ArrayList;

public class AdminInventoryActivity extends AppCompatActivity {

    private RecyclerView recyclerInventory;
    private InventoryAdapter adapter;
    private ArrayList<InventoryItem> itemList;
    private FirebaseFirestore firestore;

    private Uri selectedImageUri;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_inventory);

        firestore = FirebaseFirestore.getInstance();

        recyclerInventory = findViewById(R.id.recyclerInventory);
        recyclerInventory.setLayoutManager(new LinearLayoutManager(this));
        itemList = new ArrayList<>();
        adapter = new InventoryAdapter(this, itemList);
        recyclerInventory.setAdapter(adapter);

        // Load inventory from Firestore
        loadInventory();

        // Image picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                    }
                }
        );

        // CardView click to add product
        findViewById(R.id.cardAddInventory).setOnClickListener(v -> showAddProductDialog());
    }

    private void loadInventory() {
        firestore.collection("inventory").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                itemList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    InventoryItem item = doc.toObject(InventoryItem.class);
                    itemList.add(item);
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to load inventory", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddProductDialog() {
        // Inflate dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null);

        EditText etName = dialogView.findViewById(R.id.etProductName);
        EditText etPrice = dialogView.findViewById(R.id.etProductPrice);
        EditText etQuantity = dialogView.findViewById(R.id.etProductQuantity);
        ImageView ivImage = dialogView.findViewById(R.id.ivProductImage);
        TextView btnAdd = dialogView.findViewById(R.id.btnAddProduct);

        // Pick image click
        ivImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnAdd.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String qtyStr = etQuantity.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || qtyStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            int qty = Integer.parseInt(qtyStr);
            String id = firestore.collection("inventory").document().getId();
            String imageUrl = selectedImageUri != null ? selectedImageUri.toString() : "";

            InventoryItem newItem = new InventoryItem(id, name, price, qty, imageUrl);

            firestore.collection("inventory").document(id).set(newItem)
                    .addOnSuccessListener(aVoid -> {
                        itemList.add(newItem);
                        adapter.notifyItemInserted(itemList.size() - 1);
                        Toast.makeText(this, "Product added!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        selectedImageUri = null; // reset for next add
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add product", Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }
}
