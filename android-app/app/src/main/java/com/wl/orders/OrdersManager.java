package com.wl.orders;

import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import java.util.HashMap;
import java.util.Map;

public class OrdersManager {

    // Passed to ApiCallback.onError when the server says the login token is no
    // longer valid (expired, or the admin password was changed).
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";

    private final Context context;
    private final String token;

    public OrdersManager(Context context, String token) {
        this.context = context;
        this.token = token;
    }

    // Every order from every customer, with line items included.
    public void getOrders(ApiCallback callback) {
        send(ApiConfig.GET_ORDERS_URL, callback);
    }

    public void getOrderDetail(int orderId, ApiCallback callback) {
        send(ApiConfig.GET_ORDER_DETAIL_URL + "?order_id=" + orderId, callback);
    }

    private void send(String url, ApiCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                callback::onSuccess,
                error -> callback.onError(parseError(error))) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-Auth-Token", token);
                return headers;
            }
        };
        request.setShouldCache(false); // always show fresh orders
        request.setRetryPolicy(new DefaultRetryPolicy(20000, 1, 1f));
        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

    private String parseError(VolleyError error) {
        if (error.networkResponse != null) {
            if (error.networkResponse.statusCode == 401) {
                return SESSION_EXPIRED;
            }
            if (error.networkResponse.data != null) {
                return new String(error.networkResponse.data);
            }
        }
        return error.getMessage() != null ? error.getMessage() : "Network error";
    }
}
