package com.lu.coffeecompanion;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.database.*;

import com.lu.coffeecompanion.databinding.ActivityAdminDashboardBinding;

import java.text.SimpleDateFormat;
import java.util.*;

public class AdminDashboardActivity extends AppCompatActivity {

    ActivityAdminDashboardBinding binding;
    private DatabaseReference dbRef;
    private FirebaseFirestore firestore;

    private TextView tvTotalGcashPayments;
    private TextView tvTotalCodPayments;
    private TextView tvOverallTotal;
    private BarChart barChart;

    private Button btnStartDate, btnEndDate;
    private TextView tvChartGcashTotal, tvChartCodTotal, tvChartOverallTotal;
    private TextView tvDateRange;

    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat dateFormat, displayDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbRef = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();

        // Initialize date formatters
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        // Initialize calendars - default to last 7 days
        startCalendar = Calendar.getInstance();
        startCalendar.add(Calendar.DAY_OF_YEAR, -6);
        endCalendar = Calendar.getInstance();

        // Initialize views
        tvTotalGcashPayments = findViewById(R.id.tvTotalGcashPayments);
        tvTotalCodPayments = findViewById(R.id.tvTotalCodPayments);
        tvOverallTotal = findViewById(R.id.tvOverallTotal);
        barChart = findViewById(R.id.barChart);

        btnStartDate = findViewById(R.id.btnStartDate);
        btnEndDate = findViewById(R.id.btnEndDate);
        tvChartGcashTotal = findViewById(R.id.tvChartGcashTotal);
        tvChartCodTotal = findViewById(R.id.tvChartCodTotal);
        tvChartOverallTotal = findViewById(R.id.tvChartOverallTotal);
        tvDateRange = findViewById(R.id.tvDateRange);

        // Setup date buttons
        updateDateButtons();
        btnStartDate.setOnClickListener(v -> showStartDatePicker());
        btnEndDate.setOnClickListener(v -> showEndDatePicker());

        // Setup bar chart
        setupBarChart();

