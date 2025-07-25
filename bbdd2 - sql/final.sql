-- -----------------------------------
-- - SCRIPTS DE CREACIÓN E INSERCIÓN -
-- -----------------------------------

-- - TABLES DESTRUCTION  - - - - - - -
-- -----------------------------------

DROP TABLE Client_Lines CASCADE CONSTRAINTS;
DROP TABLE Orders_Clients CASCADE CONSTRAINTS;
DROP TABLE Client_Cards CASCADE CONSTRAINTS;
DROP TABLE Client_Addresses CASCADE CONSTRAINTS;
DROP TABLE Lines_Anonym CASCADE CONSTRAINTS;
DROP TABLE Orders_Anonym CASCADE CONSTRAINTS;
DROP TABLE AnonyPosts  CASCADE CONSTRAINTS;
DROP TABLE Posts  CASCADE CONSTRAINTS;
DROP TABLE Clients CASCADE CONSTRAINTS;
DROP TABLE Replacements CASCADE CONSTRAINTS;
DROP TABLE Supply_lines CASCADE CONSTRAINTS;
DROP TABLE Providers CASCADE CONSTRAINTS;
DROP TABLE References  CASCADE CONSTRAINTS;
DROP TABLE Products CASCADE CONSTRAINTS;
DROP TABLE Origins CASCADE CONSTRAINTS;
DROP TABLE Varietals CASCADE CONSTRAINTS;



-- - VALIDATION TABLES - - - - - - - -
-- -----------------------------------
CREATE TABLE Varietals (
  name      VARCHAR2(30),
  CONSTRAINT pk_varietal PRIMARY KEY(name) 
);
CREATE TABLE Origins (
  name      VARCHAR2(30),
  CONSTRAINT pk_origins PRIMARY KEY(name) 
);


-- - TABLES CREATION - - - - - - - - -
-- -----------------------------------

CREATE TABLE Products (
  product      VARCHAR2(50),
  coffea       CHAR(1) NOT NULL,
  varietal     VARCHAR2(30) NOT NULL,
  origin       VARCHAR2(15) NOT NULL,
  roast        CHAR(1) NOT NULL,
  decaf        CHAR(1) NOT NULL,
  CONSTRAINT pk_products PRIMARY KEY(product), 
  CONSTRAINT fk_products_varietals FOREIGN KEY(varietal) REFERENCES Varietals,
  CONSTRAINT fk_products_origins FOREIGN KEY(origin) REFERENCES Origins,
  CONSTRAINT D_coffea CHECK (coffea IN ('A','C','L')), 
  CONSTRAINT D_roast CHECK (roast IN ('N','H','B')), 
  CONSTRAINT D_decaf CHECK (decaf IN ('Y','N')) 
);


CREATE TABLE References (
  barCode      CHAR(15),
  product      VARCHAR2(50) NOT NULL,
  format       CHAR(1) NOT NULL,
  pack_type    VARCHAR2(10) NOT NULL,
  pack_unit    VARCHAR2(10) NOT NULL,
  quantity     NUMBER(6) NOT NULL,
  price        NUMBER(12,2) NOT NULL,
  cur_stock    NUMBER(5) DEFAULT(0) NOT NULL,
  min_stock    NUMBER(5) DEFAULT(5) NOT NULL,
  max_stock    NUMBER(5) DEFAULT(15) NOT NULL,
  CONSTRAINT pk_references PRIMARY KEY(barcode), 
  CONSTRAINT uk_references UNIQUE (product,barcode), 
  CONSTRAINT fk_references_products FOREIGN KEY(product) 
             REFERENCES Products ON DELETE CASCADE,
  CONSTRAINT D_format CHECK (format IN ('C','G','P','R','B','F'))
);


CREATE TABLE Providers (
  taxID     CHAR(10),
  name      VARCHAR2(35) NOT NULL,
  person    VARCHAR2(90) NOT NULL,
  email     VARCHAR2(60) NOT NULL,
  mobile    NUMBER(9) NOT NULL,
  bankAcc   VARCHAR2(30),
  address   VARCHAR2(120) NOT NULL,
  country   VARCHAR2(45) NOT NULL,
  CONSTRAINT pk_providers PRIMARY KEY(taxID) 
);


CREATE TABLE Supply_Lines (
  taxID     CHAR(10),
  barCode   CHAR(15),
  cost      NUMBER(10,2) NOT NULL,
  CONSTRAINT pk_supply PRIMARY KEY(taxID,barcode), 
  CONSTRAINT fk_supply_references FOREIGN KEY(barcode) 
             REFERENCES References ON DELETE CASCADE,
  CONSTRAINT fk_supply_providers FOREIGN KEY(taxID) 
             REFERENCES providers ON DELETE CASCADE
);


CREATE TABLE Replacements (
  taxID     CHAR(10),
  barCode   CHAR(15),
  orderdate DATE,
  status    CHAR(1) DEFAULT ('D') NOT NULL,
  units     NUMBER(5) NOT NULL,
  deldate   DATE,
  payment   NUMBER(12,2) NOT NULL,
  CONSTRAINT pk_replacements PRIMARY KEY(taxID,barcode,orderdate), 
  CONSTRAINT fk_replacements_supply FOREIGN KEY(taxID,barcode) REFERENCES Supply_Lines,
  CONSTRAINT D_status CHECK (status IN ('D','P','F'))
);


CREATE TABLE Clients (
  username      VARCHAR2(30),
  reg_datetime  DATE NOT NULL,
  user_passw    VARCHAR2(15) NOT NULL,
  name          VARCHAR2(35) NOT NULL,
  surn1         VARCHAR2(30) NOT NULL,
  surn2         VARCHAR2(30),
  email         VARCHAR2(60),
  mobile        NUMBER(9),
  preference    VARCHAR2(12) DEFAULT('EMAIL') NOT NULL,
  voucher       NUMBER(2) DEFAULT (0) NOT NULL,
  voucher_exp   DATE,
  CONSTRAINT pk_ PRIMARY KEY(username),
  CONSTRAINT ck_client CHECK (email is not null or mobile is not null) 
);


