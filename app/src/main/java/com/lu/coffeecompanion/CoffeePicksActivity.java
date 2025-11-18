package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.lu.coffeecompanion.databinding.ActivityCoffeePicksBinding;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

public class CoffeePicksActivity extends AppCompatActivity {

    ActivityCoffeePicksBinding binding;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String pickName = null, selectedShop = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCoffeePicksBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        String userId = null;
        if (currentUser == null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        Intent intent = getIntent();
        String docId = intent.getStringExtra("docId");

        if (docId != null) {
            db.collection("coffees").document(docId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    pickName = document.getString("name");
                    binding.title.setText(pickName);
                }
            });
        } else {
            Toast.makeText(this, "Item fetching failed. Please try again.", Toast.LENGTH_SHORT).show();
        }

        binding.tabLayout.removeAllTabs();


        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedShop = tab.getText().toString();
                fetchMenu(selectedShop);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        fetchShops();

        binding.back.setOnClickListener(v -> {
            finish();
        });

    }

    private void fetchShops(){
        binding.tabLayout.removeAllTabs();
        db.collection("shops").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                for (QueryDocumentSnapshot document : querySnapshot) {
                    List<String> shopList = new ArrayList<>();
                    String shopName = document.getString("name");
                    String docId = document.getId();
                    db.collection("shops").document(docId).collection("menu").get().addOnCompleteListener(task1 -> {
                        if(task1.isSuccessful()){
                            QuerySnapshot querySnapshot1 = task1.getResult();
                            for(QueryDocumentSnapshot document1 : querySnapshot1){
                                String category = document1.getString("category");
                                if(category.equals(pickName)){
                                    if(!shopList.contains(shopName)){
                                        shopList.add(shopName);
                                        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(shopName));
                                    }
                                }
                            }
                        }
                    });

                }
            }
        });
    }

    private void fetchMenu(String selectedShop){
        binding.mainContainer.removeAllViews();
        binding.progressBar.setVisibility(View.VISIBLE);
        db.collection("shops").whereEqualTo("name", selectedShop).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                QuerySnapshot querySnapshot = task.getResult();
                for(QueryDocumentSnapshot document : querySnapshot){
                    String docId = document.getId();
                    db.collection("shops").document(docId).collection("menu").get().addOnCompleteListener(task1 -> {
                        if(task1.isSuccessful()){
                            QuerySnapshot querySnapshot1 = task1.getResult();
                            for(QueryDocumentSnapshot document1 : querySnapshot1){
                                String category = document1.getString("category");
                                if(category.equals(pickName)){
                                    String itemName = (String) document1.get("name");
                                    double itemPrice = document1.getDouble("price");
                                    String itemDescription = (String) document1.get("description");
                                    String imageUrl = (String) document1.get("imageUrl");
                                    String itemId = document1.getId();

                                    View itemView = getLayoutInflater().inflate(R.layout.item_horizontalmenu, null);
                                    TextView nameTextView = itemView.findViewById(R.id.itemName);
                                    TextView priceTextView = itemView.findViewById(R.id.itemPrice);
                                    TextView descriptionTextView = itemView.findViewById(R.id.itemDescription);
                                    ImageView imageView = itemView.findViewById(R.id.imageView);
                                    ImageButton plusButton = itemView.findViewById(R.id.plusButton);

                                    LinearLayout itemContainer = itemView.findViewById(R.id.itemContainer);

                                    nameTextView.setText(itemName);
                                    priceTextView.setText("₱" + String.format("%.2f", itemPrice));
                                    descriptionTextView.setText(itemDescription);
                                    Glide.with(getApplicationContext()).load(imageUrl).into(imageView);
                                    binding.mainContainer.addView(itemView);

                                    itemContainer.setOnClickListener(v -> {
                                        Intent intent = new Intent(getApplicationContext(), ItemActivity.class);
                                        intent.putExtra("docId", itemId);
                                        intent.putExtra("documentId", docId);
                                        startActivity(intent);
                                    });
                                    plusButton.setOnClickListener(v -> {
                                        Intent intent = new Intent(getApplicationContext(), ItemActivity.class);
                                        intent.putExtra("docId", itemId);
                                        intent.putExtra("documentId", docId);
                                        startActivity(intent);
                                    });
                                }
                            }
                        }
                    });
                }
                binding.progressBar.setVisibility(View.GONE);
            }
            else{
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

}