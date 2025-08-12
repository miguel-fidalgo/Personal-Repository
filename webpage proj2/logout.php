<?php
/* Logout script */
session_start(); // Start the session

// Clear all session data
session_unset(); // Unset all session variables
$_SESSION = []; // Clear the session array
session_destroy(); // Destroy the session

// Redirect to the home page
header("Location: index.php");
exit;

?>