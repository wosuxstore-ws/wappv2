package com.wl.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.wl.orders.models.Order;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Shows ALL orders from ALL customers in one scrolling list.
public class OrdersActivity extends AppCompatActivity {

    private static final int MAX_ITEM_LINES = 4; // items shown per order card before "+N more"

    private SwipeRefreshLayout swipeRefresh;
    private TextView emptyView, summaryView;
    private OrdersManager ordersManager;
    private SessionManager sessionManager;
    private OrderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        setTitle("All Orders");

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.accent);
        RecyclerView recyclerView = findViewById(R.id.ordersRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        summaryView = findViewById(R.id.summaryView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(new ArrayList<>(), order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("order_id", order.id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        ordersManager = new OrdersManager(this, sessionManager.getToken());

        swipeRefresh.setOnRefreshListener(this::loadOrders);

        loadOrders();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.orders_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            if (ordersManager != null) {
                loadOrders();
            }
            return true;
        }
        if (id == R.id.action_logout) {
            sessionManager.clear();
            goToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void loadOrders() {
        swipeRefresh.setRefreshing(true);
        ordersManager.getOrders(new ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                swipeRefresh.setRefreshing(false);
                try {
                    if (response.getBoolean("success")) {
                        JSONArray array = response.getJSONArray("orders");
                        List<Order> orders = new ArrayList<>();
                        for (int i = 0; i < array.length(); i++) {
                            orders.add(parseOrder(array.getJSONObject(i)));
                        }
                        adapter.setOrders(orders);
                        showSummary(orders);
                        emptyView.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                    } else {
                        Toast.makeText(OrdersActivity.this, response.optString("message", "Couldn't load orders"), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(OrdersActivity.this, "Unexpected response from server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                swipeRefresh.setRefreshing(false);
                if (OrdersManager.SESSION_EXPIRED.equals(message)) {
                    sessionManager.clear();
                    Toast.makeText(OrdersActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                    goToLogin();
                    return;
                }
                Toast.makeText(OrdersActivity.this, "Couldn't reach server: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private Order parseOrder(JSONObject o) throws JSONException {
        Order order = new Order();
        order.id = o.getInt("id");
        order.orderNumber = Format.str(o, "order_number");
        order.status = Format.str(o, "status");
        order.grandTotal = Format.str(o, "grand_total");
        order.recipientName = Format.str(o, "recipient_name");
        order.phone = Format.str(o, "phone");
        order.shippingMethod = Format.str(o, "shipping_method");
        order.paymentLabel = Format.str(o, "payment_method_display_label");
        order.city = Format.str(o, "city");
        order.province = Format.str(o, "province");
        order.createdAt = Format.str(o, "created_at");

        // Flatten line items into short text lines for the list card.
        StringBuilder sb = new StringBuilder();
        JSONArray items = o.optJSONArray("items");
        int count = items == null ? 0 : items.length();
        for (int i = 0; i < count && i < MAX_ITEM_LINES; i++) {
            JSONObject item = items.getJSONObject(i);
            if (i > 0) sb.append("\n");
            sb.append(Format.str(item, "quantity")).append(" × ")
                    .append(Format.shorten(Format.str(item, "product_name"), 60));
        }
        if (count > MAX_ITEM_LINES) {
            sb.append("\n+").append(count - MAX_ITEM_LINES).append(" more item(s)");
        }
        order.itemsSummary = sb.toString();
        return order;
    }

    // "12 orders  •  10 pending  •  2 shipped"
    private void showSummary(List<Order> orders) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Order order : orders) {
            Integer current = counts.get(order.status);
            counts.put(order.status, current == null ? 1 : current + 1);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(orders.size()).append(orders.size() == 1 ? " order" : " orders");
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            sb.append("  •  ").append(entry.getValue()).append(" ").append(entry.getKey());
        }
        summaryView.setText(sb.toString());
    }
}
