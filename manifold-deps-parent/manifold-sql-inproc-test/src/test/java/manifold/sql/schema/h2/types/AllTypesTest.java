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

package manifold.sql.schema.h2.types;

import manifold.sql.schema.h2.base.H2DdlServerTest;
import manifold.sql.schema.simple.h2.H2Sakila;
import manifold.sql.schema.simple.h2.H2Sakila.*;
import org.h2.api.Interval;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.util.GeometricShapeFactory;

import java.awt.Rectangle;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.*;
import java.util.UUID;

import static org.junit.Assert.*;

public class AllTypesTest extends H2DdlServerTest
{
    private static byte Tinyint_value = (byte)8;
    private static long Bigint_value = Long.MAX_VALUE;
    private static byte[] Varbinary_value = new byte[]{4,5,6};
    private static byte[] Binary_value = new byte[]{1,2,3,4,5};
    private static UUID Uuid_value = UUID.randomUUID();
    private static String Character_value = "chars";
    private static BigDecimal Numeric_value = BigDecimal.TEN;
    private static double Float_value = 3.14;
    private static int Integer_value = 82;
    private static int Smallint_value = 32;
    private static float Real_value = 4.2f;
    private static double Double_value = 5.2;
    private static String Varchar_value = "varchar";
    private static String VarcharIgnorecase_value = "varcharIgnorecase";
    private static boolean Boolean_value = true;
    private static LocalDate Date_value = LocalDate.of( 2023, 9, 3 );
    private static LocalTime Time_value = LocalTime.of( 12, 56 );
    private static Instant Timestamp_value = LocalDateTime.of( 1987, 6, 17, 10, 0 ).toInstant( ZoneOffset.UTC );
    private static Interval IntervalYear_value = Interval.ofYears( 5 );
    private static Interval IntervalMonth_value = Interval.ofMonths( 5 );
    private static Interval IntervalDay_value = Interval.ofDays( 5 );
    private static Interval IntervalHour_value = Interval.ofHours( 5 );
    private static Interval IntervalMinute_value = Interval.ofMinutes( 5 );
    private static Interval IntervalSecond_value = Interval.ofSeconds( 5 );
    private static Interval IntervalYearToMonth_value = Interval.ofYearsMonths( 5, 6 );
    private static Interval IntervalDayToHour_value = Interval.ofDaysHours( 5, 6 );
    private static Interval IntervalDayToMinute_value = Interval.ofDaysHoursMinutes( 5, 6, 7 );
    private static Interval IntervalDayToSecond_value = Interval.ofDaysHoursMinutesSeconds( 5, 6, 7, 8 );
    private static Interval IntervalHourToMinute_value = Interval.ofHoursMinutes( 5, 6 );
    private static Interval IntervalHourToSecond_value = Interval.ofHoursMinutesSeconds( 5, 6, 7 );
    private static Interval IntervalMinuteToSecond_value = Interval.ofMinutesSeconds( 5, 6 );
    private static String Enum_value = "hi";
    private static Geometry Geometry_value = new Point( new CoordinateArraySequence( new Coordinate[] {new Coordinate(1, 2, 3)}), new GeometricShapeFactory().createRectangle().getFactory() );
    private static byte[] Json_value = "{\"json\":true}".getBytes( StandardCharsets.UTF_8 );
    private static Rectangle JavaObject_value = new Rectangle( 1, 2, 3, 4 );
    private static String[] VarcharArray_value = new String[] {"hi", "bye"};
    private static byte[] Blob_value = "blob".getBytes();
    private static String Clob_value = "clob";
    private static OffsetTime TimeWithTimeZone_value = OffsetTime.now().withNano( 0 );
    private static OffsetDateTime TimestampWithTimeZone_value = OffsetDateTime.now().withNano( 0 );

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
    a.setColVarcharIgnorecase( VarcharIgnorecase_value );
    a.setColBoolean( Boolean_value );
    a.setColDate( Date_value );
    a.setColTime( Time_value );
    a.setColTimestamp( Timestamp_value );
    a.setColIntervalYear( IntervalYear_value );
    a.setColIntervalMonth( IntervalMonth_value );
    a.setColIntervalDay( IntervalDay_value );
    a.setColIntervalHour( IntervalHour_value );
    a.setColIntervalMinute( IntervalMinute_value );
    a.setColIntervalSecond( IntervalSecond_value );
    a.setColIntervalYearToMonth( IntervalYearToMonth_value );
    a.setColIntervalDayToHour( IntervalDayToHour_value );
    a.setColIntervalDayToMinute( IntervalDayToMinute_value );
    a.setColIntervalDayToSecond( IntervalDayToSecond_value );
    a.setColIntervalHourToMinute( IntervalHourToMinute_value );
    a.setColIntervalHourToSecond( IntervalHourToSecond_value );
    a.setColIntervalMinuteToSecond( IntervalMinuteToSecond_value );
    a.setColEnum( Enum_value );
    a.setColGeometry( Geometry_value );
    a.setColJson( Json_value );
    a.setColJavaObject( JavaObject_value );
    a.setColVarcharArray( VarcharArray_value );
    a.setColBlob( Blob_value );
    a.setColClob( Clob_value );
    a.setColTimeWithTimeZone( TimeWithTimeZone_value );
    a.setColTimestampWithTimeZone( TimestampWithTimeZone_value );

