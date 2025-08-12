<?php
session_start(); // Start the session

// Store the posted data in variables
$login = trim($_POST["login"]);
$firstName = trim($_POST["first"]);
$lastName = trim($_POST["last"]);
$passwd = $_POST["passwd"];
$email = trim($_POST["email"]);
$newsletter = $_POST["newsletter"] ?? "No";

// Keep sticky copy for repopulation
$_SESSION['registerSticky'] = compact('login', 'first', 'last', 'email', 'newsletter');

// Validate the input
if (empty($login) || empty($firstName) || empty($lastName) || empty($passwd) || empty($email)) {
    $_SESSION["registerError"] = "All fields are required.";
    header("Location: register.php");
    exit;
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    $_SESSION["registerError"] = "Invalid email format.";
    header("Location: register.php");
    exit;
}

include("includes/openDBConn.php");

// Check if the login already exists
$sql = "SELECT Login FROM P2User WHERE Login='".$login."'";
// echo($sql);
// exit;

$result = mysqli_query($db, $sql);
$num_results = mysqli_num_rows($result);

if ($num_results != 0) {
    $_SESSION["registerError"] = "The login you entered already exists!";
    header("Location: register.php");
    exit;
} else {
    $_SESSION["registerError"] = ""; // Clear error message if no issues
}

// Insert the new user into the database
$sql = "INSERT INTO P2User (Login, FirstName, LastName, Passwd, Email, Newsletter)
        VALUES ('".$login."', '".$firstName."', '".$lastName."', '".$passwd."', '".$email."', '".$newsletter."')";
// echo($sql);
// exit;

$result = mysqli_query($db, $sql);

// Clean up the database connection
include ("includes/closeDbConn.php");

// Success - automatically log in the user
$_SESSION['user'] = $login; // Store the login in the session
unset($_SESSION['registerSticky']); // Clear sticky session data after successful registration
header("Location: index.php");
exit;
?>