CREATE TABLE Posts (
  username   VARCHAR2(30),
  postdate   DATE,
  barCode    CHAR(15),
  product    VARCHAR2(50) NOT NULL,
  score      NUMBER(1) NOT NULL, 
  title      VARCHAR2(50),
  text       VARCHAR2(2000) NOT NULL, 
  likes      NUMBER(9) DEFAULT(0) NOT NULL, 
  endorsed   DATE, -- null means it isn't endorsed; else, date of last purchase	
  CONSTRAINT pk_posts PRIMARY KEY(username,postdate),
  CONSTRAINT fk_posts_clients FOREIGN KEY(username) REFERENCES Clients,
  CONSTRAINT fk_posts_references FOREIGN KEY(product,barcode) 
             REFERENCES References(product,barcode),
  CONSTRAINT D_postscore CHECK (score between 0 and 5)
);

CREATE TABLE AnonyPosts (
  postdate   DATE,
  barCode    CHAR(15),
  product    VARCHAR2(50) NOT NULL,
  score      NUMBER(1) NOT NULL, 
  title      VARCHAR2(50),
  text       VARCHAR2(2000) NOT NULL, 
  likes      NUMBER(9) DEFAULT(0) NOT NULL, 
  endorsed   DATE, -- null means it isn't endorsed; else, date of last purchase	
  CONSTRAINT pk_anonyposts PRIMARY KEY(postdate),
  CONSTRAINT fk_anonyposts_references FOREIGN KEY(product,barcode) 
             REFERENCES References(product,barcode), 
  CONSTRAINT D_anonyscore CHECK (score between 0 and 5)
);


CREATE TABLE Orders_Anonym (
  orderdate     DATE,
  contact       VARCHAR2(60),  -- either email or mobile if email is null
  contact2      NUMBER(9),     -- mobile (null, unless both email and mobile not null)
  dliv_datetime DATE,
  name          VARCHAR2(35) NOT NULL,
  surn1         VARCHAR2(30) NOT NULL,
  surn2         VARCHAR2(30),
  bill_waytype  CHAR(10) NOT NULL,
  bill_wayname  CHAR(30) NOT NULL,
  bill_gate     CHAR(3),
  bill_block    CHAR(1),
  bill_stairw   CHAR(2),
  bill_floor    CHAR(7),
  bill_door     CHAR(1),
  bill_ZIP      CHAR(5) NOT NULL,
  bill_town     CHAR(45) NOT NULL,
  bill_country  CHAR(45) NOT NULL,
  dliv_waytype  CHAR(10) NOT NULL,
  dliv_wayname  CHAR(30) NOT NULL,
  dliv_gate     CHAR(3),
  dliv_block    CHAR(1),
  dliv_stairw   CHAR(2),
  dliv_floor    CHAR(7),
  dliv_door     CHAR(2),
  dliv_ZIP      CHAR(5) NOT NULL,
  dliv_town     CHAR(45),
  dliv_country  CHAR(45),
  CONSTRAINT pk_anonyorders PRIMARY KEY(orderdate,contact,dliv_town,dliv_country)
);

CREATE TABLE Lines_Anonym (
  orderdate     DATE,
  contact       VARCHAR2(60),
  dliv_town     CHAR(45),
  dliv_country  CHAR(45),
  barcode       CHAR(15),
  price         NUMBER(12,2) NOT NULL,
  quantity      NUMBER(2) NOT NULL,
  pay_type      VARCHAR2(15) NOT NULL,
  pay_datetime  DATE,
  card_comp     VARCHAR2(15),
  card_num      NUMBER(20),
  card_holder   VARCHAR2(30),
  card_expir    DATE,
  CONSTRAINT pk_anonylines PRIMARY KEY(orderdate,contact,dliv_town,dliv_country,barcode),
  CONSTRAINT fk_anonylines_anonyorders FOREIGN KEY(orderdate,contact,dliv_town,dliv_country) 
             REFERENCES Orders_Anonym ON DELETE CASCADE, 
  CONSTRAINT fk_anonylines_references FOREIGN KEY(barcode) REFERENCES References,
  CONSTRAINT D_anonycards CHECK (UPPER(pay_type)!='CREDIT CARD' OR  
                                 (card_comp IS NOT NULL AND card_num IS NOT NULL AND 
                                  card_holder IS NOT NULL AND card_expir IS NOT NULL))
);


CREATE TABLE Client_Addresses (
  username VARCHAR2(30),
  waytype  VARCHAR2(10) NOT NULL,
  wayname  VARCHAR2(30) NOT NULL,
  gate     VARCHAR2(3),
  block    VARCHAR2(1),
  stairw   VARCHAR2(2),
  floor    VARCHAR2(7),
  door     VARCHAR2(2),
  ZIP      VARCHAR2(5) NOT NULL,
  town     VARCHAR2(45),
  country  VARCHAR2(45),
  CONSTRAINT pk_address PRIMARY KEY(username,town,country),
  CONSTRAINT fk_addresses_clients FOREIGN KEY(username) 
             REFERENCES Clients ON DELETE CASCADE
);


CREATE TABLE Client_Cards (
  cardnum      NUMBER(20),
  username      VARCHAR2(30) NOT NULL,
  card_comp     VARCHAR2(15) NOT NULL,
  card_holder   VARCHAR2(30) NOT NULL,
  card_expir    DATE NOT NULL,
  CONSTRAINT pk_cards PRIMARY KEY(cardnum),
  CONSTRAINT fk_cards_clients FOREIGN KEY(username) 
             REFERENCES Clients ON DELETE CASCADE
);


CREATE TABLE Orders_Clients (
  orderdate     DATE,
  username      VARCHAR2(30),
  town          VARCHAR2(45),
  country       VARCHAR2(45),
  dliv_datetime DATE,
  bill_town     VARCHAR2(45) NOT NULL,
  bill_country  VARCHAR2(45) NOT NULL,
  discount      NUMBER(2) default(0), 
  CONSTRAINT pk_clientorders PRIMARY KEY(orderdate,username,town,country),
  CONSTRAINT fk_order_address FOREIGN KEY(username,town,country) REFERENCES Client_Addresses,
  CONSTRAINT fk_order_bill FOREIGN KEY(username,bill_town,bill_country) 
                REFERENCES Client_Addresses
);

