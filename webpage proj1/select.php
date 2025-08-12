<!-- select.php -->
<?php
session_start();
include("includes/openDbConn.php");
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Project 1 - Select</title>
    <link rel = stylesheet href = "styles.css" type = "text/css">
</head>

<body>
    <h1>Project 1 - Select</h1>

    <?php include("includes/menu.php"); ?>
    
    <div class="container">
        <?php
        // Prepare SQL statement
        $sql = "SELECT BookID, Title, Author, Topic, Genre, ISBN, DatePublished, Hardcover FROM P1Books";
        $result = mysqli_query($db, $sql);

        if(empty($result))
            $num_results = 0;
        else
            $num_results = mysqli_num_rows($result);
        ?>

        <table>
            <thead>
                <tr>
                    <th>BookID</th>
                    <th>Title</th>
                    <th>Author</th>
                    <th>Topic</th>
                    <th>Genre</th>
                    <th>ISBN</th>
                    <th>DatePublished</th>
                    <th>Hardcover</th>
                    <th>Actions</th>
                </tr>
            </thead>
            
            <tbody>
                <?php
                for($i = 0; $i < $num_results; $i++) {
                    $row = mysqli_fetch_array($result);
                    $bookID = htmlspecialchars(trim($row["BookID"]));
                ?>
                <tr>
                    <td><?php echo $bookID; ?></td>
                    <td><?php echo htmlspecialchars(trim($row["Title"])); ?></td>
                    <td><?php echo htmlspecialchars(trim($row["Author"])); ?></td>
                    <td><?php echo htmlspecialchars(trim($row["Topic"])); ?></td>
                    <td><?php echo htmlspecialchars(trim($row["Genre"])); ?></td>
                    <td><?php echo htmlspecialchars(trim($row["ISBN"])); ?></td>
                    <td><?php echo htmlspecialchars(trim($row["DatePublished"])); ?></td>
                    <td><?php echo htmlspecialchars(trim($row["Hardcover"])); ?></td>

                    <td>
                        <!-- We don't need to do php echo( trim( $row["ShipperID"] ) ); because
                        we already have the BookID. -->
                        <a href="update.php?id=<?php echo $bookID; ?>" class="btn-edit">Edit</a>
                        <a href="delete.php?id=<?php echo $bookID; ?>" class="btn-delete">Delete</a>
                    </td>
                </tr>
                <?php
                }
                ?>
            </tbody>

            <tfoot>
                <tr>
                    <td colspan="9">
                        Information pulled from the P1Books table.
                    </td>
                </tr>
            </tfoot>
        </table>
        
        <?php
        // Close connection
        include("includes/closeDbConn.php");
        ?>
    </div>
</body>
</html>
