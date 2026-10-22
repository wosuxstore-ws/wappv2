<?php
// Token helpers for the admin app. Tokens are stateless: a signed blob that says
// "admin #N logged in, valid until T". Nothing extra is stored in your database.
//
// A token stops working when: it expires, TOKEN_SECRET changes, the admin row is
// deleted from `wapp`, or that admin's password is changed.

function b64url_encode($data) {
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}

function b64url_decode($data) {
    return base64_decode(strtr($data, '-_', '+/'));
}

function ensure_secret() {
    if (!defined('TOKEN_SECRET') || strlen(TOKEN_SECRET) < 32
        || TOKEN_SECRET === 'CHANGE_ME_to_a_long_random_string_32+_chars') {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Server not configured: set TOKEN_SECRET in config.php']);
        exit;
    }
}

// Short fingerprint of the stored password, baked into the token so that
// changing the password in `wapp` invalidates every token issued before.
function password_version($storedPassword) {
    return substr(hash_hmac('sha256', (string)$storedPassword, TOKEN_SECRET), 0, 16);
}

function issue_token($adminId, $storedPassword) {
    ensure_secret();
    $ttl = defined('TOKEN_TTL') ? TOKEN_TTL : 60 * 60 * 24 * 30;
    $payload = b64url_encode(json_encode([
        'wid' => (int)$adminId,
        'exp' => time() + $ttl,
        'pv'  => password_version($storedPassword),
    ]));
    $sig = b64url_encode(hash_hmac('sha256', $payload, TOKEN_SECRET, true));
    return $payload . '.' . $sig;
}

function fail_auth() {
    http_response_code(401);
    echo json_encode([
        'success' => false,
        'message' => 'Session expired. Please log in again.',
        'auth_error' => true,
    ]);
    exit;
}

function get_request_token() {
    // Preferred: X-Auth-Token header (what the app sends).
    if (!empty($_SERVER['HTTP_X_AUTH_TOKEN'])) {
        return trim($_SERVER['HTTP_X_AUTH_TOKEN']);
    }
    // Fallback for quick browser/curl testing: ?token=...
    if (!empty($_GET['token'])) {
        return trim($_GET['token']);
    }
    return '';
}

// Call at the top of every protected endpoint. Exits with HTTP 401 if the
// token is missing/invalid; otherwise returns the admin's `wapp` row.
function require_admin($conn) {
    ensure_secret();

    $parts = explode('.', get_request_token());
    if (count($parts) !== 2) {
        fail_auth();
    }
    list($payload, $sig) = $parts;

    $expected = b64url_encode(hash_hmac('sha256', $payload, TOKEN_SECRET, true));
    if (!hash_equals($expected, $sig)) {
        fail_auth();
    }

    $data = json_decode(b64url_decode($payload), true);
    if (!is_array($data) || empty($data['wid']) || empty($data['exp']) || $data['exp'] < time()) {
        fail_auth();
    }

    $adminId = (int)$data['wid'];
    $stmt = $conn->prepare("SELECT id, `user`, `password` FROM wapp WHERE id = ?");
    $stmt->bind_param("i", $adminId);
    $stmt->execute();
    $admin = $stmt->get_result()->fetch_assoc();
    $stmt->close();

    if (!$admin || !hash_equals(password_version($admin['password']), (string)($data['pv'] ?? ''))) {
        fail_auth();
    }

    return $admin;
}
