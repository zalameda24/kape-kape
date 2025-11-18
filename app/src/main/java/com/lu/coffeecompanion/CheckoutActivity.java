package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ActivityCheckoutBinding;
import com.lu.coffeecompanion.databinding.ItemCartBinding;
import com.lu.coffeecompanion.databinding.DialogGcashQrBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    ActivityCheckoutBinding binding;
    ItemCartBinding cartBinding;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userId;
    final double[] totalPrice = {0.0};
    private String selectedPaymentMethod = "";
    private List<Map<String, Object>> cartItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        userId = currentUser.getUid();

        Intent getIntent = getIntent();
        String addressDocId = getIntent.getStringExtra("documentId");

        fetchAddress(addressDocId);
        fetchCartOptimized();

        // Payment method selection
        binding.radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioGcash) {
                selectedPaymentMethod = "GCash";
            } else if (checkedId == R.id.radioCod) {
                selectedPaymentMethod = "Cash on Delivery";
            }
        });

        binding.back.setOnClickListener(v -> finish());

        binding.placeOrder.setOnClickListener(v -> {
            if (selectedPaymentMethod.isEmpty()) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedPaymentMethod.equals("GCash")) {
                showGcashQRDialog(addressDocId);
            } else {
                checkout(addressDocId, selectedPaymentMethod);
            }
        });
    }

    private void showGcashQRDialog(String addressDocId) {
        DialogGcashQrBinding qrBinding = DialogGcashQrBinding.inflate(getLayoutInflater());

        // Load QR Code image (⚠️ Replace with your actual QR code image URL)
        Glide.with(this)
                .load("https://i.imgur.com/YourQRCodeImage.png")
                .placeholder(R.drawable.gcash_qr_placeholder)
                .error(R.drawable.gcash_qr_placeholder)
                .into(qrBinding.qrCodeImage);

        // Create and show dialog
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(qrBinding.getRoot())
                .setCancelable(false)
                .create();

        dialog.show();

        // Confirm payment button
        qrBinding.btnConfirmPayment.setOnClickListener(v -> {
            dialog.dismiss();
            checkout(addressDocId, "GCash");
        });

        // Cancel button
        qrBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
    }

    private void fetchAddress(String addressDocId) {
        db.collection("users")
                .document(userId)
                .collection("addresses")
                .document(addressDocId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String name = task.getResult().getString("name");
                        String address = task.getResult().getString("address");
                        String mobile = task.getResult().getString("mobile");

                        binding.name.setText(name);
                        binding.address.setText(address);
                        binding.mobile.setText(mobile);
                    }
                });
    }

    private void fetchCartOptimized() {
        binding.itemContainer.removeAllViews();
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(cartSnapshot -> {
                    if (cartSnapshot.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    List<String> docIds = new ArrayList<>();
                    Map<String, Double> quantityMap = new HashMap<>();

                    for (DocumentSnapshot doc : cartSnapshot.getDocuments()) {
                        String docId = doc.getString("docId");
                        Double quantity = doc.getDouble("quantity");
                        docIds.add(docId);
                        quantityMap.put(docId, quantity);

                        Map<String, Object> item = new HashMap<>();
                        item.put("docId", docId);
                        item.put("quantity", quantity);
                        cartItems.add(item);
                    }

                    db.collection("shops")
                            .get()
                            .addOnSuccessListener(shopsSnapshot -> {
                                for (DocumentSnapshot shop : shopsSnapshot.getDocuments()) {
                                    String shopId = shop.getId();
                                    String shopName = shop.getString("name");

                                    db.collection("shops")
                                            .document(shopId)
                                            .collection("menu")
                                            .get()
                                            .addOnSuccessListener(menuSnapshot -> {
                                                for (DocumentSnapshot menuItem : menuSnapshot.getDocuments()) {
                                                    if (docIds.contains(menuItem.getId())) {
                                                        displayCartItem(menuItem, shopName, quantityMap.get(menuItem.getId()));
                                                    }
                                                }
                                                binding.progressBar.setVisibility(View.GONE);
                                            });
                                }
                            });
                });
    }

    private void displayCartItem(DocumentSnapshot document, String shopName, Double quantity) {
        String itemName = document.getString("name");
        Double itemPrice = document.getDouble("price");
        String imageUrl = document.getString("imageUrl");

        totalPrice[0] += itemPrice * quantity;

        cartBinding = ItemCartBinding.inflate(getLayoutInflater());

        Glide.with(getApplicationContext()).load(imageUrl).into(cartBinding.itemImage);
        cartBinding.itemName.setText(itemName);
        cartBinding.itemPrice.setText("₱" + String.format("%.2f", itemPrice));
        cartBinding.itemShop.setText(shopName);
        cartBinding.quantity.setText(String.format("%d", quantity.intValue()));

        binding.itemContainer.addView(cartBinding.getRoot());

        binding.merchSubtotal.setText("Merchandise Subtotal: +₱" + String.format("%.2f", totalPrice[0]));
        binding.shipSubtotal.setText("Shipping Subtotal: +₱20.00");
        binding.shipDiscountSubtotal.setText("Shipping Discount Subtotal: -₱20.00");
        binding.totalPrice.setText("₱" + String.format("%.2f", totalPrice[0]));
    }

    private void checkout(String addressDocId, String paymentMethod) {
        binding.placeOrder.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .document(userId)
                .collection("addresses")
                .document(addressDocId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> checkoutDetails = new HashMap<>();
                        checkoutDetails.put("userId", userId);
                        checkoutDetails.put("name", task.getResult().getString("name"));
                        checkoutDetails.put("address", task.getResult().getString("address"));
                        checkoutDetails.put("mobile", task.getResult().getString("mobile"));
                        checkoutDetails.put("totalPrice", totalPrice[0]);
                        checkoutDetails.put("orderTimestamp", Timestamp.now());
                        checkoutDetails.put("status", "Pending");
                        checkoutDetails.put("paymentMethod", paymentMethod);
                        pushCheckout(checkoutDetails);
                    }
                });
    }

    private void pushCheckout(Map<String, Object> checkoutDetails) {
        db.collection("orders")
                .add(checkoutDetails)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();

                    // Add cart items to order
                    for (Map<String, Object> item : cartItems) {
                        db.collection("orders")
                                .document(docId)
                                .collection("items")
                                .add(item);
                    }

                    // Clear cart
                    db.collection("users")
                            .document(userId)
                            .collection("cart")
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                    doc.getReference().delete();
                                }
                            });

                    binding.progressBar.setVisibility(View.GONE);
                    Intent intent = new Intent(getApplicationContext(), OrderSuccessActivity.class);
                    intent.putExtra("orderId", docId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.placeOrder.setEnabled(true);
                    Toast.makeText(this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
