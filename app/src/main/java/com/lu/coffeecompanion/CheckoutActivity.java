package com.lu.coffeecompanion;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.lu.coffeecompanion.databinding.ActivityCheckoutBinding;
import com.lu.coffeecompanion.databinding.DialogGcashQrBinding;
import com.lu.coffeecompanion.databinding.ItemCartBinding;

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

    // image proof
    private Uri proofImageUri = null;
    private boolean hasPaidWithGcash = false;
    private boolean isFirstTimeBuyer = true;

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

        // Check if user has completed orders
        checkIfFirstTimeBuyer();

        fetchAddress(addressDocId);
        fetchCartOptimized();

        // default hide proof container initially
        binding.proofContainer.setVisibility(View.GONE);

        // Payment method selection
        binding.radioGroupPayment.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioGcash) {
                selectedPaymentMethod = "GCash";
                // Show QR dialog when GCash is selected
                showGcashQRDialog(addressDocId);
            } else if (checkedId == R.id.radioCod) {
                selectedPaymentMethod = "Cash on Delivery";
                binding.proofContainer.setVisibility(View.GONE);
                hasPaidWithGcash = false;
            }
        });

        binding.back.setOnClickListener(v -> finish());

        // Choose image
        binding.btnUploadProof.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_PICK);
            pick.setType("image/*");
            startActivityForResult(pick, 200);
        });

        binding.placeOrder.setOnClickListener(v -> {
            if (selectedPaymentMethod.isEmpty()) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedPaymentMethod.equals("GCash")) {
                if (!hasPaidWithGcash) {
                    Toast.makeText(this, "Please complete GCash payment first", Toast.LENGTH_SHORT).show();
                    showGcashQRDialog(addressDocId);
                    return;
                }

                if (proofImageUri == null) {
                    Toast.makeText(this, "Please upload proof of payment", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            checkout(addressDocId, selectedPaymentMethod);
        });
    }

    private void checkIfFirstTimeBuyer() {
        db.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // First time buyer lang kung walang anumang orders pa
                    // (hindi lang completed, kahit pending pa lang)
                    isFirstTimeBuyer = querySnapshot.isEmpty();
                    updatePaymentMethodUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("CheckFirstBuyer", "Error: " + e.getMessage());
                    isFirstTimeBuyer = true;
                    updatePaymentMethodUI();
                });
    }

    private void updatePaymentMethodUI() {
        if (isFirstTimeBuyer) {
            // First time buyer - disable COD
            binding.radioCod.setEnabled(false);
            binding.radioCod.setAlpha(0.5f);
            binding.radioGcash.setChecked(true);
            binding.radioGcash.callOnClick();
        } else {
            // Returning customer - enable both
            binding.radioCod.setEnabled(true);
            binding.radioCod.setAlpha(1.0f);
        }
    }

    private void showGcashQRDialog(String addressDocId) {
        DialogGcashQrBinding qrBinding = DialogGcashQrBinding.inflate(getLayoutInflater());

        // Load QR Code image (replace with your actual QR code image URL)
        Glide.with(this)
                .load("https://i.imgur.com/YourQRCodeImage.png")
                .placeholder(R.drawable.gcash_qr_placeholder)
                .error(R.drawable.gcash_qr_placeholder)
                .into(qrBinding.qrCodeImage);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(qrBinding.getRoot())
                .setCancelable(true)
                .setTitle("GCash Payment")
                .setMessage("Please scan the QR code to pay. After payment, click 'I've Paid' to upload proof.")
                .create();

        dialog.show();

        // I've Paid button - pupunta sa upload proof of payment
        qrBinding.btnConfirmPayment.setOnClickListener(v -> {
            hasPaidWithGcash = true;
            dialog.dismiss();

            // I-show ang proof container
            binding.proofContainer.setVisibility(View.VISIBLE);

            // I-scroll down para makita ang proof container
            scrollToProofContainer();

            Toast.makeText(this, "Please upload your proof of payment", Toast.LENGTH_LONG).show();
        });

        qrBinding.btnCancel.setOnClickListener(v -> {
            // If user cancels, uncheck GCash radio button
            if (!isFirstTimeBuyer) {
                binding.radioCod.setChecked(true);
            }
            dialog.dismiss();
        });
    }

    // New method to handle scrolling to proof container
    private void scrollToProofContainer() {
        binding.main.post(new Runnable() {
            @Override
            public void run() {
                // Use scrollTo instead of smoothScrollTo for LinearLayout
                binding.main.scrollTo(0, binding.proofContainer.getTop());
            }
        });
    }

    private void fetchAddress(String addressDocId) {
        if (addressDocId == null) return;
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

                        // Display address info if needed
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
                        checkoutDetails.put("hasPaidWithGcash", hasPaidWithGcash);
                        pushCheckout(checkoutDetails);
                    } else {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.placeOrder.setEnabled(true);
                        Toast.makeText(this, "Failed to fetch address", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void pushCheckout(Map<String, Object> checkoutDetails) {
        // create empty order first to get docId
        db.collection("orders")
                .add(new HashMap<>())
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    uploadProofThenCheckout(docId, checkoutDetails);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.placeOrder.setEnabled(true);
                    Toast.makeText(this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadProofThenCheckout(String docId, Map<String, Object> checkoutDetails) {
        if (proofImageUri == null) {
            // No proof uploaded: just set checkout details
            db.collection("orders").document(docId)
                    .set(checkoutDetails)
                    .addOnSuccessListener(unused -> finalizeCheckoutAndNotify(docId, checkoutDetails))
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.placeOrder.setEnabled(true);
                        Toast.makeText(this, "Failed to save order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        try {
            // Use user ID in the storage path for better security
            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReference("payment_proofs/" + userId + "/" + docId + ".jpg");

            storageRef.putFile(proofImageUri)
                    .addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) ->
                            storageRef.getDownloadUrl().addOnSuccessListener((OnSuccessListener<Uri>) uri -> {
                                checkoutDetails.put("proofImageUrl", uri.toString());

                                db.collection("orders").document(docId)
                                        .set(checkoutDetails)
                                        .addOnSuccessListener(unused -> finalizeCheckoutAndNotify(docId, checkoutDetails))
                                        .addOnFailureListener(e -> {
                                            binding.progressBar.setVisibility(View.GONE);
                                            binding.placeOrder.setEnabled(true);
                                            Toast.makeText(this, "Failed to save order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }))
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.placeOrder.setEnabled(true);
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Log the error for debugging
                        Log.e("UploadError", "Upload failed: " + e.getMessage());
                    });
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            binding.placeOrder.setEnabled(true);
            Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("UploadError", "Exception: " + e.getMessage());
        }
    }

    private void finalizeCheckoutAndNotify(String docId, Map<String, Object> checkoutDetails) {
        // ADD ITEMS
        for (Map<String, Object> item : cartItems) {
            db.collection("orders").document(docId)
                    .collection("items")
                    .add(item);
        }

        // CLEAR CART
        db.collection("users").document(userId)
                .collection("cart")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                });

        // create admin notification document
        Map<String, Object> notif = new HashMap<>();
        notif.put("orderId", docId);
        notif.put("type", "payment_proof_uploaded");
        notif.put("timestamp", Timestamp.now());
        notif.put("read", false);
        notif.put("userId", userId);
        notif.put("name", checkoutDetails.get("name"));
        notif.put("totalPrice", checkoutDetails.get("totalPrice"));

        db.collection("admin_notifications").add(notif);

        // DONE
        binding.progressBar.setVisibility(View.GONE);
        Intent intent = new Intent(getApplicationContext(), OrderSuccessActivity.class);
        intent.putExtra("orderId", docId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            proofImageUri = data.getData();
            binding.imgProof.setImageURI(proofImageUri);
            Toast.makeText(this, "Proof of payment uploaded successfully", Toast.LENGTH_SHORT).show();
        }
    }
}