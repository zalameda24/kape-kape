package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.lu.coffeecompanion.databinding.ActivityOrderDetailsBinding;

import java.text.SimpleDateFormat;
import java.util.Date;

public class OrderDetailsActivity extends AppCompatActivity {

    ActivityOrderDetailsBinding binding;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityOrderDetailsBinding.inflate(getLayoutInflater());
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
        String orderId = getIntent.getStringExtra("orderId");

        db.collection("orders").document(orderId).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                DocumentSnapshot document = task.getResult();
                String name = document.getString("name");
                String address = document.getString("address");
                String mobile = document.getString("mobile");
                String status = document.getString("status");
                Timestamp orderTimestamp = document.getTimestamp("orderTimestamp");
                Date date = orderTimestamp.toDate();
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy - h:mma");
                String formattedDate = dateFormat.format(date);
                Double totalPrice = document.getDouble("totalPrice");

                binding.name.setText(name);
                binding.address.setText(address);
                binding.mobile.setText(mobile);
                binding.status.setText(status);
                binding.date.setText(formattedDate);

                db.collection("orders").document(orderId).collection("items").get().addOnCompleteListener(task1 -> {
                    if(task1.isSuccessful()){
                        QuerySnapshot querySnapshot1 = task1.getResult();
                        for(QueryDocumentSnapshot document1 : querySnapshot1){
                            String docId = document1.getString("docId");
                            Double quantity = document1.getDouble("quantity");

                            db.collection("shops").get().addOnCompleteListener(task2 -> {
                               if(task2.isSuccessful()){
                                   QuerySnapshot querySnapshot2 = task2.getResult();
                                   for(QueryDocumentSnapshot document2 : querySnapshot2){
                                       String shopId = document2.getId();
                                       db.collection("shops").document(shopId).collection("menu").document(docId).get().addOnCompleteListener(task3 -> {
                                           if(task3.isSuccessful()){
                                               DocumentSnapshot document3 = task3.getResult();
                                               String itemName = document3.getString("name");
                                               Double price = document3.getDouble("price");
                                               String imageUrl = document3.getString("imageUrl");

                                               autoPopulate(itemName, price, imageUrl, quantity);
                                           }
                                       });
                                   }
                               }
                            });
                        }
                    }
                });


            }
        });

    }

    public void autoPopulate(String itemName, Double price, String imageUrl, Double quantity){
        View orderView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.item_orderitems, binding.itemContainer, false);
        ImageView imageItem = orderView.findViewById(R.id.img_item);
        TextView productName = orderView.findViewById(R.id.product_name);
        TextView productPrice = orderView.findViewById(R.id.product_price);
        TextView quantityText = orderView.findViewById(R.id.quantity);

        Glide.with(orderView.getContext())
                .load(imageUrl)
                .into(imageItem);

        productName.setText(itemName);
        String formattedPrice = String.format("₱%.2f", price);
        productPrice.setText(formattedPrice);
        quantityText.setText(String.format("x%.0f", quantity));

        binding.itemContainer.addView(orderView);
    }
}