CREATE TABLE Client_Lines (
  orderdate     DATE,
  username      VARCHAR2(30),
  town          VARCHAR2(45),
  country       VARCHAR2(45),
  barcode       CHAR(15),
  price         NUMBER(12,2) NOT NULL,
  quantity      VARCHAR2(2) NOT NULL,
  pay_type      VARCHAR2(15) NOT NULL,
  pay_datetime  DATE,
  cardnum       NUMBER(20),
  CONSTRAINT pk_clientlines PRIMARY KEY(orderdate,username,town,country,barcode),
  CONSTRAINT fk_clientlines_anonyorders FOREIGN KEY(orderdate,username,town,country) 
             REFERENCES Orders_Clients ON DELETE CASCADE, 
  CONSTRAINT fk_clientlines_references FOREIGN KEY(barcode) REFERENCES References,
  CONSTRAINT fk_lines_creditcard FOREIGN KEY(cardnum) REFERENCES Client_Cards,
  CONSTRAINT D_clientcards CHECK (UPPER(pay_type)!='CREDIT CARD' OR cardnum IS NOT NULL)
);

-- -----------------------------------
-- - SCRIPTS DE INSERCIÓN DE DATOS -
-- -----------------------------------

INSERT INTO Varietals(name)  (SELECT DISTINCT rtrim(varietal) FROM FSDB.catalogue);
-- 66 rows created.
INSERT INTO Origins(name) (SELECT DISTINCT rtrim(origin) FROM FSDB.catalogue);
-- 33 rows created.

INSERT INTO Products (product, coffea, varietal, origin, roast, decaf)
 (SELECT DISTINCT rtrim(product), substr(coffea,1,1), rtrim(varietal), rtrim(origin), 
                  upper(substr(roasting,1,1)), upper(substr(decaf,1,1)) 
     FROM FSDB.catalogue
 );
-- 750 rows created.

INSERT INTO Products (product, coffea, varietal, origin, roast, decaf)
 (SELECT DISTINCT rtrim(product), substr(coffea,1,1), rtrim(varietal), rtrim(origin), 
                  upper(substr(roasting,1,1)), CASE substr(dcafprocess,1,1) WHEN '-' then 'N' else 'Y' end 
     FROM FSDB.trolley where rtrim(product) not in (select product from products)
 );
-- 2 rows created.
-- notice for these there will be no supply line by now


INSERT INTO References (barCode, product, format, pack_type, pack_unit, quantity, 
                        price, cur_stock, min_stock, max_stock)
 (SELECT DISTINCT barCode, trim(product), case format when 'raw bean' then 'B' else upper(substr(format,1,1)) end,
                  substr(packaging,1,instr(packaging,' ')-1), substr(packaging,instr(packaging,' ',1,2)+1),
                  to_number(substr(substr(packaging,1,instr(packaging,' ',1,2)-1),instr(packaging,' ')+1)), 
                  to_number(substr(retail_price,1,instr(retail_price,' ')-1),'9999.99'), 
                  to_number(cur_stock), to_number(min_stock), to_number(max_stock) 
     FROM FSDB.catalogue 
     WHERE rtrim(barCode) is not null
 );
-- 3240 rows created.

INSERT INTO References (barCode, product, format, pack_type, pack_unit, quantity,price)
 (SELECT DISTINCT barCode, trim(product), case prodtype when 'raw bean' then 'B' else upper(substr(prodtype,1,1)) end,
                  substr(packaging,1,instr(packaging,' ')-1), substr(packaging,instr(packaging,' ',1,2)+1),
                  to_number(substr(substr(packaging,1,instr(packaging,' ',1,2)-1),instr(packaging,' ')+1)), 
                  to_number(substr(base_price,1,instr(base_price,' ')-1),'9999.99')
     FROM FSDB.trolley 
     WHERE rtrim(barCode) is not null AND rtrim(barcode) not in (select barcode from references)
 );
-- 1 rows created.
-- notice for this one there will be no supply line by now


INSERT INTO Providers (taxID, name, person, email, mobile, bankAcc, address, country)
 (SELECT DISTINCT rtrim(prov_taxID), rtrim(supplier), rtrim(prov_person), rtrim(prov_email), to_number(prov_mobile), 
                  rtrim(prov_bankAcc), rtrim(prov_address), rtrim(prov_country)
     FROM FSDB.catalogue 
     WHERE prov_taxid is not null
 );
-- 249 rows created.
-- first problem (primary key violation; document)
-- 1 row skipped. 


INSERT INTO Supply_Lines (taxID, barCode, cost)
 (SELECT p,b,min(c) FROM
   (SELECT DISTINCT rtrim(prov_taxID) p, barCode b, to_number(substr(cost_price,1,instr(cost_price,' ')-1),'9999.99') c
       FROM FSDB.catalogue
       WHERE prov_taxid is not null AND rtrim(barCode) is not null
   ) GROUP BY p,b
 );
-- 6468 rows created.
-- 1 null row skipped. -- first problem (same row; document)
-- second problem (primary key violation; several costs for the same supplier&barcode; document)
-- 5 repeated supply_lines; heal data: kept minimum cost (implicit semantic assumption)


-- INSERT INTO Replacements (taxID, barCode, orderdate, status, units, deldate, payment)
-- no data


INSERT INTO Clients (username, reg_datetime, user_passw, name, surn1, surn2, email, mobile)
 (SELECT DISTINCT rtrim(username), to_date(reg_date||reg_time,'yyyy / mm / ddhh:mi:ss am'), rtrim(user_passw),
                  rtrim(client_name), rtrim(client_surn1), rtrim(client_surn2), rtrim(client_email), to_number(client_mobile)
     FROM FSDB.trolley
     WHERE username is not null AND client_email is not null
);
-- 688 rows created.

INSERT INTO Clients (username, reg_datetime, user_passw, name, surn1, surn2, mobile, preference)
 (SELECT DISTINCT rtrim(username), to_date(reg_date||reg_time,'yyyy / mm / ddhh:mi:ss am'), rtrim(user_passw),
                  rtrim(client_name), rtrim(client_surn1), rtrim(client_surn2), to_number(client_mobile), 'SMS'
     FROM FSDB.trolley
     WHERE username is not null AND client_email is null 
);
-- implicit assumption: when email is null, default preference is SMS
-- 80 rows created.


INSERT INTO Posts (username, postdate, barCode, product, score, title, text, likes)
 -- null means it isn't endorsed; else, date of last purchase	
 (SELECT DISTINCT rtrim(username), to_date(post_date||post_time,'yyyy / mm / ddhh:mi:ss am'), 
         rtrim(barCode), rtrim(product), to_number(score), rtrim(title), rtrim(text), to_number(likes) 
     FROM FSDB.posts
     WHERE rtrim(product) IN (SELECT distinct product FROM references)
);
-- no post is endorsed yet
-- third problem: one product (and all its references) missing; 
-- solution: skip them (27 rows skipped); document the product
-- 3429 rows created.

