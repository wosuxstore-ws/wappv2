<?php
// ============================================
// DATABASE CONFIG - EDIT THESE WITH YOUR
// HOSTING / cPANEL MYSQL DATABASE DETAILS
// (cPanel > MySQL Databases to find/create these)
// ============================================
define('DB_SERVER', 'localhost');               // usually "localhost" on cPanel/shared hosting
define('DB_USERNAME', 'yourcpaneluser_dbuser'); // the MySQL user cPanel created for you
define('DB_PASSWORD', 'your_db_password');
define('DB_NAME', 'yourcpaneluser_wl');         // the database you imported wl.sql into

// Secret used to sign login tokens. Make it long and random (32+ characters),
// and never share it. Changing it logs everyone out.
define('TOKEN_SECRET', 'CHANGE_ME_to_a_long_random_string_32+_chars');

// How long a login stays valid, in seconds (default: 30 days).
define('TOKEN_TTL', 60 * 60 * 24 * 30);
