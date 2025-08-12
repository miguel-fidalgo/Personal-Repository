<?php
session_start();
include("includes/openDBConn.php");

if (empty($_SESSION['user'])) {
    header("Location: billing.php"); // Shouldn't happen but as a safety net
    exit;
}

// Store the posted data in variables and sanitize them
$billingID = addslashes(trim($_POST["billingID"]));
$user = $_SESSION['user'];
$name = addslashes(trim($_POST["name"]));
$address = addslashes(trim($_POST["address"]));
$city = addslashes(trim($_POST["city"]));
$state = addslashes(trim($_POST["state"]));
$zip = addslashes(trim($_POST["zip"]));
$cardType = addslashes(trim($_POST["cardType"]));
$cardNum = addslashes(trim($_POST["cardNum"]));

$expMonth = addslashes($_POST["expMonth"] ?? "");
$expYear  = addslashes($_POST["expYear"]  ?? "");
$expDate  = $expMonth . "/" . $expYear;           // "05/31"
$expDate  = str_replace(array("<?php","<?","</","<","?>","/>",">",";"), "", $expDate);

// Simple required field validation
if (empty($name) || empty($address) || empty($city) || empty($state) || empty($zip) || 
   empty($billingID) || empty($cardType) || empty($cardNum) || empty($expDate)) {
    $_SESSION["billError"] = "All fields are required.";
    header("Location: billing.php");
    exit;
}

// Validate the ZIP code format (5 digits)
if (!is_numeric($zip) || strlen($zip) != 5) {
    $_SESSION["billError"] = "Invalid ZIP code format. Must be 5 digits.";
    header("Location: billing.php");
    exit;
}

// Prepare the SQL statement to check for existing records with the same primary key (BillingID and Login)
$sql = "SELECT BillingID, Login FROM P2Billing WHERE BillingID='$billingID' AND Login='$user'";
$result = mysqli_query($db, $sql);
$num_results = mysqli_num_rows($result);

// Check if the BillingID and Login already exist in the database
if ($num_results != 0) {
    // If you get here, the BillingID and Login already exist in the database
    $_SESSION["billError"] = "The BillingID and Login already exist in the database.";
    header("Location: billing.php");
    exit;
} else {
    // If you get here, there are no errors and we can insert the new record safely
    $_SESSION["billError"] = "";
}

// Insert the billing information into the database
$sql = "INSERT INTO P2Billing (BillingID, Login, BillName, BillAddress, BillCity, BillState, BillZip, CardType, CardNumber, ExpDate) 
        VALUES ('$billingID', '$user', '$name', '$address', '$city', '$state', '$zip', '$cardType', '$cardNum', '$expDate')";
// echo($sql);
// exit;
$result = mysqli_query($db, $sql);

// Clean up the database connection
include("includes/closeDbConn.php");
header("Location: billing.php");
exit;

?>