/*

Sakila for Oracle is a port of the Sakila example database available for MySQL, which was originally developed by Mike Hillyer of the MySQL AB documentation team.
This project is designed to help database administrators to decide which database to use for development of new products
The user can run the same SQL against different kind of databases and compare the performance

License: BSD
Copyright DB Software Laboratory
http://www.etl-tools.com

*/

alter session set "_ORACLE_SCRIPT"=true;
DROP USER SAKILA CASCADE;
CREATE USER SAKILA IDENTIFIED BY SAKILA;
GRANT CONNECT, RESOURCE, DBA TO SAKILA;
GRANT CREATE SESSION TO SAKILA;
GRANT GRANT ANY PRIVILEGE TO SAKILA;
GRANT UNLIMITED TABLESPACE TO SAKILA;
alter session set current_schema = SAKILA;

--DROP SCHEMA IF EXISTS OracleSakila CASCADE;
--/
--CREATE SCHEMA IF NOT EXISTS OracleSakila;
--/


--
-- all_types table (separate from actual sakila schema, piggybacking sakila to simplify testing)
--

create table all_types (
                           COL_INTERVALDS                     INTERVAL DAY TO SECOND         ,-- null(-104)
                           COL_INTERVALYM                     INTERVAL YEAR TO MONTH         ,-- null(-103)
                           COL_TIMESTAMP_WITH_LOCAL_TIME_ZONE TIMESTAMP WITH LOCAL TIME ZONE ,-- null(-102)
                           COL_TIMESTAMP_WITH_TIME_ZONE       TIMESTAMP WITH TIME ZONE       ,-- null(-101)
                           COL_NCHAR                          NCHAR                          ,-- NCHAR(-15)
                           COL_NVARCHAR2                      NVARCHAR2(10)                  ,-- NVARCHAR(-9)
                           COL_NUMBER_1                       NUMBER(1)                      ,-- BIT(-7)
                           COL_NUMBER_3                       NUMBER(3)                      ,-- TINYINT(-6)
                           COL_NUMBER_5                       NUMBER(5)                      ,-- SMALLINT(5)
                           COL_NUMBER_10                      NUMBER(10)                     ,-- INTEGER(4)
                           COL_NUMBER_38                      NUMBER(38)                     ,-- NUMERIC
                           COL_RAW                            RAW(8)                         ,-- VARBINARY(-3)

-- oracle allows only one LONG column per table
--                            COL_LONG_RAW                       LONG RAW                       ,-- LONGVARBINARY(-4)
                           COL_LONG                           LONG                           ,-- LONGVARCHAR(-1)

                           COL_CHAR                           CHAR                           ,-- CHAR(1)
                           COL_NUMBER                         NUMBER                         ,-- NUMERIC(2)
                           COL_FLOAT                          FLOAT                          ,-- FLOAT(6)
                           COL_REAL                           REAL                           ,-- REAL(7)
                           COL_VARCHAR2                       VARCHAR2(10)                   ,-- VARCHAR(12)
                           COL_DATE                           DATE                           ,-- TIME(92)
                           COL_TIMESTAMP                      TIMESTAMP                      ,-- TIMESTAMP(93)
--                            COL_STRUCT                         STRUCT                         ,-- STRUCT(2002)
--                            COL_ARRAY                          ARRAY                          ,-- ARRAY(2003)
                           COL_BLOB                           BLOB                           ,-- BLOB(2004)
                           COL_CLOB                           CLOB                           ,-- CLOB(2005)
--    COL_REF                            REF                            ,-- REF(2006)
                           COL_NCLOB                          NCLOB                          -- NCLOB(2011)
);

--
-- Table structure for table actor
--
--DROP TABLE actor;

CREATE TABLE actor (
                       actor_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                       first_name VARCHAR(45) NOT NULL,
                       last_name VARCHAR(45) NOT NULL,
                       last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       CONSTRAINT pk_actor PRIMARY KEY  (actor_id)
);

CREATE  INDEX idx_actor_last_name ON actor(last_name);
/


--
-- Table structure for table country
--

CREATE TABLE country (
--                       NUMBER here (no 10) is intentional to test that FKs cope with the type difference: java BigDecimal vs Integer
                         country_id NUMBER GENERATED ALWAYS AS IDENTITY,
                         country VARCHAR(50) NOT NULL,
                         last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         CONSTRAINT pk_country PRIMARY KEY (country_id)
);

