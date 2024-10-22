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

package manifold.sql.schema.duckdb.types;

import manifold.sql.schema.duckdb.base.DuckdbDdlServerTest;
import manifold.sql.schema.simple.duckdb.DuckdbSakila;
import manifold.sql.schema.simple.duckdb.DuckdbSakila.*;
import org.duckdb.DuckDBStruct;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.*;
import java.util.Map;
import java.util.UUID;

import static java.lang.System.out;
import static org.junit.Assert.*;

public class AllTypesTest extends DuckdbDdlServerTest
{
  private static ZonedDateTime ourTIME_value = ZonedDateTime.of( 1987, 3, 3, 10, 48, 0, 0, ZoneId.systemDefault() );
  private static LocalTime utcTIME_value = ourTIME_value.withZoneSameInstant( ZoneOffset.UTC ).toLocalTime();

  
  private static Long	BIGINT_value = 500L;
  private static String BIT_value = "11100100101";
  private static byte[] BLOB_value = new byte[] {1, 2, 3};
  private static boolean BOOLEAN_value = true;
  private static LocalDate DATE_value = LocalDate.of( 1987, 6, 12 );
  private static BigDecimal DECIMAL_value = new BigDecimal( "1.23" );
  private static Double DOUBLE_value = 3.14;
  private static BigInteger HUGEINT_value = BigInteger.TEN;
  private static Integer INTEGER_value = 10;
  private static String INTERVAL_value = "05:00:00"; //"5 HOUR" also works, but it will be converted to "05:00:00" as a standard
  private static Double REAL_value = 1.0d;
  private static Integer SMALLINT_value = 32;
  private static LocalTime TIME_value = ourTIME_value.toLocalTime();
  private static OffsetDateTime TIMESTAMPTZ_value = OffsetDateTime.of( 1987, 6, 12, 11, 38, 27, 0, ZoneOffset.UTC );
  private static LocalDateTime DATETIME_value = LocalDateTime.of( 1987, 6, 12, 11, 38, 27, 0 );
  private static Byte TINYINT_value = 127;
  private static BigInteger UBIGINT_value = BigInteger.TEN;
  private static BigInteger	UHUGEINT_value = BigInteger.TEN;
  private static Long UINTEGER_value = 123L;
  private static Integer USMALLINT_value = 32;
  private static Short UTINYINT_value = 200;
  private static UUID UUID_value = UUID.randomUUID();
  private static String VARCHAR_value = "hi";

//  private static DuckDBArray ARRAY_value = dbmetadata.createArrayOf(...);
//  private static DuckDBArray LIST_value = dbmetadata.createArrayOf(...)

  //todo: periodically check if these are fixed
  @Test
  public void testTypesThatAreWriteBroken() throws SQLException
  {
    // ARRAY
    Object[] array = (Object[]) "[.sql:DuckdbSakila/] SELECT array_value(1, 2, 3) as arraySample".fetchOne().getArraySample();
    for( Object o : array ) {
      out.println(o.getClass());
    }

    // STRUCT
    DuckDBStruct structSample = "[.sql:DuckdbSakila/] SELECT {'x': 1, 'y': 2, 'z': 3} as structSample".fetchOne().getStructSample();
    structSample.getMap().forEach((key, value) -> out.println(key + ": " + value));

    // MAP
    Map<?,?> mapSample = "[.sql:DuckdbSakila/] SELECT MAP {'x': 1, 'y': 2, 'z': 3} as mapSample".fetchOne().getMapSample();
    mapSample.forEach((key, value) -> out.println(key + ": " + value));

    // BIT (BITSTRING)
// duckdb jdbc driver is all broken, BIT is treated as BOOLEAN
//        String bits = "[.sql/] SELECT '101010'::BIT as my_bits".fetchOne().getMyBits();
  }

