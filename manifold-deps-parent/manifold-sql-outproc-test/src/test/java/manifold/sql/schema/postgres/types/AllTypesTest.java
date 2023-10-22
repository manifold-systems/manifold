/*
 * Copyright (c) 2023 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.sql.schema.postgres.types;

import manifold.sql.schema.postgres.PostgresDdlServerTest;
import manifold.sql.schema.simple.postgres.PostgresSakila;
import manifold.sql.schema.simple.postgres.PostgresSakila.*;
import org.junit.Test;
import org.postgresql.geometric.*;
import org.postgresql.util.PGInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.Assert.*;

public class AllTypesTest extends PostgresDdlServerTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger( AllTypesTest.class );

  private static Long Bigint_value = Long.MAX_VALUE;
  private static Boolean Boolean_value = true;
  private static byte[] Bytea_value = {0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9};
  private static String Char5_value = "char5";
  private static LocalDate Date_value = LocalDate.of(2023, 9, 20);
  private static Double Double_value = Double.MAX_VALUE;
  private static PGInterval Interval_value = new PGInterval(1,2,3,4,5,6);
  private static Integer Integer_value = Integer.MAX_VALUE;
  private static java.math.BigDecimal Numeric_value = BigDecimal.TEN;
  private static Float Real_value = Float.MAX_VALUE;
  private static Short Smallint_value = 5000;
  private static String Text_value = "text text text text text text text text text text text text text text text";
  private static LocalTime Time_value = LocalTime.of(1,2,3);
  private static LocalTime Timetz_value = LocalTime.of(4,5,6);
  private static Instant Timestamp_value = Instant.now();
  private static Instant Timestamptz_value = Instant.now();
  private static String Varchar_value = "varchar";
  private static UUID Uuid_value = UUID.randomUUID();

// These sql types require a cast when inserting values and for query parameters. There is no Java type the driver
// accepts via setObject() or setXxx(), all result in type mismatch errors despite getObject() and getXxx() delivering
// values of those same Java types. As a consequence, these types involve ValueAccessor#getParameterExpression() intervention.
  private static Boolean Bit_value = true;                   // bit (single)
  private static String Bit5_value = "101111101101";                // bit5 (many)
  private static String Varbit_value = "11100100101";        // varbit
  private static String Cidr_value = "192.168.100.128/25";   // cidr
  private static String Inet_value = "192.0.2.1";            // inet
  private static String Macaddr_value = "08:00:2b:01:02:03"; // macaddr
  private static Double Money_value = 1.01;                  // money

//todo:
// Following types either do not have a '=' operator or support '=' in a strange way where equality means "the same areas."
// Thus, they can't be used normally in a where clause. Note, some types support the postgres "same as" operator ~=, but
// postgres is becoming another sqlite in terms of nonsensical behavior. Not supporting these types (for now).
//
// See https://dba.stackexchange.com/questions/252066/how-to-formulate-equality-predicate-on-point-column-in-postgresql
//
//  private static PGbox Box_value = new PGbox(1,2,3,4);
//  private static PGcircle Circle_value = new PGcircle(1,2,3);
//  private static PGline Line_value = new PGline(1,2,3);
//  private static PGlseg Lseg_value = new PGlseg(1,2,3,4);
//  private static PGpath Path_value = new PGpath(new PGpoint[]{new PGpoint(1,2), new PGpoint(3,4)}, true);
//  private static PGpoint Point_value = new PGpoint(1,2);
//  private static PGpolygon Polygon_value = new PGpolygon(new PGpoint[]{new PGpoint(1,2), new PGpoint(3,4), new PGpoint(5,2)});

  @Test
  public void testAllTypesSetValues() throws SQLException
  {
    //AllTypes a = createAllTypes();
    AllTypes a = AllTypes.create();

    a.setColBigint( Bigint_value );
    a.setColBit( Bit_value );
    a.setColBit5( Bit5_value );
    a.setColVarbit( Varbit_value );
    a.setColBoolean( Boolean_value );
    a.setColBytea( Bytea_value );
    a.setColChar5( Char5_value );
    a.setColDate( Date_value );
    a.setColDouble( Double_value );
    a.setColInteger( Integer_value );
    a.setColInterval( Interval_value );
    a.setColInet( Inet_value );
    a.setColCidr( Cidr_value );
    a.setColMacaddr( Macaddr_value );
    a.setColMoney( Money_value );
    a.setColNumeric( Numeric_value );
    a.setColReal( Real_value );
    a.setColSmallint( Smallint_value );
    a.setColText( Text_value );
    a.setColTime( Time_value );
    a.setColTimetz( Timetz_value );
    a.setColTimestamp( Timestamp_value );
    a.setColTimestamptz( Timestamptz_value );
    a.setColVarchar( Varchar_value );
    a.setColUuid( Uuid_value );
//    a.setColLine( Line_value );
//    a.setColLseg( Lseg_value );
//    a.setColPath( Path_value );
//    a.setColPoint( Point_value );
//    a.setColCircle( Circle_value );
//    a.setColBox( Box_value );
//    a.setColPolygon( Polygon_value );

    PostgresSakila.commit();

    assertEquals( Bigint_value, a.getColBigint() );
    assertTrue( a.getColBigserial() >= 1L );
    assertEquals( Bit_value, a.getColBit() );
    assertEquals( Bit5_value, a.getColBit5() );
    assertEquals( Varbit_value, a.getColVarbit() );
    assertEquals( Boolean_value, a.getColBoolean() );
    assertArrayEquals( Bytea_value, a.getColBytea() );
    assertEquals( Char5_value, a.getColChar5() );
    assertEquals( Date_value, a.getColDate() );
    assertEquals( Double_value, a.getColDouble() );
    assertEquals( Integer_value, a.getColInteger() );
    assertEquals( Interval_value, a.getColInterval() );
    assertEquals( Inet_value, a.getColInet() );
    assertEquals( Cidr_value, a.getColCidr() );
    assertEquals( Macaddr_value, a.getColMacaddr() );
    assertEquals( Money_value, a.getColMoney() );
    assertEquals( 0, Numeric_value.compareTo( a.getColNumeric() ) );
    assertEquals( Real_value, a.getColReal() );
    assertEquals( 1, (int)a.getColSerial() );
    assertEquals( Smallint_value, a.getColSmallint() );
    assertEquals( Text_value, a.getColText() );
    assertEquals( Time_value, a.getColTime() );
    assertEquals( Timetz_value, a.getColTimetz() );
    assertEquals( Timestamp_value, a.getColTimestamp() );
    assertEquals( Timestamptz_value, a.getColTimestamptz() );
    assertEquals( Varchar_value, a.getColVarchar() );
    assertEquals( Uuid_value, a.getColUuid() );
//    assertEquals( Box_value, a.getColBox() );
//    assertEquals( Circle_value, a.getColCircle() );
//    assertEquals( Line_value, a.getColLine() );
//    assertEquals( Lseg_value, a.getColLseg() );
//    assertEquals( Path_value, a.getColPath() );
//    assertEquals( Point_value, a.getColPoint() );
//    assertEquals( Polygon_value, a.getColPolygon() );

    a = "[.sql:PostgresSakila/] SELECT * FROM all_types".fetchOne();
    {
      assertEquals( Bigint_value, a.getColBigint() );
      assertTrue( a.getColBigserial() >= 1L );
      assertEquals( Bit_value, a.getColBit() );
      assertEquals( Bit5_value, a.getColBit5() );
      assertEquals( Varbit_value, a.getColVarbit() );
      assertEquals( Boolean_value, a.getColBoolean() );
      assertArrayEquals( Bytea_value, a.getColBytea() );
      assertEquals( Char5_value, a.getColChar5() );
      assertEquals( Date_value, a.getColDate() );
      assertEquals( Double_value, a.getColDouble() );
      assertEquals( Integer_value, a.getColInteger() );
      assertEquals( Interval_value, a.getColInterval() );
      assertEquals( Inet_value, a.getColInet() );
      assertEquals( Cidr_value, a.getColCidr() );
      assertEquals( Macaddr_value, a.getColMacaddr() );
      assertEquals( Money_value, a.getColMoney() );
      assertEquals( 0, Numeric_value.compareTo( a.getColNumeric() ) );
      assertEquals( Real_value, a.getColReal() );
      assertEquals( 1, (int)a.getColSerial() );
      assertEquals( Smallint_value, a.getColSmallint() );
      assertEquals( Text_value, a.getColText() );
      assertEquals( Time_value, a.getColTime() );
      assertEquals( Timetz_value, a.getColTimetz() );
      assertEquals( Timestamp_value, a.getColTimestamp() );
      assertEquals( Timestamptz_value, a.getColTimestamptz() );
      assertEquals( Varchar_value, a.getColVarchar() );
      assertEquals( Uuid_value, a.getColUuid() );
//    assertEquals( Box_value, a.getColBox() );
//    assertEquals( Circle_value, a.getColCircle() );
//    assertEquals( Line_value, a.getColLine() );
//    assertEquals( Lseg_value, a.getColLseg() );
//    assertEquals( Path_value, a.getColPath() );
//    assertEquals( Point_value, a.getColPoint() );
//    assertEquals( Polygon_value, a.getColPolygon() );
    }
  }
//
//  @Test
//  public void testAllTypesSetNullValues() throws SQLException
//  {
//    AllTypes a = createAllTypes();
//
//    a.setColTinyint( null );
//    a.setColBigint( null );
//    a.setColVarbinary( null );
//    a.setColBinary( null );
//    a.setColUuid( null );
//    a.setColCharacter( null );
//    a.setColNumeric( null );
//    a.setColFloat( null );
//    a.setColInteger( null );
//    a.setColSmallint( null );
//    a.setColReal( null );
//    a.setColDouble( null );
//    a.setColVarchar( null );
//    a.setColVarcharIgnorecase( null );
//    a.setColBoolean( null );
//    a.setColDate( null );
//    a.setColTime( null );
//    a.setColTimestamp( null );
//    a.setColEnum( null );
//    a.setColJson( null );
//    a.setColJavaObject( null );
//    a.setColVarcharArray( null );
//    a.setColBlob( null );
//    a.setColClob( null );
//    a.setColTimeWithTimeZone( null );
//    a.setColTimestampWithTimeZone( null );
//
//    H2AllTypes.commit();
//
//    assertNull( a.getColTinyint() );
//    assertNull( a.getColBigint() );
//    assertNull( a.getColVarbinary() );
//    assertNull( a.getColBinary() );
//    assertNull( a.getColUuid() );
//    assertNull( a.getColCharacter() );
//    assertNull( a.getColNumeric() );
//    assertNull( a.getColFloat() );
//    assertNull( a.getColInteger() );
//    assertNull( a.getColSmallint() );
//    assertNull( a.getColReal() );
//    assertNull( a.getColDouble() );
//    assertNull( a.getColVarchar() );
//    assertNull( a.getColVarcharIgnorecase() );
//    assertNull( a.getColBoolean() );
//    assertNull( a.getColDate() );
//    assertNull( a.getColTime() );
//    assertNull( a.getColTimestamp() );
//    assertNull( a.getColEnum() );
//    assertNull( a.getColJson() );
//    assertNull( a.getColJavaObject() );
//    assertNull( a.getColVarcharArray() );
//    assertNull( a.getColBlob() );
//    assertNull( a.getColClob() );
//    assertNull( a.getColTimeWithTimeZone() );
//    assertNull( a.getColTimestampWithTimeZone() );
//
//
//    assertEquals( Tinyint_value,                a.getColNotNullTinyint() );
//    assertEquals( Bigint_value,                 a.getColNotNullBigint() );
//    assertArrayEquals( Varbinary_value,         a.getColNotNullVarbinary() );
//    assertArrayEquals( Binary_value,            a.getColNotNullBinary() );
//    assertEquals( Uuid_value,                   a.getColNotNullUuid() );
//    assertEquals( Character_value,              a.getColNotNullCharacter() );
//    assertEquals( Numeric_value,                a.getColNotNullNumeric() );
//    assertEquals( Float_value,                  a.getColNotNullFloat(), 0 );
//    assertEquals( Integer_value,                a.getColNotNullInteger() );
//    assertEquals( Smallint_value,               a.getColNotNullSmallint() );
//    assertEquals( Real_value,                   a.getColNotNullReal(), 0 );
//    assertEquals( Double_value,                 a.getColNotNullDouble(), 0 );
//    assertEquals( Varchar_value,                a.getColNotNullVarchar() );
//    assertEquals( VarcharIgnorecase_value,      a.getColNotNullVarcharIgnorecase() );
//    assertEquals( Boolean_value,                a.getColNotNullBoolean() );
//    assertEquals( Date_value,                   a.getColNotNullDate() );
//    assertEquals( Time_value,                   a.getColNotNullTime() );
//    assertEquals( Timestamp_value,              a.getColNotNullTimestamp() );
//    assertEquals( Enum_value,                   a.getColNotNullEnum() );
//    assertArrayEquals( Json_value,              a.getColNotNullJson() );
//    assertEquals( JavaObject_value,             a.getColNotNullJavaObject() );
//    assertArrayEquals( VarcharArray_value,      (Object[])a.getColNotNullVarcharArray() );
//    assertArrayEquals( Blob_value,              a.getColNotNullBlob() );
//    assertEquals( Clob_value,                   a.getColNotNullClob() );
//    assertEquals( TimeWithTimeZone_value,       a.getColNotNullTimeWithTimeZone() );
//    assertEquals( TimestampWithTimeZone_value,  a.getColNotNullTimestampWithTimeZone() );
//  }
}
