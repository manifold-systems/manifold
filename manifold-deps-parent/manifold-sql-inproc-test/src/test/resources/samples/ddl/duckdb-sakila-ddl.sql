-- DROP SCHEMA IF EXISTS Sakila CASCADE;
-- CREATE SCHEMA Sakila;
-- USE memory.Sakila;

create table all_types (
       col_BIGINT              BIGINT,
       col_BIT                 BIT,
       col_BLOB                BLOB,
       col_BOOLEAN             BOOLEAN,
       col_DATE                DATE,
       col_DECIMAL             DECIMAL(3, 2),
       col_DOUBLE              DOUBLE,
       col_HUGEINT             HUGEINT,
       col_INTEGER             INTEGER,
       col_INTERVAL            INTERVAL,
       col_REAL                REAL,
       col_SMALLINT            SMALLINT,
       col_TIME                TIME,
       col_TIMESTAMPTZ         TIMESTAMPTZ,
       col_DATETIME            DATETIME,
       col_TINYINT             TINYINT,
       col_UBIGINT             UBIGINT,
       col_UHUGEINT            UHUGEINT,
       col_UINTEGER            UINTEGER,
       col_USMALLINT           USMALLINT,
       col_UTINYINT            UTINYINT,
       col_UUID                UUID,
       col_VARCHAR             VARCHAR,
-- note, can only read these now, jdbc driver does not array creation yet e.g., dbmetadata.createArrayOf(...) not implemented
       col_ARRAY               VARCHAR[3],
       col_LIST                VARCHAR[],


--
-- commented out columns can't be set correctly in the duckdb jdbc driver, calling setXxx for them fails, thus they can't be non-null
--
       col_Not_Null_BIGINT              BIGINT         NOT NULL,

-- jdbc driver thinks BIT is BOOLEAN in DuckDBPreparedStatement setObject, sb bitstring
--        col_Not_Null_BIT                 BIT            NOT NULL,
-- jdbc driver does not support setBinaryStream or setObject w BLOB
--        col_Not_Null_BLOB                BLOB           NOT NULL,

       col_Not_Null_BOOLEAN             BOOLEAN        NOT NULL,
       col_Not_Null_DATE                DATE           NOT NULL,
       col_Not_Null_DECIMAL             DECIMAL(3, 2)  NOT NULL,
       col_Not_Null_DOUBLE              DOUBLE         NOT NULL,
--        col_Not_Null_HUGEINT             HUGEINT        NOT NULL,
       col_Not_Null_INTEGER             INTEGER        NOT NULL,
       col_Not_Null_INTERVAL            INTERVAL       NOT NULL,
       col_Not_Null_REAL                REAL           NOT NULL,
       col_Not_Null_SMALLINT            SMALLINT       NOT NULL,
       col_Not_Null_TIME                TIME           NOT NULL,
       col_Not_Null_TIMESTAMPTZ         TIMESTAMPTZ    NOT NULL,
       col_Not_Null_DATETIME            DATETIME       NOT NULL,
       col_Not_Null_TINYINT             TINYINT        NOT NULL,
--        col_Not_Null_UBIGINT             UBIGINT        NOT NULL,
--        col_Not_Null_UHUGEINT            UHUGEINT       NOT NULL,
       col_Not_Null_UINTEGER            UINTEGER       NOT NULL,
       col_Not_Null_USMALLINT           USMALLINT      NOT NULL,
       col_Not_Null_UTINYINT            UTINYINT       NOT NULL,
       col_Not_Null_UUID                UUID           NOT NULL,
       col_Not_Null_VARCHAR             VARCHAR        NOT NULL

--  jdbc driver does not support array creation yet e.g., dbmetadata.createArrayOf(...) not implemented
--        col_Not_Null_ARRAY               VARCHAR[3]     NOT NULL,
--        col_Not_Null_LIST                VARCHAR[]      NOT NULL
);                                                      

--
-- Table structure for table `actor`
--

