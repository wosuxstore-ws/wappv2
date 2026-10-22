package com.wl.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OrderDetailActivity extends AppCompatActivity {

    private TextView orderNumberView, statusView, dateView, addressView, totalsView;
    private LinearLayout itemsContainer;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        orderNumberView = findViewById(R.id.detailOrderNumber);
        statusView = findViewById(R.id.detailStatus);
        dateView = findViewById(R.id.detailDate);
        addressView = findViewById(R.id.detailAddress);
        totalsView = findViewById(R.id.detailTotal);
        itemsContainer = findViewById(R.id.itemsContainer);
        progressBar = findViewById(R.id.detailProgressBar);

        int orderId = getIntent().getIntExtra("order_id", -1);
        sessionManager = new SessionManager(this);

        if (orderId <= 0 || !sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Missing order", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        new OrdersManager(this, sessionManager.getToken()).getOrderDetail(orderId, new ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if (!response.getBoolean("success")) {
                        Toast.makeText(OrderDetailActivity.this, response.optString("message", "Order not found"), Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    show(response.getJSONObject("order"));
                } catch (JSONException e) {
                    Toast.makeText(OrderDetailActivity.this, "Unexpected response from server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                if (OrdersManager.SESSION_EXPIRED.equals(message)) {
                    sessionManager.clear();
                    Toast.makeText(OrderDetailActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(OrderDetailActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return;
                }
                Toast.makeText(OrderDetailActivity.this, "Couldn't reach server: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void show(JSONObject order) throws JSONException {
        orderNumberView.setText(Format.str(order, "order_number"));
        statusView.setText(Format.str(order, "status").toUpperCase());
        statusView.setTextColor(OrderAdapter.statusColor(Format.str(order, "status")));
        dateView.setText("Placed " + Format.dateTime(Format.str(order, "created_at")));

        // Customer + delivery address
        StringBuilder addr = new StringBuilder();
        addr.append(Format.str(order, "recipient_name")).append("\n");
        addr.append("Phone: ").append(Format.str(order, "phone")).append("\n");
        addr.append(Format.str(order, "address_line1"));
        String line2 = Format.str(order, "address_line2");
        if (!line2.isEmpty()) addr.append("\n").append(line2);
        addr.append("\n").append(Format.str(order, "city")).append(", ")
                .append(Format.str(order, "province")).append(" ")
                .append(Format.str(order, "postal_code")).append(", ")
                .append(Format.str(order, "country"));
        addressView.setText(addr.toString());

        // Payment + totals
        StringBuilder totals = new StringBuilder();
        totals.append(Format.str(order, "payment_method_display_label")).append("\n");
        totals.append(Format.str(order, "shipping_method"));
        String days = Format.str(order, "shipping_days");
        if (!days.isEmpty()) totals.append(" (").append(days).append(" days)");
        totals.append("\n\n");
        totals.append("Subtotal: ").append(Format.money(Format.str(order, "retail_total"))).append("\n");
        appendIfPositive(totals, "Product discount", order, "product_discount");
        appendIfPositive(totals, "Coupon discount", order, "coupon_discount");
        appendIfPositive(totals, "Points discount", order, "points_discount");
        totals.append("Shipping: ").append(Format.money(Format.str(order, "shipping_fee"))).append("\n");
        totals.append("Total: ").append(Format.money(Format.str(order, "grand_total")));
        totalsView.setText(totals.toString());

        // Line items
        JSONArray items = order.getJSONArray("items");
        itemsContainer.removeAllViews();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String qty = Format.str(item, "quantity");
            String unit = Format.str(item, "unit_price");
            TextView row = new TextView(this);
            row.setPadding(0, 16, 0, 16);
            row.setText(qty + " × " + Format.str(item, "product_name") + "\n" + Format.money(unit) + " each");
            itemsContainer.addView(row);
        }
    }

    private void appendIfPositive(StringBuilder sb, String label, JSONObject order, String key) {
        String raw = Format.str(order, key);
        try {
            if (!raw.isEmpty() && Double.parseDouble(raw) > 0) {
                sb.append(label).append(": -").append(Format.money(raw)).append("\n");
            }
        } catch (NumberFormatException ignored) {
            // skip malformed value
        }
    }
}
