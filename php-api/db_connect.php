<?php
// Shared DB connection used by every endpoint.
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, X-Auth-Token');

require_once 'config.php';

// Never leak PHP/MySQL error details to the client; log them on the server instead.
ini_set('display_errors', '0');
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
set_exception_handler(function ($e) {
    error_log('API error: ' . $e->getMessage());
    if (!headers_sent()) {
        http_response_code(500);
    }
    echo json_encode(['success' => false, 'message' => 'Server error']);
    exit;
});

// Let browsers' CORS preflight through without needing a DB connection.
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
    exit;
}

$conn = new mysqli(DB_SERVER, DB_USERNAME, DB_PASSWORD, DB_NAME);
$conn->set_charset('utf8mb4');
