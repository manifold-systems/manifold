
-- Sakila Sample Database Schema
-- Version 0.8

-- Copyright (c) 2006, MySQL AB
-- All rights reserved.

-- Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

--  * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
--  * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
--  * Neither the name of MySQL AB nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


-- SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
-- SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
-- SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

-- DROP SCHEMA IF EXISTS sakila;
-- CREATE SCHEMA sakila;
-- USE sakila;
--

create table all_types (--              SQL Type                           JDBC Type
                        --              ~~~~~~~~                           ~~~~~~~~~
        col_TINYINT                     TINYINT                        ,-- TINYINT(-6)
        col_BIGINT                      BIGINT                         ,-- BIGINT(-5)
        col_VARBINARY                   VARBINARY                      ,-- VARBINARY(-3)
        col_BINARY                      BINARY(5)                      ,-- BINARY(-2)
        col_UUID                        UUID                           ,-- BINARY(-2)
        col_CHARACTER                   CHARACTER(5)                   ,-- CHAR(1)
        col_NUMERIC                     NUMERIC                        ,-- NUMERIC(2)
        col_FLOAT                       FLOAT                          ,-- NUMERIC(2)
        col_INTEGER                     INTEGER                        ,-- INTEGER(4)
        col_SMALLINT                    SMALLINT                       ,-- SMALLINT(5)
        col_REAL                        REAL                           ,-- REAL(7)
        col_DOUBLE                      DOUBLE                         ,-- DOUBLE(8)
        col_VARCHAR                     VARCHAR                        ,-- VARCHAR(12)
        col_VARCHAR_IGNORECASE          VARCHAR_IGNORECASE             ,-- VARCHAR(12)
        col_BOOLEAN                     BOOLEAN                        ,-- BOOLEAN(16)
        col_DATE                        DATE                           ,-- DATE(91)
        col_TIME                        TIME                           ,-- TIME(92)
        col_TIMESTAMP                   TIMESTAMP                      ,-- TIMESTAMP(93)
        col_INTERVAL_YEAR               INTERVAL YEAR                  ,-- OTHER(1111)
        col_INTERVAL_MONTH              INTERVAL MONTH                 ,-- OTHER(1111)
        col_INTERVAL_DAY                INTERVAL DAY                   ,-- OTHER(1111)
        col_INTERVAL_HOUR               INTERVAL HOUR                  ,-- OTHER(1111)
        col_INTERVAL_MINUTE             INTERVAL MINUTE                ,-- OTHER(1111)
        col_INTERVAL_SECOND             INTERVAL SECOND                ,-- OTHER(1111)
        col_INTERVAL_YEAR_TO_MONTH      INTERVAL YEAR TO MONTH         ,-- OTHER(1111)
        col_INTERVAL_DAY_TO_HOUR        INTERVAL DAY TO HOUR           ,-- OTHER(1111)
        col_INTERVAL_DAY_TO_MINUTE      INTERVAL DAY TO MINUTE         ,-- OTHER(1111)
        col_INTERVAL_DAY_TO_SECOND      INTERVAL DAY TO SECOND         ,-- OTHER(1111)
        col_INTERVAL_HOUR_TO_MINUTE     INTERVAL HOUR TO MINUTE        ,-- OTHER(1111)
        col_INTERVAL_HOUR_TO_SECOND     INTERVAL HOUR TO SECOND        ,-- OTHER(1111)
        col_INTERVAL_MINUTE_TO_SECOND   INTERVAL MINUTE TO SECOND      ,-- OTHER(1111)
        col_ENUM                        ENUM ('hi', 'bye')             ,-- OTHER(1111)
        col_GEOMETRY                    GEOMETRY                       ,-- OTHER(1111)
        col_JSON                        JSON                           ,-- OTHER(1111)
--         c                            ROW(A INT, B VARCHAR)          ,-- OTHER(1111)
        col_JAVA_OBJECT                 JAVA_OBJECT                    ,-- JAVA_OBJECT(2000)
        col_VARCHAR_ARRAY               VARCHAR ARRAY                  ,-- ARRAY(2003)
        col_BLOB                        BLOB                           ,-- BLOB(2004)
        col_CLOB                        CLOB                           ,-- CLOB(2005)
        col_TIME_WITH_TIME_ZONE         TIME WITH TIME ZONE            ,-- TIME_WITH_TIMEZONE(2013)
        col_TIMESTAMP_WITH_TIME_ZONE    TIMESTAMP WITH TIME ZONE       ,-- TIMESTAMP_WITH_TIMEZONE(2014)


        col_Not_Null_TINYINT                     TINYINT                        NOT NULL,-- TINYINT(-6)
        col_Not_Null_BIGINT                      BIGINT                         NOT NULL,-- BIGINT(-5)
        col_Not_Null_VARBINARY                   VARBINARY                      NOT NULL,-- VARBINARY(-3)
        col_Not_Null_BINARY                      BINARY(5)                      NOT NULL,-- BINARY(-2)
        col_Not_Null_UUID                        UUID                           NOT NULL,-- BINARY(-2)
        col_Not_Null_CHARACTER                   CHARACTER(5)                   NOT NULL,-- CHAR(1)
        col_Not_Null_NUMERIC                     NUMERIC                        NOT NULL,-- NUMERIC(2)
        col_Not_Null_FLOAT                       FLOAT                          NOT NULL,-- NUMERIC(2)
        col_Not_Null_INTEGER                     INTEGER                        NOT NULL,-- INTEGER(4)
        col_Not_Null_SMALLINT                    SMALLINT                       NOT NULL,-- SMALLINT(5)
        col_Not_Null_REAL                        REAL                           NOT NULL,-- REAL(7)
        col_Not_Null_DOUBLE                      DOUBLE                         NOT NULL,-- DOUBLE(8)
        col_Not_Null_VARCHAR                     VARCHAR                        NOT NULL,-- VARCHAR(12)
        col_Not_Null_VARCHAR_IGNORECASE          VARCHAR_IGNORECASE             NOT NULL,-- VARCHAR(12)
        col_Not_Null_BOOLEAN                     BOOLEAN                        NOT NULL,-- BOOLEAN(16)
        col_Not_Null_DATE                        DATE                           NOT NULL,-- DATE(91)
        col_Not_Null_TIME                        TIME                           NOT NULL,-- TIME(92)
        col_Not_Null_TIMESTAMP                   TIMESTAMP                      NOT NULL,-- TIMESTAMP(93)
        col_Not_Null_INTERVAL_YEAR               INTERVAL YEAR                  NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_MONTH              INTERVAL MONTH                 NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_DAY                INTERVAL DAY                   NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_HOUR               INTERVAL HOUR                  NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_MINUTE             INTERVAL MINUTE                NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_SECOND             INTERVAL SECOND                NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_YEAR_TO_MONTH      INTERVAL YEAR TO MONTH         NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_DAY_TO_HOUR        INTERVAL DAY TO HOUR           NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_DAY_TO_MINUTE      INTERVAL DAY TO MINUTE         NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_DAY_TO_SECOND      INTERVAL DAY TO SECOND         NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_HOUR_TO_MINUTE     INTERVAL HOUR TO MINUTE        NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_HOUR_TO_SECOND     INTERVAL HOUR TO SECOND        NOT NULL,-- OTHER(1111)
        col_Not_Null_INTERVAL_MINUTE_TO_SECOND   INTERVAL MINUTE TO SECOND      NOT NULL,-- OTHER(1111)
        col_Not_Null_ENUM                        ENUM ('hi', 'bye')             NOT NULL,-- OTHER(1111)
        col_Not_Null_GEOMETRY                    GEOMETRY                       NOT NULL,-- OTHER(1111)
        col_Not_Null_JSON                        JSON                           NOT NULL,-- OTHER(1111)
--         c                            ROW(A INTNOT NULL, B VARCHAR)          NOT NULL,-- OTHER(1111)
        col_Not_Null_JAVA_OBJECT                 JAVA_OBJECT                    NOT NULL,-- JAVA_OBJECT(2000)
        col_Not_Null_VARCHAR_ARRAY               VARCHAR ARRAY                  NOT NULL,-- ARRAY(2003)
        col_Not_Null_BLOB                        BLOB                           NOT NULL,-- BLOB(2004)
        col_Not_Null_CLOB                        CLOB                           NOT NULL,-- CLOB(2005)
        col_Not_Null_TIME_WITH_TIME_ZONE         TIME WITH TIME ZONE            NOT NULL,-- TIME_WITH_TIMEZONE(2013)
        col_Not_Null_TIMESTAMP_WITH_TIME_ZONE    TIMESTAMP WITH TIME ZONE       NOT NULL -- TIMESTAMP_WITH_TIMEZONE(2014)

);

