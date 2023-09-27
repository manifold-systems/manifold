create table all_types (
    col_bigint                                  bigint,                     -- signed eight-byte integer
    col_bigserial                               bigserial,                  -- (serial8) autoincrementing eight-byte integer
    col_boolean	                                boolean,                    -- logical Boolean (true/false)
    col_bytea	 	                            bytea,                      -- binary data ("byte array")
    col_varchar	                                varchar,                    -- variable-length character string
    col_char5                                   char(5),                    -- fixed-length character string
    col_date                                    date,                       -- calendar date (year, month, day)
    col_double                                  double precision,           -- (float8) double precision floating-point number
    col_integer	                                integer,                    -- (int, int4) signed four-byte integer
    col_interval                                interval,                   -- time span
    col_numeric                                 numeric(5,2),               -- (decimal[(p, s)]) exact numeric of selectable precision
    col_real                                    real,                       -- (float4) single precision floating-point number
    col_smallint                                smallint,                   -- (int2) signed two-byte integer
    col_serial                                  serial,                     -- (serial4) autoincrementing four-byte integer
    col_text                                    text,                       -- variable-length character string
    col_time                                    time,                       -- time of day
    col_timetz                                  time with time zone,        -- (timetz) time of day, including time zone
    col_timestamp                               timestamp,                  -- date and time
    col_timestamptz                             timestamp with time zone,   -- (timestamptz) date and time, including time zone
    col_uuid                                    uuid,                        -- UUID

-- These sql types require a cast when inserting values and for query parameters. There is no Java type the driver
-- accepts via setObject() or setXxx(), all result in type mismatch errors despite getObject() and getXxx() delivering
-- values of those same Java types. Not supporting these for now. Will add one-off support for casting at some point.
--  
    col_bit                                     bit,                        -- fixed-length bit string
    col_bit5                                    bit(5),                     -- fixed-length bit string
    col_varbit                                  varbit,                     -- variable-length bit string
    col_cidr                                    cidr,                       -- IPv4 or IPv6 network address
    col_inet                                    inet,                       -- IPv4 or IPv6 host address
    col_macaddr                                 macaddr,                    -- MAC address
    col_money                                   money                      -- currency amount

-- Following types either do not have a '=' operator or support '=' in a strange way where equality means "the same areas."
-- Thus, they can't be used normally in a where clause. Note, some types support the postgres "same as" operator ~=, but
-- postgres is turning out to be another sqlite in terms of nonsensical behavior. Not supporting these types (for now).
--
-- See https:--dba.stackexchange.com/questions/252066/how-to-formulate-equality-predicate-on-point-column-in-postgresql
--
--    col_box                                     box,                        -- rectangular box in the plane
--    col_circle                                  circle,                     -- circle in the plane
--    col_line                                    line,                       -- infinite line in the plane
--    col_lseg                                    lseg,                       -- line segment in the plane
--    col_path                                    path,                       -- geometric path in the plane
--    col_point                                   point,                      -- geometric point in the plane
--    col_polygon                                 polygon,                    -- closed geometric path in the plane
        
);