--
-- Table structure for table city
--

CREATE TABLE city (
                      city_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                      city VARCHAR(50) NOT NULL,
                      country_id NUMBER(10) NOT NULL,
                      last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                      CONSTRAINT pk_city PRIMARY KEY (city_id),
                      CONSTRAINT fk_city_country FOREIGN KEY (country_id) REFERENCES country (country_id)
);

--
-- Table structure for table address
--

CREATE TABLE address (
                         address_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                         address VARCHAR(50) NOT NULL,
                         address2 VARCHAR(50) DEFAULT NULL,
                         district VARCHAR(20) NOT NULL,
                         city_id NUMBER(10)  NOT NULL,
                         postal_code VARCHAR(10) DEFAULT NULL,
                         phone VARCHAR(20) NOT NULL,
                         last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         CONSTRAINT pk_address PRIMARY KEY (address_id),
                         CONSTRAINT fk_address_city FOREIGN KEY (city_id) REFERENCES city (city_id)
);
/

--
-- Table structure for table language
--

CREATE TABLE language (
                          language_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                          name CHAR(20) NOT NULL,
                          last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          CONSTRAINT pk_language PRIMARY KEY (language_id)
);

--
-- Table structure for table category
--

CREATE TABLE category (
                          category_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                          name VARCHAR(25) NOT NULL,
                          last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          CONSTRAINT pk_category PRIMARY KEY  (category_id)
);
/

--
-- Table structure for table customer
--

CREATE TABLE customer (
                          customer_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                          store_id NUMBER(10) NOT NULL,
                          first_name VARCHAR(45) NOT NULL,
                          last_name VARCHAR(45) NOT NULL,
                          email VARCHAR(50) DEFAULT NULL,
                          address_id NUMBER(10) NOT NULL,
                          active CHAR(1) DEFAULT 'Y' NOT NULL,
                          create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          CONSTRAINT pk_customer PRIMARY KEY  (customer_id),
                          CONSTRAINT fk_customer_address FOREIGN KEY (address_id) REFERENCES address(address_id)
);

--
-- Table structure for table film
--

CREATE TABLE film (
                      film_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                      title VARCHAR(255) NOT NULL,
                      description CLOB DEFAULT NULL,
                      release_year VARCHAR(4) DEFAULT NULL,
                      language_id NUMBER(10) NOT NULL,
                      original_language_id NUMBER(10) DEFAULT NULL,
                      rental_duration SMALLINT  DEFAULT 3 NOT NULL,
                      rental_rate DECIMAL(4,2) DEFAULT 4.99 NOT NULL,
                      length SMALLINT DEFAULT NULL,
                      replacement_cost DECIMAL(5,2) DEFAULT 19.99 NOT NULL,
                      rating VARCHAR(10) DEFAULT 'G',
                      special_features VARCHAR(100) DEFAULT NULL,
                      last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                      CONSTRAINT pk_film PRIMARY KEY  (film_id),
                      CONSTRAINT fk_film_language FOREIGN KEY (language_id) REFERENCES language (language_id) ,
                      CONSTRAINT fk_film_language_original FOREIGN KEY (original_language_id) REFERENCES language (language_id)
);

ALTER TABLE film ADD CONSTRAINT CHECK_special_features CHECK(special_features is null or
                                                             special_features like '%Trailers%' or
                                                             special_features like '%Commentaries%' or
                                                             special_features like '%Deleted Scenes%' or
                                                             special_features like '%Behind the Scenes%');
/
ALTER TABLE film ADD CONSTRAINT CHECK_special_rating CHECK(rating in ('G','PG','PG-13','R','NC-17'));
/

--
-- Table structure for table film_actor
--

CREATE TABLE film_actor (
                            actor_id NUMBER(10) NOT NULL,
                            film_id  NUMBER(10) NOT NULL,
                            last_update DATE NOT NULL,
                            CONSTRAINT pk_film_actor PRIMARY KEY  (actor_id,film_id),
                            CONSTRAINT fk_film_actor_actor FOREIGN KEY (actor_id) REFERENCES actor (actor_id),
                            CONSTRAINT fk_film_actor_film FOREIGN KEY (film_id) REFERENCES film (film_id)
);
/

--
-- Table structure for table film_category
--

