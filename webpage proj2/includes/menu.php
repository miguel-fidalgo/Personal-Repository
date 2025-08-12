<!-- menu.php -->
<?php
session_start();        // all pages that include the menu inherit the session
?>
<nav class="main-menu">
    <a class="nav-link" href="index.php">HOME</a>
    <?php if (empty($_SESSION['user'])): ?>
        <a class="nav-link" href="login-register.php">LOGIN - REGISTER</a>
    <?php else: ?>
        <!-- Regular centred aligned menu items -->
        <a class="nav-link" href="shipping.php">SHIPPING</a>
        <a class="nav-link" href="billing.php">BILLING</a>

        <!-- Right aligned menu items -->
        <span class="user-info">
            Welcome back, <?= htmlspecialchars($_SESSION['user']) ?>!
            <a href="logout.php" class="logout-link">(logout)</a>
        </span>
    <?php endif; ?>
</nav>