-- INSERT INTO AnonyPosts (postdate, barCode, product, score, title, text, likes, endorsed) 
-- no data


INSERT INTO Orders_Anonym (orderdate, contact, contact2, dliv_datetime, name, surn1, surn2, bill_waytype,
                           bill_wayname, bill_gate, bill_block, bill_stairw, bill_floor, bill_door, bill_ZIP, 
                           bill_town, bill_country, dliv_waytype, dliv_wayname, dliv_gate, dliv_block, 
                           dliv_stairw, dliv_floor, dliv_door, dliv_ZIP, dliv_town, dliv_country)
 (SELECT DISTINCT to_date(orderdate||ordertime,'yyyy / mm / ddhh:mi:ss am'), 
                  nvl(rtrim(client_email),rtrim(client_mobile)), nvl2(rtrim(client_email),rtrim(client_mobile),null),
                  to_date(dliv_date||dliv_time,'yyyy / mm / ddhh:mi:ss am'), 
                  rtrim(client_name), rtrim(client_surn1), rtrim(client_surn2), 
                  rtrim(bill_waytype), rtrim(bill_wayname), rtrim(bill_gate), rtrim(bill_block), rtrim(bill_stairw), 
                  rtrim(bill_floor), rtrim(bill_door), rtrim(bill_ZIP), rtrim(bill_town), rtrim(bill_country),
                  rtrim(dliv_waytype), rtrim(dliv_wayname), rtrim(dliv_gate), rtrim(dliv_block), rtrim(dliv_stairw), 
                  rtrim(dliv_floor), rtrim(dliv_door), rtrim(dliv_ZIP), rtrim(dliv_town), rtrim(dliv_country)
 FROM FSDB.trolley
 WHERE rtrim(username) IS NULL
);
-- either email or mobile if email is null
-- mobile (null, unless both email and mobile )
-- 3207 rows created.


INSERT INTO Lines_Anonym (orderdate, contact, dliv_town, dliv_country, barcode, price, quantity, 
                           pay_type, pay_datetime, card_comp, card_num, card_holder, card_expir)
 (SELECT DISTINCT to_date(orderdate||ordertime,'yyyy / mm / ddhh:mi:ss am'), 
                  nvl(rtrim(client_email),rtrim(client_mobile)), rtrim(dliv_town), rtrim(dliv_country), rtrim(barcode), 
                  to_number(substr(base_price,1,instr(base_price,' ')-1),'9999.99'), to_number(quantity),
                  rtrim(payment_type), to_date(payment_date||payment_time,'yyyy / mm / ddhh:mi:ss am'),
                  rtrim(card_company), to_number(card_number), rtrim(card_holder), to_date(card_expiratn,'mm/yy')
     FROM FSDB.trolley
     WHERE rtrim(username) IS NULL
);
-- 3537 rows created.


INSERT INTO Client_Addresses (username, waytype, wayname, gate, block, stairw, floor, door, ZIP, town, country)
 (SELECT DISTINCT rtrim(username), rtrim(bill_waytype), rtrim(bill_wayname), rtrim(bill_gate), 
                  rtrim(bill_block), rtrim(bill_stairw), rtrim(bill_floor), rtrim(bill_door), 
                  rtrim(bill_ZIP), rtrim(bill_town), rtrim(bill_country)
     FROM FSDB.trolley 
     WHERE rtrim(username) is not null
);
-- 768 rows created.

INSERT INTO Client_Addresses (username, waytype, wayname, gate, block, stairw, floor, door, ZIP, town, country)
 (SELECT DISTINCT rtrim(username), rtrim(dliv_waytype), rtrim(dliv_wayname), rtrim(dliv_gate), 
                  rtrim(dliv_block), rtrim(dliv_stairw), rtrim(dliv_floor), rtrim(dliv_door), 
                  rtrim(dliv_ZIP), rtrim(dliv_town), rtrim(dliv_country)
     FROM FSDB.trolley 
     WHERE rtrim(username) is not null AND 
           (rtrim(username),rtrim(dliv_town), rtrim(dliv_country)) NOT IN (SELECT username,town,country from Client_Addresses)
);
-- 1222 rows created.


INSERT INTO Client_Cards (cardnum, username, card_comp, card_holder, card_expir)
 (SELECT DISTINCT to_number(card_number), rtrim(username), rtrim(card_company), rtrim(card_holder), 
                  to_date(card_expiratn,'mm/yy')
 FROM FSDB.trolley
    WHERE rtrim(username) is not null AND to_number(card_number) IS NOT NULL
);
-- 472 rows created.


INSERT INTO Orders_Clients (orderdate, username, town, country, dliv_datetime, bill_town, bill_country) 
 (SELECT DISTINCT to_date(orderdate||ordertime,'yyyy / mm / ddhh:mi:ss am'), 
                  rtrim(username), rtrim(dliv_town), rtrim(dliv_country), 
                  to_date(dliv_date||dliv_time,'yyyy / mm / ddhh:mi:ss am'), 
                  rtrim(bill_town), rtrim(bill_country)
    FROM FSDB.trolley
    WHERE rtrim(username) IS NOT NULL
);
-- 50140 rows created.

INSERT INTO Client_Lines (orderdate, username, town, country, barcode, cardnum, price, quantity, pay_type, pay_datetime)
 (SELECT DISTINCT to_date(orderdate||ordertime,'yyyy / mm / ddhh:mi:ss am'), 
                  rtrim(username), rtrim(dliv_town), rtrim(dliv_country), barcode, to_number(card_number), 
                  to_number(substr(base_price,1,instr(base_price,' ')-1),'9999.99'), to_number(quantity),
                  rtrim(payment_type), to_date(payment_date||payment_time,'yyyy / mm / ddhh:mi:ss am')
    FROM FSDB.trolley
    WHERE rtrim(username) IS NOT NULL
);
-- 55206 rows created.
/


-------------INICIAMOS LA SALIDA DEL SERVIDOR-------------

SET SERVEROUTPUT ON;

-------------CONSULTA 1--------------


