<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Project 2 - Billing</title>
    <link rel="stylesheet" href="estilos.css">
</head>
<body>
    <h1>Project 2 - Billing</h1>
    
    <?php 
        include("includes/menu.php"); 
        // Kick out visitors who are not logged in
        if (empty($_SESSION['user'])) {
            header("Location: login-register.php");
            exit;
        }

        // Pull this user's billing address from the database
        include("includes/openDBConn.php");
        $user = $_SESSION['user'];
        // echo("User session variable: ".$_SESSION['user']."<br>");
        // exit;

        // Get from the query string if we are editing an address
        $billingID = isset($_GET['action'], $_GET['id']) && $_GET['action'] === 'edit' 
                ? $_GET['id'] : null;
        $editing = !empty($billingID);
        
        $sql = "SELECT * FROM P2Billing WHERE Login='$user'";
        $result = mysqli_query($db, $sql);

        // If editing, fetch that row address for the sticky values
        $sticky = ['name' => '', 'address' => '', 'city' => '', 'state' => '', 
                   'zip' => '', 'cardType' => '', 'cardNum' => '', 'expDate' => ''];
        if ($editing) {
            $sql_row = "SELECT * FROM P2Billing WHERE Login='$user' AND BillingID='$billingID'";
            $result_row = mysqli_query($db, $sql_row);
            $row_edit = mysqli_fetch_assoc($result_row);
            $expMonth = $expYear = '';
            if ($row_edit) {
                $sticky['name'] = htmlspecialchars($row_edit['BillName']);
                $sticky['address'] = htmlspecialchars($row_edit['BillAddress']);
                $sticky['city'] = htmlspecialchars($row_edit['BillCity']);
                $sticky['state'] = htmlspecialchars($row_edit['BillState']);
                $sticky['zip'] = htmlspecialchars($row_edit['BillZip']);
                $sticky['cardType'] = htmlspecialchars($row_edit['CardType']);
                $sticky['cardNum'] = htmlspecialchars($row_edit['CardNumber']);
                if (!empty($row_edit['ExpDate'])) {
                    $parts = explode("/", $row_edit['ExpDate']); // e.g. "12/2025"
                    if (count($parts) == 2) {
                        $expMonth = trim($parts[0]);
                        $expYear = trim($parts[1]);
                    }
                }
                // $sticky['expDate'] = htmlspecialchars($row_edit['ExpDate']);
            } else {
                $_SESSION["billError"] = "Error: Address not found.";
            }
        }

        // If billError has never been used, create it
        if (empty($_SESSION["billError"])) {
            $_SESSION["billError"] = "";
        }
    ?>

    <!-- Table with actions to edit or delete existing addresses -->
    <div class="container">
        <!-- Display the existing billing addresses -->
        <h2>Billing Addresses</h2>
        <p>Here you can view and manage your billing addresses.</p>
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
                    <th>Card Type</th>
                    <th>Card Number</th>
                    <th>Expiration Date</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                <?php while($row = mysqli_fetch_assoc($result)): ?>
                <tr>
                    <td><?= htmlspecialchars($row['BillName']) ?></td>
                    <td><?= htmlspecialchars($row['BillAddress']) ?></td>
                    <td><?= htmlspecialchars($row['BillCity']) ?></td>
                    <td><?= htmlspecialchars($row['BillState']) ?></td>
                    <td><?= htmlspecialchars($row['BillZip']) ?></td>
                    <td><?= htmlspecialchars($row['CardType']) ?></td>
                    <td><?= htmlspecialchars($row['CardNumber']) ?></td>
                    <td><?= htmlspecialchars($row['ExpDate']) ?></td>
                    <td>
                        <a class="btn-edit"
                            href="billing.php?action=edit&id=<?= $row['BillingID'] ?>">Edit</a>
                        <a class="btn-delete"
                            href="doDeleteBilling.php?id=<?= $row['BillingID'] ?>"
                            onclick="return confirm('Delete this address?');">Delete</a>
                    </td>
                </tr>
                <?php endwhile; ?>
            </tbody>
        </table>
        <?php endif; ?>
    </div>

    <!-- Form to add or edit a billing address -->
    <div class="container">
        <form action="<?= $editing ? 'doUpdateBilling.php' : 'doAddBilling.php' ?>"
        method="post">
        <h2><?= $editing ? 'Edit Billing Address' : 'Add New Billing Address' ?></h2>
        <!-- Add new billing address form -->
            <fieldset>
                <legend><?= $editing ? 'Update Billing Address'
                            : 'Add New Billing Address' ?></legend>

                <!-- Hidden field for BillingID if editing -->
                <?php if($editing): ?>
                    <div class="form-group">
                        <label for="billingIDdis">BillingID</label>
                        <input type="text" id="billingIDdis" name="billingIDdis"
                               value="<?= htmlspecialchars($row_edit['BillingID']) ?>" disabled />
                        <!-- Hidden field to pass the actual ID to doUpdateShipping.php -->
                        <input type="hidden" name="billingID" id="billingID"
                               value="<?= htmlspecialchars($row_edit['BillingID']) ?>" />
                    </div>
                <!-- BillingID if inserting a new address -->
                <?php else: ?>
                    <div class="form-group">
                        <label for="billingID" title="billingID">BillingID</label>
                        <input type="text" name="billingID" id="billingID" maxlength="30"/>
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

                <!-- Card Type -->
                <div class="form-group">
                    <?php
                        $cardTypes = ['Visa', 'MasterCard', 'Discover', 'American Express'];
                    ?>
                    <label for="cardType" title="cardType">Card Type</label>
                    <select name="cardType" id="cardType" required>
                        <option value="" disabled
                            <?= empty($sticky['cardType']) ? 'selected' : '' ?>> Choose the card type
                        </option>
                        <?php 
                            foreach ($cardTypes as $opt): ?>
                                <option value="<?= $opt ?>"
                                    <?= $sticky['cardType'] === $opt ? 'selected' : '' ?>>
                                    <?= $opt ?>
                                </option>
                            <?php endforeach; 
                        ?>
                    </select>
                </div>

                <!-- Card Number -->
                <div class="form-group">
                    <label for="cardNum" title="cardNum">Card Number</label>
                    <input type="text" name="cardNum" id="cardNum" maxlength="16"
                           value="<?= htmlspecialchars($sticky['cardNum']) ?>"/>
                </div>

                <!-- Expiration Date -->
                <div class="form-group">
                    <label>Expiration Date (MM/YY)</label>
                    <div class="date-group">
                        <!-- MONTH -->
                        <select name="expMonth" id="expMonth" required>
                            <option value="">Month</option>
                            <?php
                            $months = array("01","02","03","04","05","06",
                                            "07","08","09","10","11","12");
                            foreach($months as $m) {
                                $selected = ($m == $expMonth) ? "selected" : "";
                                echo "<option value=\"$m\" $selected>$m</option>";
                            }
                            ?>
                        </select>

                        <!-- YEAR (two-digit, this year → +15) -->
                        <select name="expYear" id="expYear" required>
                            <option value="">Year</option>
                            <?php
                            for($y = date("y"); $y <= date("y") + 15; $y++) {
                                $yy = sprintf("%02d", $y);              // two-digit
                                $selected = ($yy == $expYear) ? "selected" : "";
                                echo "<option value=\"$yy\" $selected>$yy</option>";
                            }
                            ?>
                        </select>
                    </div>
                </div>
                
                <!-- Error message, if any -->
                <?php if(!empty($_SESSION["billError"])): ?>
                <div class="form-message">
                    <?php echo $_SESSION["billError"]; ?>
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
    $_SESSION["billError"] = "";
    // Clean up the database connection
    include ("includes/closeDbConn.php");
    ?>

    <!-- Auto-focus the first field on page load -->
    <!-- <script>
        document.getElementById("shippingID").focus();
    </script> -->
</body>
</html>
