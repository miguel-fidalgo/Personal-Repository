<?php
session_start();
include("includes/openDBConn.php");

if (empty($_SESSION['user'])) {
    header("Location: shipping.php"); // Shouldn't happen but as a safety net
    exit;
}

// Store the posted data in variables and sanitize them
$shippingID = $_GET["id"];
$user = $_SESSION['user'];

// Delete the shipping information from the database
$sql = "DELETE FROM P2Shipping WHERE ShippingID='$shippingID' AND Login='$user'";
// echo($sql);
// exit;
$result = mysqli_query($db, $sql);

// Clean up the database connection
include("includes/closeDbConn.php");
header("Location: shipping.php");
exit;
?>