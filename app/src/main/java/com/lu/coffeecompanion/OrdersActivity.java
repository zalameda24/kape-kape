package com.lu.coffeecompanion;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.lu.coffeecompanion.adapters.UserOrderAdapter;
import com.lu.coffeecompanion.databinding.ActivityOrdersBinding;
import com.lu.coffeecompanion.models.Order;

import java.util.ArrayList;
import java.util.List;

public class OrdersActivity extends AppCompatActivity {

    private static final String TAG = "OrdersActivity";

    private ActivityOrdersBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userId;
    private UserOrderAdapter adapter;
    private List<Order> orderList;
    private ListenerRegistration orderListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();

        if (!checkUserAuthentication()) {
            return;
        }

        setupUI();
        loadAllOrders();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private boolean checkUserAuthentication() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        userId = currentUser.getUid();
        Log.d(TAG, "Authenticated user ID: " + userId);
        return true;
    }

    private void setupUI() {
        binding.back.setOnClickListener(v -> finish());

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadAllOrders();
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        orderList = new ArrayList<>();
        adapter = new UserOrderAdapter(this, orderList);

        binding.recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewOrders.setAdapter(adapter);
    }

    private void loadAllOrders() {
        showLoading(true);
        showEmptyState(false);

        Log.d(TAG, "Loading all orders for userId: " + userId);

        // Remove old listener
        if (orderListener != null) {
            orderListener.remove();
        }

        // Try with orderTimestamp first
        Query query = db.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("orderTimestamp", Query.Direction.DESCENDING);

        orderListener = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error loading orders", error);

                // Check if it's an index error
                if (error.getMessage() != null && error.getMessage().contains("requires an index")) {
                    Log.d(TAG, "Index error detected, trying alternative query...");
                    loadOrdersAlternative();
                    return;
                }

                showLoading(false);
                showEmptyState(true);
                Toast.makeText(this, "Failed to load orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            processOrders(querySnapshot);
        });
    }

    private void loadOrdersAlternative() {
        // Alternative 1: Try without ordering
        Query query = db.collection("orders")
                .whereEqualTo("userId", userId);

        orderListener = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error in alternative query", error);

                // Alternative 2: Load all and filter manually
                loadAllOrdersManual();
                return;
            }

            processOrders(querySnapshot);
        });
    }

    private void loadAllOrdersManual() {
        // Load all orders and filter manually
        Query query = db.collection("orders")
                .orderBy("orderTimestamp", Query.Direction.DESCENDING)
                .limit(50); // Limit to reasonable number

        orderListener = query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error in manual loading", error);
                showLoading(false);
                showEmptyState(true);
                Toast.makeText(this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                return;
            }

            if (querySnapshot == null || querySnapshot.isEmpty()) {
                showLoading(false);
                showEmptyState(true);
                return;
            }

            orderList.clear();

            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                String docUserId = document.getString("userId");

                // Filter by userId manually
                if (userId.equals(docUserId)) {
                    Order order = documentToOrder(document);
                    orderList.add(order);
                }
            }

            updateUIAfterLoading();
        });
    }

    private void processOrders(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
        if (querySnapshot == null || querySnapshot.isEmpty()) {
            Log.d(TAG, "No orders found for user");
            showLoading(false);
            showEmptyState(true);
            return;
        }

        orderList.clear();

        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
            Order order = documentToOrder(document);
            orderList.add(order);
        }

        updateUIAfterLoading();
    }

    private void updateUIAfterLoading() {
        Log.d(TAG, "Loaded " + orderList.size() + " orders");

        if (orderList.isEmpty()) {
            showLoading(false);
            showEmptyState(true);
            return;
        }

        adapter.notifyDataSetChanged();
        showLoading(false);
        showEmptyState(false);

        // Log order details for debugging
        for (Order order : orderList) {
            Log.d(TAG, "Order: " + order.getOrderId() +
                    ", Status: " + order.getStatus() +
                    ", Total: " + order.getTotalAmount());
        }
    }

    private Order documentToOrder(DocumentSnapshot document) {
        Order order = new Order();

        // Set basic fields
        order.setOrderId(document.getId());
        order.setReceiptNumber(document.getString("receiptNumber"));
        order.setUserId(document.getString("userId"));

        // User information - check multiple possible field names
        order.setUserName(getFieldWithFallback(document, "userName", "name", "customerName"));
        order.setName(getFieldWithFallback(document, "name", "customerName", "userName"));
        order.setAddress(document.getString("address"));
        order.setMobile(document.getString("mobile"));
        order.setPaymentMethod(document.getString("paymentMethod"));
        order.setStatus(document.getString("status"));
        order.setProofImageUrl(document.getString("proofImageUrl"));

        // Price fields
        Double totalPrice = document.getDouble("totalPrice");
        Double deliveryFee = document.getDouble("deliveryFee");

        // Check for alternative field names
        if (totalPrice == null) {
            totalPrice = document.getDouble("total");
        }

        order.setTotalPrice(totalPrice != null ? totalPrice : 0.0);
        order.setDeliveryFee(deliveryFee != null ? deliveryFee : 0.0);

        // Timestamp fields - try multiple possibilities
        com.google.firebase.Timestamp orderTimestamp = document.getTimestamp("orderTimestamp");
        if (orderTimestamp == null) {
            orderTimestamp = document.getTimestamp("timestamp");
        }

        order.setOrderTimestamp(orderTimestamp);

        // Legacy timestamp field
        Long timestamp = document.getLong("timestamp");
        if (timestamp == null && orderTimestamp != null) {
            timestamp = orderTimestamp.toDate().getTime();
        }
        order.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());

        return order;
    }

    // Helper method to get field with fallback options
    private String getFieldWithFallback(DocumentSnapshot document, String primaryField, String... fallbackFields) {
        String value = document.getString(primaryField);

        if (value != null && !value.isEmpty()) {
            return value;
        }

        for (String field : fallbackFields) {
            value = document.getString(field);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        return "";
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewOrders.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
    }

    private void showEmptyState(boolean show) {
        binding.emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.recyclerViewOrders.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh orders when activity resumes
        if (userId != null) {
            loadAllOrders();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
    }
}