WITH AUX AS (
    SELECT BARCODE, QUANTITY, PRICE, ORDERDATE, COUNTRY FROM CLIENT_LINES
    WHERE ORDERDATE > ADD_MONTHS(SYSDATE, -12)
    UNION
    SELECT BARCODE, TO_CHAR(QUANTITY), PRICE, ORDERDATE, DLIV_COUNTRY FROM LINES_ANONYM
    WHERE ORDERDATE > ADD_MONTHS(SYSDATE, -12)
),

AUX2 AS(
    SELECT BARCODE, COUNT('X') AS N_COMPRADORES, SUM(QUANTITY) AS VENTAS, PRICE, SUM(QUANTITY)*PRICE AS INGRESOS, ROUND(AVG(QUANTITY),2) AS PROMEDIO_VENTAS
    FROM AUX
    GROUP BY BARCODE, PRICE
),

-- Sirve para mezclar los barcodes y saber el varietal de cada uno
A AS(
    SELECT DISTINCT barcode, varietal, origin FROM PRODUCTS NATURAL JOIN REFERENCES
),

-- Sirve para mezclar la subconsulta anterior con los datos de cada varietal
ORDEN_VENTAS AS (
    SELECT ORIGIN, VARIETAL, N_COMPRADORES, VENTAS, PRICE, INGRESOS, PROMEDIO_VENTAS,
           ROW_NUMBER() OVER (PARTITION BY ORIGIN ORDER BY VENTAS DESC) AS RN
    FROM A NATURAL JOIN AUX2
),
--SELECT ORIGIN, VARIETAL, VENTAS, RN FROM VENTAS_RANKED; --Para ver como se hace la partición
MAX_VENTAS_VARIETAL AS(
    SELECT ORIGIN, VARIETAL, VENTAS, N_COMPRADORES, PRICE, INGRESOS, PROMEDIO_VENTAS
    FROM ORDEN_VENTAS
    WHERE RN = 1
),

-- Cantidades totales del varietal por país comprador
CONSUMIDORES_1 AS (
    SELECT VARIETAL, COUNTRY, SUM(QUANTITY) AS CANTIDAD
    FROM A NATURAL JOIN AUX GROUP BY VARIETAL, COUNTRY
),

-- Se añade la columna con máximo de ventas por varietal
CONSUMIDORES_2 AS (
    SELECT VARIETAL, COUNTRY, CANTIDAD, VENTAS, ROUND(CANTIDAD/VENTAS,3)*100 AS PORCENTAJE
    FROM CONSUMIDORES_1
    NATURAL JOIN MAX_VENTAS_VARIETAL
),

-- Filtras por los mayores del 1%
CONSUMIDORES_3 AS(
    SELECT VARIETAL, COUNT('X') AS PAISES_POTENCIALES FROM CONSUMIDORES_2 WHERE PORCENTAJE > 1 GROUP BY VARIETAL
)

SELECT ORIGIN, VARIETAL, VENTAS, N_COMPRADORES, PRICE, INGRESOS, PROMEDIO_VENTAS, PAISES_POTENCIALES 
FROM MAX_VENTAS_VARIETAL NATURAL JOIN CONSUMIDORES_3;


-------------CONSULTA 2--------------


WITH AUX1 AS (
    SELECT BARCODE, QUANTITY, PRICE, SUBSTR(ORDERDATE, 4, 2) AS MES FROM CLIENT_LINES
    WHERE ORDERDATE > ADD_MONTHS(SYSDATE, -12)
    UNION
    SELECT BARCODE, TO_CHAR(QUANTITY), PRICE, SUBSTR(ORDERDATE, 4, 2) AS MES FROM LINES_ANONYM
    WHERE ORDERDATE > ADD_MONTHS(SYSDATE, -12)
),

AUX2 AS(
    SELECT MES, BARCODE, COUNT('X') AS N_PEDIDOS, SUM(QUANTITY) AS VENTAS, SUM(QUANTITY)*PRICE AS INGRESOS
    FROM AUX1
    GROUP BY BARCODE, MES, PRICE
),

AUX3 AS(
    SELECT DISTINCT MES, MAX(VENTAS) AS VENTAS FROM AUX2 GROUP BY MES
),

AUX4 AS(
    SELECT MES, BARCODE, N_PEDIDOS, VENTAS, INGRESOS FROM AUX3 NATURAL JOIN AUX2
)

SELECT MES, BARCODE, N_PEDIDOS, VENTAS, INGRESOS, INGRESOS - VENTAS * MAX(COST) AS BENEFICIO FROM AUX4 NATURAL JOIN SUPPLY_LINES 
GROUP BY MES, BARCODE, N_PEDIDOS, VENTAS, INGRESOS ORDER BY MES;


-------------PAQUETE--------------


DELETE FROM REPLACEMENTS;
INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('J38320106J','OOO28882I889528', TO_DATE('07/01/24', 'DD/MM/YY'), 25, TO_DATE('14/01/24'), 70*25);
INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('J38320106J','OOO28882I889528', TO_DATE('10/02/24', 'DD/MM/YY'), 72, TO_DATE('14/02/24'), 73.3*72);
INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('J38320106J','OOO28882I889528', TO_DATE('18/03/24', 'DD/MM/YY'), 138, TO_DATE('20/03/24'), 75.7*138);

INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('J38320106J','OIQ47059I515289', TO_DATE('07/01/24', 'DD/MM/YY'), 22, TO_DATE('18/01/24'), 18.3*22);
INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('J38320106J','OIQ47059I515289', TO_DATE('5/02/24', 'DD/MM/YY'), 89, TO_DATE('14/02/24'), 22.8*89);
INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('J38320106J','OIQ47059I515289', TO_DATE('12/03/24', 'DD/MM/YY'), 53, TO_DATE('17/03/24'), 21.3*53);

INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('J38320106J','IQQ99988O827109', TO_DATE('07/02/24', 'DD/MM/YY'), 13, TO_DATE('09/02/24'), 35*13);
INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('J38320106J','IQQ99988O827109', TO_DATE('14/02/24', 'DD/MM/YY'), 53, TO_DATE('15/02/24'), 39.99*53);
INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('J38320106J','IQQ99988O827109', TO_DATE('28/03/24', 'DD/MM/YY'), 168, TO_DATE('30/03/24'), 42*168);

INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('J38320106J','OOO28882I889528', TO_DATE('22/01/07', 'DD/MM/YY'), 25, TO_DATE('14/01/24'), 70*25);

