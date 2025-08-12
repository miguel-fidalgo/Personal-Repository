<?php
session_start();

// Get the data posted from the form
// addslashes() is used to prevent SQL injection
$BookID = $_POST["bookID"];
$Title = addslashes($_POST["title"]);
$Author = addslashes($_POST["author"]);
$Topic = addslashes($_POST["topic"]);
$Genre = addslashes($_POST["genre"]);
$ISBN = addslashes($_POST["isbn"]);
$DatePublished = addslashes($_POST["datepublished"]);
// If hardcover is "No" $_POST["hardcover"] will trigger an error since it is not set
// So we need to check if it is set and if not, set it to "No"
$Hardcover = isset($_POST["hardcover"]) ? addslashes($_POST["hardcover"]) : "No";
// The date is split into month, day, and year and then combined into a string
$month = $_POST["month"];
$day = $_POST["day"];
$year = $_POST["year"];
$DatePublished = $month . " " . $day . ", " . $year; // "Jan 15, 2020"
$DatePublished = addslashes($DatePublished); // sanitize it

$removeThese = array("<?php", "<?", "</", "<", "?>", "/>", ">", ";");
$BookID = str_replace($removeThese, "", $BookID);
$Title = str_replace($removeThese, "", $Title);
$Author = str_replace($removeThese, "", $Author);
$Topic = str_replace($removeThese, "", $Topic);
$Genre = str_replace($removeThese, "", $Genre);
$ISBN = str_replace($removeThese, "", $ISBN);
$DatePublished = str_replace($removeThese, "", $DatePublished);
$Hardcover = str_replace($removeThese, "", $Hardcover);

// First check to see if there are any empty fields and if so, redirect to insert.php
if ( ($BookID == "") || ($Title == "") || ($Author == "") || ($Topic == "") || 
    ($Genre == "") || ($ISBN == "") || ($DatePublished == "") || ($Hardcover == "") ) {
    // if you get here, one or more of the fields is empty
    $_SESSION["errorMessage"] = "You must enter a value for all boxes!";
    header("Location: insert.php");
    exit;

// Then check to see if BookID is a number and if not, redirect to insert.php
} else if (!is_numeric($BookID)) {
    // make sure the BookID is a number
    // if you get here, the BookID is not a number
    $_SESSION["errorMessage"] = "The BookID must be a number!";
    header("Location: insert.php");
    exit;

// Ensure that the ISBN is exactly 13 characters long
} else if (strlen($ISBN) != 13) {
    // if you get here, the ISBN is not 13 characters long
    $_SESSION["errorMessage"] = "The ISBN must be exactly 13 characters long!";
    header("Location: insert.php");
    exit;

// Also ensure that the ISBN is a number
// Then check to see if BookID is a number and if not, redirect to insert.php
} else if (!is_numeric($ISBN)) {
    // if you get here, the ISBN is not a number
    $_SESSION["errorMessage"] = "The BookID must be a number!";
    header("Location: insert.php");
    exit;

} else {
    // if you get here, there are no errors.
    $_SESSION["errorMessage"] = "";
}
include("includes/openDBConn.php");

// Prepare my select to see if there are any existing records with the same BookID
$sql = "SELECT BookID FROM P1Books WHERE BookID=".$BookID;
// echo( $sql );
// exit;

$result = mysqli_query($db, $sql);
$num_results = mysqli_num_rows($result);

// check to see if BookID from the user is already in the database
if ($num_results != 0) {
    // if you get here, the BookID is already in the database
    $_SESSION["errorMessage"] = "The BookID you entered already exits!";
    header("Location: insert.php");
    exit;

} else {
    // The BookID does not exits in the database, so insert the record
    $_SESSION["errorMessage"] = "";
}

// Prepare my SQL statement for inserting the record in the database
$sql = "INSERT INTO P1Books (BookID, Title, Author, Topic, Genre, ISBN, DatePublished, Hardcover) 
        VALUES (".$BookID.", '".$Title."', '".$Author."', '".$Topic."', '".$Genre."', '".$ISBN."', '".$DatePublished."', '".$Hardcover."')";
// echo( $sql );
// exit;

$result = mysqli_query($db, $sql);

// Clean up the database connection
include ("includes/closeDbConn.php");

// Redirect to the select page
header("Location: select.php");
exit;
?>