        // Card clicks
        CardView userManagementCard = findViewById(R.id.cardUserManagement);
        userManagementCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminManageUsersActivity.class);
            startActivity(intent);
        });

        CardView paymentsCard = findViewById(R.id.cardPayments);
        paymentsCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminPaymentsActivity.class);
            startActivity(intent);
        });

        CardView addCodPaymentCard = findViewById(R.id.cardAddCodPayment);
        addCodPaymentCard.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminAddCodPaymentActivity.class);
            startActivity(intent);
        });

        binding.btnLogout.setOnClickListener(v -> showLogoutDialog());

        // Load data
        loadUserStats();
        loadPaymentStatistics();
    }

    private void showStartDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    startCalendar.set(year, month, dayOfMonth);
                    // Ensure start is not after end
                    if (startCalendar.after(endCalendar)) {
                        endCalendar.setTime(startCalendar.getTime());
                    }
                    updateDateButtons();
                    loadPaymentStatistics();
                },
                startCalendar.get(Calendar.YEAR),
                startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showEndDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    endCalendar.set(year, month, dayOfMonth);
                    if (endCalendar.before(startCalendar)) {
                        startCalendar.setTime(endCalendar.getTime());
                    }
                    updateDateButtons();
                    loadPaymentStatistics();
                },
                endCalendar.get(Calendar.YEAR),
                endCalendar.get(Calendar.MONTH),
                endCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void updateDateButtons() {
        btnStartDate.setText(displayDateFormat.format(startCalendar.getTime()));
        btnEndDate.setText(displayDateFormat.format(endCalendar.getTime()));

        String rangeText = displayDateFormat.format(startCalendar.getTime()) +
                " - " + displayDateFormat.format(endCalendar.getTime());
        tvDateRange.setText(rangeText);
    }

    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(false);
        barChart.setPinchZoom(true);
        barChart.setScaleEnabled(true);
        barChart.setDrawValueAboveBar(true);
        barChart.setExtraBottomOffset(10f);
        barChart.setExtraTopOffset(10f);

        // X-Axis settings
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#5D4037"));
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(-45f);

        // Y-Axis settings
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

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.parseColor("#5D4037"));
        legend.setTextSize(12f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);

        barChart.animateY(800);
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

    /**
     * Main payment loader.
     * Strategy:
     * 1) Query All-time totals for GCash (orders where paymentMethod == "GCash") and cod_payments (all docs).
     * 2) Query range-filtered documents for chart grouping (use Firestore range queries on "timestamp")
     * This ensures the chart "period totals" match values derived from documents' timestamps,
     * and the all-time totals remain complete.
     */
    private void loadPaymentStatistics() {
        // Prepare maps for date-range grouping (initialize every day between start and end with 0)
        Map<String, Double> gcashByDate = new TreeMap<>();
        Map<String, Double> codByDate = new TreeMap<>();

        Calendar current = (Calendar) startCalendar.clone();
        while (!current.after(endCalendar)) {
            String key = dateFormat.format(current.getTime());
            gcashByDate.put(key, 0.0);
            codByDate.put(key, 0.0);
            current.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Start / end boundaries for Firestore queries (as Date objects)
        Calendar startOfDay = (Calendar) startCalendar.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) endCalendar.clone();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);

        Date startDate = startOfDay.getTime();
        Date endDate = endOfDay.getTime();

        // 1) ALL-TIME totals for GCash (for tvTotalGcashPayments)
        firestore.collection("orders")
                .whereEqualTo("paymentMethod", "GCash")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalGcashAllTime = 0.0;
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Double totalPrice = document.getDouble("totalPrice");
                        if (totalPrice != null) {
                            totalGcashAllTime += totalPrice;
                        }
                    }

                    // 2) ALL-TIME totals for COD (tvTotalCodPayments)
                    double finalTotalGcashAllTime = totalGcashAllTime;
                    double finalTotalGcashAllTime1 = totalGcashAllTime;
                    firestore.collection("cod_payments")
                            .get()
                            .addOnSuccessListener(codSnapshot -> {
                                double totalCodAllTime = 0.0;
                                for (QueryDocumentSnapshot doc : codSnapshot) {
                                    Double amount = doc.getDouble("amount");
                                    if (amount != null) totalCodAllTime += amount;
                                }

                                // Update all-time display
                                updatePaymentStatistics(finalTotalGcashAllTime, totalCodAllTime);

                                // Now load RANGE-FILTERED docs for chart grouping (GCash orders in range)
                                loadRangeForChart(gcashByDate, codByDate, startDate, endDate);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("AdminDashboard", "Error loading cod all-time: " + e.getMessage());
                                // Still proceed to build chart using range queries
                                updatePaymentStatistics(finalTotalGcashAllTime1, 0.0);
                                loadRangeForChart(gcashByDate, codByDate, startDate, endDate);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("AdminDashboard", "Error loading gcash all-time: " + e.getMessage());
                    // Fallback: still load range for the chart (maybe gcash all-time failed)
                    loadRangeForChart(gcashByDate, codByDate, startDate, endDate);
                });
    }

    /**
     * Loads only documents in the selected date range and groups them by yyyy-MM-dd for the chart.
     * This ensures that chart totals and chart graph reflect the documents' timestamps just like the all-time totals
     * reflect documents in their collections.
     */
    private void loadRangeForChart(Map<String, Double> gcashByDate, Map<String, Double> codByDate,
                                   Date startDate, Date endDate) {

        // Query GCash orders within range
        firestore.collection("orders")
                .whereEqualTo("paymentMethod", "GCash")
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .get()
                .addOnSuccessListener(gcashRangeSnapshots -> {
                    double chartGcashTotal = 0.0;

                    for (QueryDocumentSnapshot document : gcashRangeSnapshots) {
                        Double totalPrice = document.getDouble("totalPrice");
                        if (totalPrice == null) continue;

                        chartGcashTotal += totalPrice;

                        Timestamp ts = document.getTimestamp("timestamp");
                        if (ts != null) {
                            String dateKey = dateFormat.format(ts.toDate());
                            if (gcashByDate.containsKey(dateKey)) {
                                gcashByDate.put(dateKey, gcashByDate.get(dateKey) + totalPrice);
                            }
                        }
                    }

                    // Query COD payments within range
                    double finalChartGcashTotal = chartGcashTotal;
                    double finalChartGcashTotal1 = chartGcashTotal;
                    firestore.collection("cod_payments")
                            .whereGreaterThanOrEqualTo("timestamp", startDate)
                            .whereLessThanOrEqualTo("timestamp", endDate)
                            .get()
                            .addOnSuccessListener(codRangeSnapshots -> {
                                double chartCodTotal = 0.0;

                                for (QueryDocumentSnapshot doc : codRangeSnapshots) {
                                    Double amount = doc.getDouble("amount");
                                    if (amount == null) continue;

                                    chartCodTotal += amount;

                                    Timestamp ts = doc.getTimestamp("timestamp");
                                    if (ts != null) {
                                        String dateKey = dateFormat.format(ts.toDate());
                                        if (codByDate.containsKey(dateKey)) {
                                            codByDate.put(dateKey, codByDate.get(dateKey) + amount);
                                        }
                                    }
                                }

                                // Now update the chart using the grouped maps
                                updateBarChart(gcashByDate, codByDate, finalChartGcashTotal1, chartCodTotal);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("AdminDashboard", "Error loading cod range: " + e.getMessage());
                                // Still show chart with gcash data only
                                updateBarChart(gcashByDate, codByDate, finalChartGcashTotal, 0.0);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("AdminDashboard", "Error loading gcash range: " + e.getMessage());
                    // If range query fails, try fallback: fetch all GCash and filter locally (less preferred)
                    fallbackGcahRangeLocalFilter(gcashByDate, codByDate, startDate, endDate);
                });
    }

    /**
     * Fallback: if range query failed for some reason, fetch all GCash and locally filter by timestamp range.
     * This preserves functionality even when Firestore range queries are restricted.
     */
    private void fallbackGcahRangeLocalFilter(Map<String, Double> gcashByDate, Map<String, Double> codByDate,
                                              Date startDate, Date endDate) {
        firestore.collection("orders")
                .whereEqualTo("paymentMethod", "GCash")
                .get()
                .addOnSuccessListener(allSnapshots -> {
                    double chartGcashTotal = 0.0;

                    for (QueryDocumentSnapshot document : allSnapshots) {
                        Double totalPrice = document.getDouble("totalPrice");
                        Timestamp ts = document.getTimestamp("timestamp");
                        if (totalPrice == null || ts == null) continue;

                        Date d = ts.toDate();
                        if (!d.before(startDate) && !d.after(endDate)) {
                            chartGcashTotal += totalPrice;
                            String dateKey = dateFormat.format(d);
                            if (gcashByDate.containsKey(dateKey)) {
                                gcashByDate.put(dateKey, gcashByDate.get(dateKey) + totalPrice);
                            }
                        }
                    }

                    // Then load COD range (try proper range query first)
                    double finalChartGcashTotal = chartGcashTotal;
                    double finalChartGcashTotal1 = chartGcashTotal;
                    firestore.collection("cod_payments")
                            .whereGreaterThanOrEqualTo("timestamp", startDate)
                            .whereLessThanOrEqualTo("timestamp", endDate)
                            .get()
                            .addOnSuccessListener(codRangeSnapshots -> {
                                double chartCodTotal = 0.0;
                                for (QueryDocumentSnapshot doc : codRangeSnapshots) {
                                    Double amount = doc.getDouble("amount");
                                    if (amount == null) continue;
                                    chartCodTotal += amount;
                                    Timestamp ts = doc.getTimestamp("timestamp");
                                    if (ts != null) {
                                        String dateKey = dateFormat.format(ts.toDate());
                                        if (codByDate.containsKey(dateKey)) {
                                            codByDate.put(dateKey, codByDate.get(dateKey) + amount);
                                        }
                                    }
                                }
                                updateBarChart(gcashByDate, codByDate, finalChartGcashTotal1, chartCodTotal);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("AdminDashboard", "Fallback COD range failed: " + e.getMessage());
                                updateBarChart(gcashByDate, codByDate, finalChartGcashTotal, 0.0);
                            });

                }).addOnFailureListener(e -> {
                    Log.e("AdminDashboard", "Fallback gcash fetch failed: " + e.getMessage());
                    updateBarChart(gcashByDate, codByDate, 0.0, 0.0);
                });
    }

    private void updateBarChart(Map<String, Double> gcashByDate, Map<String, Double> codByDate,
                                double gcashTotal, double codTotal) {
        ArrayList<BarEntry> gcashEntries = new ArrayList<>();
        ArrayList<BarEntry> codEntries = new ArrayList<>();
        ArrayList<String> dateLabels = new ArrayList<>();

        SimpleDateFormat chartDateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        int index = 0;
        for (String dateKey : gcashByDate.keySet()) {
            float gcashValue = gcashByDate.get(dateKey).floatValue();
            float codValue = codByDate.get(dateKey).floatValue();

            gcashEntries.add(new BarEntry(index, gcashValue));
            codEntries.add(new BarEntry(index, codValue));

            try {
                Date date = dateFormat.parse(dateKey);
                dateLabels.add(chartDateFormat.format(date));
            } catch (Exception e) {
                dateLabels.add(dateKey);
            }

            index++;
        }

        BarDataSet gcashDataSet = new BarDataSet(gcashEntries, "GCash");
        gcashDataSet.setColor(Color.parseColor("#1565C0"));
        gcashDataSet.setValueTextColor(Color.parseColor("#5D4037"));
        gcashDataSet.setValueTextSize(9f);
        gcashDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return "₱" + String.format("%.0f", value);
            }
        });

        BarDataSet codDataSet = new BarDataSet(codEntries, "COD");
        codDataSet.setColor(Color.parseColor("#EF6C00"));
        codDataSet.setValueTextColor(Color.parseColor("#5D4037"));
        codDataSet.setValueTextSize(9f);
        codDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";
                return "₱" + String.format("%.0f", value);
            }
        });

        BarData barData = new BarData(gcashDataSet, codDataSet);

        float groupSpace = 0.12f;
        float barSpace = 0.02f;
        float barWidth = 0.42f; // two bars grouped -> 0.42 + 0.02 + 0.42 = ~0.86 < 1.0
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        barChart.getXAxis().setLabelCount(dateLabels.size());
        barChart.getXAxis().setAxisMinimum(-0.5f);
        barChart.getXAxis().setAxisMaximum(dateLabels.size() - 0.5f);

        if (dateLabels.size() > 1) {
            // groupBars requires axisMin/Max to be set first
            barChart.groupBars(-0.5f, groupSpace, barSpace);
        }

        barChart.invalidate();

        // Update totals shown under chart
        updateChartTotals(gcashTotal, codTotal);
    }

    private void updateChartTotals(double gcashTotal, double codTotal) {
        double overallTotal = gcashTotal + codTotal;

        if (tvChartGcashTotal != null) {
            tvChartGcashTotal.setText(String.format("₱%.2f", gcashTotal));
        }

        if (tvChartCodTotal != null) {
            tvChartCodTotal.setText(String.format("₱%.2f", codTotal));
        }

        if (tvChartOverallTotal != null) {
            tvChartOverallTotal.setText(String.format("₱%.2f", overallTotal));
        }
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