-- Añadimos otros proveedores a la base de datos
INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('Q53972745W','III07057O402995', TO_DATE('07/01/24', 'DD/MM/YY'), 25, TO_DATE('14/01/24'), 70*25);
INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('Q53972745W','III07057O402995', TO_DATE('10/02/24', 'DD/MM/YY'), 72, TO_DATE('14/02/24'), 73.3*72);
INSERT INTO REPLACEMENTS(taxID, barCode, orderdate, units, deldate, payment) 
VALUES ('Q53972745W','III07057O402995', TO_DATE('18/03/24', 'DD/MM/YY'), 138, TO_DATE('20/03/24'), 75.7*138);

CREATE OR REPLACE PACKAGE caffeine AS
    PROCEDURE actualizar;
    PROCEDURE info (proveedor in VARCHAR2);

END caffeine;
/
CREATE OR REPLACE PACKAGE BODY caffeine AS

    -- Definir variables para almacenar los resultados

    v_taxid REPLACEMENTS.TAXID%TYPE;
    v_confirmados NUMBER;
    v_media_entrega NUMBER(5,2);
    v_coste_media NUMBER(5,2);
    v_coste_min NUMBER;
    v_coste_max NUMBER;
    v_resta_media NUMBER;
    v_resta_min NUMBER;

    PROCEDURE actualizar AS
    BEGIN
        -- Actualizamos la tabla REPLACEMENTS

        UPDATE REPLACEMENTS
        SET STATUS = 'P'
        WHERE STATUS = 'D';
    END;
    
    PROCEDURE info (proveedor in VARCHAR2) AS
    BEGIN
    SELECT TAXID, AVG(deldate - orderdate), COUNT('X')
        INTO v_taxid, v_media_entrega, v_confirmados
        FROM REPLACEMENTS
        WHERE TAXID = proveedor AND STATUS = 'P' AND ORDERDATE >= ADD_MONTHS(SYSDATE, -12) GROUP BY TAXID;

    DBMS_OUTPUT.PUT_LINE('Tax ID: ' || v_taxid || ', Pedidos confirmados: ' || v_confirmados
    || ', Promedio entrega: ' || v_media_entrega || ' días, Ofertas del proveedor:');
    
    FOR fila IN (SELECT * FROM SUPPLY_LINES WHERE TAXID = proveedor)
    LOOP
        SELECT MIN(payment/units), MAX(payment/units), AVG(payment/units)
        INTO v_coste_min, v_coste_max, v_coste_media
        FROM REPLACEMENTS
        WHERE TAXID = proveedor AND STATUS = 'P' AND BARCODE = fila.BARCODE AND ORDERDATE >= ADD_MONTHS(SYSDATE, -12);
        v_resta_media := fila.cost - v_coste_media;
        v_resta_min := fila.cost - v_coste_min;

        DBMS_OUTPUT.PUT_LINE('Barcode: ' || fila.barcode || ', Coste actual: ' || fila.cost || 
        ', Coste mínimo: ' || v_coste_min || ', Coste máximo: ' || v_coste_max  || ', Coste medio: ' || v_coste_media ||
        ', Coste actual - coste media: ' || v_resta_media || 
        ', Coste actual - coste mínimo: ' || v_resta_min);
    
    END LOOP;
    
    EXCEPTION
    when no_data_found then dbms_output.put_line('No hay datos');
    when too_many_rows then dbms_output.put_line('Demasiadas filas');
    when others then dbms_output.put_line('Se ha producido otro error');
    END;    

END caffeine;
/

exec caffeine.actualizar();
exec caffeine.info('J38320106J');


-------------VISTA 1--------------


CREATE OR REPLACE VIEW Mis_compras AS
    SELECT * FROM CLIENT_LINES
    WHERE USERNAME = USER
    -- Para las pruebas
    --WHERE USERNAME = 'abby'
    WITH READ ONLY;


-------------VISTA 2--------------


CREATE OR REPLACE VIEW Mi_perfil AS
    WITH AUX AS(
    SELECT USERNAME, NAME, SURN1, CARDNUM
    FROM CLIENTS
    NATURAL JOIN CLIENT_CARDS)
    SELECT * FROM AUX NATURAL JOIN CLIENT_ADDRESSES
    WHERE USERNAME = USER
    -- Para las pruebas
    -- Como abelarda si cumple las condiciones crea la vista correctamente
    -- WHERE USERNAME = 'abelarda'
    -- Como abby no cumple las condiciones crea la vista vacia
    -- WHERE USERNAME = 'ojeda'
    WITH READ ONLY;


-------------VISTA 3--------------


CREATE OR REPLACE VIEW mis_comentarios AS
    SELECT ROW_NUMBER() OVER(ORDER BY POSTDATE ASC) as RN,
    USERNAME, POSTDATE, BARCODE, PRODUCT, SCORE, TITLE, TEXT, LIKES, ENDORSED
    FROM POSTS
    WHERE USERNAME = USER
    WITH CHECK OPTION;

CREATE OR REPLACE TRIGGER CHK_comentarios
    BEFORE DELETE OR UPDATE OR INSERT ON POSTS
    FOR EACH ROW 
        --DECLARE mg NUMBER;
    BEGIN
        --SELECT NEW.LIKES FROM POSTS WHERE LIKES = 0;
        if (:OLD.LIKES != 0) then 
            RAISE_APPLICATION_ERROR(-20002, 'No puedes eliminar/modificar/insertar si los likes no son 0');            
        END IF;
  
        IF UPDATING('USERNAME') OR UPDATING('POSTDATE') OR UPDATING('BARCODE') OR UPDATING('PRODUCT') OR UPDATING('SCORE') OR
        UPDATING('TITLE') OR UPDATING('LIKES') OR UPDATING('ENDORSED') THEN
            RAISE_APPLICATION_ERROR(-20002, 'Solo se puede editar texto');
        END IF;
END;
/

ALTER TRIGGER CHK_COMENTARIOS DISABLE;

UPDATE POSTS SET LIKES = 0 WHERE USERNAME = 'abelarda' and TO_CHAR(POSTDATE) = '23/01/15';
UPDATE POSTS SET LIKES = 0 WHERE USERNAME = 'abelarda' and TO_CHAR(POSTDATE) = '05/06/21';

ALTER TRIGGER CHK_COMENTARIOS ENABLE;

