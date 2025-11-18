package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lu.coffeecompanion.databinding.ActivityAdminDashboardBinding;

public class AdminDashboardActivity extends AppCompatActivity {

    ActivityAdminDashboardBinding binding;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbRef = FirebaseDatabase.getInstance().getReference();

        // User Management Card Click - ITO ANG MAGPAPANavigate SA MANAGE USERS
        CardView userManagementCard = findViewById(R.id.cardUserManagement);
        userManagementCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminManageUsersActivity.class);
            startActivity(intent);
        });

        // Logout Button
        binding.btnLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });

        // Load user statistics
        loadUserStats();
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
                // Handle error
            }
        });
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
    }
}