  @Test
  public void testAllTypesSetValues() throws SQLException
  {
    AllTypes a = createAllTypes();

    a.setColBigint( BIGINT_value );

//    a.setColBit( BIT_value );     // BIT type is BITSTRING in duckdb, but driver treats as BOOLEAN
//    a.setColBlob( BLOB_value );   // setBinaryStream not implemented

    a.setColBoolean( BOOLEAN_value );
    a.setColDate( DATE_value );
    a.setColDecimal( DECIMAL_value );
    a.setColDouble( DOUBLE_value );
//    a.setColHugeint( HUGEINT_value ); // results in "Invalid Input Error: Unsupported parameter type"
    a.setColInteger( INTEGER_value );
    a.setColInterval( INTERVAL_value );
    a.setColReal( REAL_value );
    a.setColSmallint( SMALLINT_value );
    a.setColTime( TIME_value );
    a.setColTimestamptz( TIMESTAMPTZ_value );
    a.setColDatetime( DATETIME_value );
    a.setColTinyint( TINYINT_value );
//    a.setColUbigint( UBIGINT_value );   // results in "Invalid Input Error: Unsupported parameter type"
//    a.setColUhugeint( UHUGEINT_value ); // results in "Invalid Input Error: Unsupported parameter type"
    a.setColUinteger( UINTEGER_value );
    a.setColUsmallint( USMALLINT_value );
    a.setColUtinyint( UTINYINT_value );
    a.setColUuid( UUID_value );
    a.setColVarchar( VARCHAR_value );

//    a.setColArray( ARRAY_value );  // metadata.createArrayOf() not implemented, can only read Array data from queries, can't insert/update programmatically from Java
//    a.setColList( LIST_value );    // ditto

    DuckdbSakila.commit();

    assertEquals( BIGINT_value, a.getColBigint() );
//    assertEquals( BIT_value, a.getColBit() );
//    assertArrayEquals( BLOB_value, a.getColBlob() );
    assertEquals( BOOLEAN_value, a.getColBoolean() );
    assertEquals( DATE_value, a.getColDate() );
    assertEquals( DECIMAL_value, a.getColDecimal() );
    assertEquals( DOUBLE_value, a.getColDouble() );
//    assertEquals( HUGEINT_value, a.getColHugeint() );
    assertEquals( INTEGER_value, a.getColInteger() );
    assertEquals( INTERVAL_value, a.getColInterval() );
    assertEquals( REAL_value, a.getColReal() );
    assertEquals( SMALLINT_value, a.getColSmallint() );
    assertEquals( utcTIME_value, a.getColTime() );
    assertEquals( TIMESTAMPTZ_value, a.getColTimestamptz() );
    assertEquals( DATETIME_value, a.getColDatetime() );
    assertEquals( TINYINT_value, a.getColTinyint() );
//    assertEquals( UBIGINT_value, a.getColUbigint() );
//    assertEquals( UHUGEINT_value, a.getColUhugeint() );
    assertEquals( UINTEGER_value, a.getColUinteger() );
    assertEquals( USMALLINT_value, a.getColUsmallint() );
    assertEquals( UTINYINT_value, a.getColUtinyint() );
//    assertEquals( UUID_value, a.getColUuid() );
    assertEquals( VARCHAR_value, a.getColVarchar() );
//    assertArrayEquals( ARRAY_value, (String[])a.getColArray() );
//    assertEquals( LIST_value, a.getColList() );

    assertEquals( BIGINT_value, (Long)a.getColNotNullBigint() );
//    assertEquals( BIT_value, a.getColNotNullBit() );
//    assertArrayEquals( BLOB_value, a.getColNotNullBlob() );
    assertEquals( BOOLEAN_value, (Boolean)a.isColNotNullBoolean() );
    assertEquals( DATE_value, a.getColNotNullDate() );
    assertEquals( DECIMAL_value, a.getColNotNullDecimal() );
    assertEquals( DOUBLE_value, (Double)a.getColNotNullDouble() );
//    assertEquals( HUGEINT_value, a.getColNotNullHugeint() );
    assertEquals( INTEGER_value, (Integer)a.getColNotNullInteger() );
    assertEquals( INTERVAL_value, a.getColNotNullInterval() );
    assertEquals( REAL_value, (Double)a.getColNotNullReal() );
    assertEquals( SMALLINT_value, (Integer)a.getColNotNullSmallint() );
    assertEquals( utcTIME_value, a.getColNotNullTime() );
    assertEquals( TIMESTAMPTZ_value, a.getColNotNullTimestamptz() );
    assertEquals( DATETIME_value, a.getColNotNullDatetime() );
    assertEquals( TINYINT_value, (Byte)a.getColNotNullTinyint() );
//    assertEquals( UBIGINT_value, a.getColNotNullUbigint() );
//    assertEquals( UHUGEINT_value, a.getColNotNullUhugeint() );
    assertEquals( UINTEGER_value, a.getColNotNullUinteger() );
    assertEquals( USMALLINT_value, a.getColNotNullUsmallint() );
    assertEquals( UTINYINT_value, a.getColNotNullUtinyint() );
//    assertEquals( UUID_value, a.getColNotNullUuid() );
    assertEquals( VARCHAR_value, a.getColNotNullVarchar() );
//    assertArrayEquals( ARRAY_value, (String[])a.getColNotNullArray() );
//    assertEquals( LIST_value, a.getColNotNullList() );

    a = "[.sql:DuckdbSakila/] SELECT * FROM all_types".fetchOne();
    {
      assertEquals( BIGINT_value, a.getColBigint() );
//      assertEquals( BIT_value, a.getColBit() );
//      assertArrayEquals( BLOB_value, a.getColBlob() );
      assertEquals( BOOLEAN_value, a.getColBoolean() );
      assertEquals( DATE_value, a.getColDate() );
      assertEquals( DECIMAL_value, a.getColDecimal() );
      assertEquals( DOUBLE_value, a.getColDouble() );
//      assertEquals( HUGEINT_value, a.getColHugeint() );
      assertEquals( INTEGER_value, a.getColInteger() );
      assertEquals( INTERVAL_value, a.getColInterval() );
      assertEquals( REAL_value, a.getColReal() );
      assertEquals( SMALLINT_value, a.getColSmallint() );
      assertEquals( utcTIME_value, a.getColTime() );
      assertEquals( TIMESTAMPTZ_value, a.getColTimestamptz() );
      assertEquals( DATETIME_value, a.getColDatetime() );
      assertEquals( TINYINT_value, a.getColTinyint() );
//      assertEquals( UBIGINT_value, a.getColUbigint() );
//      assertEquals( UHUGEINT_value, a.getColUhugeint() );
      assertEquals( UINTEGER_value, a.getColUinteger() );
      assertEquals( USMALLINT_value, a.getColUsmallint() );
      assertEquals( UTINYINT_value, a.getColUtinyint() );
//      assertEquals( UUID_value, a.getColUuid() );
      assertEquals( VARCHAR_value, a.getColVarchar() );
//      assertArrayEquals( ARRAY_value, (String[])a.getColArray() );
//      assertEquals( LIST_value, a.getColList() );

      assertEquals( BIGINT_value, (Long)a.getColNotNullBigint() );
//      assertEquals( BIT_value, a.getColNotNullBit() );
//      assertArrayEquals( BLOB_value, a.getColNotNullBlob() );
      assertEquals( BOOLEAN_value, (Boolean)a.isColNotNullBoolean() );
      assertEquals( DATE_value, a.getColNotNullDate() );
      assertEquals( DECIMAL_value, a.getColNotNullDecimal() );
      assertEquals( DOUBLE_value, (Double)a.getColNotNullDouble() );
//      assertEquals( HUGEINT_value, a.getColNotNullHugeint() );
      assertEquals( INTEGER_value, (Integer)a.getColNotNullInteger() );
      assertEquals( INTERVAL_value, a.getColNotNullInterval() );
      assertEquals( REAL_value, (Double)a.getColNotNullReal() );
      assertEquals( SMALLINT_value, (Integer)a.getColNotNullSmallint() );
      assertEquals( utcTIME_value, a.getColNotNullTime() );
      assertEquals( TIMESTAMPTZ_value, a.getColNotNullTimestamptz() );
      assertEquals( DATETIME_value, a.getColNotNullDatetime() );
      assertEquals( TINYINT_value, (Byte)a.getColNotNullTinyint() );
//      assertEquals( UBIGINT_value, a.getColNotNullUbigint() );
//      assertEquals( UHUGEINT_value, a.getColNotNullUhugeint() );
      assertEquals( UINTEGER_value, a.getColNotNullUinteger() );
      assertEquals( USMALLINT_value, a.getColNotNullUsmallint() );
      assertEquals( UTINYINT_value, a.getColNotNullUtinyint() );
//      assertEquals( UUID_value, a.getColNotNullUuid() );
      assertEquals( VARCHAR_value, a.getColNotNullVarchar() );
//      assertArrayEquals( ARRAY_value, (String[])a.getColNotNullArray() );
//      assertEquals( LIST_value, a.getColNotNullList() );
    }
  }

  private static LocalTime utcTime( LocalTime time )
  {
    return OffsetTime.of( time, ZoneOffset.UTC ).toLocalTime();
  }

  private static AllTypes createAllTypes()
  {
    AllTypes a = AllTypes.create(
      BIGINT_value,
      /* BIT_value, */  // read-only for now
      /* BLOB_value, */ // read-only for now
      BOOLEAN_value,
      DATE_value,
      DECIMAL_value,
      DOUBLE_value,
//      HUGEINT_value,
      INTEGER_value,
      INTERVAL_value,
      REAL_value,
      SMALLINT_value,
      TIME_value,
      TIMESTAMPTZ_value,
      DATETIME_value,
      TINYINT_value,
//      UBIGINT_value,
//      UHUGEINT_value,
      UINTEGER_value,
      USMALLINT_value,
      UTINYINT_value,
      UUID_value,
      VARCHAR_value
      /* ARRAY_value, */  // read-only for now
      /* LIST_value ); */ // read-only for now
    );
    return a;
  }
}
