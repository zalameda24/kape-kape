package com.lu.coffeecompanion.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lu.coffeecompanion.R;
import com.lu.coffeecompanion.models.InventoryItem;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder> {

    private Context context;
    private List<InventoryItem> itemList;
    private FirebaseFirestore firestore;

    public InventoryAdapter(Context context, List<InventoryItem> itemList) {
        this.context = context;
        this.itemList = itemList;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_inventory, parent, false);
        return new InventoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        InventoryItem item = itemList.get(position);

        holder.tvName.setText(item.getName());
        holder.tvPrice.setText("₱" + String.format("%.2f", item.getPrice()));
        holder.tvQuantity.setText("Stock: " + item.getQuantity());

        // Load image with Glide
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.image_placeholder_bg)
                    .into(holder.ivProduct);
        } else {
            holder.ivProduct.setImageResource(R.drawable.image_placeholder_bg);
        }

        // Add Stock button
        holder.btnAddStock.setOnClickListener(v -> showAddStockDialog(item, holder));

        // Delete button
        holder.btnDelete.setOnClickListener(v -> showDeleteConfirmation(item, position));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // ViewHolder
    public static class InventoryViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivProduct;
        TextView tvName, tvPrice, tvQuantity;
        Button btnAddStock, btnDelete;

        public InventoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardInventoryItem);
            ivProduct = itemView.findViewById(R.id.ivProductImage);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvProductQuantity);
            btnAddStock = itemView.findViewById(R.id.btnAddStock);
            btnDelete = itemView.findViewById(R.id.btnDeleteStock);
        }
    }

    // Show Add Stock dialog
    private void showAddStockDialog(InventoryItem item, InventoryViewHolder holder) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_stock, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        TextView tvProductName = view.findViewById(R.id.tvStockProductName);
        TextView etStockAmount = view.findViewById(R.id.etStockAmount);
        Button btnConfirm = view.findViewById(R.id.btnConfirmStock);

        tvProductName.setText(item.getName());

        btnConfirm.setOnClickListener(v -> {
            String qtyStr = etStockAmount.getText().toString().trim();
            if (qtyStr.isEmpty()) {
                Toast.makeText(context, "Enter quantity to add", Toast.LENGTH_SHORT).show();
                return;
            }
            int qty = Integer.parseInt(qtyStr);
            int newQty = item.getQuantity() + qty;

            firestore.collection("inventory").document(item.getId())
                    .update("quantity", newQty)
                    .addOnSuccessListener(aVoid -> {
                        item.setQuantity(newQty);
                        holder.tvQuantity.setText("Stock: " + newQty);
                        Toast.makeText(context, "Stock updated!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });

        dialog.show();
    }

    // Show Delete Confirmation dialog
    private void showDeleteConfirmation(InventoryItem item, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + item.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firestore.collection("inventory").document(item.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                itemList.remove(position);
                                notifyItemRemoved(position);
                                Toast.makeText(context, "Product deleted!", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
