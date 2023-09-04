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

package manifold.sql.schema.sqlite.types;

import manifold.sql.DdlResourceFileTest;
import manifold.sql.schema.simple.sqlite.SqliteAllTypes;
import manifold.sql.schema.simple.sqlite.SqliteAllTypes.*;
import org.junit.Test;

import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.util.UUID;

import static org.junit.Assert.*;

public class AllTypesTest extends DdlResourceFileTest
{
    private static byte Tinyint_value = (byte)8;
    private static long Bigint_value = Long.MAX_VALUE;
    private static byte[] Varbinary_value = new byte[]{4,5,6};
    private static byte[] Binary_value = new byte[]{1,2,3,4,5};
    private static String Uuid_value = UUID.randomUUID().toString();
    private static String Character_value = "chars";
    private static BigDecimal Numeric_value = BigDecimal.TEN;
    private static double Float_value = 3.14;
    private static int Integer_value = 82;
    private static short Smallint_value = 32;
    private static float Real_value = 4.2f;
    private static double Double_value = 5.2;
    private static String Varchar_value = "varchar";
    private static boolean Boolean_value = true;
    private static LocalDate Date_value = LocalDate.of( 2023, 9, 3 );
    private static LocalTime Time_value = LocalTime.of( 12, 56 );
    private static Instant Timestamp_value = Instant.now();
    private static byte[] Blob_value = "blob".getBytes();
    private static String Clob_value = "clob";

  @Test
  public void testAllTypesSetValues() throws SQLException
  {
    AllTypes a = createAllTypes();

    a.setColTinyint( Tinyint_value );
    a.setColBigint( Bigint_value );
    a.setColVarbinary( Varbinary_value );
    a.setColBinary( Binary_value );
    a.setColUuid( Uuid_value );
    a.setColCharacter( Character_value );
    a.setColNumeric( Numeric_value );
    a.setColFloat( Float_value );
    a.setColInteger( Integer_value );
    a.setColSmallint( Smallint_value );
    a.setColReal( Real_value );
    a.setColDouble( Double_value );
    a.setColVarchar( Varchar_value );
    a.setColBoolean( Boolean_value );
    a.setColDate( Date_value );
    a.setColTime( Time_value );
    a.setColTimestamp( Timestamp_value );
    a.setColBlob( Blob_value );
    a.setColClob( Clob_value );

    SqliteAllTypes.commit();

    assertEquals( Tinyint_value,                (byte)a.getColTinyint() );
    assertEquals( Bigint_value,                 (long)a.getColBigint() );
    assertArrayEquals( Varbinary_value,         a.getColVarbinary() );
    assertArrayEquals( Binary_value,            a.getColBinary() );
    assertEquals( Uuid_value,                   a.getColUuid() );
    assertEquals( Character_value,              a.getColCharacter() );
    assertEquals( Numeric_value,                a.getColNumeric() );
    assertEquals( Float_value,                  a.getColFloat(), 0 );
    assertEquals( Integer_value,                (int)a.getColInteger() );
    assertEquals( Smallint_value,               (short)a.getColSmallint() );
    assertEquals( Real_value,                   (float)a.getColReal(), 0 );
    assertEquals( Double_value,                 a.getColDouble(), 0 );
    assertEquals( Varchar_value,                a.getColVarchar() );
    assertEquals( Boolean_value,                a.getColBoolean() );
    assertEquals( Date_value,                   a.getColDate() );
    assertEquals( Time_value,                   a.getColTime() );
    assertEquals( Timestamp_value,              a.getColTimestamp() );
    assertArrayEquals( Blob_value,              a.getColBlob() );
    assertEquals( Clob_value,                   a.getColClob() );


    assertEquals( Tinyint_value,                a.getColNotNullTinyint() );
    assertEquals( Bigint_value,                 a.getColNotNullBigint() );
    assertArrayEquals( Varbinary_value,         a.getColNotNullVarbinary() );
    assertArrayEquals( Binary_value,            a.getColNotNullBinary() );
    assertEquals( Uuid_value,                   a.getColNotNullUuid() );
    assertEquals( Character_value,              a.getColNotNullCharacter() );
    assertEquals( Numeric_value,                a.getColNotNullNumeric() );
    assertEquals( Float_value,                  a.getColNotNullFloat(), 0 );
    assertEquals( Integer_value,                a.getColNotNullInteger() );
    assertEquals( Smallint_value,               a.getColNotNullSmallint() );
    assertEquals( Real_value,                   a.getColNotNullReal(), 0 );
    assertEquals( Double_value,                 a.getColNotNullDouble(), 0 );
    assertEquals( Varchar_value,                a.getColNotNullVarchar() );
    assertEquals( Boolean_value,                a.getColNotNullBoolean() );
    assertEquals( Date_value,                   a.getColNotNullDate() );
    assertEquals( Time_value,                   a.getColNotNullTime() );
    assertEquals( Timestamp_value,              a.getColNotNullTimestamp() );
    assertArrayEquals( Blob_value,              a.getColNotNullBlob() );
    assertEquals( Clob_value,                   a.getColNotNullClob() );
  }

