package com.lu.coffeecompanion.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.lu.coffeecompanion.R;
import com.lu.coffeecompanion.models.Order;
import com.lu.coffeecompanion.models.OrderItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private Context context;
    private ArrayList<Order> orderList;
    private DatabaseReference ordersRef;

    public OrderAdapter(Context context, ArrayList<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
        this.ordersRef = FirebaseDatabase.getInstance().getReference("orders");
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(order.getTimestamp()));

        holder.tvOrderId.setText("Order #" + order.getOrderId().substring(0, Math.min(8, order.getOrderId().length())));
        holder.tvCustomerName.setText(order.getUserName() != null ? order.getUserName() : "Unknown Customer");
        holder.tvOrderDate.setText(dateStr);
        holder.tvTotal.setText(String.format("₱%.2f", order.getTotal()));
        holder.tvStatus.setText(order.getStatus() != null ? order.getStatus().toUpperCase() : "PENDING");

        // Set status color
        int statusColor;
        switch (order.getStatus() != null ? order.getStatus().toLowerCase() : "pending") {
            case "completed":
            case "delivered":
                statusColor = context.getResources().getColor(android.R.color.holo_green_dark);
                break;
            case "processing":
                statusColor = context.getResources().getColor(android.R.color.holo_orange_dark);
                break;
            case "cancelled":
                statusColor = context.getResources().getColor(android.R.color.holo_red_dark);
                break;
            default:
                statusColor = context.getResources().getColor(android.R.color.holo_blue_dark);
        }
        holder.tvStatus.setTextColor(statusColor);

        // Display order items
        holder.layoutItems.removeAllViews();
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                TextView tvItem = new TextView(context);
                tvItem.setText(String.format("• %s x%d - ₱%.2f",
                        item.getProductName(), item.getQuantity(), item.getSubtotal()));
                tvItem.setTextSize(14);
                tvItem.setPadding(0, 4, 0, 4);
                holder.layoutItems.addView(tvItem);
            }
        }

        // Click listeners
        holder.btnViewDetails.setOnClickListener(v -> showOrderDetails(order));
        holder.btnUpdateStatus.setOnClickListener(v -> showStatusDialog(order, position));
    }

    private void showOrderDetails(Order order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Order Details");

        StringBuilder details = new StringBuilder();
        details.append("Order ID: ").append(order.getOrderId()).append("\n\n");
        details.append("Customer: ").append(order.getUserName()).append("\n");
        details.append("Email: ").append(order.getUserEmail()).append("\n");
        details.append("Payment: ").append(order.getPaymentMethod() != null ? order.getPaymentMethod() : "N/A").append("\n");
        details.append("Address: ").append(order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "N/A").append("\n\n");

        details.append("Items:\n");
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                details.append(String.format("• %s\n  Qty: %d × ₱%.2f = ₱%.2f\n",
                        item.getProductName(), item.getQuantity(), item.getPrice(), item.getSubtotal()));
            }
        }

        details.append("\nTotal: ₱").append(String.format("%.2f", order.getTotal()));

        builder.setMessage(details.toString());
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showStatusDialog(Order order, int position) {
        String[] statuses = {"pending", "processing", "completed", "delivered", "cancelled"};
        String[] displayStatuses = {"Pending", "Processing", "Completed", "Delivered", "Cancelled"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Order Status");

        builder.setItems(displayStatuses, (dialog, which) -> {
            String newStatus = statuses[which];

            ordersRef.child(order.getOrderId()).child("status").setValue(newStatus)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Status updated to " + displayStatuses[which], Toast.LENGTH_SHORT).show();
                        order.setStatus(newStatus);
                        notifyItemChanged(position);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvCustomerName, tvOrderDate, tvTotal, tvStatus;
        LinearLayout layoutItems;
        Button btnViewDetails, btnUpdateStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            layoutItems = itemView.findViewById(R.id.layoutItems);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
        }
    }
}