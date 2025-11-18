package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lu.coffeecompanion.databinding.AccountFragmentBinding;

import java.util.HashMap;
import java.util.Map;

public class AccountFragment extends Fragment {

    AccountFragmentBinding binding;
    DatabaseReference db;
    FirebaseAuth auth;
    FirebaseUser currentUser;
    String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = AccountFragmentBinding.inflate(inflater, container, false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseDatabase.getInstance().getReference();

        if (currentUser == null) {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
            return binding.getRoot();
        }

        userId = currentUser.getUid();

        loadUserData();

        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUserData();
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        binding.btnName.setOnClickListener(v -> showEditDialog("Name", binding.name.getText().toString(), "name"));
        binding.btnEmail.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Email cannot be changed", Toast.LENGTH_SHORT).show();
        });
        binding.btnMobile.setOnClickListener(v -> showEditDialog("Mobile Number", binding.mobile.getText().toString(), "mobile"));

        binding.addresses.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddressListActivity.class);
            startActivity(intent);
        });

        binding.orders.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OrdersActivity.class);
            startActivity(intent);
        });

        binding.btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        auth.signOut();
                        Intent intent = new Intent(requireContext(), MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        return binding.getRoot();
    }

    private void loadUserData() {
        binding.nameProgressBar.setVisibility(View.VISIBLE);
        binding.emailProgressBar.setVisibility(View.VISIBLE);
        binding.mobileProgressBar.setVisibility(View.VISIBLE);

        db.child("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String name = dataSnapshot.child("name").getValue(String.class);
                            String email = dataSnapshot.child("email").getValue(String.class);
                            String mobile = dataSnapshot.child("mobile").getValue(String.class);
                            String role = dataSnapshot.child("role").getValue(String.class);
                            Boolean blocked = dataSnapshot.child("blocked").getValue(Boolean.class);

                            // Check if user is blocked
                            if (blocked != null && blocked) {
                                Toast.makeText(requireContext(), "Your account has been blocked", Toast.LENGTH_LONG).show();
                                auth.signOut();
                                Intent intent = new Intent(requireContext(), MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                requireActivity().finish();
                                return;
                            }

                            binding.textName.setText(name != null ? name : "User");
                            binding.name.setText(name != null ? name : "Not set");
                            binding.email.setText(email != null ? email : "Not set");
                            binding.mobile.setText(mobile != null ? mobile : "Not set");

                            binding.nameProgressBar.setVisibility(View.GONE);
                            binding.emailProgressBar.setVisibility(View.GONE);
                            binding.mobileProgressBar.setVisibility(View.GONE);
                        } else {
                            // Create user data if doesn't exist with default values
                            createUserData();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                        binding.nameProgressBar.setVisibility(View.GONE);
                        binding.emailProgressBar.setVisibility(View.GONE);
                        binding.mobileProgressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void createUserData() {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User");
        userData.put("email", currentUser.getEmail());
        userData.put("mobile", "Not set");
        userData.put("role", "user");
        userData.put("blocked", false);

        db.child("users").child(userId)
                .setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    // Reload data after creating
                    loadUserData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to create user data", Toast.LENGTH_SHORT).show();
                    binding.nameProgressBar.setVisibility(View.GONE);
                    binding.emailProgressBar.setVisibility(View.GONE);
                    binding.mobileProgressBar.setVisibility(View.GONE);
                });
    }

    private void showEditDialog(String title, String currentValue, String field) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit " + title);

        // Create input field
        final EditText input = new EditText(requireContext());
        input.setText(currentValue);
        input.setPadding(50, 30, 50, 30);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();

            if (newValue.isEmpty()) {
                Toast.makeText(requireContext(), title + " cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (field.equals("mobile") && newValue.length() < 10) {
                Toast.makeText(requireContext(), "Invalid mobile number", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put(field, newValue);

            db.child("users").child(userId)
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), title + " updated successfully", Toast.LENGTH_SHORT).show();
                        loadUserData();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to update " + title, Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}