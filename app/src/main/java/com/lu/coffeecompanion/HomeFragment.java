package com.lu.coffeecompanion;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ActivityUserDashboardBinding;
import com.lu.coffeecompanion.databinding.HomeFragmentBinding;

public class HomeFragment extends Fragment {


    HomeFragmentBinding binding;
    FirebaseFirestore db;

    FirebaseAuth auth;
    FirebaseUser currentUser;
    String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //initializations
        binding = HomeFragmentBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        //check if user is not logged in
        if(currentUser == null){
            Intent intent = new Intent(requireContext(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
        //remove as comment if required
        /* else{
            userId = currentUser.getUid();
        }
        */

        //swipe refresh listener
        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                binding.itemContainer1.removeAllViews();
                //binding.itemContainer2.removeAllViews();
                binding.itemContainer3.removeAllViews();
                fetchData();
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        binding.cart.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CartActivity.class);
            startActivity(intent);
        });

        //generate items from firebase
        fetchData();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //generate items from database
    private void fetchData() {
        fetchHorizontalScrollView(binding.itemContainer1, "coffees");
        //fetchHorizontalScrollView(binding.itemContainer2, "bestwithcoffee");
        fetchVerticalScrollView(binding.itemContainer3, "shops");
    }
    private void fetchHorizontalScrollView(LinearLayout itemContainer, String collectionPath) {
        db.collection(collectionPath).orderBy("order").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                String coffeeName = document.getString("name");
                String imageUrl = document.getString("imageUrl");
                String docId = document.getId();

                View coffeeLayout = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_coffee, itemContainer, false);

                ImageView coffeeImage = coffeeLayout.findViewById(R.id.coffee_image);
                TextView coffeeText = coffeeLayout.findViewById(R.id.coffee_name);

                coffeeText.setText(coffeeName);
                Glide.with(requireContext()).load(imageUrl).into(coffeeImage);

                coffeeLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(requireContext(), CoffeePicksActivity.class);
                    intent.putExtra("docId", docId);
                    startActivity(intent);
                });

                itemContainer.addView(coffeeLayout);
            }
        });
    }
    private void fetchBanner(ImageView imageView, String bannerName){
        db.collection("banners").document(bannerName)
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String bannerImageUrl = documentSnapshot.getString("imageUrl");
                        Glide.with(requireContext()).load(bannerImageUrl).into(imageView);
                    }
                });
    }
    private void fetchVerticalScrollView(LinearLayout itemContainer, String collectionPath) {
        ProgressBar progressBar = new ProgressBar(requireContext());
        LinearLayout.LayoutParams progressBarParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        progressBar.setLayoutParams(progressBarParams);
        itemContainer.addView(progressBar);
        progressBar.setVisibility(View.VISIBLE);

        db.collection(collectionPath)
                .orderBy("order")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    itemContainer.removeAllViews();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String coffeeName = document.getString("name");
                        String imageUrl = document.getString("imageUrl");
                        String location = document.getString("location");

                        Long ratingValue = document.getLong("rating");
                        double doubleRatingValue = ratingValue.doubleValue();
                        String rating = String.format("%.1f", doubleRatingValue);

                        View coffeeLayout = LayoutInflater.from(requireContext())
                                .inflate(R.layout.item_bestwithcoffee, itemContainer, false);

                        ImageView coffeeImage = coffeeLayout.findViewById(R.id.coffee_image);
                        TextView coffeeText = coffeeLayout.findViewById(R.id.coffee_name);
                        TextView locationText = coffeeLayout.findViewById(R.id.location_text);

                        coffeeText.setText(coffeeName);
                        locationText.setText(location);
                        Glide.with(requireContext()).load(imageUrl).into(coffeeImage);

                        coffeeLayout.setOnClickListener(v -> {
                            Intent intent = new Intent(requireContext(), ShopMenuActivity.class);
                            intent.putExtra("documentId", document.getId());
                            startActivity(intent);
                        });

                        itemContainer.addView(coffeeLayout);
                    }
                });
    }
}
