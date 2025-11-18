package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.lu.coffeecompanion.databinding.ActivityAddressListBinding;

public class AddressListActivity extends AppCompatActivity {
    ActivityAddressListBinding binding;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser currentUser = auth.getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityAddressListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadAddresses();

        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadAddresses();
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        binding.back.setOnClickListener(v -> {
            finish();
        });

        binding.addAddress.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), AddAddressActivity.class);
            startActivity(intent);
        });
    }

    private void loadAddresses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.parentLayout.removeAllViews();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).collection("addresses")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null) {
                                for (QueryDocumentSnapshot document : querySnapshot) {
                                    String documentId = document.getId();
                                    String name = document.getString("name");
                                    String address = document.getString("address");
                                    String mobile = document.getString("mobile");

                                    addAddressView(documentId, address, mobile, name);
                                    binding.progressBar.setVisibility(View.GONE);
                                }
                            }
                        }
                    });
        }
        else{
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void addAddressView(String documentId, String address, String mobile, String name) {
        View addressView = LayoutInflater.from(this).inflate(R.layout.item_addresscard, null);

        TextView addressTextView = addressView.findViewById(R.id.addressTextView);
        TextView mobileTextView = addressView.findViewById(R.id.mobileTextView);
        TextView nameTextView = addressView.findViewById(R.id.nameTextView);

        addressTextView.setText(address);
        mobileTextView.setText(mobile);
        nameTextView.setText(name);

        addressView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), EditAddressActivity.class);
            intent.putExtra("documentId", documentId);
            startActivity(intent);
        });

        binding.parentLayout.addView(addressView);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadAddresses();
    }
}