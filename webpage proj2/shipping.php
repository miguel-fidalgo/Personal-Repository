<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Project 2 - Shipping</title>
    <link rel="stylesheet" href="estilos.css">
</head>
<body>
    <h1>Project 2 - Shipping</h1>
    
    <?php 
        include("includes/menu.php"); 
        // Kick out visitors who are not logged in
        if (empty($_SESSION['user'])) {
            header("Location: login-register.php");
            exit;
        }

        // Pull this user's shipping address from the database
        include("includes/openDBConn.php");
        $user = $_SESSION['user'];
        // echo("User session variable: ".$_SESSION['user']."<br>");
        // exit;

        // Get from the query string if we are editing an address
        $shippingID = isset($_GET['action'], $_GET['id']) && $_GET['action'] === 'edit' 
                ? $_GET['id'] : null;
        $editing = !empty($shippingID);
        
        $sql = "SELECT * FROM P2Shipping WHERE Login='$user'";
        $result = mysqli_query($db, $sql);

        // If editing, fetch that row address for the sticky values
        $sticky = ['name' => '', 'address' => '', 'city' => '', 'state' => '', 'zip' => ''];
        if ($editing) {
            $sql_row = "SELECT * FROM P2Shipping WHERE Login='$user' AND ShippingID='$shippingID'";
            $result_row = mysqli_query($db, $sql_row);
            $row_edit = mysqli_fetch_assoc($result_row);
            if ($row_edit) {
                $sticky['name'] = htmlspecialchars($row_edit['Name']);
                $sticky['address'] = htmlspecialchars($row_edit['Address']);
                $sticky['city'] = htmlspecialchars($row_edit['City']);
                $sticky['state'] = htmlspecialchars($row_edit['State']);
                $sticky['zip'] = htmlspecialchars($row_edit['Zip']);
            } else {
                $_SESSION["shipError"] = "Error: Address not found.";
            }
        }

        // If shipError has never been used, create it
        if (empty($_SESSION["shipError"])) {
            $_SESSION["shipError"] = "";
        }
    ?>

    <!-- Table with actions to edit or delete existing addresses -->
    <div class="container">
        <!-- Display the existing shipping addresses -->
        <h2>Shipping Addresses</h2>
        <p>Here you can view and manage your shipping addresses.</p>
        <?php if (mysqli_num_rows($result) === 0): ?>
            <p style="font-style:italic">No addresses yet - add one below.</p>
        <?php else: ?>

        <table>
            <thead>
                <tr>
                    <th>Name</th>
                    <th>Address</th>
                    <th>City</th>
                    <th>State</th>
                    <th>Zip</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <?php while($row = mysqli_fetch_assoc($result)): ?>
                <tr>
                    <td><?= htmlspecialchars($row['Name']) ?></td>
                    <td><?= htmlspecialchars($row['Address']) ?></td>
                    <td><?= htmlspecialchars($row['City']) ?></td>
                    <td><?= htmlspecialchars($row['State']) ?></td>
                    <td><?= htmlspecialchars($row['Zip']) ?></td>
                    <td>
                        <a class="btn-edit"
                            href="shipping.php?action=edit&id=<?= $row['ShippingID'] ?>">Edit</a>
                        <a class="btn-delete"
                            href="doDeleteShipping.php?id=<?= $row['ShippingID'] ?>"
                            onclick="return confirm('Delete this address?');">Delete</a>
                    </td>
                </tr>
                <?php endwhile; ?>
            </tbody>
        </table>
        <?php endif; ?>
    </div>

    <!-- Form to add or edit a shipping address -->
    <div class="container">
        <form action="<?= $editing ? 'doUpdateShipping.php' : 'doAddShipping.php' ?>"
        method="post">
        <h2><?= $editing ? 'Edit Shipping Address' : 'Add New Shipping Address' ?></h2>
        <!-- Add new shipping address form -->
            <fieldset>
                <legend><?= $editing ? 'Update Shipping Address'
                            : 'Add New Shipping Address' ?></legend>

                <!-- Hidden field for ShippingID if editing -->
                <?php if($editing): ?>
                    <div class="form-group">
                        <label for="shippingIDdis">ShippingID</label>
                        <input type="text" id="shippingIDdis" name="shippingIDdis"
                               value="<?= htmlspecialchars($row_edit['ShippingID']) ?>" disabled />
                        <!-- Hidden field to pass the actual ID to doUpdateShipping.php -->
                        <input type="hidden" name="shippingID" id="shippingID"
                               value="<?= htmlspecialchars($row_edit['ShippingID']) ?>" />
                    </div>
                <!-- ShippingID -->
                <?php else: ?>
                    <div class="form-group">
                        <label for="shippingID" title="shippingID">ShippingID</label>
                        <input type="text" name="shippingID" id="shippingID" maxlength="30"/>
                    </div>
                <?php endif; ?>

                <!-- Name -->
                <div class="form-group">
                    <label for="name" title="name">Name</label>
                    <input type="text" name="name" id="name" maxlength="50"
                           value="<?= htmlspecialchars($sticky['name']) ?>"/>
                </div>

                <!-- Address -->
                <div class="form-group">
                    <label for="address" title="address">Address</label>
                    <input type="text" name="address" id="address" maxlength="30"
                           value="<?= htmlspecialchars($sticky['address']) ?>"/>
                </div>

                <!-- City -->
                <div class="form-group">
                    <label for="city" title="city">City</label>
                    <input type="text" name="city" id="city" maxlength="30"
                           value="<?= htmlspecialchars($sticky['city']) ?>"/>
                </div>

                <!-- State -->
                <div class="form-group">
                    <label for="state" title="state">State</label>
                    <input type="text" name="state" id="state" maxlength="20"
                           value="<?= htmlspecialchars($sticky['state']) ?>"/>
                </div>

                <!-- Zip -->
                <div class="form-group">
                    <label for="zip" title="zip">Zip</label>
                    <input type="text" name="zip" id="zip" maxlength="5"
                           value="<?= htmlspecialchars($sticky['zip']) ?>"/>
                </div>

                <!-- Error message, if any -->
                <?php if(!empty($_SESSION["shipError"])): ?>
                <div class="form-message">
                    <?php echo $_SESSION["shipError"]; ?>
                </div>
                <?php endif; ?>

                <!-- Submit Button -->
                <div class="form-group">
                    <input type="submit" 
                           value="<?= $editing ? 'Update Address' : 'Add Address' ?>"/>
                </div>
                
            </fieldset>
        </form>
    </div>

    <?php
    // Clear the error message after display
    $_SESSION["shipError"] = "";
    // Clean up the database connection
    include ("includes/closeDbConn.php");
    ?>

    <!-- Auto-focus the first field on page load -->
    <!-- <script>
        document.getElementById("shippingID").focus();
    </script> -->
</body>
</html>