CREATE TABLE film_category (
                               film_id NUMBER(10) NOT NULL,
                               category_id NUMBER(10)  NOT NULL,
                               last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                               CONSTRAINT pk_film_category PRIMARY KEY (film_id, category_id),
                               CONSTRAINT fk_film_category_film FOREIGN KEY (film_id) REFERENCES film (film_id),
                               CONSTRAINT fk_film_category_category FOREIGN KEY (category_id) REFERENCES category (category_id)
);
/

--
-- Table structure for table film_text
--

CREATE TABLE film_text (
                           film_id NUMBER(10) NOT NULL,
                           title VARCHAR(255) NOT NULL,
                           description CLOB,
                           CONSTRAINT pk_film_text PRIMARY KEY  (film_id)
);

--
-- Table structure for table inventory
--

CREATE TABLE inventory (
                           inventory_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                           film_id NUMBER(10) NOT NULL,
                           store_id NUMBER(10) NOT NULL,
                           last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                           CONSTRAINT pk_inventory PRIMARY KEY  (inventory_id),
                           CONSTRAINT fk_inventory_film FOREIGN KEY (film_id) REFERENCES film (film_id)
);
/

--
-- Table structure for table staff
--

CREATE TABLE staff (
                       staff_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                       first_name VARCHAR(45) NOT NULL,
                       last_name VARCHAR(45) NOT NULL,
                       address_id NUMBER(10) NOT NULL,
                       picture BLOB DEFAULT NULL,
                       email VARCHAR(50) DEFAULT NULL,
                       store_id NUMBER(10) NOT NULL,
                       active SMALLINT DEFAULT 1 NOT NULL,
                       username VARCHAR(16) NOT NULL,
                       password VARCHAR(40) DEFAULT NULL,
                       last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       CONSTRAINT pk_staff PRIMARY KEY  (staff_id),
                       CONSTRAINT fk_staff_address FOREIGN KEY (address_id) REFERENCES address (address_id)
);


--
-- Table structure for table store
--

CREATE TABLE store (
                       store_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                       manager_staff_id NUMBER(10) NOT NULL,
                       address_id NUMBER(10) NOT NULL,
                       last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       CONSTRAINT pk_store PRIMARY KEY  (store_id),
                       CONSTRAINT fk_store_staff FOREIGN KEY (manager_staff_id) REFERENCES staff (staff_id) ,
                       CONSTRAINT fk_store_address FOREIGN KEY (address_id) REFERENCES address (address_id)
);
/

--
-- Table structure for table payment
--

CREATE TABLE payment (
                         payment_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                         customer_id NUMBER(10)  NOT NULL,
                         staff_id NUMBER(10) NOT NULL,
                         rental_id NUMBER(10) DEFAULT NULL,
                         amount DECIMAL(5,2) NOT NULL,
                         payment_date DATE NOT NULL,
                         last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         CONSTRAINT pk_payment PRIMARY KEY  (payment_id),
                         CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ,
                         CONSTRAINT fk_payment_staff FOREIGN KEY (staff_id) REFERENCES staff (staff_id)
);
/

CREATE TABLE rental (
                        rental_id NUMBER(10) GENERATED ALWAYS AS IDENTITY,
                        rental_date DATE NOT NULL,
                        inventory_id NUMBER(10)  NOT NULL,
                        customer_id NUMBER(10)  NOT NULL,
                        return_date DATE DEFAULT NULL,
                        staff_id NUMBER(10)  NOT NULL,
                        last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        CONSTRAINT pk_rental PRIMARY KEY (rental_id),
                        CONSTRAINT fk_rental_staff FOREIGN KEY (staff_id) REFERENCES staff (staff_id) ,
                        CONSTRAINT fk_rental_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (inventory_id) ,
                        CONSTRAINT fk_rental_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
);
/

-- FK CONSTRAINTS
ALTER TABLE customer ADD CONSTRAINT fk_customer_store FOREIGN KEY (store_id) REFERENCES store (store_id);
/
ALTER TABLE inventory ADD CONSTRAINT fk_inventory_store FOREIGN KEY (store_id) REFERENCES store (store_id);
/
ALTER TABLE staff ADD CONSTRAINT fk_staff_store FOREIGN KEY (store_id) REFERENCES store (store_id);
/
ALTER TABLE payment ADD CONSTRAINT fk_payment_rental FOREIGN KEY (rental_id) REFERENCES rental (rental_id) ON DELETE SET NULL;
/
--
-- View structure for view customer_list
--

