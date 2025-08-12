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

// Prepare the SQL statement to check for existing records with the same primary key (ShippingID and Login)
$sql = "SELECT ShippingID, Login FROM P2Shipping WHERE ShippingID='$shippingID' AND Login='$user'";
$result = mysqli_query($db, $sql);
$num_results = mysqli_num_rows($result);

// Check if the Shipping and Login already exist in the database
if ($num_results != 0) {
    // If you get here, the ShippingID and Login already exist in the database
    $_SESSION["shipError"] = "The ShippingID and Login already exist in the database.";
    header("Location: shipping.php");
    exit;
} else {
    // If you get here, there are no errors and we can insert the new record safely
    $_SESSION["shipError"] = "";
}

// Insert the shipping information into the database
$sql = "INSERT INTO P2Shipping (ShippingID, Login, Name, Address, City, State, Zip) 
        VALUES ('$shippingID', '$user', '$name', '$address', '$city', '$state', '$zip')";
// echo($sql);
// exit;
$result = mysqli_query($db, $sql);

// Clean up the database connection
include("includes/closeDbConn.php");
header("Location: shipping.php");
exit;

?>