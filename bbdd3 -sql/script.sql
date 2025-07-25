-- ----------------------------------------------------
-- ----------------------------------------------------
-- -- TESTS & STATISTICS SCRIPT -----------------------
-- ----------------------------------------------------
-- -- Course: File Structures and DataBases -----------
-- ----------------------------------------------------
-- -- (c) 2024 Javier Calle ---------------------------
-- ------ Carlos III University of Madrid -------------
-- ----------------------------------------------------
-- ----------------------------------------------------
-- -- Part I: Package Definition ----------------------
-- ----------------------------------------------------

CREATE OR REPLACE PACKAGE PKG_COSTES AS

-- WORKLOAD definition
	PROCEDURE PR_WORKLOAD(N NUMBER);
-- Execution of workload (10 times) displaying some measurements 
	PROCEDURE RUN_TEST(ite NUMBER);

END PKG_COSTES;
/

-- ----------------------------------------------------
-- -- Part II: Package BODY ---------------------------
-- ----------------------------------------------------

CREATE OR REPLACE PACKAGE BODY PKG_COSTES AS

-- auxiliary function converting an interval into a number (milliseconds)
FUNCTION interval_to_milliseconds(x INTERVAL DAY TO SECOND ) RETURN NUMBER IS
  BEGIN
    return (((extract( day from x)*24 + extract( hour from x))*60 + extract( minute from x))*60 + extract( second from x))*1000;
  END interval_to_milliseconds;


PROCEDURE PR_WORKLOAD(N NUMBER) IS
-- this year, the WL does not need to distinguish iterations, so N is not taken into account
-- notice that the fourth query appears twice (double the frequency) and the fifth appears four times
-- so each step represents 10% frequency, and no query is repeated immediately
BEGIN

-- STEP 1 - QUERY 1
FOR fila in (
select * from posts where barcode='OII04455O419282'
) LOOP null; END LOOP;

-- STEP 2 - QUERY 5
FOR fila in (
select (quantity*price) as total, bill_town||'/'||bill_country as place 
   	from orders_clients join client_lines using (orderdate,username,town,country) 
 	where username='chamorro'
) LOOP null; END LOOP;

-- STEP 3 - QUERY 2
FOR fila in (
select * from posts where product='Compromiso'
) LOOP null; END LOOP;

-- STEP 4 - QUERY 5
FOR fila in (
select (quantity*price) as total, bill_town||'/'||bill_country as place 
   	from orders_clients join client_lines using (orderdate,username,town,country) 
 	where username='chamorro'
) LOOP null; END LOOP;

-- STEP 5 - QUERY 3
FOR fila in (
select * from posts where score>=4
) LOOP null; END LOOP;

-- STEP 6 - QUERY 5
FOR fila in (
select (quantity*price) as total, bill_town||'/'||bill_country as place 
   	from orders_clients join client_lines using (orderdate,username,town,country) 
 	where username='chamorro'
) LOOP null; END LOOP;

-- STEP 7 - QUERY 4
FOR fila in (
select * from posts
) LOOP null; END LOOP;

-- STEP 8 - QUERY 5
FOR fila in (
select (quantity*price) as total, bill_town||'/'||bill_country as place 
   	from orders_clients join client_lines using (orderdate,username,town,country) 
 	where username='chamorro'
) LOOP null; END LOOP;

-- STEP 9 - QUERY 4
FOR fila in (
select * from posts
) LOOP null; END LOOP;

-- STEP 10 - QUERY 5
FOR fila in (
select (quantity*price) as total, bill_town||'/'||bill_country as place 
   	from orders_clients join client_lines using (orderdate,username,town,country) 
 	where username='chamorro'
) LOOP null; END LOOP;

END PR_WORKLOAD;


PROCEDURE RUN_TEST(ite NUMBER) IS
   t1 TIMESTAMP;
   t2 TIMESTAMP;
   auxt NUMBER := 0;
   g1 NUMBER;
   g2 NUMBER;
   auxg NUMBER := 0;
   localsid NUMBER;
BEGIN
      PKG_COSTES.PR_WORKLOAD(0);  -- idle run for preparing db_buffers
      select distinct sid into localsid from v$mystat;
--- LOOP WORKLOAD ITERATIONS (ite times) --------------------------------
      FOR i IN 1..ite LOOP
        DBMS_OUTPUT.PUT_LINE('Iteration '||i);