CREATE OR REPLACE VIEW customer_list
AS
SELECT cu.customer_id AS ID,
       cu.first_name||' '||cu.last_name AS name,
       a.address AS address,
       a.postal_code AS zip_code,
       a.phone AS phone,
       city.city AS city,
       country.country AS country,
       decode(cu.active, 1,'active','') AS notes,
       cu.store_id AS SID
FROM customer cu JOIN address a ON cu.address_id = a.address_id JOIN city ON a.city_id = city.city_id
                 JOIN country ON city.country_id = country.country_id;
/
--
-- View structure for view film_list
--

CREATE OR REPLACE VIEW film_list
AS
SELECT film.film_id AS FID,
       film.title AS title,
       film.description AS description,
       category.name AS category,
       film.rental_rate AS price,
       film.length AS length,
       film.rating AS rating,
       actor.first_name||' '||actor.last_name AS actors
FROM category LEFT JOIN film_category ON category.category_id = film_category.category_id LEFT JOIN film ON film_category.film_id = film.film_id
              JOIN film_actor ON film.film_id = film_actor.film_id
              JOIN actor ON film_actor.actor_id = actor.actor_id;
/

--
-- View structure for view staff_list
--

CREATE OR REPLACE VIEW staff_list
AS
SELECT s.staff_id AS ID,
       s.first_name||' '||s.last_name AS name,
       a.address AS address,
       a.postal_code AS zip_code,
       a.phone AS phone,
       city.city AS city,
       country.country AS country,
       s.store_id AS SID
FROM staff s JOIN address a ON s.address_id = a.address_id JOIN city ON a.city_id = city.city_id
             JOIN country ON city.country_id = country.country_id;
/
--
-- View structure for view sales_by_store
--

CREATE OR REPLACE VIEW sales_by_store
AS
SELECT
    s.store_id
     ,c.city||','||cy.country AS store
     ,m.first_name||' '||m.last_name AS manager
     ,SUM(p.amount) AS total_sales
FROM payment p
         INNER JOIN rental r ON p.rental_id = r.rental_id
         INNER JOIN inventory i ON r.inventory_id = i.inventory_id
         INNER JOIN store s ON i.store_id = s.store_id
         INNER JOIN address a ON s.address_id = a.address_id
         INNER JOIN city c ON a.city_id = c.city_id
         INNER JOIN country cy ON c.country_id = cy.country_id
         INNER JOIN staff m ON s.manager_staff_id = m.staff_id
GROUP BY
    s.store_id
       , c.city||','||cy.country
       , m.first_name||' '||m.last_name;
/
--
-- View structure for view sales_by_film_category
--
-- Note that total sales will add up to >100% because
-- some titles belong to more than 1 category
--

CREATE OR REPLACE VIEW sales_by_film_category
AS
SELECT
    c.name AS category
     , SUM(p.amount) AS total_sales
FROM payment p
         INNER JOIN rental r ON p.rental_id = r.rental_id
         INNER JOIN inventory i ON r.inventory_id = i.inventory_id
         INNER JOIN film f ON i.film_id = f.film_id
         INNER JOIN film_category fc ON f.film_id = fc.film_id
         INNER JOIN category c ON fc.category_id = c.category_id
GROUP BY c.name;
/

--
-- View structure for view actor_info
--

/*
CREATE VIEW actor_info
AS
SELECT
a.actor_id,
a.first_name,
a.last_name,
GROUP_CONCAT(DISTINCT CONCAT(c.name, ': ',
        (SELECT GROUP_CONCAT(f.title ORDER BY f.title SEPARATOR ', ')
                    FROM sakila.film f
                    INNER JOIN sakila.film_category fc
                      ON f.film_id = fc.film_id
                    INNER JOIN sakila.film_actor fa
                      ON f.film_id = fa.film_id
                    WHERE fc.category_id = c.category_id
                    AND fa.actor_id = a.actor_id
                 )
             )
             ORDER BY c.name SEPARATOR '; ')
AS film_info
FROM sakila.actor a
LEFT JOIN sakila.film_actor fa
  ON a.actor_id = fa.actor_id
LEFT JOIN sakila.film_category fc
  ON fa.film_id = fc.film_id
LEFT JOIN sakila.category c
  ON fc.category_id = c.category_id
GROUP BY a.actor_id, a.first_name, a.last_name;
*/

-- TO DO PROCEDURES