DELETE FROM POSTS WHERE USERNAME = 'abelarda' and TO_CHAR(POSTDATE) = '23/01/15'; -- Se elimina una fila porque los likes son 0
UPDATE POSTS SET LIKES = 0 WHERE USERNAME = 'abelarda' and TO_CHAR(POSTDATE) = '05/06/21'; -- Error solo se puede editar texto
UPDATE POSTS SET SCORE = 3 WHERE USERNAME = 'abelarda' and TO_CHAR(POSTDATE) = '05/06/21'; -- Error solo se puede editar texto
UPDATE POSTS SET TEXT = 'HOLA', SCORE = 3 WHERE USERNAME = 'abelarda' and TO_CHAR(POSTDATE) = '05/06/21'; -- Error solo se puede editar texto
UPDATE POSTS SET TEXT = 'HOLA' WHERE USERNAME = 'abelarda' and TO_CHAR(POSTDATE) = '05/06/21'; -- Se modifica con éxito

ALTER TRIGGER CHK_COMENTARIOS DISABLE;


-------------TRIGGER 1--------------


ALTER TABLE POSTS MODIFY ENDORSED VARCHAR2(1);

CREATE OR REPLACE TRIGGER trigger_1
    BEFORE UPDATE OR INSERT ON POSTS
    FOR EACH ROW
        DECLARE v_endorsed NUMBER;
    BEGIN
        
        SELECT COUNT('X') INTO v_endorsed FROM CLIENT_LINES 
        WHERE USERNAME = :NEW.USERNAME AND BARCODE = :NEW.BARCODE;
                
        IF v_endorsed >= 1 THEN
            :NEW.ENDORSED := 'Y';
        ELSE
            :NEW.ENDORSED := 'N';
        END IF;
    END;
/

INSERT INTO POSTS(USERNAME, POSTDATE, BARCODE, PRODUCT, SCORE, TEXT, LIKES) VALUES ('austro', SYSDATE, 'QOO14099I548032', 'Puercos',
3, 'QUEASE', 10); -- INSERTA 1 FILA CON ENDORSED = 'Y' PORQUE AUSTRO HA COMPRADO ANTERIORMENTE ESE PRODUCTO
INSERT INTO POSTS(USERNAME, POSTDATE, BARCODE, PRODUCT, SCORE, TEXT, LIKES) VALUES ('austro', TO_DATE('10/04/24', 'DD/MM/YY'), 'OIQ75670Q167198', 'Milagros o joyas',
3, 'QUEASE', 10); -- INSERTA 1 FILA CON ENDORSED = 'N' PORQUE AUSTRO NO HA COMPRADO ANTERIORMENTE ESE PRODUCTO
UPDATE POSTS SET TEXT = 'HOLA' WHERE USERNAME = 'abelarda' and TO_CHAR(POSTDATE) = '10/02/15';
-- ACTUALIZA 1 FILA CON ENDORSED = 'Y' PORQUE AUSTRO HA COMPRADO ANTERIORMENTE ESE PRODUCTO
UPDATE POSTS SET TEXT = 'asdfasdfasdfasd' WHERE USERNAME = 'austro' and TO_CHAR(POSTDATE) = '10/04/24';
-- ACTUALIZA 1 FILA CON ENDORSED = 'N' PORQUE AUSTRO NO HA COMPRADO ANTERIORMENTE ESE PRODUCTO

ALTER TRIGGER trigger_1 DISABLE;


-------------TRIGGER 2--------------


ALTER TABLE CLIENT_ADDRESSES
DROP CONSTRAINT fk_addresses_clients; 
ALTER TABLE CLIENT_ADDRESSES
ADD CONSTRAINT fk_addresses_clients FOREIGN KEY(username) REFERENCES Clients;

ALTER TABLE CLIENT_CARDS
DROP CONSTRAINT fk_cards_clients;
ALTER TABLE CLIENT_CARDS
ADD CONSTRAINT fk_cards_clients FOREIGN KEY(username) REFERENCES Clients;


CREATE OR REPLACE TRIGGER trigger_2
    BEFORE DELETE ON CLIENTS
    FOR EACH ROW
    DECLARE v_contacto1 VARCHAR2(60);
            v_contacto2 VARCHAR2(60);
    BEGIN
        IF :OLD.PREFERENCE = 'EMAIL' THEN
            v_contacto1 := :OLD.EMAIL;
            v_contacto2 := TO_CHAR(:OLD.MOBILE);
        ELSIF :OLD.PREFERENCE = 'SMS' THEN
            v_contacto1 := TO_CHAR(:OLD.MOBILE);
            v_contacto1 := :OLD.EMAIL;
        END IF;
                
        INSERT INTO ORDERS_ANONYM(
            SELECT ORDERDATE, v_contacto1, v_contacto2, DLIV_DATETIME, :OLD.NAME, :OLD.SURN1, :OLD.SURN2, 
                WAYTYPE, WAYNAME, GATE, BLOCK, STAIRW, FLOOR, DOOR, ZIP, TOWN, COUNTRY,
                WAYTYPE, WAYNAME, GATE, BLOCK, STAIRW, FLOOR, DOOR, ZIP, TOWN, COUNTRY
            FROM CLIENT_ADDRESSES NATURAL JOIN ORDERS_CLIENTS
            WHERE USERNAME = :OLD.USERNAME);
        
        INSERT INTO LINES_ANONYM(
            SELECT ORDERDATE, v_contacto1, TOWN, COUNTRY, BARCODE, PRICE, QUANTITY, 
                PAY_TYPE, PAY_DATETIME, CARD_COMP, CARDNUM, CARD_HOLDER, CARD_EXPIR
            FROM CLIENT_LINES NATURAL JOIN CLIENT_CARDS
            WHERE USERNAME = :OLD.USERNAME);
            
        INSERT INTO ANONYPOSTS(
            SELECT POSTDATE, BARCODE, PRODUCT, SCORE, TITLE, TEXT, LIKES, ENDORSED
            FROM POSTS
            WHERE USERNAME = :OLD.USERNAME);
        
        DELETE FROM Orders_Clients
        WHERE (username, town, country) IN (SELECT username, town, country FROM Client_Addresses WHERE username = :OLD.USERNAME);
        
        DELETE FROM Posts WHERE username = :OLD.USERNAME;
        DELETE FROM CLIENT_ADDRESSES WHERE USERNAME = :OLD.USERNAME;
        DELETE FROM CLIENT_CARDS WHERE USERNAME = :OLD.USERNAME;
        DELETE FROM CLIENT_LINES WHERE USERNAME = :OLD.USERNAME;