  @Test
  public void testAllTypesSetNullValues() throws SQLException
  {
    AllTypes a = createAllTypes();

    a.setColTinyint( null );
    a.setColBigint( null );
    a.setColVarbinary( null );
    a.setColBinary( null );
    a.setColUuid( null );
    a.setColCharacter( null );
    a.setColNumeric( null );
    a.setColFloat( null );
    a.setColInteger( null );
    a.setColSmallint( null );
    a.setColReal( null );
    a.setColDouble( null );
    a.setColVarchar( null );
    a.setColBoolean( null );
    a.setColDate( null );
    a.setColTime( null );
    a.setColTimestamp( null );
    a.setColBlob( null );
    a.setColClob( null );

    SqliteAllTypes.commit();

    assertNull( a.getColTinyint() );
    assertNull( a.getColBigint() );
    assertNull( a.getColVarbinary() );
    assertNull( a.getColBinary() );
    assertNull( a.getColUuid() );
    assertNull( a.getColCharacter() );
    assertNull( a.getColNumeric() );
    assertNull( a.getColFloat() );
    assertNull( a.getColInteger() );
    assertNull( a.getColSmallint() );
    assertNull( a.getColReal() );
    assertNull( a.getColDouble() );
    assertNull( a.getColVarchar() );
    assertNull( a.getColBoolean() );
    assertNull( a.getColDate() );
    assertNull( a.getColTime() );
    assertNull( a.getColTimestamp() );
    assertNull( a.getColBlob() );
    assertNull( a.getColClob() );


    assertEquals( Tinyint_value,                a.getColNotNullTinyint() );
    assertEquals( Bigint_value,                 a.getColNotNullBigint() );
    assertArrayEquals( Varbinary_value,         a.getColNotNullVarbinary() );
    assertArrayEquals( Binary_value,            a.getColNotNullBinary() );
    assertEquals( Uuid_value,                   a.getColNotNullUuid() );
    assertEquals( Character_value,              a.getColNotNullCharacter() );
    assertEquals( Numeric_value,                a.getColNotNullNumeric() );
    assertEquals( Float_value,                  a.getColNotNullFloat(), 0 );
    assertEquals( Integer_value,                a.getColNotNullInteger() );
    assertEquals( Smallint_value,               a.getColNotNullSmallint() );
    assertEquals( Real_value,                   a.getColNotNullReal(), 0 );
    assertEquals( Double_value,                 a.getColNotNullDouble(), 0 );
    assertEquals( Varchar_value,                a.getColNotNullVarchar() );
    assertEquals( Boolean_value,                a.getColNotNullBoolean() );
    assertEquals( Date_value,                   a.getColNotNullDate() );
    assertEquals( Time_value,                   a.getColNotNullTime() );
    assertEquals( Timestamp_value,              a.getColNotNullTimestamp() );
    assertArrayEquals( Blob_value,              a.getColNotNullBlob() );
    assertEquals( Clob_value,                   a.getColNotNullClob() );
  }

  private static AllTypes createAllTypes()
  {
    AllTypes a = AllTypes.create(
      Tinyint_value,
      Bigint_value,
      Varbinary_value,
      Binary_value,
      Uuid_value,
      Character_value,
      Numeric_value,
      Float_value,
      Integer_value,
      Smallint_value,
      Real_value,
      Double_value,
      Varchar_value,
      Boolean_value,
      Date_value,
      Time_value,
      Timestamp_value,
      Blob_value,
      Clob_value );
    return a;
  }
}
