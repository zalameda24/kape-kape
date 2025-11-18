package com.lu.coffeecompanion;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.databinding.ActivityUserDashboardBinding;

import java.util.Objects;

public class UserDashboardActivity extends AppCompatActivity {

    ActivityUserDashboardBinding binding;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseUser currentUser;
    String userId;

    private boolean isNavigationDebounced = false;
    private static final long NAVIGATION_DEBOUNCE_DELAY_MS = 500;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_dashboard);

        //initializations
        binding = ActivityUserDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        //check if user is not logged in
        if(currentUser == null){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        //remove as comment if required
        else{
            userId = currentUser.getUid();
        }

        //if user's details is not initialized yet
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if(document.exists()){
                            if(!document.contains("name")){
                                Intent intent = new Intent(getApplicationContext(), InitializeUserDetailsActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    }
                });

        //load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        //bottom navigation listener
        BottomNavigationView bottomNavigationView = binding.bottomNavigation;
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                if (isNavigationDebounced) {
                    return false;
                }
                isNavigationDebounced = true;
                new Handler().postDelayed(() -> isNavigationDebounced = false, NAVIGATION_DEBOUNCE_DELAY_MS);

                if (item.getItemId() == R.id.nav_home) {
                    loadFragment(new HomeFragment());
                    return true;
                } else if (item.getItemId() == R.id.nav_shops) {
                    loadFragment(new ShopsFragment());
                    return true;
                } else if (item.getItemId() == R.id.nav_account) {
                    loadFragment(new AccountFragment());
                    return true;
                } else {
                    return false;
                }
            }
        });

    }
    public void loadFragment(Fragment fragment){
        if (!(getSupportFragmentManager().findFragmentById(R.id.fragment_container) != null
                && Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment_container)).getClass().equals(fragment.getClass()))) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}