    H2Sakila.commit();

    assertEquals( Tinyint_value,                (byte)a.getColTinyint() );
    assertEquals( Bigint_value,                 (long)a.getColBigint() );
    assertArrayEquals( Varbinary_value,         a.getColVarbinary() );
    assertArrayEquals( Binary_value,            a.getColBinary() );
    assertEquals( Uuid_value,                   a.getColUuid() );
    assertEquals( Character_value,              a.getColCharacter() );
    assertEquals( Numeric_value,                a.getColNumeric() );
    assertEquals( Float_value,                  a.getColFloat(), 0 );
    assertEquals( Integer_value,                (int)a.getColInteger() );
    assertEquals( Smallint_value,               (int)a.getColSmallint() );
    assertEquals( Real_value,                   (float)a.getColReal(), 0 );
    assertEquals( Double_value,                 a.getColDouble(), 0 );
    assertEquals( Varchar_value,                a.getColVarchar() );
    assertEquals( VarcharIgnorecase_value,      a.getColVarcharIgnorecase() );
    assertEquals( Boolean_value,                a.getColBoolean() );
    assertEquals( Date_value,                   a.getColDate() );
    assertEquals( Time_value,                   a.getColTime() );
    assertEquals( Timestamp_value,              a.getColTimestamp() );
    assertEquals( IntervalYear_value,           a.getColIntervalYear() );
    assertEquals( IntervalMonth_value,          a.getColIntervalMonth() );
    assertEquals( IntervalDay_value,            a.getColIntervalDay() );
    assertEquals( IntervalHour_value,           a.getColIntervalHour() );
    assertEquals( IntervalMinute_value,         a.getColIntervalMinute() );
    assertEquals( IntervalSecond_value,         a.getColIntervalSecond() );
    assertEquals( IntervalYearToMonth_value,    a.getColIntervalYearToMonth() );
    assertEquals( IntervalDayToHour_value,      a.getColIntervalDayToHour() );
    assertEquals( IntervalDayToMinute_value,    a.getColIntervalDayToMinute() );
    assertEquals( IntervalDayToSecond_value,    a.getColIntervalDayToSecond() );
    assertEquals( IntervalHourToMinute_value,   a.getColIntervalHourToMinute() );
    assertEquals( IntervalHourToSecond_value,   a.getColIntervalHourToSecond() );
    assertEquals( IntervalMinuteToSecond_value, a.getColIntervalMinuteToSecond() );
    assertEquals( Enum_value,                   a.getColEnum() );
    assertEquals( Geometry_value,               a.getColGeometry() );
    assertArrayEquals( Json_value,              a.getColJson() );
    assertEquals( JavaObject_value,             a.getColJavaObject() );
    assertArrayEquals( VarcharArray_value,      (Object[])a.getColVarcharArray() );
    assertArrayEquals( Blob_value,              a.getColBlob() );
    assertEquals( Clob_value,                   a.getColClob() );
    assertEquals( TimeWithTimeZone_value,       a.getColTimeWithTimeZone() );
    assertEquals( TimestampWithTimeZone_value,  a.getColTimestampWithTimeZone() );


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
    assertEquals( VarcharIgnorecase_value,      a.getColNotNullVarcharIgnorecase() );
    assertEquals( Boolean_value,                a.getColNotNullBoolean() );
    assertEquals( Date_value,                   a.getColNotNullDate() );
    assertEquals( Time_value,                   a.getColNotNullTime() );
    assertEquals( Timestamp_value,              a.getColNotNullTimestamp() );
    assertEquals( IntervalYear_value,           a.getColNotNullIntervalYear() );
    assertEquals( IntervalMonth_value,          a.getColNotNullIntervalMonth() );
    assertEquals( IntervalDay_value,            a.getColNotNullIntervalDay() );
    assertEquals( IntervalHour_value,           a.getColNotNullIntervalHour() );
    assertEquals( IntervalMinute_value,         a.getColNotNullIntervalMinute() );
    assertEquals( IntervalSecond_value,         a.getColNotNullIntervalSecond() );
    assertEquals( IntervalYearToMonth_value,    a.getColNotNullIntervalYearToMonth() );
    assertEquals( IntervalDayToHour_value,      a.getColNotNullIntervalDayToHour() );
    assertEquals( IntervalDayToMinute_value,    a.getColNotNullIntervalDayToMinute() );
    assertEquals( IntervalDayToSecond_value,    a.getColNotNullIntervalDayToSecond() );
    assertEquals( IntervalHourToMinute_value,   a.getColNotNullIntervalHourToMinute() );
    assertEquals( IntervalHourToSecond_value,   a.getColNotNullIntervalHourToSecond() );
    assertEquals( IntervalMinuteToSecond_value, a.getColNotNullIntervalMinuteToSecond() );
    assertEquals( Enum_value,                   a.getColNotNullEnum() );
    assertEquals( Geometry_value,               a.getColNotNullGeometry() );
    assertArrayEquals( Json_value,              a.getColNotNullJson() );
    assertEquals( JavaObject_value,             a.getColNotNullJavaObject() );
    assertArrayEquals( VarcharArray_value,      (Object[])a.getColNotNullVarcharArray() );
    assertArrayEquals( Blob_value,              a.getColNotNullBlob() );
    assertEquals( Clob_value,                   a.getColNotNullClob() );
    assertEquals( TimeWithTimeZone_value,       a.getColNotNullTimeWithTimeZone() );
    assertEquals( TimestampWithTimeZone_value,  a.getColNotNullTimestampWithTimeZone() );
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
    a.setColVarcharIgnorecase( null );
    a.setColBoolean( null );
    a.setColDate( null );
    a.setColTime( null );
    a.setColTimestamp( null );
    a.setColIntervalYear( null );
    a.setColIntervalMonth( null );
    a.setColIntervalDay( null );
    a.setColIntervalHour( null );
    a.setColIntervalMinute( null );
    a.setColIntervalSecond( null );
    a.setColIntervalYearToMonth( null );
    a.setColIntervalDayToHour( null );
    a.setColIntervalDayToMinute( null );
    a.setColIntervalDayToSecond( null );
    a.setColIntervalHourToMinute( null );
    a.setColIntervalHourToSecond( null );
    a.setColIntervalMinuteToSecond( null );
    a.setColEnum( null );
    a.setColGeometry( null );
    a.setColJson( null );
    a.setColJavaObject( null );
    a.setColVarcharArray( null );
    a.setColBlob( null );
    a.setColClob( null );
    a.setColTimeWithTimeZone( null );
    a.setColTimestampWithTimeZone( null );

