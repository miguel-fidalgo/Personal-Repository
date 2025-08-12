<?php
session_start(); // Start the session

// Store the posted data in variables
$login = trim($_POST["login"]);
$passwd = $_POST["passwd"];

$_SESSION['loginSticky'] = ['login'=>$login];

// Validate the input
if (empty($login) || empty($passwd)) {
    $_SESSION["loginError"] = "All fields are required.";
    header("Location: login-register.php");
    exit;
}

include("includes/openDBConn.php");

$sql = "SELECT Passwd FROM P2User WHERE Login='".$login."'";
// echo($sql);
// exit;

$result = mysqli_query($db,$sql);
$num_results = mysqli_num_rows($result);

if ($num_results == 0) {
    $_SESSION["loginError"] = "Login not found.";
    header("Location: login-register.php");
    exit;
} else {
    $_SESSION["loginError"] = ""; // Clear error message if no issues
}

// Fetch the result
$row = mysqli_fetch_assoc($result);

// echo("The password is: ".$row['Passwd']." while the user entered is: ".$passwd."<br>");
// exit;

if($passwd !== $row['Passwd']){
    $_SESSION["loginError"] = "Incorrect password.";
    header("Location: login-register.php");
    exit;
} else {
    $_SESSION["loginError"] = ""; // Clear error message if no issues
}

// Clean up the database connection
include ("includes/closeDbConn.php");

// Success - automatically log in the user
$_SESSION['user'] = $login;
unset($_SESSION['loginSticky']);
header("Location: index.php");
exit;

?>
