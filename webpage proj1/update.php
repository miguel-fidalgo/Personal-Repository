<?php
session_start();

// if errorMessage has never been used, create it
if (empty($_SESSION["errorMessage"])) {
    $_SESSION["errorMessage"] = "";
}

include("includes/openDbConn.php");

$id = $_GET["id"];

// Attempt to retrieve record from DB
$sql = "SELECT BookID, Title, Author, Topic, Genre, ISBN, DatePublished, Hardcover FROM P1Books WHERE BookID=". $id;
$result = mysqli_query($db, $sql);

if(empty($result)) {
    $num_results = 0;
} else {
    $num_results = mysqli_num_rows($result);
    $row = mysqli_fetch_array($result);
}

// If no result, set an error message or do something appropriate
if($num_results == 0) {
    $_SESSION["errorMessage"] = "Record not found or no record with that ID.";
}

$month = "";
$day = "";
$year = "";

if (!empty($row["DatePublished"])) {
    $parts = explode(" ", $row["DatePublished"]); // e.g. "Mar 15, 2020"
    if (count($parts) == 3) {
        $month = trim($parts[0]);
        $day = str_replace(",", "", trim($parts[1]));
        $year = trim($parts[2]);
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Project 1 - Update</title>
    <link rel="stylesheet" href="styles.css" type="text/css">
</head>
<body>
    <h1>Project 1 - Update</h1>
    <?php include("includes/menu.php"); ?>
    
    <div class="container">
        <form id="form0" name="form0" method="post" action="doUpdate.php">
            <fieldset>
                <legend>Update Book Registration</legend>
                
                <!-- BookID (Disabled, plus hidden real field) -->
                <div class="form-group">
                    <label for="bookIDdis">BookID</label>
                    <input type="text" id="bookIDdis" name="bookIDdis" 
                           value="<?php if($num_results != 0) {echo trim($row['BookID']);} ?>" 
                           disabled />
                    <!-- Hidden field to pass the actual ID to doUpdate.php -->
                    <input type="hidden" name="bookID" id="bookID"
                           value="<?php if($num_results != 0) {echo trim($row['BookID']);} ?>" />
                </div>
                
                <!-- Title -->
                <div class="form-group">
                    <label for="title">Title</label>
                    <input type="text" id="title" name="title" 
                           value="<?php if($num_results != 0) {echo trim($row['Title']);} ?>" />
                </div>

                <!-- Author -->
                <div class="form-group">
                    <label for="author">Author</label>
                    <input type="text" id="author" name="author" 
                           value="<?php if($num_results != 0) {echo trim($row['Author']);} ?>" />
                </div>

                <!-- Topic -->
                <div class="form-group">
                    <label for="topic">Topic</label>
                    <select id="topic" name="topic">
                        <option value="Psychology" <?php if($row['Topic'] == 'Psychology') echo 'selected'; ?>>Psychology</option>
                        <option value="Technology" <?php if($row['Topic'] == 'Technology') echo 'selected'; ?>>Technology</option>
                        <option value="Business" <?php if($row['Topic'] == 'Business') echo 'selected'; ?>>Business</option>
                        <option value="Politics" <?php if($row['Topic'] == 'Politics') echo 'selected'; ?>>Politics</option>
                        <option value="Education" <?php if($row['Topic'] == 'Education') echo 'selected'; ?>>Education</option>
                        <option value="Environment" <?php if($row['Topic'] == 'Environment') echo 'selected'; ?>>Environment</option>
                        <option value="Health & Wellness" <?php if($row['Topic'] == 'Health & Wellness') echo 'selected'; ?>>Health & Wellness</option>
                        <option value="Space" <?php if($row['Topic'] == 'Space') echo 'selected'; ?>>Space</option>
                        <option value="Philosophy" <?php if($row['Topic'] == 'Philosophy') echo 'selected'; ?>>Philosophy</option>
                        <option value="Sports" <?php if($row['Topic'] == 'Sports') echo 'selected'; ?>>Sports</option>
                        <option value="Culture" <?php if($row['Topic'] == 'Culture') echo 'selected'; ?>>Culture</option>
                        <option value="Religion" <?php if($row['Topic'] == 'Religion') echo 'selected'; ?>>Religion</option>
                        <option value="Science & Nature" <?php if($row['Topic'] == 'Science & Nature') echo 'selected'; ?>>Science & Nature</option>
                        <option value="Economics" <?php if($row['Topic'] == 'Economics') echo 'selected'; ?>>Economics</option>
                        <option value="Art & Design" <?php if($row['Topic'] == 'Art & Design') echo 'selected'; ?>>Art & Design</option>
                    </select>
                </div>

                <!-- Genre -->
                <div class="form-group">
                    <label title="genre">Genre</label>
                    <!-- The 'checked' attribute is used to tell the browser which radio button is selected by default -->
                    <div>
                        <label><input type="radio" name="genre" value="Novel" <?php if($row['Genre'] == 'Novel') echo 'checked'; ?>> Novel</label>
                        <label><input type="radio" name="genre" value="Short Story" <?php if($row['Genre'] == 'Short Story') echo 'checked'; ?>> Short Story</label>
                        <label><input type="radio" name="genre" value="Poetry" <?php if($row['Genre'] == 'Poetry') echo 'checked'; ?>> Poetry</label>
                        <label><input type="radio" name="genre" value="Drama" <?php if($row['Genre'] == 'Drama') echo 'checked'; ?>> Drama</label>
                        <label><input type="radio" name="genre" value="Essay" <?php if($row['Genre'] == 'Essay') echo 'checked'; ?>> Essay</label>
                        <label><input type="radio" name="genre" value="Memoir" <?php if($row['Genre'] == 'Memoir') echo 'checked'; ?>> Memoir</label>
                        <label><input type="radio" name="genre" value="Comic" <?php if($row['Genre'] == 'Comic') echo 'checked'; ?>> Comic</label>
                        <label><input type="radio" name="genre" value="Graphic Novel" <?php if($row['Genre'] == 'Graphic Novel') echo 'checked'; ?>> Graphic Novel</label>
                    </div>
                </div>

                <!-- ISBN -->
                <div class ="form-group">
                    <label for="isbn">ISBN</label>
                    <input type="text" id="isbn" name="isbn" 
                           value="<?php if($num_results != 0) {echo trim($row['ISBN']);} ?>" />
                </div>

                <!-- Date Published -->
                <div class="form-group">
                    <label>Date Published</label>
                    <div class="date-group">
                        <select name="month" id="month">
                            <option value="">Month</option>
                            <?php
                            $months = array("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec");
                            foreach($months as $m) {
                                $selected = ($m == $month) ? "selected" : "";
                                echo "<option value=\"$m\" $selected>$m</option>";
                            }
                            ?>
                        </select>

                        <select name="day" id="day">
                            <option value="">Day</option>
                            <?php
                            for($d=1; $d<=31; $d++) {
                                $selected = ($d == $day) ? "selected" : "";
                                echo "<option value=\"$d\" $selected>$d</option>";
                            }
                            ?>
                        </select>

                        <select name="year" id="year">
                            <option value="">Year</option>
                            <?php
                            for($y=date("Y"); $y>=1900; $y--) {
                                $selected = ($y == $year) ? "selected" : "";
                                echo "<option value=\"$y\" $selected>$y</option>";
                            }
                            ?>
                        </select>
                    </div>
                </div>

                <!-- Hardcover -->
                <div class="form-group">
                    <label for="hardcover">Hardcover</label>
                    <input type="checkbox" id="hardcover" name="hardcover" value="Yes"
                        <?php if($num_results != 0 && trim($row['Hardcover']) == "Yes") echo 'checked="checked"'; ?> />
                </div>
                                
                <!-- Error message, if any -->
                <?php if(!empty($_SESSION["errorMessage"])): ?>
                <div class="form-message">
                    <?php echo $_SESSION["errorMessage"]; ?>
                </div>
                <?php endif; ?>
                
                <!-- Submit Button -->
                <div class="form-group">
                    <input type="submit" value="Update info" name="submit" id="submit"/>
                </div>
            </fieldset>
        </form>
    </div>
    
    <?php
    // clear the error message
    $_SESSION["errorMessage"] = "";
    // close the DB connection
    include("includes/closeDbConn.php");
    ?>

    <script>
        document.getElementById("title").focus();
    </script>
</body>
</html>