CREATE SEQUENCE actor_id START 1;
CREATE TABLE actor (
                       actor_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('actor_id'),
                       first_name VARCHAR(45) NOT NULL,
                       last_name VARCHAR(45) NOT NULL,
                       last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Name: mpaa_rating; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE mpaa_rating AS ENUM (
    'G',
    'PG',
    'PG-13',
    'R',
    'NC-17'
    );

--
-- Table structure for table `country`
--

CREATE SEQUENCE country_id START 1;
CREATE TABLE country (
                         country_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('country_id'),
                         country VARCHAR(50) NOT NULL,
                         last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `city`
--

CREATE SEQUENCE city_id START 1;
CREATE TABLE city (
                      city_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('city_id'),
                      city VARCHAR(50) NOT NULL,
                      country_id INTEGER NOT NULL REFERENCES country(country_id),
                      last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
);

--
-- Table structure for table `address`
--

CREATE SEQUENCE address_id START 1;
CREATE TABLE address (
                         address_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('address_id'),
                         address VARCHAR(50) NOT NULL,
                         address2 VARCHAR(50) DEFAULT NULL,
                         district VARCHAR(20) NOT NULL,
                         city_id INTEGER NOT NULL REFERENCES city(city_id),
                         postal_code VARCHAR(10) DEFAULT NULL,
                         phone VARCHAR(20) NOT NULL,
                         last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `language`
--

CREATE SEQUENCE language_id START 1;
CREATE TABLE 'language' (
    language_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('language_id'),
    name VARCHAR(20) NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

--
-- Table structure for table `staff`
--

CREATE SEQUENCE staff_id START 1;
CREATE TABLE staff (
                       staff_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('staff_id'),
                       first_name VARCHAR(45) NOT NULL,
                       last_name VARCHAR(45) NOT NULL,
                       address_id INTEGER NOT NULL REFERENCES address(address_id),
                       picture BLOB DEFAULT NULL,
                       email VARCHAR(50) DEFAULT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       store_id INTEGER NOT NULL, -- REFERENCES store(store_id)
                       username VARCHAR(16) NOT NULL,
                       password VARCHAR(40) DEFAULT NULL,
                       last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `store`
--
CREATE SEQUENCE store_id START 1;
CREATE TABLE store (
                       store_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('store_id'),
                       manager_staff_id INTEGER NOT NULL UNIQUE REFERENCES staff(staff_id),
                       address_id INTEGER NOT NULL REFERENCES address(address_id),
                       last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `customer`
--

CREATE SEQUENCE customer_id START 1;
CREATE TABLE customer (
                          customer_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('customer_id'),
                          store_id INTEGER NOT NULL REFERENCES store(store_id),
                          first_name VARCHAR(45) NOT NULL,
                          last_name VARCHAR(45) NOT NULL,
                          email VARCHAR(50) DEFAULT NULL,
                          address_id INTEGER NOT NULL REFERENCES address(address_id),
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `film`
--

CREATE SEQUENCE film_id START 1;
CREATE TABLE film (
                      film_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('film_id'),
                      title VARCHAR(255) NOT NULL,
                      description TEXT DEFAULT NULL,
                      release_year SMALLINT DEFAULT NULL,
                      language_id INTEGER NOT NULL REFERENCES 'language'(language_id),
                      original_language_id INTEGER DEFAULT NULL REFERENCES 'language'(language_id),
                      rental_duration TINYINT NOT NULL DEFAULT 3,
                      rental_rate DECIMAL(4,2) NOT NULL DEFAULT 4.99,
                      length SMALLINT DEFAULT NULL,
                      replacement_cost DECIMAL(5,2) NOT NULL DEFAULT 19.99,
                      rating mpaa_rating DEFAULT 'G',
--   special_features SET('Trailers','Commentaries','Deleted Scenes','Behind the Scenes') DEFAULT NULL,
                      special_features VARCHAR(255) DEFAULT NULL,
                      last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `film_actor`
--

CREATE TABLE film_actor (
                            actor_id INTEGER NOT NULL REFERENCES actor(actor_id),
                            film_id INTEGER NOT NULL REFERENCES film(film_id),
                            PRIMARY KEY  (actor_id,film_id),
                            last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `category`
--

CREATE SEQUENCE category_id START 1;
CREATE TABLE category (
                          category_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('category_id'),
                          name VARCHAR(25) NOT NULL,
                          last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `film_category`
--

CREATE TABLE film_category (
                               film_id INTEGER NOT NULL REFERENCES film(film_id),
                               category_id INTEGER NOT NULL REFERENCES category(category_id),
                               PRIMARY KEY (film_id, category_id),
                               last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `inventory`
--

CREATE SEQUENCE inventory_id START 1;
CREATE TABLE inventory (
                           inventory_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('inventory_id'),
                           film_id INTEGER NOT NULL REFERENCES film(film_id),
                           store_id INTEGER NOT NULL REFERENCES store(store_id),
                           last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `rental`
--

CREATE SEQUENCE rental_id START 1;
CREATE TABLE rental (
                        rental_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('rental_id'),
                        rental_date DATETIME NOT NULL,
                        inventory_id INTEGER NOT NULL REFERENCES inventory(inventory_id),
                        customer_id INTEGER NOT NULL REFERENCES customer(customer_id),
                        return_date DATETIME DEFAULT NULL,
                        staff_id INTEGER NOT NULL REFERENCES staff(staff_id),
                        last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT secondary_key UNIQUE (rental_date,inventory_id,customer_id)
);

--
-- Table structure for table `payment`
--

CREATE SEQUENCE payment_id START 1;
CREATE TABLE payment (
                         payment_id INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('payment_id'),
                         customer_id INTEGER NOT NULL REFERENCES customer(customer_id),
                         staff_id INTEGER NOT NULL REFERENCES staff(staff_id),
                         rental_id INTEGER DEFAULT NULL,
                         amount DECIMAL(5,2) NOT NULL,
                         payment_date DATETIME NOT NULL,
                         last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



