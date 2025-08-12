-- /////////////////////////////////////////////////////////////////////
-- This SQL Script will create 3 tables: P2User, P2Shipping, P2Billing 
-- Then this script will insert a row into each table
-- /////////////////////////////////////////////////////////////////////

-- Create the P2User Table
CREATE TABLE P2User(
Login      char(15) PRIMARY KEY,
FirstName  char(25),
LastName   char(60),
Passwd     char(60),
Email      char(40),
NewsLetter char(4)
);

-- Create the P2Shipping Table
CREATE TABLE P2Shipping(
ShippingID char(30),
Login      char(15) REFERENCES P2User(Login),
Name       char(50),
Address    char(30),
City       char(30),
State      char(20),
Zip        char(5),
PRIMARY KEY(ShippingID, Login)
);

-- Create the P2Billing Table
CREATE TABLE P2Billing(
BillingID    char(30),
Login        char(15) REFERENCES P2User(Login),
BillName     char(50),
BillAddress  char(30),
BillCity     char(30),
BillState    char(20),
BillZip      char(5),
CardType     char(20), 
CardNumber   char(16),
ExpDate      char(5),
PRIMARY KEY(BillingID, Login)
);

-- must insert the user first because shipping and billing reference the user
INSERT INTO P2User(Login, FirstName, LastName, Passwd, Email, NewsLetter) 
    VALUES('rjg', 'Prof', 'Glotzbach', 'php', 'rjglotzbach@purdue.edu', 'No');

-- can insert into billing and/or shipping after the user exists
-- you need both Login and ShippingID to uniquely identify one row in the P2Shipping table
-- notice how the Login of 'rjg' matches the P2User 'rjg'
--   that's how you know that address belongs to that user
--   there could be several ShippingID of 'Knoy', but one could belong to 'rjg' and another to 'xyz' and another to 'qrs', etc
INSERT INTO P2Shipping(ShippingID, Login, Name, Address, City, State, Zip) 
    VALUES('Knoy', 'rjg', 'Prof Glotzbach', '401 N. Grant Ave', 'West Lafayette', 'IN', '47906');

-- you need both Login and BillingID to uniquely identify one row in the P2Billing table
-- notice how the Login of 'rjg' matches the P2User 'rjg'
--   that's how you know that billing info belongs to that user
--   there could be several BillingID of 'VISA', but one belongs to 'rjg' and another to 'abc' and another to 'def' etc
INSERT INTO P2Billing(BillingID, Login, BillName, BillAddress, BillCity, BillState, BillZip, CardType, CardNumber, ExpDate) 
    VALUES('VISA', 'rjg', 'Prof Glotzbach', '401 N. Grant Ave', 'West Lafayette', 'IN', '47906', 'Visa', '1234123412341234', '04/24');

-- if you need these, uncomment them, they drop the tables so that you can start over
-- DROP TABLE P2User;
-- DROP TABLE P2Shipping;
-- DROP TABLE P2Billing;




