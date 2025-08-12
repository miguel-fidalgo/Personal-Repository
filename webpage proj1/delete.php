<?php
session_start();

$id = $_GET["id"];

// Open the database connection
include("includes/openDbConn.php");

$sql = "DELETE FROM P1Books WHERE BookID=".$id;
// echo( $sql );
// exit;

$result = mysqli_query($db, $sql);

include("includes/closeDbConn.php");

header("Location: select.php");
?>