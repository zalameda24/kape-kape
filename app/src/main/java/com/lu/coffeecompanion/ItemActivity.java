package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ActivityItemBinding;
import java.util.Map;
import java.util.HashMap;

public class ItemActivity extends AppCompatActivity {

    ActivityItemBinding binding;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userId;
    int counter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityItemBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();


        if(currentUser == null){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        else{
            userId = currentUser.getUid();
        }

        Intent getIntent = getIntent();
        String documentId = getIntent.getStringExtra("documentId");
        String docId = getIntent.getStringExtra("docId");

        db.collection("shops")
                .document(documentId)
                .collection("menu")
                .document(docId)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String itemName = (String) document.get("name");
                            double itemPrice = document.getDouble("price");
                            String itemDescription = (String) document.get("description");
                            String imageUrl = (String) document.get("imageUrl");
                            String category = (String) document.get("category");

                            Glide.with(getApplicationContext()).load(imageUrl).into(binding.itemImage);
                            binding.itemName.setText(itemName);
                            binding.itemTitle.setText(itemName);
                            binding.itemPrice.setText("₱" + String.format("%.2f", itemPrice));
                            binding.itemDescription.setText(itemDescription);
                        }
                    }
                });

        binding.btnAdd.setOnClickListener(v -> {
            String counterString = binding.quantity.getText().toString().trim();
            counter = Integer.parseInt(counterString);
            counter++;
            binding.quantity.setText(String.valueOf(counter));
        });
        binding.btnMinus.setOnClickListener(v -> {
            String counterString = binding.quantity.getText().toString().trim();
            counter = Integer.parseInt(counterString);
            if(counter == 0){
                counter = 0;
            }
            else{
                counter--;
            }
            binding.quantity.setText(String.valueOf(counter));
        });

        binding.back.setOnClickListener(v -> {
            finish();
        });

        binding.addToCart.setOnClickListener(v -> {
            Map<String, Object> cartItem = new HashMap<>();
            String currentCountString = binding.quantity.getText().toString().trim();
            int currentCount = Integer.parseInt(currentCountString);
            cartItem.put("docId", docId);
            cartItem.put("quantity", currentCount);

            db.collection("users")
                    .document(userId)
                    .collection("cart")
                    .whereEqualTo("docId", docId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String existingDocId = document.getId();

                            db.collection("users")
                                    .document(userId)
                                    .collection("cart")
                                    .document(existingDocId)
                                    .update("quantity", FieldValue.increment(currentCount))
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Item quantity updated.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Unexpected error.", Toast.LENGTH_SHORT).show();
                                    });
                        } else{
                            db.collection("users")
                                    .document(userId)
                                    .collection("cart")
                                    .add(cartItem)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "Item added to cart successfully.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }).addOnFailureListener(e -> {
                                        Toast.makeText(this, "Unexpected error.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    });


        }); 



    }
}