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

package manifold.sql.schema.mysql.types;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;

import manifold.sql.schema.mysql.MysqlDdlServerTest;
import manifold.sql.schema.simple.mysql.MysqlSakila;
import manifold.sql.schema.simple.mysql.MysqlSakila.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class AllTypesTest extends MysqlDdlServerTest
{
  Boolean Bit_value = true;                                  // BIT(-7)
  String Bit8_value = "10010110";                            // BIT(-7)
  Byte Tinyint_value = Byte.MAX_VALUE;                       // TINYINT(-6)
  Byte TinyintUnsigned_value = Byte.MAX_VALUE;               // TINYINT(-6)
  Long Bigint_value = Long.MAX_VALUE;                        // BIGINT(-5)
  Long BigintUnsigned_value = Long.MAX_VALUE;                // BIGINT(-5)
  byte[] LongVarbinary_value = {6, 7, 8, 9, 10, 11};         // LONGVARBINARY(-4)
  byte[] Mediumblob_value = {6, 7, 8, 9, 10, 11};            // LONGVARBINARY(-4)
  byte[] Longblob_value = {6, 7, 8, 9, 10, 11};              // LONGVARBINARY(-4)
  byte[] Blob_value = {6, 7, 8, 9, 10, 11};                  // LONGVARBINARY(-4)
  byte[] Varbinary4_value = {0, 1, 2, 3};                    // VARBINARY(-3)
  byte[] Tinyblob_value = {0, 1, 2, 3, 4, 5};                // VARBINARY(-3)
  byte[] Binary4_value = {0, 1, 2, 3};                       // BINARY(-2)
  String LongVarchar_value = "hello";                        // LONGVARCHAR(-1)
  String Mediumtext_value = "hello again";                   // LONGVARCHAR(-1)
  String Longtext_value = "Do you like my hat?";             // LONGVARCHAR(-1)
  String Text_value = "Yes, I like that hat.";               // LONGVARCHAR(-1)
  String Char8_value = "12345";                              // CHAR(1)
  String Enum_value = "A";                                   // CHAR(1)
  String Set_value = "D";                                    // CHAR(1)
  BigDecimal Decimal_value = BigDecimal.TEN;                 // DECIMAL(3)
  BigDecimal Numeric_value = BigDecimal.ONE;                 // DECIMAL(3)
  Integer Integer_value = Integer.MAX_VALUE;                 // INTEGER(4)
  Integer Int_value = Integer.MAX_VALUE;                     // INTEGER(4)
  Integer Mediumint_value = (int)Short.MAX_VALUE;            // INTEGER(4)
  Integer IntegerUnsigned_value = Integer.MAX_VALUE;         // INTEGER(4)
  Integer IntUnsigned_value = Integer.MAX_VALUE;             // INTEGER(4)
  Integer MediumintUnsigned_value = Short.MAX_VALUE + 1;     // INTEGER(4)
  Integer Smallint_value = (int)Short.MAX_VALUE;             // SMALLINT(5)
  Integer SmallintUnsigned_value = (int)Short.MAX_VALUE;     // SMALLINT(5)
  Float Float_value = Float.MAX_VALUE/2;                     // REAL(7)
  Double Double_value = Double.MAX_VALUE;                    // DOUBLE(8)
  Double DoublePrecision_value = Double.MAX_VALUE;           // DOUBLE(8)
  Double Real_value = Double.MAX_VALUE;                      // DOUBLE(8)
  Double DoubleUnsigned_value = Double.MAX_VALUE;            // DOUBLE(8)
  Double DoublePrecisionUnsigned_value = Double.MAX_VALUE;   // DOUBLE(8)
  String Varchar64_value = "VARCHAR_value";                  // VARCHAR(12)
  String Tinytext_value = "TINYTEXT_value";                  // VARCHAR(12)
  Boolean Bool_value = true;                                 // BOOLEAN(16)
  LocalDate Date_value = LocalDate.now();                    // DATE(91)
  Year Year_value = Year.now();                              // DATE(91)
  LocalTime Time_value = LocalTime.now().truncatedTo( ChronoUnit.SECONDS ); // TIME(92)
  Instant Datetime_value = Instant.now().truncatedTo( ChronoUnit.SECONDS ); // TIMESTAMP(93)
  Instant Timestamp_value = Datetime_value;                  // TIMESTAMP(93)

  @Test
  public void testAllTypesSetValues() throws SQLException
  {
    AllTypes a = AllTypes.create();

    a.setColBigint(Bigint_value);
    a.setColBigintUnsigned(BigintUnsigned_value);
    a.setColBinary4(Binary4_value);
    a.setColBit(Bit_value);
    a.setColBit8(Bit8_value);
    a.setColBlob(Blob_value);
    a.setColBool(Bool_value);
    a.setColChar8(Char8_value);
    a.setColDate(Date_value);
    a.setColDatetime(Datetime_value);
    a.setColDecimal(Decimal_value);
    a.setColDouble(Double_value);
    a.setColDoublePrecision(DoublePrecision_value);
    a.setColDoublePrecisionUnsigned(DoublePrecisionUnsigned_value);
    a.setColDoubleUnsigned(DoubleUnsigned_value);
    a.setColEnum(Enum_value);
    a.setColFloat(Float_value);
    a.setColInt(Int_value);
    a.setColIntUnsigned(IntUnsigned_value);
    a.setColInteger(Integer_value);
    a.setColIntegerUnsigned(IntegerUnsigned_value);
    a.setColLongVarbinary(LongVarbinary_value);
    a.setColLongVarchar(LongVarchar_value);
    a.setColLongblob(Longblob_value);
    a.setColLongtext(Longtext_value);
    a.setColMediumblob(Mediumblob_value);
    a.setColMediumint(Mediumint_value);
    a.setColMediumintUnsigned(MediumintUnsigned_value);
    a.setColMediumtext(Mediumtext_value);
    a.setColNumeric(Numeric_value);
    a.setColReal(Real_value);
    a.setColSet(Set_value);
    a.setColSmallint(Smallint_value);
    a.setColSmallintUnsigned(SmallintUnsigned_value);
    a.setColText(Text_value);
    a.setColTime(Time_value);
    a.setColTimestamp(Timestamp_value);
    a.setColTinyblob(Tinyblob_value);
    a.setColTinyint(Tinyint_value);
    a.setColTinyintUnsigned(TinyintUnsigned_value);
    a.setColTinytext(Tinytext_value);
    a.setColVarbinary4(Varbinary4_value);
    a.setColVarchar64(Varchar64_value);
    a.setColYear(Year_value);

    MysqlSakila.commit();

    assertEquals(Bigint_value, a.getColBigint());
    assertEquals(BigintUnsigned_value, a.getColBigintUnsigned());
    assertEquals(Binary4_value, a.getColBinary4());
    assertEquals(Bit_value, a.getColBit());
    assertEquals(Bit8_value, a.getColBit8());
    assertEquals(Blob_value, a.getColBlob());
    assertEquals(Bool_value, a.getColBool());
    assertEquals(Char8_value, a.getColChar8());
    assertEquals(Date_value, a.getColDate());
    assertEquals(Datetime_value, a.getColDatetime());
    assertEquals(Decimal_value, a.getColDecimal());
    assertEquals(Double_value, a.getColDouble());
    assertEquals(DoublePrecision_value, a.getColDoublePrecision());
    assertEquals(DoublePrecisionUnsigned_value, a.getColDoublePrecisionUnsigned());
    assertEquals(DoubleUnsigned_value, a.getColDoubleUnsigned());
    assertEquals(Enum_value, a.getColEnum());
    assertEquals(Float_value, a.getColFloat());
    assertEquals(Int_value, a.getColInt());
    assertEquals(IntUnsigned_value, a.getColIntUnsigned());
    assertEquals(Integer_value, a.getColInteger());
    assertEquals(IntegerUnsigned_value, a.getColIntegerUnsigned());
    assertEquals(LongVarbinary_value, a.getColLongVarbinary());
    assertEquals(LongVarchar_value, a.getColLongVarchar());
    assertEquals(Longblob_value, a.getColLongblob());
    assertEquals(Longtext_value, a.getColLongtext());
    assertEquals(Mediumblob_value, a.getColMediumblob());
    assertEquals(Mediumint_value, a.getColMediumint());
    assertEquals(MediumintUnsigned_value, a.getColMediumintUnsigned());
    assertEquals(Mediumtext_value, a.getColMediumtext());
    assertEquals(Numeric_value, a.getColNumeric());
    assertEquals(Real_value, a.getColReal());
    assertEquals(Set_value, a.getColSet());
    assertEquals(Smallint_value, a.getColSmallint());
    assertEquals(SmallintUnsigned_value, a.getColSmallintUnsigned());
    assertEquals(Text_value, a.getColText());
    assertEquals(Time_value, a.getColTime());
    assertEquals(Timestamp_value, a.getColTimestamp());
    assertEquals(Tinyblob_value, a.getColTinyblob());
    assertEquals(Tinyint_value, a.getColTinyint());
    assertEquals(TinyintUnsigned_value, a.getColTinyintUnsigned());
    assertEquals(Tinytext_value, a.getColTinytext());
    assertEquals(Varbinary4_value, a.getColVarbinary4());
    assertEquals(Varchar64_value, a.getColVarchar64());
    assertEquals(Year_value, a.getColYear());

    a = "[.sql:MysqlSakila/] SELECT * FROM all_types".fetchOne();
    {
      assertEquals( Bigint_value, a.getColBigint() );
      assertEquals( BigintUnsigned_value, a.getColBigintUnsigned() );
      assertArrayEquals( Binary4_value, a.getColBinary4() );
      assertEquals( Bit_value, a.getColBit() );
      assertEquals( Bit8_value, a.getColBit8() );
      assertArrayEquals( Blob_value, a.getColBlob() );
      assertEquals( Bool_value, a.getColBool() );
      assertEquals( Char8_value, a.getColChar8() );
      assertEquals( Date_value, a.getColDate() );
      assertEquals( Datetime_value, a.getColDatetime() );
      assertEquals( 0, Decimal_value.compareTo( a.getColDecimal() ) );
      assertEquals( Double_value, a.getColDouble() );
      assertEquals( DoublePrecision_value, a.getColDoublePrecision() );
      assertEquals( DoublePrecisionUnsigned_value, a.getColDoublePrecisionUnsigned() );
      assertEquals( DoubleUnsigned_value, a.getColDoubleUnsigned() );
      assertEquals( Enum_value, a.getColEnum() );
//      assertEquals( Float_value, a.getColFloat() );
      assertEquals( Int_value, a.getColInt() );
      assertEquals( IntUnsigned_value, a.getColIntUnsigned() );
      assertEquals( Integer_value, a.getColInteger() );
      assertEquals( IntegerUnsigned_value, a.getColIntegerUnsigned() );
      assertArrayEquals( LongVarbinary_value, a.getColLongVarbinary() );
      assertEquals( LongVarchar_value, a.getColLongVarchar() );
      assertArrayEquals( Longblob_value, a.getColLongblob() );
      assertEquals( Longtext_value, a.getColLongtext() );
      assertArrayEquals( Mediumblob_value, a.getColMediumblob() );
      assertEquals( Mediumint_value, a.getColMediumint() );
      assertEquals( MediumintUnsigned_value, a.getColMediumintUnsigned() );
      assertEquals( Mediumtext_value, a.getColMediumtext() );
      assertEquals( 0, Numeric_value.compareTo( a.getColNumeric() ) );
      assertEquals( Real_value, a.getColReal() );
      assertEquals( Set_value, a.getColSet() );
      assertEquals( Smallint_value, a.getColSmallint() );
      assertEquals( SmallintUnsigned_value, a.getColSmallintUnsigned() );
      assertEquals( Text_value, a.getColText() );
      assertEquals( Time_value, a.getColTime() );
      assertEquals( Timestamp_value, a.getColTimestamp() );
      assertArrayEquals( Tinyblob_value, a.getColTinyblob() );
      assertEquals( Tinyint_value, a.getColTinyint() );
      assertEquals( TinyintUnsigned_value, a.getColTinyintUnsigned() );
      assertEquals( Tinytext_value, a.getColTinytext() );
      assertArrayEquals( Varbinary4_value, a.getColVarbinary4() );
      assertEquals( Varchar64_value, a.getColVarchar64() );
      assertEquals( Year_value, a.getColYear() );
    }
  }
}