--
-- Table structure for table `actor`
--

CREATE TABLE actor (
  actor_id IDENTITY NOT NULL PRIMARY KEY,
  first_name VARCHAR(45) NOT NULL,
  last_name VARCHAR(45) NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

--
-- Table structure for table `country`
--

CREATE TABLE country (
  country_id IDENTITY NOT NULL PRIMARY KEY,
  country VARCHAR(50) NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

--
-- Table structure for table `city`
--

CREATE TABLE city (
  city_id IDENTITY NOT NULL PRIMARY KEY,
  city VARCHAR(50) NOT NULL,
  country_id BIGINT NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_city_country FOREIGN KEY (country_id) REFERENCES country (country_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

--
-- Table structure for table `address`
--

CREATE TABLE address (
  address_id IDENTITY NOT NULL PRIMARY KEY,
  address VARCHAR(50) NOT NULL,
  address2 VARCHAR(50) DEFAULT NULL,
  district VARCHAR(20) NOT NULL,
  city_id BIGINT NOT NULL,
  postal_code VARCHAR(10) DEFAULT NULL,
  phone VARCHAR(20) NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_address_city FOREIGN KEY (city_id) REFERENCES city (city_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

--
-- Table structure for table `language`
--

CREATE TABLE language (
  language_id IDENTITY NOT NULL PRIMARY KEY,
  name CHAR(20) NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

--
-- Table structure for table `staff`
--

CREATE TABLE staff (
  staff_id IDENTITY NOT NULL PRIMARY KEY,
  first_name VARCHAR(45) NOT NULL,
  last_name VARCHAR(45) NOT NULL,
  address_id BIGINT NOT NULL,
  picture MEDIUMBLOB DEFAULT NULL,
  email VARCHAR(50) DEFAULT NULL,
  store_id BIGINT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  username VARCHAR(16) NOT NULL,
  password VARCHAR(40) DEFAULT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_staff_address FOREIGN KEY (address_id) REFERENCES address (address_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

--
-- Table structure for table `store`
--

CREATE TABLE store (
  store_id IDENTITY NOT NULL PRIMARY KEY,
  manager_staff_id BIGINT NOT NULL,
  address_id BIGINT NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY  (store_id),
  CONSTRAINT idx_unique_manager UNIQUE (manager_staff_id),
  CONSTRAINT fk_store_staff FOREIGN KEY (manager_staff_id) REFERENCES staff (staff_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_store_address FOREIGN KEY (address_id) REFERENCES address (address_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

ALTER TABLE staff ADD CONSTRAINT fk_staff_store FOREIGN KEY (store_id) REFERENCES store (store_id) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Table structure for table `customer`
--

CREATE TABLE customer (
  customer_id IDENTITY NOT NULL PRIMARY KEY,
  store_id BIGINT NOT NULL,
  first_name VARCHAR(45) NOT NULL,
  last_name VARCHAR(45) NOT NULL,
  email VARCHAR(50) DEFAULT NULL,
  address_id BIGINT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_customer_address FOREIGN KEY (address_id) REFERENCES address (address_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_customer_store FOREIGN KEY (store_id) REFERENCES store (store_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

--
-- Table structure for table `film`
--

CREATE TABLE film (
  film_id IDENTITY NOT NULL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description TEXT DEFAULT NULL,
  release_year SMALLINT DEFAULT NULL,
  language_id BIGINT NOT NULL,
  original_language_id LONG DEFAULT NULL,
  rental_duration TINYINT NOT NULL DEFAULT 3,
  rental_rate DECIMAL(4,2) NOT NULL DEFAULT 4.99,
  length SMALLINT DEFAULT NULL,
  replacement_cost DECIMAL(5,2) NOT NULL DEFAULT 19.99,
  rating ENUM('G','PG','PG-13','R','NC-17') DEFAULT 'G',
--   special_features SET('Trailers','Commentaries','Deleted Scenes','Behind the Scenes') DEFAULT NULL,
  special_features VARCHAR(255) DEFAULT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_film_language FOREIGN KEY (language_id) REFERENCES language (language_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_film_language_original FOREIGN KEY (original_language_id) REFERENCES language (language_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

--
-- Table structure for table `film_actor`
--

CREATE TABLE film_actor (
  actor_id BIGINT NOT NULL,
  film_id BIGINT NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY  (actor_id,film_id),
  CONSTRAINT fk_film_actor_actor FOREIGN KEY (actor_id) REFERENCES actor (actor_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_film_actor_film FOREIGN KEY (film_id) REFERENCES film (film_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

--
-- Table structure for table `category`
--

CREATE TABLE category (
  category_id IDENTITY NOT NULL PRIMARY KEY,
  name VARCHAR(25) NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

--
-- Table structure for table `film_category`
--

CREATE TABLE film_category (
  film_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (film_id, category_id),
  CONSTRAINT fk_film_category_film FOREIGN KEY (film_id) REFERENCES film (film_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_film_category_category FOREIGN KEY (category_id) REFERENCES category (category_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

--
-- Table structure for table `inventory`
--

CREATE TABLE inventory (
  inventory_id IDENTITY NOT NULL PRIMARY KEY,
  film_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY  (inventory_id),
  CONSTRAINT fk_inventory_store FOREIGN KEY (store_id) REFERENCES store (store_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_inventory_film FOREIGN KEY (film_id) REFERENCES film (film_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

--
-- Table structure for table `rental`
--

CREATE TABLE rental (
  rental_id IDENTITY NOT NULL PRIMARY KEY,
  rental_date DATETIME NOT NULL,
  inventory_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  return_date DATETIME DEFAULT NULL,
  staff_id BIGINT NOT NULL,
  last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (rental_id),
  CONSTRAINT secondary_key UNIQUE (rental_date,inventory_id,customer_id),
  CONSTRAINT fk_rental_staff FOREIGN KEY (staff_id) REFERENCES staff (staff_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_rental_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (inventory_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_rental_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

--
-- Table structure for table `payment`
--

CREATE TABLE payment (
  payment_id IDENTITY NOT NULL PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  staff_id BIGINT NOT NULL,
  rental_id INT DEFAULT NULL,
  amount DECIMAL(5,2) NOT NULL,
  payment_date DATETIME NOT NULL,
  last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY  (payment_id),
  CONSTRAINT fk_payment_rental FOREIGN KEY (rental_id) REFERENCES rental (rental_id) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_payment_staff FOREIGN KEY (staff_id) REFERENCES staff (staff_id) ON DELETE RESTRICT ON UPDATE CASCADE
);

--
-- View structure for view `customer_list`
--

CREATE VIEW customer_list
AS
SELECT cu.customer_id AS ID, CONCAT(cu.first_name, ' ', cu.last_name) AS name, a.address AS address, a.postal_code AS `zip code`,
	a.phone AS phone, city.city AS city, country.country AS country, CASE WHEN cu.active THEN 'active' ELSE '' END AS notes, cu.store_id AS SID
FROM customer AS cu JOIN address AS a ON cu.address_id = a.address_id JOIN city ON a.city_id = city.city_id
	JOIN country ON city.country_id = country.country_id;

--
-- View structure for view `film_list`
--

CREATE VIEW film_list
AS
SELECT film.film_id AS FID, film.title AS title, film.description AS description, category.name AS category, film.rental_rate AS price,
	film.length AS length, film.rating AS rating, GROUP_CONCAT(CONCAT(actor.first_name, ' ', actor.last_name) SEPARATOR ', ') AS actors
FROM category LEFT JOIN film_category ON category.category_id = film_category.category_id LEFT JOIN film ON film_category.film_id = film.film_id
        JOIN film_actor ON film.film_id = film_actor.film_id
	JOIN actor ON film_actor.actor_id = actor.actor_id
GROUP BY film.film_id, film.title, film.description, film.rental_rate, film.length, film.rating, category.name;

--
-- View structure for view `nicer_but_slower_film_list`
--

--CREATE VIEW nicer_but_slower_film_list
--AS
--SELECT film.film_id AS FID, film.title AS title, film.description AS description, category.name AS category, film.rental_rate AS price,
--	film.length AS length, film.rating AS rating, GROUP_CONCAT(CONCAT(CONCAT(UCASE(SUBSTR(actor.first_name,1,1)),
--	LCASE(SUBSTR(actor.first_name,2,LENGTH(actor.first_name))),' ',CONCAT(UCASE(SUBSTR(actor.last_name,1,1)),
--	LCASE(SUBSTR(actor.last_name,2,LENGTH(actor.last_name)))))) SEPARATOR ', ') AS actors
--FROM category LEFT JOIN film_category ON category.category_id = film_category.category_id LEFT JOIN film ON film_category.film_id = film.film_id
--        JOIN film_actor ON film.film_id = film_actor.film_id
--	JOIN actor ON film_actor.actor_id = actor.actor_id
--GROUP BY film.film_id, film.title, film.description, film.rental_rate, film.length, film.rating, category.name;

--
-- View structure for view `staff_list`
--

CREATE VIEW staff_list
AS
SELECT s.staff_id AS ID, CONCAT(s.first_name, ' ', s.last_name) AS name, a.address AS address, a.postal_code AS `zip code`, a.phone AS phone,
	city.city AS city, country.country AS country, s.store_id AS SID
FROM staff AS s JOIN address AS a ON s.address_id = a.address_id JOIN city ON a.city_id = city.city_id
	JOIN country ON city.country_id = country.country_id;

--
-- View structure for view `sales_by_store`
--

CREATE VIEW sales_by_store
AS
SELECT
CONCAT(c.city, ',', cy.country) AS store
, CONCAT(m.first_name, ' ', m.last_name) AS manager
, SUM(p.amount) AS total_sales
FROM payment AS p
INNER JOIN rental AS r ON p.rental_id = r.rental_id
INNER JOIN inventory AS i ON r.inventory_id = i.inventory_id
INNER JOIN store AS s ON i.store_id = s.store_id
INNER JOIN address AS a ON s.address_id = a.address_id
INNER JOIN city AS c ON a.city_id = c.city_id
INNER JOIN country AS cy ON c.country_id = cy.country_id
INNER JOIN staff AS m ON s.manager_staff_id = m.staff_id
GROUP BY s.store_id
ORDER BY cy.country, c.city;

--
-- View structure for view `sales_by_film_category`
--
-- Note that total sales will add up to >100% because
-- some titles belong to more than 1 category
--

CREATE VIEW sales_by_film_category
AS
SELECT
c.name AS category, SUM(p.amount) AS total_sales
FROM payment AS p
INNER JOIN rental AS r ON p.rental_id = r.rental_id
INNER JOIN inventory AS i ON r.inventory_id = i.inventory_id
INNER JOIN film AS f ON i.film_id = f.film_id
INNER JOIN film_category AS fc ON f.film_id = fc.film_id
INNER JOIN category AS c ON fc.category_id = c.category_id
GROUP BY c.name
ORDER BY total_sales DESC;

--
-- View structure for view `actor_info`
--

CREATE VIEW actor_info
AS
SELECT
a.actor_id,
a.first_name,
a.last_name,
GROUP_CONCAT(DISTINCT CONCAT(c.name, ': ',
		(SELECT GROUP_CONCAT(f.title ORDER BY f.title SEPARATOR ', ')
                    FROM film f
                    INNER JOIN film_category fc
                      ON f.film_id = fc.film_id
                    INNER JOIN film_actor fa
                      ON f.film_id = fa.film_id
                    WHERE fc.category_id = c.category_id
                    AND fa.actor_id = a.actor_id
                 )
             )
             ORDER BY c.name SEPARATOR '; ')
AS film_info
FROM actor a
LEFT JOIN film_actor fa
  ON a.actor_id = fa.actor_id
LEFT JOIN film_category fc
  ON fa.film_id = fc.film_id
LEFT JOIN category c
  ON fc.category_id = c.category_id
GROUP BY a.actor_id, a.first_name, a.last_name;

-- --
-- -- Procedure structure for procedure `rewards_report`
-- --
--
-- DELIMITER //
--
-- CREATE PROCEDURE rewards_report (
--     IN min_monthly_purchases TINYINT
--     , IN min_dollar_amount_purchased DECIMAL(10,2) UNSIGNED
--     , OUT count_rewardees INT
-- )
-- LANGUAGE SQL
-- NOT DETERMINISTIC
-- READS SQL DATA
-- SQL SECURITY DEFINER
-- COMMENT 'Provides a customizable report on best customers'
-- proc: BEGIN
--
--     DECLARE last_month_start DATE;
--     DECLARE last_month_end DATE;
--
--     /* Some sanity checks... */
--     IF min_monthly_purchases = 0 THEN
--         SELECT 'Minimum monthly purchases parameter must be > 0';
--         LEAVE proc;
--     END IF;
--     IF min_dollar_amount_purchased = 0.00 THEN
--         SELECT 'Minimum monthly dollar amount purchased parameter must be > $0.00';
--         LEAVE proc;
--     END IF;
--
--     /* Determine start and end time periods */
--     SET last_month_start = DATE_SUB(CURRENT_DATE(), INTERVAL 1 MONTH);
--     SET last_month_start = STR_TO_DATE(CONCAT(YEAR(last_month_start),'-',MONTH(last_month_start),'-01'),'%Y-%m-%d');
--     SET last_month_end = LAST_DAY(last_month_start);
--
--     /*
--         Create a temporary storage area for
--         Customer IDs.
--     */
--     CREATE TEMPORARY TABLE tmpCustomer (customer_id LONG NOT NULL PRIMARY KEY);
--
--     /*
--         Find all customers meeting the
--         monthly purchase requirements
--     */
--     INSERT INTO tmpCustomer (customer_id)
--     SELECT p.customer_id
--     FROM payment AS p
--     WHERE DATE(p.payment_date) BETWEEN last_month_start AND last_month_end
--     GROUP BY customer_id
--     HAVING SUM(p.amount) > min_dollar_amount_purchased
--     AND COUNT(customer_id) > min_monthly_purchases;
--
--     /* Populate OUT parameter with count of found customers */
--     SELECT COUNT(*) FROM tmpCustomer INTO count_rewardees;
--
--     /*
--         Output ALL customer information of matching rewardees.
--         Customize output as needed.
--     */
--     SELECT c.*
--     FROM tmpCustomer AS t
--     INNER JOIN customer AS c ON t.customer_id = c.customer_id;
--
--     /* Clean up */
--     DROP TABLE tmpCustomer;
-- END //
--
-- DELIMITER ;
--
-- DELIMITER $$
--
-- CREATE FUNCTION get_customer_balance(p_customer_id INT, p_effective_date DATETIME) RETURNS DECIMAL(5,2)
--     DETERMINISTIC
--     READS SQL DATA
-- BEGIN
--
--        #OK, WE NEED TO CALCULATE THE CURRENT BALANCE GIVEN A CUSTOMER_ID AND A DATE
--        #THAT WE WANT THE BALANCE TO BE EFFECTIVE FOR. THE BALANCE IS:
--        #   1) RENTAL FEES FOR ALL PREVIOUS RENTALS
--        #   2) ONE DOLLAR FOR EVERY DAY THE PREVIOUS RENTALS ARE OVERDUE
--        #   3) IF A FILM IS MORE THAN RENTAL_DURATION * 2 OVERDUE, CHARGE THE REPLACEMENT_COST
--        #   4) SUBTRACT ALL PAYMENTS MADE BEFORE THE DATE SPECIFIED
--
--   DECLARE v_rentfees DECIMAL(5,2); #FEES PAID TO RENT THE VIDEOS INITIALLY
--   DECLARE v_overfees INTEGER;      #LATE FEES FOR PRIOR RENTALS
--   DECLARE v_payments DECIMAL(5,2); #SUM OF PAYMENTS MADE PREVIOUSLY
--
--   SELECT IFNULL(SUM(film.rental_rate),0) INTO v_rentfees
--     FROM film, inventory, rental
--     WHERE film.film_id = inventory.film_id
--       AND inventory.inventory_id = rental.inventory_id
--       AND rental.rental_date <= p_effective_date
--       AND rental.customer_id = p_customer_id;
--
--   SELECT IFNULL(SUM(IF((TO_DAYS(rental.return_date) - TO_DAYS(rental.rental_date)) > film.rental_duration,
--         ((TO_DAYS(rental.return_date) - TO_DAYS(rental.rental_date)) - film.rental_duration),0)),0) INTO v_overfees
--     FROM rental, inventory, film
--     WHERE film.film_id = inventory.film_id
--       AND inventory.inventory_id = rental.inventory_id
--       AND rental.rental_date <= p_effective_date
--       AND rental.customer_id = p_customer_id;
--
--
--   SELECT IFNULL(SUM(payment.amount),0) INTO v_payments
--     FROM payment
--
--     WHERE payment.payment_date <= p_effective_date
--     AND payment.customer_id = p_customer_id;
--
--   RETURN v_rentfees + v_overfees - v_payments;
-- END $$
--
-- DELIMITER ;
--
-- DELIMITER $$
--
-- CREATE PROCEDURE film_in_stock(IN p_film_id INT, IN p_store_id INT, OUT p_film_count INT)
-- READS SQL DATA
-- BEGIN
--      SELECT inventory_id
--      FROM inventory
--      WHERE film_id = p_film_id
--      AND store_id = p_store_id
--      AND inventory_in_stock(inventory_id);
--
--      SELECT FOUND_ROWS() INTO p_film_count;
-- END $$
--
-- DELIMITER ;
--
-- DELIMITER $$
--
-- CREATE PROCEDURE film_not_in_stock(IN p_film_id INT, IN p_store_id INT, OUT p_film_count INT)
-- READS SQL DATA
-- BEGIN
--      SELECT inventory_id
--      FROM inventory
--      WHERE film_id = p_film_id
--      AND store_id = p_store_id
--      AND NOT inventory_in_stock(inventory_id);
--
--      SELECT FOUND_ROWS() INTO p_film_count;
-- END $$
--
-- DELIMITER ;
--
-- DELIMITER $$
--
-- CREATE FUNCTION inventory_held_by_customer(p_inventory_id INT) RETURNS INT
-- READS SQL DATA
-- BEGIN
--   DECLARE v_customer_id INT;
--   DECLARE EXIT HANDLER FOR NOT FOUND RETURN NULL;
--
--   SELECT customer_id INTO v_customer_id
--   FROM rental
--   WHERE return_date IS NULL
--   AND inventory_id = p_inventory_id;
--
--   RETURN v_customer_id;
-- END $$
--
-- DELIMITER ;
--
-- DELIMITER $$
--
-- CREATE FUNCTION inventory_in_stock(p_inventory_id INT) RETURNS BOOLEAN
-- READS SQL DATA
-- BEGIN
--     DECLARE v_rentals INT;
--     DECLARE v_out     INT;
--
--     #AN ITEM IS IN-STOCK IF THERE ARE EITHER NO ROWS IN THE rental TABLE
--     #FOR THE ITEM OR ALL ROWS HAVE return_date POPULATED
--
--     SELECT COUNT(*) INTO v_rentals
--     FROM rental
--     WHERE inventory_id = p_inventory_id;
--
--     IF v_rentals = 0 THEN
--       RETURN TRUE;
--     END IF;
--
--     SELECT COUNT(rental_id) INTO v_out
--     FROM inventory LEFT JOIN rental USING(inventory_id)
--     WHERE inventory.inventory_id = p_inventory_id
--     AND rental.return_date IS NULL;
--
--     IF v_out > 0 THEN
--       RETURN FALSE;
--     ELSE
--       RETURN TRUE;
--     END IF;
-- END $$
--
-- DELIMITER ;
--
-- SET SQL_MODE=@OLD_SQL_MODE;
-- SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
-- SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;


