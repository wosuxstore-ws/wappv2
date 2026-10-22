package com.wl.orders;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.wl.orders.models.Order;
import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    public interface OnOrderClick {
        void onClick(Order order);
    }

    private List<Order> orders;
    private final OnOrderClick onOrderClick;

    public OrderAdapter(List<Order> orders, OnOrderClick onOrderClick) {
        this.orders = orders;
        this.onOrderClick = onOrderClick;
    }

    public void setOrders(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.orderNumber.setText(order.orderNumber);
        holder.status.setText(order.status.toUpperCase());
        holder.status.setTextColor(statusColor(order.status));
        holder.customer.setText(order.recipientName + "  •  " + order.phone);
        holder.meta.setText(order.city + ", " + order.province + "  •  " + order.paymentLabel);
        holder.items.setText(order.itemsSummary);
        holder.items.setVisibility(order.itemsSummary.isEmpty() ? View.GONE : View.VISIBLE);
        holder.total.setText(Format.money(order.grandTotal));
        holder.date.setText(Format.dateTime(order.createdAt));
        holder.itemView.setOnClickListener(v -> onOrderClick.onClick(order));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static int statusColor(String status) {
        switch (status) {
            case "pending":    return Color.parseColor("#E65100"); // orange
            case "paid":       return Color.parseColor("#2080BB"); // brand blue
            case "processing": return Color.parseColor("#6A1B9A"); // purple
            case "shipped":    return Color.parseColor("#00838F"); // teal
            case "delivered":  return Color.parseColor("#000000"); // black (done)
            case "cancelled":  return Color.parseColor("#C62828"); // red
            default:           return Color.parseColor("#616161"); // grey
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNumber, status, customer, meta, items, total, date;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            status = itemView.findViewById(R.id.orderStatus);
            customer = itemView.findViewById(R.id.orderCustomer);
            meta = itemView.findViewById(R.id.orderMeta);
            items = itemView.findViewById(R.id.orderItems);
            total = itemView.findViewById(R.id.orderTotal);
            date = itemView.findViewById(R.id.orderDate);
        }
    }
}
