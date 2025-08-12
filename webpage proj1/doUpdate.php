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

if (empty($BookID)) {
    // if shipperID is empty here, then somebody typed the URL to this page
    // the did not get here by going through update.php
    header("Location: select.php");
    exit;
}

include("includes/openDbConn.php");

$sql = "UPDATE P1Books SET Title='".$Title."', Author='".$Author."', Topic='".$Topic."', 
        Genre='".$Genre."', ISBN='".$ISBN."', DatePublished='".$DatePublished."', 
        Hardcover='".$Hardcover."' WHERE BookID=".$BookID;
// echo( $sql );
// exit;

$result = mysqli_query($db, $sql);

include("includes/closeDbConn.php");

header("Location: select.php");
?>