package com.wl.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.wl.orders.models.Order;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

// Dashboard. Only "Latest orders" is live (same get_orders.php call as OrdersActivity);
// header, categories, products and bottom nav are static for now.
public class HomeActivity extends AppCompatActivity {

    private static final int MAX_CARDS = 6; // how many latest orders to show on Home

    private SwipeRefreshLayout swipeRefresh;
    private TextView ordersMessage;
    private RecyclerView ordersRecycler;
    private HomeOrderAdapter orderAdapter;
    private OrdersManager ordersManager;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        swipeRefresh = findViewById(R.id.homeSwipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.home_blue);
        ordersMessage = findViewById(R.id.ordersMessage);
        ordersRecycler = findViewById(R.id.ordersRecycler);

        // Latest orders (dynamic)
        orderAdapter = new HomeOrderAdapter(order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("order_id", order.id);
            startActivity(intent);
        });
        ordersRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ordersRecycler.setAdapter(orderAdapter);

        // Products (static)
        RecyclerView productsRecycler = findViewById(R.id.productsRecycler);
        productsRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        productsRecycler.setAdapter(new HomeProductAdapter());

        // "See all" on Latest orders -> the full orders list
        findViewById(R.id.ordersSeeAll).setOnClickListener(v ->
                startActivity(new Intent(this, OrdersActivity.class)));

        // Top-left grid icon: small menu so Refresh / Log out stay reachable from Home
        findViewById(R.id.menuButton).setOnClickListener(this::showMenu);

        // Tap the empty/error message to retry
        ordersMessage.setOnClickListener(v -> loadOrders());

        ordersManager = new OrdersManager(this, sessionManager.getToken());
        swipeRefresh.setOnRefreshListener(this::loadOrders);

        loadOrders();
    }

    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenuInflater().inflate(R.menu.orders_menu, menu.getMenu()); // Refresh + Log out
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_refresh) {
                loadOrders();
                return true;
            }
            if (id == R.id.action_logout) {
                sessionManager.clear();
                goToLogin();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadOrders() {
        swipeRefresh.setRefreshing(true);
        ordersManager.getOrders(new ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                swipeRefresh.setRefreshing(false);
                try {
                    if (!response.getBoolean("success")) {
                        showMessage(response.optString("message", "Couldn't load orders"));
                        return;
                    }
                    JSONArray array = response.getJSONArray("orders"); // already newest first
                    List<Order> latest = new ArrayList<>();
                    for (int i = 0; i < array.length() && i < MAX_CARDS; i++) {
                        latest.add(parseOrder(array.getJSONObject(i)));
                    }
                    if (latest.isEmpty()) {
                        showMessage("No orders yet");
                    } else {
                        orderAdapter.setOrders(latest);
                        ordersMessage.setVisibility(View.GONE);
                        ordersRecycler.setVisibility(View.VISIBLE);
                        ordersRecycler.scrollToPosition(0);
                    }
                } catch (JSONException e) {
                    showMessage("Unexpected response from server");
                }
            }

            @Override
            public void onError(String message) {
                swipeRefresh.setRefreshing(false);
                if (OrdersManager.SESSION_EXPIRED.equals(message)) {
                    sessionManager.clear();
                    Toast.makeText(HomeActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                    goToLogin();
                    return;
                }
                showMessage("Couldn't reach server. Tap to retry.");
            }
        });
    }

    private void showMessage(String text) {
        ordersRecycler.setVisibility(View.INVISIBLE);
        ordersMessage.setText(text);
        ordersMessage.setVisibility(View.VISIBLE);
    }

    private Order parseOrder(JSONObject o) throws JSONException {
        Order order = new Order();
        order.id = o.getInt("id");
        order.orderNumber = Format.str(o, "order_number");
        order.status = Format.str(o, "status");
        order.grandTotal = Format.str(o, "grand_total");
        order.recipientName = Format.str(o, "recipient_name");
        order.createdAt = Format.str(o, "created_at");

        int count = 0;
        JSONArray items = o.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                count += Math.max(1, items.getJSONObject(i).optInt("quantity", 1));
            }
        }
        order.itemCount = count;
        return order;
    }
}
