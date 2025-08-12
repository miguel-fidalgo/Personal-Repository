<?php
session_start();
include("includes/openDBConn.php");

if (empty($_SESSION['user'])) {
    header("Location: shipping.php"); // Shouldn't happen but as a safety net
    exit;
}

// Store the posted data in variables and sanitize them
$shippingID = addslashes(trim($_POST["shippingID"]));
$user = $_SESSION['user'];
$name = addslashes(trim($_POST["name"]));
$address = addslashes(trim($_POST["address"]));
$city = addslashes(trim($_POST["city"]));
$state = addslashes(trim($_POST["state"]));
$zip = addslashes(trim($_POST["zip"]));

// Simple required field validation
if (empty($name) || empty($address) || empty($city) || empty($state) || empty($zip) || empty($shippingID)) {
    $_SESSION["shipError"] = "All fields are required.";
    header("Location: shipping.php");
    exit;
}

// Validate the ZIP code format (5 digits)
if (!is_numeric($zip) || strlen($zip) != 5) {
    $_SESSION["shipError"] = "Invalid ZIP code format. Must be 5 digits.";
    header("Location: shipping.php");
    exit;
}

// Update the shipping information in the database
$sql = "UPDATE P2Shipping SET Name='$name', Address='$address', City='$city', State='$state', Zip='$zip' 
        WHERE ShippingID='$shippingID' AND Login='$user'";
// echo($sql);
// exit;
$result = mysqli_query($db, $sql);

// Clean up the database connection
include("includes/closeDbConn.php");
header("Location: shipping.php");
exit;
?>