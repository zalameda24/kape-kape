package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lu.coffeecompanion.databinding.ActivityAdminDashboardBinding;

public class AdminDashboardActivity extends AppCompatActivity {

    ActivityAdminDashboardBinding binding;
    private DatabaseReference dbRef;
    private FirebaseFirestore firestore;

    private TextView tvTotalGcashPayments;
    private TextView tvTotalCodPayments;
    private TextView tvOverallTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbRef = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();

        // Initialize statistics TextViews
        tvTotalGcashPayments = findViewById(R.id.tvTotalGcashPayments);
        tvTotalCodPayments = findViewById(R.id.tvTotalCodPayments);
        tvOverallTotal = findViewById(R.id.tvOverallTotal);

        // User Management Card Click
        CardView userManagementCard = findViewById(R.id.cardUserManagement);
        userManagementCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminManageUsersActivity.class);
            startActivity(intent);
        });

        // Payments Card Click
        CardView paymentsCard = findViewById(R.id.cardPayments);
        paymentsCard.setOnClickListener(v -> {
            Log.d("AdminDashboard", "Payments card clicked");
            Intent intent = new Intent(AdminDashboardActivity.this, AdminPaymentsActivity.class);
            startActivity(intent);
        });

        // Add COD Payment Card Click (NEW)
        CardView addCodPaymentCard = findViewById(R.id.cardAddCodPayment);
        addCodPaymentCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminAddCodPaymentActivity.class);
            startActivity(intent);
        });

        // Logout Button
        binding.btnLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });

        // Load user statistics
        loadUserStats();

        // Load payment statistics
        loadPaymentStatistics();
    }

    private void loadUserStats() {
        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int totalUsers = (int) snapshot.getChildrenCount();
                    int activeUsers = 0;

                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        Boolean blocked = userSnapshot.child("blocked").getValue(Boolean.class);
                        if (blocked == null || !blocked) {
                            activeUsers++;
                        }
                    }

                    TextView tvTotalUsers = findViewById(R.id.tvTotalUsers);
                    TextView tvActiveUsers = findViewById(R.id.tvActiveUsers);

                    if (tvTotalUsers != null) tvTotalUsers.setText(String.valueOf(totalUsers));
                    if (tvActiveUsers != null) tvActiveUsers.setText(String.valueOf(activeUsers));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("AdminDashboard", "Error loading user stats: " + error.getMessage());
            }
        });
    }

    private void loadPaymentStatistics() {
        // Load GCash payments from orders collection
        firestore.collection("orders")
                .whereEqualTo("paymentMethod", "GCash")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalGcash = 0.0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double totalPrice = document.getDouble("totalPrice");
                        if (totalPrice != null) {
                            totalGcash += totalPrice;
                        }
                    }

                    final double finalGcashTotal = totalGcash;

                    // Load COD payments from cod_payments collection
                    firestore.collection("cod_payments")
                            .get()
                            .addOnSuccessListener(codSnapshots -> {
                                double totalCod = 0.0;
                                for (QueryDocumentSnapshot doc : codSnapshots) {
                                    Double amount = doc.getDouble("amount");
                                    if (amount != null) {
                                        totalCod += amount;
                                    }
                                }

                                // Update UI with statistics
                                updatePaymentStatistics(finalGcashTotal, totalCod);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("AdminDashboard", "Error loading COD payments: " + e.getMessage());
                                updatePaymentStatistics(finalGcashTotal, 0.0);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminDashboard", "Error loading GCash payments: " + e.getMessage());
                });
    }

    private void updatePaymentStatistics(double gcashTotal, double codTotal) {
        double overallTotal = gcashTotal + codTotal;

        if (tvTotalGcashPayments != null) {
            tvTotalGcashPayments.setText(String.format("₱%.2f", gcashTotal));
        }

        if (tvTotalCodPayments != null) {
            tvTotalCodPayments.setText(String.format("₱%.2f", codTotal));
        }

        if (tvOverallTotal != null) {
            tvOverallTotal.setText(String.format("₱%.2f", overallTotal));
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserStats();
        loadPaymentStatistics();
    }
}