<?php
// POST { "user": "...", "password": "..." }
// Checks the `wapp` table (columns: id, user, password).
// Returns a signed token the app sends back on every later request.
require_once 'db_connect.php';
require_once 'auth.php';

$data = json_decode(file_get_contents("php://input"), true) ?: [];

$user = trim($data['user'] ?? '');
$password = (string)($data['password'] ?? '');

if ($user === '' || $password === '') {
    echo json_encode(['success' => false, 'message' => 'Username and password are required']);
    exit;
}

$stmt = $conn->prepare("SELECT id, `user`, `password` FROM wapp WHERE `user` = ? LIMIT 1");
$stmt->bind_param("s", $user);
$stmt->execute();
$admin = $stmt->get_result()->fetch_assoc();
$stmt->close();

$ok = false;
if ($admin) {
    $stored = (string)$admin['password'];
    if (password_get_info($stored)['algo']) {
        // Password column holds a bcrypt/argon hash (if you upgrade it later).
        $ok = password_verify($password, $stored);
    } else {
        // Password column holds plain text, as in the current wapp table.
        $ok = hash_equals($stored, $password);
    }
}

if (!$ok) {
    usleep(500000); // small delay to slow down password guessing
    echo json_encode(['success' => false, 'message' => 'Invalid username or password']);
    exit;
}

echo json_encode([
    'success' => true,
    'message' => 'Login successful',
    'token'   => issue_token($admin['id'], $admin['password']),
    'admin'   => ['id' => (int)$admin['id'], 'user' => $admin['user']],
]);

$conn->close();