END;
/

DELETE FROM CLIENTS WHERE USERNAME = 'alberto';
DELETE FROM CLIENTS WHERE USERNAME = 'arce';
DELETE FROM CLIENTS WHERE USERNAME = 'pedro';

ALTER TRIGGER trigger_2 DISABLE;


-------------TRIGGER 3--------------


CREATE OR REPLACE TRIGGER trigger_3
    BEFORE INSERT ON LINES_ANONYM
    FOR EACH ROW
    DECLARE v_tarjeta NUMBER;
    BEGIN
        SELECT COUNT('X') INTO v_tarjeta FROM CLIENT_CARDS 
        WHERE CARDNUM = :NEW.CARD_NUM;
        
        if v_tarjeta > 0 THEN
            RAISE_APPLICATION_ERROR(-20002, 'La tarjeta ya está registrada como tarjeta de cliente');
        END IF;
END;
/

-- No puedes hacer la compra porque la tarjeta ya pertenece a un cliente registrado -> Error: "La tarjeta ya está registrada como tarjeta de cliente"
INSERT INTO LINES_ANONYM VALUES(SYSDATE, 'diagi@servcorreo.ucetreseme.kom', 'Val de las Caballerizas', 'Namibia',
'QQO04562I911080', '1,15', '2', 'credit card', '25/06/16', 'Cabestro', 10694341628, 'M Altagracia Lago', '01/03/26');

INSERT INTO ORDERS_ANONYM VALUES (sysdate, '1234@gmail.com', '', '', 'John', 'Doe', '', 'Street', '123 Main St', 'A',
'', '', '', '', '12345', 'UC3M', 'Spain', 'Calle', 'Nano', 'C', '', '', '', '', '13579', 'UC3M', 'Spain');

INSERT INTO LINES_ANONYM VALUES (sysdate, '1234@gmail.com', 'UC3M', 'Spain', 'QII85636I380417', 13, 2, 'credit card', SYSDATE, 'khkhgklghkl', 333333333, 'Mariogod', sysdate);

ALTER TRIGGER trigger_3 DISABLE;


-------------TRIGGER 4--------------


ALTER TABLE REPLACEMENTS
DROP CONSTRAINT pk_replacements; -- Elimina la clave primaria que deseas eliminar

-- Agregar una nueva clave primaria
ALTER TABLE REPLACEMENTS
ADD CONSTRAINT nueva_pk PRIMARY KEY (barcode,orderdate); 
--Agrega una nueva clave primaria con las columnas deseadas


create or replace TRIGGER  trigger_4_1
AFTER INSERT ON CLIENT_LINES
--OR AFTER INSERT ON ANONYM_LINES
FOR EACH ROW
DECLARE v_stock NUMBER;
        v_min_stock NUMBER;
        v_max_stock NUMBER;
        v_resta NUMBER;
        v_coste_unidad NUMBER;
BEGIN

    SELECT CUR_STOCK, MIN_STOCK, MAX_STOCK INTO v_stock, v_min_stock, v_max_stock FROM REFERENCES
    WHERE BARCODE = :NEW.BARCODE;

    SELECT MIN(COST) INTO v_coste_unidad FROM SUPPLY_LINES
    WHERE BARCODE = :NEW.BARCODE GROUP BY BARCODE;

    v_resta := v_stock - :NEW.QUANTITY;
    if v_resta >= v_min_stock then
        UPDATE REFERENCES
        SET CUR_STOCK = CUR_STOCK - :NEW.QUANTITY;

    else
        UPDATE REFERENCES
        SET CUR_STOCK = CUR_STOCK - :NEW.QUANTITY;
        INSERT INTO REPLACEMENTS values(NULL, :NEW.BARCODE, SYSDATE, 'D', v_max_stock - v_resta, NULL, v_coste_unidad * :NEW.QUANTITY);

    END IF;
END;
/

create or replace TRIGGER  trigger_4_2
AFTER INSERT ON LINES_ANONYM
--OR AFTER INSERT ON ANONYM_LINES
FOR EACH ROW
DECLARE v_stock NUMBER;
        v_min_stock NUMBER;
        v_max_stock NUMBER;
        v_resta NUMBER;
        v_coste_unidad NUMBER;
BEGIN

    SELECT CUR_STOCK, MIN_STOCK, MAX_STOCK INTO v_stock, v_min_stock, v_max_stock FROM REFERENCES
    WHERE BARCODE = :NEW.BARCODE;

    SELECT MIN(COST) INTO v_coste_unidad FROM SUPPLY_LINES
    WHERE BARCODE = :NEW.BARCODE GROUP BY BARCODE;

    v_resta := v_stock - :NEW.QUANTITY;
    if v_resta >= v_min_stock then
        UPDATE REFERENCES
        SET CUR_STOCK = CUR_STOCK - :NEW.QUANTITY;

    else
        UPDATE REFERENCES
        SET CUR_STOCK = CUR_STOCK - :NEW.QUANTITY;
        INSERT INTO REPLACEMENTS values(NULL, :NEW.BARCODE, SYSDATE, 'D', v_max_stock - v_resta, NULL, v_coste_unidad * :NEW.QUANTITY);

    END IF;
END;
/
insert into orders_clients 
values(TO_DATE('20/02/23'), 'cristi', 'Serrania de las Tripas', 'Congo', NULL, 'Serrania de las Tripas', 'Congo', NULL);

-- Se actualiza el stock en referencias
insert into client_lines 
values(TO_DATE('20/02/23'), 'cristi', 'Serrania de las Tripas', 'Congo', 'III01816Q633880', 13, 2, 'COD', SYSDATE, NULL);

delete from client_lines 
where username = 'cristi' and orderdate = TO_DATE('20/02/23') and town = 'Serrania de las Tripas' and country = 'Congo';

-- Inicialmente hay 1438 unidades y el stock mínimo es 20
ALTER TABLE CLIENT_LINES MODIFY QUANTITY VARCHAR2(10);
insert into client_lines 
values(TO_DATE('20/02/23'), 'cristi', 'Serrania de las Tripas', 'Congo', 'III01816Q633880', 13, 1425, 'COD', SYSDATE, NULL);

ALTER TRIGGER trigger_4_1 DISABLE;
ALTER TRIGGER trigger_4_2 DISABLE;