    H2Sakila.commit();

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
    assertNull( a.getColVarcharIgnorecase() );
    assertNull( a.getColBoolean() );
    assertNull( a.getColDate() );
    assertNull( a.getColTime() );
    assertNull( a.getColTimestamp() );
    assertNull( a.getColIntervalYear() );
    assertNull( a.getColIntervalMonth() );
    assertNull( a.getColIntervalDay() );
    assertNull( a.getColIntervalHour() );
    assertNull( a.getColIntervalMinute() );
    assertNull( a.getColIntervalSecond() );
    assertNull( a.getColIntervalYearToMonth() );
    assertNull( a.getColIntervalDayToHour() );
    assertNull( a.getColIntervalDayToMinute() );
    assertNull( a.getColIntervalDayToSecond() );
    assertNull( a.getColIntervalHourToMinute() );
    assertNull( a.getColIntervalHourToSecond() );
    assertNull( a.getColIntervalMinuteToSecond() );
    assertNull( a.getColEnum() );
    assertNull( a.getColGeometry() );
    assertNull( a.getColJson() );
    assertNull( a.getColJavaObject() );
    assertNull( a.getColVarcharArray() );
    assertNull( a.getColBlob() );
    assertNull( a.getColClob() );
    assertNull( a.getColTimeWithTimeZone() );
    assertNull( a.getColTimestampWithTimeZone() );


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
    assertEquals( VarcharIgnorecase_value,      a.getColNotNullVarcharIgnorecase() );
    assertEquals( Boolean_value,                a.getColNotNullBoolean() );
    assertEquals( Date_value,                   a.getColNotNullDate() );
    assertEquals( Time_value,                   a.getColNotNullTime() );
    assertEquals( Timestamp_value,              a.getColNotNullTimestamp() );
    assertEquals( IntervalYear_value,           a.getColNotNullIntervalYear() );
    assertEquals( IntervalMonth_value,          a.getColNotNullIntervalMonth() );
    assertEquals( IntervalDay_value,            a.getColNotNullIntervalDay() );
    assertEquals( IntervalHour_value,           a.getColNotNullIntervalHour() );
    assertEquals( IntervalMinute_value,         a.getColNotNullIntervalMinute() );
    assertEquals( IntervalSecond_value,         a.getColNotNullIntervalSecond() );
    assertEquals( IntervalYearToMonth_value,    a.getColNotNullIntervalYearToMonth() );
    assertEquals( IntervalDayToHour_value,      a.getColNotNullIntervalDayToHour() );
    assertEquals( IntervalDayToMinute_value,    a.getColNotNullIntervalDayToMinute() );
    assertEquals( IntervalDayToSecond_value,    a.getColNotNullIntervalDayToSecond() );
    assertEquals( IntervalHourToMinute_value,   a.getColNotNullIntervalHourToMinute() );
    assertEquals( IntervalHourToSecond_value,   a.getColNotNullIntervalHourToSecond() );
    assertEquals( IntervalMinuteToSecond_value, a.getColNotNullIntervalMinuteToSecond() );
    assertEquals( Enum_value,                   a.getColNotNullEnum() );
    assertEquals( Geometry_value,               a.getColNotNullGeometry() );
    assertArrayEquals( Json_value,              a.getColNotNullJson() );
    assertEquals( JavaObject_value,             a.getColNotNullJavaObject() );
    assertArrayEquals( VarcharArray_value,      (Object[])a.getColNotNullVarcharArray() );
    assertArrayEquals( Blob_value,              a.getColNotNullBlob() );
    assertEquals( Clob_value,                   a.getColNotNullClob() );
    assertEquals( TimeWithTimeZone_value,       a.getColNotNullTimeWithTimeZone() );
    assertEquals( TimestampWithTimeZone_value,  a.getColNotNullTimestampWithTimeZone() );
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
      VarcharIgnorecase_value,
      Boolean_value,
      Date_value,
      Time_value,
      Timestamp_value,
      IntervalYear_value,
      IntervalMonth_value,
      IntervalDay_value,
      IntervalHour_value,
      IntervalMinute_value,
      IntervalSecond_value,
      IntervalYearToMonth_value,
      IntervalDayToHour_value,
      IntervalDayToMinute_value,
      IntervalDayToSecond_value,
      IntervalHourToMinute_value,
      IntervalHourToSecond_value,
      IntervalMinuteToSecond_value,
      Enum_value,
      Geometry_value,
      Json_value,
      JavaObject_value,
      VarcharArray_value,
      Blob_value,
      Clob_value,
      TimeWithTimeZone_value,
      TimestampWithTimeZone_value );
    return a;
  }
}