--- GET PREVIOUS MEASURES -----------------------------------
        SELECT SYSTIMESTAMP INTO t1 FROM DUAL;
        select S.value into g1
           from (select * from v$sesstat where sid=localsid) S
                join (select * from v$statname where name='consistent gets') using(STATISTIC#);
--- EXECUTION OF THE WORKLOAD -----------------------------------
        PKG_COSTES.PR_WORKLOAD (i);
--- GET AFTER-RUN MEASURES -----------------------------------
        SELECT SYSTIMESTAMP INTO t2 FROM DUAL;
        select S.value into g2
           from (select * from v$sesstat where sid=localsid) S
                join (select * from v$statname where name='consistent gets') using(STATISTIC#);
--- ACCUMULATE MEASURES -----------------------------------
        auxt:= auxt + interval_to_milliseconds(t2-t1);
        auxg:= auxg + g2-g1;
--- END TESTS ---------------------------------------------------
      END LOOP;
      auxt:= auxt / ite;
      auxg:= auxg / ite;
--- DISPLAY RESULTS -----------------------------------
    DBMS_OUTPUT.PUT_LINE('RESULTS AT '||to_char(sysdate,'dd/mm/yyyy hh24:mi:ss'));
    DBMS_OUTPUT.PUT_LINE('TIME CONSUMPTION (run): '|| auxt ||' milliseconds.');
    DBMS_OUTPUT.PUT_LINE('CONSISTENT GETS (workload):'|| auxg ||' acc');
    DBMS_OUTPUT.PUT_LINE('CONSISTENT GETS (weighted average):'|| auxg/10 ||' acc');
END RUN_TEST;


BEGIN
-- alter system flush buffer_cache;
   DBMS_OUTPUT.ENABLE (buffer_size => NULL);

END PKG_COSTES;
/

  
DROP INDEX index_barcode;
DROP INDEX index_product;
DROP INDEX index_score;
DROP INDEX index_posts;
DROP MATERIALIZED VIEW LOG ON POSTS;
DROP MATERIALIZED VIEW posts_view;

-- ANALYZE TABLE POSTS COMPUTE STATISTICS;
-- SELECT AVG_ROW_LEN FROM USER_TABLES WHERE TABLE_NAME = 'POSTS';
-- SELECT NUM_ROWS FROM USER_TABLES WHERE TABLE_NAME = 'POSTS';
-- SELECT BLOCKS FROM USER_TABLES WHERE TABLE_NAME = 'POSTS';

set serveroutput on
-- ---------------------CONSULTA 1---------------------

-- PLAN 1
EXPLAIN PLAN SET statement_id = 'consulta1_plan1' FOR select * from posts where barcode='OII04455O419282';

SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta1_plan1'));
    
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/

-- PLAN 2
CREATE INDEX index_barcode ON POSTS(BARCODE);

EXPLAIN PLAN SET statement_id = 'consulta1_plan2' FOR select /*+index(index_barcode)*/ *
from posts where barcode='OII04455O419282';

SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta1_plan2'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/

-- ---------------------CONSULTA 2---------------------


-- PLAN 1
EXPLAIN PLAN SET statement_id = 'consulta2_plan1' FOR select * from posts where product='Compromiso';
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta2_plan1'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/

-- PLAN 2
CREATE INDEX index_product ON POSTS(PRODUCT);

EXPLAIN PLAN SET statement_id = 'consulta2_plan2' FOR select * from posts where product='Compromiso';
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta2_plan2'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/    

-- PLAN 3 (CON HINT)
EXPLAIN PLAN SET statement_id = 'consulta2_plan3' FOR select /*+index(index_product)*/ * 
from posts where product='Compromiso';
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta2_plan3'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/

-- ---------------------CONSULTA 3---------------------


-- PLAN 1
EXPLAIN PLAN SET statement_id = 'consulta3_plan1' FOR select * from posts where score>=4;
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta3_plan1'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/  

-- PLAN 2
CREATE INDEX index_score ON POSTS(SCORE);

EXPLAIN PLAN SET statement_id = 'consulta3_plan2' FOR select * from posts where score>=4;
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta3_plan2'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/ 

-- PLAN 3 (CON HINT)
EXPLAIN PLAN SET statement_id = 'consulta3_plan3' FOR select /*+ INDEX(Posts index_score) */ * from posts where score>=4;
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta3_plan3'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/

-- PLAN 4 (VISTA MATERIALIZADA con actualización)
CREATE MATERIALIZED VIEW LOG ON POSTS
WITH PRIMARY KEY
INCLUDING NEW VALUES;

CREATE MATERIALIZED VIEW posts_view
REFRESH FAST ON COMMIT
AS
SELECT * FROM posts WHERE score >= 4;

EXPLAIN PLAN SET statement_id = 'consulta3_plan4' FOR select * from posts_view where score>=4;
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta3_plan4'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/

-- PLAN 5 (CON MULTIHILO)
EXPLAIN PLAN SET statement_id = 'consulta3_plan5' FOR select  /*+ parallel(64) */ * from posts where score>=4;
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta3_plan5'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/


-- ---------------------CONSULTA 4---------------------


-- PLAN 1
EXPLAIN PLAN SET statement_id = 'consulta4_plan1' FOR select * from posts;
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta4_plan1'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/


-- PLAN 2
CREATE INDEX index_posts ON POSTS(POSTDATE, USERNAME);

EXPLAIN PLAN SET statement_id = 'consulta4_plan2' FOR select /*+ INDEX(Posts index_posts) */* from posts;
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta4_plan2'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/

-- PLAN 3 (MULTIHILO)
EXPLAIN PLAN SET statement_id = 'consulta4_plan3' FOR select /*+ parallel(64) */* from posts;
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta4_plan3'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/

-- PLAN 4 (TABLESPACE 16K)
ALTER TABLE POSTS MOVE TABLESPACE TAB_16K;
ALTER INDEX index_barcode REBUILD TABLESPACE TAB_16K;
ALTER INDEX index_product REBUILD TABLESPACE TAB_16K;
ALTER INDEX index_score REBUILD TABLESPACE TAB_16K;
ALTER INDEX index_posts REBUILD TABLESPACE TAB_16K;

EXPLAIN PLAN SET statement_id = 'consulta4_plan4' FOR select * from posts;
SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta4_plan4'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/

-- ---------------------CONSULTA 5---------------------


-- PLAN 1
EXPLAIN PLAN SET statement_id = 'consulta5_plan1' FOR  
select (quantity*price) as total, bill_town||'/'||bill_country as place  
from orders_clients join client_lines 
using (orderdate,username,town,country)  
where username='chamorro';

SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta5_plan1'));

begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/

-- PLAN 2
EXPLAIN PLAN SET statement_id = 'consulta5_plan2' FOR  
select (quantity*price) as total, bill_town||'/'||bill_country as place  
from orders_clients join client_lines 
using (orderdate,username,town,country)  
where username='chamorro';

SELECT PLAN_TABLE_OUTPUT
    FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'consulta5_plan2'));
begin
PKG_COSTES.RUN_TEST(10); --Número de repeticiones
end;
/
