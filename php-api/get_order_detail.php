<?php
// GET get_order_detail.php?order_id=1   (header: X-Auth-Token: <token>)
// Full details for any single order, including line items.
require_once 'db_connect.php';
require_once 'auth.php';

require_admin($conn);

$order_id = intval($_GET['order_id'] ?? 0);
if ($order_id <= 0) {
    echo json_encode(['success' => false, 'message' => 'order_id is required']);
    exit;
}

$stmt = $conn->prepare("SELECT * FROM orders WHERE id = ?");
$stmt->bind_param("i", $order_id);
$stmt->execute();
$order = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$order) {
    echo json_encode(['success' => false, 'message' => 'Order not found']);
    $conn->close();
    exit;
}

$stmt = $conn->prepare(
    "SELECT product_id, product_name, product_image, unit_price, original_price, quantity
     FROM order_items
     WHERE order_id = ?
     ORDER BY id"
);
$stmt->bind_param("i", $order_id);
$stmt->execute();
$result = $stmt->get_result();

$items = [];
while ($row = $result->fetch_assoc()) {
    $items[] = $row;
}
$order['items'] = $items;

echo json_encode(['success' => true, 'order' => $order]);

$stmt->close();
$conn->close();
