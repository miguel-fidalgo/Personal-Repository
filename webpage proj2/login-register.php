<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Project 2 - Register or Login</title>
    <link rel="stylesheet" href="estilos.css">

    <style>
        .flex-row   {display:flex;gap:2rem;flex-wrap:wrap;}
        .half-box   {flex:1 1 320px;}
    </style>
</head>
<body>

    <h1>Register or Login</h1>
    <?php 
        include("includes/menu.php");

        // Create the session if it doesn't exist
        $_SESSION['loginError']     = $_SESSION['loginError']     ?? "";
        $_SESSION['registerError']  = $_SESSION['registerError']  ?? "";
        $_SESSION['loginSticky']    = $_SESSION['loginSticky']    ?? [];
        $_SESSION['registerSticky'] = $_SESSION['registerSticky'] ?? [];

    ?>

    <div class="container flex-row">

        <!-- ───────────────  LEFT: login  ─────────────── -->
        <form class="half-box" method="post" action="doLogin.php" autocomplete="off">
            <fieldset>
                <legend>Login</legend>

                <div class="form-group">
                    <label for="login_user">Username</label>
                    <input type="text" name="login" id="login_user" maxlength="15"
                        value="<?=htmlspecialchars($_SESSION['loginSticky']['login'] ?? '')?>">
                </div>

                <div class="form-group">
                    <label for="login_pass">Password</label>
                    <input type="password" name="passwd" id="login_pass" maxlength="60">
                </div>

                <?php if($_SESSION['loginError']!==""): ?>
                <div class="form-message"><?= $_SESSION['loginError']; ?></div>
                <?php endif; ?>

                <div class="form-group">
                    <input type="submit" value="Login">
                </div>
            </fieldset>
        </form>

        <!-- ──────────────  RIGHT: register  ───────────── -->
        <form class="half-box" method="post" action="doRegister.php" autocomplete="off">
            <fieldset>
                <legend>Register</legend>

                <div class="form-group">
                    <label>Username <span style="color:#c00">*</span></label>
                    <input type="text" name="login" maxlength="15"
                        value="<?=htmlspecialchars($_SESSION['registerSticky']['login'] ?? '')?>">
                </div>

                <div class="form-group">
                    <label>First Name <span style="color:#c00">*</span></label>
                    <input type="text" name="first" maxlength="25"
                        value="<?=htmlspecialchars($_SESSION['registerSticky']['first'] ?? '')?>">
                </div>

                <div class="form-group">
                    <label>Last Name <span style="color:#c00">*</span></label>
                    <input type="text" name="last" maxlength="60"
                        value="<?=htmlspecialchars($_SESSION['registerSticky']['last'] ?? '')?>">
                </div>

                <div class="form-group">
                    <label>Password <span style="color:#c00">*</span></label>
                    <input type="password" name="passwd" maxlength="60">
                </div>

                <div class="form-group">
                    <label>Email <span style="color:#c00">*</span></label>
                    <input type="text" name="email" maxlength="40"
                        value="<?=htmlspecialchars($_SESSION['registerSticky']['email'] ?? '')?>">
                </div>

                <div class="form-group">
                    <label>Newsletter <span style="color:#c00">*</span></label>
                    <input type="radio" name="newsletter" value="Yes"
                        <?= (($_SESSION['registerSticky']['newsletter'] ?? 'No')==='Yes')?'checked':''; ?>> Yes
                    <input type="radio" name="newsletter" value="No"
                        <?= (($_SESSION['registerSticky']['newsletter'] ?? 'No')==='No')?'checked':''; ?>> No
                </div>

                <?php if($_SESSION['registerError']!==""): ?>
                <div class="form-message"><?= $_SESSION['registerError']; ?></div>
                <?php endif; ?>

                <div class="form-group">
                    <input type="submit" value="Register">
                </div>
            </fieldset>
        </form>
    </div>

    <?php
    /* wipe flash once rendered */
    $_SESSION['loginError'] = $_SESSION['registerError'] = "";
    $_SESSION['loginSticky'] = $_SESSION['registerSticky'] = [];
    ?>
    <script>
        document.getElementById("login_user").focus();
    </script>

</body>
</html>
