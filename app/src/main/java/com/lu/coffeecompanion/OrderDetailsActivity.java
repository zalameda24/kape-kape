package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.lu.coffeecompanion.databinding.ActivityOrderDetailsBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetailsActivity";

    private ActivityOrderDetailsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private String orderId;
    private ListenerRegistration statusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();

        if (!checkUserAuthentication()) {
            return;
        }

        orderId = getIntent().getStringExtra("orderId");
        if (orderId == null) {
            Toast.makeText(this, "Invalid order", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadOrderDetails();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private boolean checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return false;
        }
        userId = currentUser.getUid();
        return true;
    }

    private void setupUI() {
        // Add back button functionality if you have a back ImageView/Button
        if (binding.getRoot().findViewById(R.id.back) != null) {
            binding.getRoot().findViewById(R.id.back).setOnClickListener(v -> finish());
        }
    }

    private void loadOrderDetails() {
        showLoading(true);

        // Real-time listener for status updates
        statusListener = db.collection("orders")
                .document(orderId)
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading order", error);
                        showLoading(false);
                        Toast.makeText(this, "Failed to load order", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (document != null && document.exists()) {
                        displayOrderDetails(document);
                        loadOrderItems();
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void displayOrderDetails(DocumentSnapshot document) {
        try {
            String name = document.getString("name");
            String address = document.getString("address");
            String mobile = document.getString("mobile");
            String status = document.getString("status");
            String paymentMethod = document.getString("paymentMethod");
            Double totalPrice = document.getDouble("totalPrice");
            Double deliveryFee = document.getDouble("deliveryFee");
            Timestamp orderTimestamp = document.getTimestamp("orderTimestamp");

            // Set basic info
            if (binding.name != null) {
                binding.name.setText(name != null ? name : "N/A");
            }

            if (binding.address != null) {
                binding.address.setText(address != null ? address : "N/A");
            }

            if (binding.mobile != null) {
                binding.mobile.setText(mobile != null ? mobile : "N/A");
            }

            if (binding.status != null) {
                binding.status.setText(status != null ? status.toUpperCase() : "PENDING");
                setStatusColor(binding.status, status);
            }

            if (binding.date != null && orderTimestamp != null) {
                Date date = orderTimestamp.toDate();
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy - h:mm a", Locale.getDefault());
                binding.date.setText(dateFormat.format(date));
            }

            // Set payment info if you have those views
            if (binding.getRoot().findViewById(R.id.paymentMethod) != null) {
                TextView tvPayment = binding.getRoot().findViewById(R.id.paymentMethod);
                tvPayment.setText(paymentMethod != null ? paymentMethod : "N/A");
            }

            // Set total if you have that view
            if (binding.getRoot().findViewById(R.id.totalAmount) != null) {
                TextView tvTotal = binding.getRoot().findViewById(R.id.totalAmount);
                double total = (totalPrice != null ? totalPrice : 0.0) +
                        (deliveryFee != null ? deliveryFee : 0.0);
                tvTotal.setText(String.format("₱%.2f", total));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error displaying order details", e);
        }
    }

    private void setStatusColor(TextView tvStatus, String status) {
        if (status == null) status = "pending";

        int backgroundColor;
        switch (status.toLowerCase()) {
            case "pending":
                backgroundColor = 0xFFFFC107;
                break;
            case "preparing":
            case "confirmed":
                backgroundColor = 0xFF2196F3;
                break;
            case "out for delivery":
                backgroundColor = 0xFFFF9800;
                break;
            case "delivered":
            case "completed":
                backgroundColor = 0xFF4CAF50;
                break;
            case "cancelled":
                backgroundColor = 0xFFF44336;
                break;
            default:
                backgroundColor = 0xFFFFC107;
        }

        tvStatus.setBackgroundColor(backgroundColor);
        tvStatus.setTextColor(0xFFFFFFFF);
        tvStatus.setPadding(24, 12, 24, 12);
    }

    private void loadOrderItems() {
        if (binding.itemContainer == null) {
            showLoading(false);
            return;
        }

        binding.itemContainer.removeAllViews();

        db.collection("orders")
                .document(orderId)
                .collection("items")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        showLoading(false);
                        return;
                    }

                    for (DocumentSnapshot itemDoc : querySnapshot.getDocuments()) {
                        String docId = itemDoc.getString("docId");
                        Double quantity = itemDoc.getDouble("quantity");

                        if (docId != null && quantity != null) {
                            loadMenuItem(docId, quantity);
                        }
                    }

                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading items", e);
                    showLoading(false);
                });
    }

    private void loadMenuItem(String menuItemId, Double quantity) {
        db.collection("shops")
                .get()
                .addOnSuccessListener(shopsSnapshot -> {
                    for (DocumentSnapshot shop : shopsSnapshot.getDocuments()) {
                        String shopId = shop.getId();

                        db.collection("shops")
                                .document(shopId)
                                .collection("menu")
                                .document(menuItemId)
                                .get()
                                .addOnSuccessListener(menuItem -> {
                                    if (menuItem.exists()) {
                                        String itemName = menuItem.getString("name");
                                        Double price = menuItem.getDouble("price");
                                        String imageUrl = menuItem.getString("imageUrl");

                                        if (itemName != null && price != null) {
                                            displayMenuItem(itemName, price, imageUrl, quantity);
                                        }
                                    }
                                });
                    }
                });
    }

    private void displayMenuItem(String itemName, Double price, String imageUrl, Double quantity) {
        if (binding.itemContainer == null) return;

        View orderView = LayoutInflater.from(this)
                .inflate(R.layout.item_orderitems, binding.itemContainer, false);

        ImageView imageItem = orderView.findViewById(R.id.img_item);
        TextView productName = orderView.findViewById(R.id.product_name);
        TextView productPrice = orderView.findViewById(R.id.product_price);
        TextView quantityText = orderView.findViewById(R.id.quantity);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(imageItem);
        }

        productName.setText(itemName);
        productPrice.setText(String.format("₱%.2f", price));
        quantityText.setText(String.format("x%.0f", quantity));

        binding.itemContainer.addView(orderView);
    }

    private void showLoading(boolean show) {
        // Implement loading indicator if you have one in your layout
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null) {
            statusListener.remove();
        }
    }
}