CREATE TABLE product (
    id IDENTITY PRIMARY KEY,
    sku VARCHAR(256),
    name VARCHAR(256),
    price DECIMAL(20, 2),
    UNIQUE(sku)
);

CREATE TABLE customer (
    id IDENTITY PRIMARY KEY,
    name VARCHAR(256),
    birthdate DATE
);

CREATE TABLE address (
    id INT8 PRIMARY KEY,
    street VARCHAR(128),
    postal_code VARCHAR(16),
    FOREIGN KEY(id) REFERENCES customer(id) ON DELETE CASCADE
);

CREATE TABLE email_address (
    id IDENTITY PRIMARY KEY,
    customer_id INT8,
    address VARCHAR(128),
    FOREIGN KEY(customer_id) REFERENCES customer(id) ON DELETE CASCADE
);

CREATE TABLE purchase_order (
    id IDENTITY PRIMARY KEY,
    customer_id INT8,
    order_date DATE,
    FOREIGN KEY(customer_id) REFERENCES customer(id)
);

CREATE TABLE item (
    id IDENTITY PRIMARY KEY,
    order_id INT8,
    product_id INT8,
    quantity INT,
    total DECIMAL(20, 2),
    FOREIGN KEY(order_id) REFERENCES purchase_order(id),
    FOREIGN KEY(product_id) REFERENCES product(id)
);

INSERT INTO product (sku, name, price) VALUES ('keyboard', 'Keyboard', 7.99);
INSERT INTO product (sku, name, price) VALUES ('tv', 'Television', 351.96);
INSERT INTO product (sku, name, price) VALUES ('shirt', 'Shirt', 3.57);
INSERT INTO product (sku, name, price) VALUES ('bed', 'Bed', 131.00);
INSERT INTO product (sku, name, price) VALUES ('cell-phone', 'Cell Phone', 1000.00);
INSERT INTO product (sku, name, price) VALUES ('spoon', 'Spoon', 1.00);

INSERT INTO customer (name, birthdate) VALUES ('Emily White', '1960-10-30');
INSERT INTO customer (name, birthdate) VALUES ('Cheryl Dunno', '1954-07-15');
INSERT INTO customer (name, birthdate) VALUES ('Alisson Fuller', '1956-05-12');

INSERT INTO address (street, postal_code) VALUES ('300 Greendale Dr. Starlington OH', '38076');
INSERT INTO address (street, postal_code) VALUES ('400 Depeche Mode Ln. Harland TX', '84210');
INSERT INTO address (street, postal_code) VALUES ('500 Swan Ct. Riverdale CA', '92013');

INSERT INTO email_address (customer_id, address) VALUES (1, 'john.doe@gmail.com');
INSERT INTO email_address (customer_id, address) VALUES (1, 'john.doe@hotmail.com');
INSERT INTO email_address (customer_id, address) VALUES (2, 'joe.schmo@gmail.com');
INSERT INTO email_address (customer_id, address) VALUES (3, 'hi.hello@lol.com');

INSERT INTO purchase_order (customer_id, order_date) VALUES (2, '2023-11-10');
INSERT INTO purchase_order (customer_id, order_date) VALUES (1, '2023-20-22');
INSERT INTO purchase_order (customer_id, order_date) VALUES (2, '2023-17-08');

INSERT INTO item (order_id, product_id, quantity, total) VALUES (1, 1, 10, 79.90);
INSERT INTO item (order_id, product_id, quantity, total) VALUES (1, 2, 2, 703.92);
INSERT INTO item (order_id, product_id, quantity, total) VALUES (1, 3, 7, 24.99);
INSERT INTO item (order_id, product_id, quantity, total) VALUES (2, 4, 2, 262.00);
INSERT INTO item (order_id, product_id, quantity, total) VALUES (2, 5, 15, 15000.00);
INSERT INTO item (order_id, product_id, quantity, total) VALUES (3, 1, 7, 55.93);
INSERT INTO item (order_id, product_id, quantity, total) VALUES (3, 6, 18, 18.00);