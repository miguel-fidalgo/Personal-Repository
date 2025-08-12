-- /////////////////////////////////////////////////////////////////////
-- This SQL Script shows examples of select, update, delete 
-- Insert examples where shown in the create tables file
-- /////////////////////////////////////////////////////////////////////

-- P2User examples:
SELECT * FROM P2User WHERE Login='rjg' and Passwd='php';

UPDATE P2User SET NewsLetter='Yes' WHERE Login='rjg';

INSERT INTO P2User(Login, FirstName, LastName, Passwd, Email, NewsLetter) 
    VALUES('deleteMe', 'Prof', 'Glotzbach', 'php', 'rjglotzbach@purdue.edu', 'No');

DELETE FROM P2User WHERE Login='deleteMe';



-- P2Shipping examples: 
-- You must use both Login and ShippingID in the WHERE clause to change 1 row
-- Forgetting to use both would change all rows meeting that condition
SELECT * FROM P2Shipping WHERE Login='rjg' AND ShippingID='Knoy';

UPDATE P2Shipping SET Name='Prof. Glotzbach' WHERE Login='rjg' AND ShippingID='Knoy';

INSERT INTO P2Shipping(ShippingID, Login, Name, Address, City, State, Zip) 
    VALUES('deleteMe', 'rjg', 'Prof Glotzbach', '401 N. Grant Ave', 'West Lafayette', 'IN', '47906');

DELETE FROM P2Shipping WHERE Login='rjg' AND ShippingID='deleteMe';



-- P2Billing examples: 
-- You must use both Login and BillingID in the WHERE clause to change 1 row
-- Forgetting to use both would change all rows meeting that condition
SELECT * FROM P2Billing WHERE Login='rjg' AND BillingID='VISA';

UPDATE P2Billing SET ExpDate='05/24' WHERE Login='rjg' AND BillingID='VISA';

INSERT INTO P2Billing(BillingID, Login, BillName, BillAddress, BillCity, BillState, BillZip, CardType, CardNumber, ExpDate) 
    VALUES('deleteMe', 'rjg', 'Prof Glotzbach', '401 N. Grant Ave', 'West Lafayette', 'IN', '47906', 'Visa', '1234123412341234', '04/24');

DELETE FROM P2Billing WHERE Login='rjg' AND BillingID='deleteMe';



-- if you need these, uncomment them, they drop the tables so that you can start over
-- DROP TABLE P2User;
-- DROP TABLE P2Shipping;
-- DROP TABLE P2Billing;




