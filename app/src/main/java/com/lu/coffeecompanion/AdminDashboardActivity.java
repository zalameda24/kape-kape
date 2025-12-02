package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lu.coffeecompanion.databinding.ActivityAdminDashboardBinding;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;
    private DatabaseReference dbRef;
    private FirebaseFirestore firestore;

    private TextView tvTotalGcashPayments, tvTotalCodPayments, tvOverallTotal;
    private BarChart barChart;

    private TextView tvChartGcashTotal, tvChartCodTotal, tvChartOverallTotal, tvDateRange;
    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat dateFormat, displayDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbRef = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();

        // Date formatters
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        startCalendar = Calendar.getInstance();
        startCalendar.add(Calendar.DAY_OF_YEAR, -6);
        endCalendar = Calendar.getInstance();

        // Initialize views
        tvTotalGcashPayments = binding.tvTotalGcashPayments;
        tvTotalCodPayments = binding.tvTotalCodPayments;
        tvOverallTotal = binding.tvOverallTotal;
        barChart = binding.barChart;

        tvChartGcashTotal = binding.tvChartGcashTotal;
        tvChartCodTotal = binding.tvChartCodTotal;
        tvChartOverallTotal = binding.tvChartOverallTotal;
        tvDateRange = binding.tvDateRange;

        // Date pickers
        binding.btnStartDate.setOnClickListener(v -> showStartDatePicker());
        binding.btnEndDate.setOnClickListener(v -> showEndDatePicker());
        updateDateButtons();

        // Chart
        setupBarChart();

        // -------------------------
        // CARD CLICKS
        // -------------------------
        binding.cardUserManagement.setOnClickListener(v -> startActivity(new Intent(this, AdminManageUsersActivity.class)));
        binding.cardPayments.setOnClickListener(v -> startActivity(new Intent(this, AdminPaymentsActivity.class)));
        binding.cardAddCodPayment.setOnClickListener(v -> startActivity(new Intent(this, AdminAddCodPaymentActivity.class)));
        binding.cardInventory.setOnClickListener(v -> startActivity(new Intent(this, AdminInventoryActivity.class)));

        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());

        // Load data
        loadUserStats();
        loadPaymentStatistics();
    }

    // -------------------------
    // DATE PICKERS
    // -------------------------
    private void showStartDatePicker() {
        DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, day) -> {
            startCalendar.set(year, month, day);
            if (startCalendar.after(endCalendar)) endCalendar.setTime(startCalendar.getTime());
            updateDateButtons();
            loadPaymentStatistics();
        }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH));
        dp.getDatePicker().setMaxDate(System.currentTimeMillis());
        dp.show();
    }

    private void showEndDatePicker() {
        DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, day) -> {
            endCalendar.set(year, month, day);
            if (endCalendar.before(startCalendar)) startCalendar.setTime(endCalendar.getTime());
            updateDateButtons();
            loadPaymentStatistics();
        }, endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH));
        dp.getDatePicker().setMaxDate(System.currentTimeMillis());
        dp.show();
    }

    private void updateDateButtons() {
        binding.btnStartDate.setText(displayDateFormat.format(startCalendar.getTime()));
        binding.btnEndDate.setText(displayDateFormat.format(endCalendar.getTime()));
        tvDateRange.setText(displayDateFormat.format(startCalendar.getTime()) + " - " + displayDateFormat.format(endCalendar.getTime()));
    }

    // -------------------------
    // BAR CHART SETUP
    // -------------------------
    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setPinchZoom(true);
        barChart.setDrawValueAboveBar(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#5D4037"));
        xAxis.setLabelRotationAngle(-45f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setTextColor(Color.parseColor("#5D4037"));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "₱" + String.format("%.0f", value);
            }
        });

        barChart.getAxisRight().setEnabled(false);

        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.parseColor("#5D4037"));
        legend.setTextSize(12f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        barChart.animateY(800);
    }

    // -------------------------
    // USER STATS
    // -------------------------
    private void loadUserStats() {
        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    int totalUsers = (int) snapshot.getChildrenCount();
                    int activeUsers = 0;
                    for (DataSnapshot user : snapshot.getChildren()) {
                        Boolean blocked = user.child("blocked").getValue(Boolean.class);
                        if (blocked == null || !blocked) activeUsers++;
                    }
                    binding.tvTotalUsers.setText(String.valueOf(totalUsers));
                    binding.tvActiveUsers.setText(String.valueOf(activeUsers));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("AdminDashboard", "Error loading user stats: " + error.getMessage());
            }
        });
    }

    // -------------------------
    // PAYMENT STATS
    // -------------------------
    private void loadPaymentStatistics() {
        Map<String, Double> gcashByDate = new TreeMap<>();
        Map<String, Double> codByDate = new TreeMap<>();
        Calendar current = (Calendar) startCalendar.clone();
        while (!current.after(endCalendar)) {
            String key = dateFormat.format(current.getTime());
            gcashByDate.put(key, 0.0);
            codByDate.put(key, 0.0);
            current.add(Calendar.DAY_OF_YEAR, 1);
        }

        Calendar startOfDay = (Calendar) startCalendar.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0); startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0); startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) endCalendar.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23); endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59); endOfDay.set(Calendar.MILLISECOND, 999);

        loadAllPaymentsDirect(gcashByDate, codByDate, startOfDay.getTime(), endOfDay.getTime());
    }

    private void loadAllPaymentsDirect(Map<String, Double> gcashByDate, Map<String, Double> codByDate,
                                       Date startDate, Date endDate) {
        final double[] allTimeGcash = {0.0}, allTimeCod = {0.0};
        final double[] chartGcash = {0.0}, chartCod = {0.0};

        firestore.collection("orders").get().addOnSuccessListener(ordersSnapshot -> {
            for (QueryDocumentSnapshot doc : ordersSnapshot) {
                String method = doc.getString("paymentMethod");
                Double total = doc.getDouble("totalPrice");
                Timestamp ts = doc.getTimestamp("timestamp");
                if (total == null || ts == null) continue;

                Date orderDate = ts.toDate();
                if ("GCash".equals(method)) {
                    allTimeGcash[0] += total;
                    if (!orderDate.before(startDate) && !orderDate.after(endDate)) {
                        chartGcash[0] += total;
                        String key = dateFormat.format(orderDate);
                        gcashByDate.put(key, gcashByDate.getOrDefault(key, 0.0) + total);
                    }
                }
            }

            firestore.collection("cod_payments").get().addOnSuccessListener(codSnapshot -> {
                for (QueryDocumentSnapshot doc : codSnapshot) {
                    Double amount = doc.getDouble("amount");
                    Timestamp ts = doc.getTimestamp("timestamp");
                    if (amount == null || ts == null) continue;

                    allTimeCod[0] += amount;
                    Date paymentDate = ts.toDate();
                    if (!paymentDate.before(startDate) && !paymentDate.after(endDate)) {
                        chartCod[0] += amount;
                        String key = dateFormat.format(paymentDate);
                        codByDate.put(key, codByDate.getOrDefault(key, 0.0) + amount);
                    }
                }

                updatePaymentStatistics(allTimeGcash[0], allTimeCod[0]);
                updateBarChart(gcashByDate, codByDate, chartGcash[0], chartCod[0]);
            });

        });
    }

    private void updateBarChart(Map<String, Double> gcashByDate, Map<String, Double> codByDate,
                                double gcashTotal, double codTotal) {
        ArrayList<BarEntry> gcashEntries = new ArrayList<>();
        ArrayList<BarEntry> codEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        SimpleDateFormat chartFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        int index = 0;
        for (String dateKey : gcashByDate.keySet()) {
            gcashEntries.add(new BarEntry(index, gcashByDate.get(dateKey).floatValue()));
            codEntries.add(new BarEntry(index, codByDate.get(dateKey).floatValue()));
            try {
                labels.add(chartFormat.format(dateFormat.parse(dateKey)));
            } catch (Exception e) {
                labels.add(dateKey);
            }
            index++;
        }

        BarDataSet gcashSet = new BarDataSet(gcashEntries, "GCash");
        gcashSet.setColor(Color.parseColor("#1565C0"));
        gcashSet.setValueTextColor(Color.parseColor("#5D4037"));
        gcashSet.setValueTextSize(9f);
        gcashSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value == 0 ? "" : "₱" + String.format("%.0f", value);
            }
        });

        BarDataSet codSet = new BarDataSet(codEntries, "COD");
        codSet.setColor(Color.parseColor("#EF6C00"));
        codSet.setValueTextColor(Color.parseColor("#5D4037"));
        codSet.setValueTextSize(9f);
        codSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value == 0 ? "" : "₱" + String.format("%.0f", value);
            }
        });

        BarData barData = new BarData(gcashSet, codSet);
        float groupSpace = 0.12f, barSpace = 0.02f, barWidth = 0.42f;
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(labels.size());
        barChart.getXAxis().setAxisMinimum(-0.5f);
        barChart.getXAxis().setAxisMaximum(labels.size() - 0.5f);
        if (labels.size() > 1) barChart.groupBars(-0.5f, groupSpace, barSpace);
        barChart.invalidate();

        updateChartTotals(gcashTotal, codTotal);
    }

    private void updateChartTotals(double gcash, double cod) {
        double overall = gcash + cod;
        tvChartGcashTotal.setText(String.format("₱%.2f", gcash));
        tvChartCodTotal.setText(String.format("₱%.2f", cod));
        tvChartOverallTotal.setText(String.format("₱%.2f", overall));
    }

    private void updatePaymentStatistics(double gcash, double cod) {
        double overall = gcash + cod;
        tvTotalGcashPayments.setText(String.format("₱%.2f", gcash));
        tvTotalCodPayments.setText(String.format("₱%.2f", cod));
        tvOverallTotal.setText(String.format("₱%.2f", overall));
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
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
