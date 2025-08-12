<?php
session_start();

// If errorMessage has never been used, create it
if (empty($_SESSION["errorMessage"])) {
    $_SESSION["errorMessage"] = "";
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Project 1 - Insert</title>
    <!-- Link to your external stylesheet (same as select.php) -->
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <h1>Project 1 - Insert</h1>
    
    <?php include("includes/menu.php"); ?>

    <div class="container">
        <form id="form0" name="form0" method="post" action="doInsert.php">
            <fieldset>
                <legend>Insert into P1Books table</legend>

                <!-- BookID -->
                <div class="form-group">
                    <label for="bookID" title="bookID">BookID</label>
                    <input type="text" name="bookID" id="bookID" size="20" maxlength="3"/>
                </div>

                <!-- Title -->
                <div class="form-group">
                    <label for="title" title="title">Title</label>
                    <input type="text" name="title" id="title" size="20" maxlength="20"/>
                </div>

                <!-- Author -->
                <div class="form-group">
                    <label for="author" title="author">Author</label>
                    <input type="text" name="author" id="author" size="20" maxlength="20"/>
                </div>
                
                <!-- Topic -->
                <div class="form-group">
                    <label for="topic" title="topic">Topic</label>
                    <select name="topic" id="topic">
                        <option value="Psychology">Psychology</option>
                        <option value="Technology">Technology</option>
                        <option value="Business">Business</option>
                        <option value="Politics">Politics</option>
                        <option value="Education">Education</option>
                        <option value="Environment">Environment</option>
                        <option value="Health & Wellness">Health & Wellness</option>
                        <option value="Space">Space</option>
                        <option value="Philosophy">Philosophy</option>
                        <option value="Sports">Sports</option>
                        <option value="Culture">Culture</option>
                        <option value="Religion">Religion</option>
                        <option value="Science & Nature">Science & Nature</option>
                        <option value="Economics">Economics</option>
                        <option value="Art & Design">Art & Design</option>
                    </select>
                </div>


                <!-- Genre -->
                <div class="form-group">
                    <label title="genre" title="genre">Genre</label>

                    <div>
                        <label><input type="radio" name="genre" value="Novel"> Novel</label>
                        <label><input type="radio" name="genre" value="Short Story"> Short Story</label>
                        <label><input type="radio" name="genre" value="Poetry"> Poetry</label>
                        <label><input type="radio" name="genre" value="Drama"> Drama</label>
                        <label><input type="radio" name="genre" value="Essay"> Essay</label>
                        <label><input type="radio" name="genre" value="Memoir"> Memoir</label>
                        <label><input type="radio" name="genre" value="Comic"> Comic</label>
                        <label><input type="radio" name="genre" value="Graphic Novel"> Graphic Novel</label>
                    </div>
                </div>

                <!-- ISBN -->
                <div class="form-group">
                    <label for="isbn" title="isbn">ISBN</label>
                    <input type="text" name="isbn" id="isbn" size="20" maxlength="20"/>
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
                                echo "<option value=\"$m\">$m</option>";
                            }
                            ?>
                        </select>

                        <select name="day" id="day">
                            <option value="">Day</option>
                            <?php
                            for($d=1; $d<=31; $d++) {
                                echo "<option value=\"$d\">$d</option>";
                            }
                            ?>
                        </select>

                        <select name="year" id="year">
                            <option value="">Year</option>
                            <?php
                            for($y=date("Y"); $y>=1900; $y--) {
                                echo "<option value=\"$y\">$y</option>";
                            }
                            ?>
                        </select>
                    </div>
                </div>
                <!-- Hardcover -->
                <div class="form-group">
                    <label for="hardcover" title="hardcover">Hardcover</label>
                    <!-- If the checkbox is checked, the submitted value will be "Yes"
                         If unchecked, $_POST['hardcover'] won’t exist at all -->
                    <input type="checkbox" name="hardcover" id="hardcover" value="Yes"/>
                </div>

                <!-- Error message, if any -->
                <?php if(!empty($_SESSION["errorMessage"])): ?>
                <div class="form-message">
                    <?php echo $_SESSION["errorMessage"]; ?>
                </div>
                <?php endif; ?>

                <!-- Submit button -->
                <div class="form-group">
                    <input type="submit" name="submit" id="submit" value="Insert info"/>
                </div>
            </fieldset>
        </form>
    </div>

    <?php
    // Clear the error message after display
    $_SESSION["errorMessage"] = "";
    ?>

    <!-- Auto-focus the first field on page load -->
    <script>
        document.getElementById("bookID").focus();
    </script>
</body>
</html>
