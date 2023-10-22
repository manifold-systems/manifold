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

package manifold.sql.schema.oracle.types;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;

import manifold.sql.schema.simple.oracle.OracleSakila;
import manifold.sql.schema.simple.oracle.OracleSakila.*;
import manifold.sql.schema.oracle.OracleDdlServerTest;
import microsoft.sql.DateTimeOffset;
import oracle.sql.INTERVALDS;
import oracle.sql.INTERVALYM;
import oracle.sql.TIMESTAMPLTZ;
import oracle.sql.TIMESTAMPTZ;
import org.junit.Test;

import static org.junit.Assert.*;

public class AllTypesTest extends OracleDdlServerTest
{
  private static final byte[] Blob_value = {1,2,3,4,5,6,7,8};
  private static final String Char_value = "a";
  private static final String Clob_value = "clob";
  private static final Instant Date_value = Instant.now().truncatedTo( ChronoUnit.MINUTES );
  private static final Double Float_value = 3.14;
  private static final Duration Intervalds_value = Duration.ofHours( 10 ).plusMinutes( 5 );
  private static final Period Intervalym_value = Period.ofYears( 2 ).plusMonths( 5 );
  private static final String Long_value = "longstring";
  private static final String Nchar_value = "A";
  private static final String Nclob_value = "nclob";
  private static final Boolean Number1_value = true;
  private static final Integer Number10_value = 1234567890;
  private static final Integer Number3_value = 123;
  private static final BigDecimal Number38_value = BigDecimal.valueOf( Long.MAX_VALUE );
  private static final BigDecimal Number_value = new BigDecimal( "12345678901234567890.01234567890123456789" );
  private static final Integer Number5_value = 12345;
  private static final String Nvarchar2_value = "hellothere";
  private static final byte[] Raw_value = {1,2,3,4,5};
  private static final Double Real_value = 3.14;
  private static final Instant Timestamp_value = Instant.now().truncatedTo( ChronoUnit.MINUTES );
  private static final LocalDateTime TimestampWithLocalTimeZone_value = LocalDateTime.now();
  private static final OffsetDateTime TimestampWithTimeZone_value = OffsetDateTime.now();
  private static final String Varchar2_value = "therehello";

    @Test
  public void testNothing() {
    AllTypes at = null;
    System.out.println("hi");
  }

  @Test
  public void testAllTypesSetValues() throws SQLException
  {
    AllTypes a = AllTypes.create();

    a.setColBlob( Blob_value );
    a.setColChar( Char_value );
    a.setColClob( Clob_value );
    a.setColDate( Date_value );
    a.setColFloat( Float_value );
    a.setColIntervalds( Intervalds_value );
    a.setColIntervalym( Intervalym_value );
    a.setColLong( Long_value );
    a.setColNchar( Nchar_value );
    a.setColNclob( Nclob_value );
    a.setColNumber1( Number1_value );
    a.setColNumber10( Number10_value );
    a.setColNumber3( Number3_value );
    a.setColNumber38( Number38_value );
    a.setColNumber( Number_value );
    a.setColNumber5( Number5_value );
    a.setColNvarchar2( Nvarchar2_value );
    a.setColRaw( Raw_value );
    a.setColReal( Real_value );
    a.setColTimestamp( Timestamp_value );
    a.setColTimestampWithLocalTimeZone( TimestampWithLocalTimeZone_value );
    a.setColTimestampWithTimeZone( TimestampWithTimeZone_value );
    a.setColVarchar2( Varchar2_value );

    OracleSakila.commit();

    assertEquals( Blob_value, a.getColBlob() );
    assertEquals( Char_value, a.getColChar() );
    assertEquals( Clob_value, a.getColClob() );
    assertEquals( Date_value, a.getColDate() );
    assertEquals( Float_value, a.getColFloat() );
    assertEquals( Intervalds_value, a.getColIntervalds() );
    assertEquals( Intervalym_value, a.getColIntervalym() );
    assertEquals( Long_value, a.getColLong() );
    assertEquals( Nchar_value, a.getColNchar() );
    assertEquals( Nclob_value, a.getColNclob() );
    assertEquals( Number1_value, a.getColNumber1() );
    assertEquals( Number10_value, a.getColNumber10() );
    assertEquals( Number3_value, a.getColNumber3() );
    assertEquals( Number38_value, a.getColNumber38() );
    assertEquals( Number_value, a.getColNumber() );
    assertEquals( Number5_value, a.getColNumber5() );
    assertEquals( Nvarchar2_value, a.getColNvarchar2() );
    assertEquals( Raw_value, a.getColRaw() );
    assertEquals( Real_value, a.getColReal() );
    assertEquals( Timestamp_value, a.getColTimestamp() );
    assertEquals( TimestampWithLocalTimeZone_value, a.getColTimestampWithLocalTimeZone() );
    assertEquals( TimestampWithTimeZone_value, a.getColTimestampWithTimeZone() );
    assertEquals( Varchar2_value, a.getColVarchar2() );

    a = "[.sql:OracleSakila/] SELECT * FROM all_types".fetchOne();
    {
      assertArrayEquals( Blob_value, a.getColBlob() );
      assertEquals( Char_value, a.getColChar() );
      assertEquals( Clob_value, a.getColClob() );
      assertEquals( Date_value, a.getColDate() );
      assertEquals( Float_value, a.getColFloat() );
      assertEquals( Intervalds_value, a.getColIntervalds() );
      assertEquals( Intervalym_value, a.getColIntervalym() );
      assertEquals( Long_value, a.getColLong() );
      assertEquals( Nchar_value, a.getColNchar() );
      assertEquals( Nclob_value, a.getColNclob() );
      assertEquals( Number1_value, a.getColNumber1() );
      assertEquals( Number10_value, a.getColNumber10() );
      assertEquals( Number3_value, a.getColNumber3() );
      assertEquals( Number38_value, a.getColNumber38() );
      assertEquals( Number_value, a.getColNumber() );
      assertEquals( Number5_value, a.getColNumber5() );
      assertEquals( Nvarchar2_value, a.getColNvarchar2() );
      assertArrayEquals( Raw_value, a.getColRaw() );
      assertEquals( Real_value, a.getColReal() );
      assertEquals( Timestamp_value, a.getColTimestamp() );
      assertEquals( TimestampWithLocalTimeZone_value, a.getColTimestampWithLocalTimeZone() );
      assertEquals( TimestampWithTimeZone_value, a.getColTimestampWithTimeZone() );
      assertEquals( Varchar2_value, a.getColVarchar2() );
    }
  }
  
//  @Test
//  public void printAll() throws SQLException
//  {
//    Connection c = DriverManager.getConnection( "jdbc:oracle://localhost;database=sakila;integratedSecurity=true;encrypt=true;trustServerCertificate=true" );
//    try( PreparedStatement ps = c.prepareStatement( "select * from all_types" ) )
//    {
//      StringBuilder sb = new StringBuilder();
//      ResultSetMetaData md = ps.getMetaData();
//      ValueAccessorProvider vp = Dependencies.instance().getValueAccessorProvider();
//      for( int i = 1; i <= md.getColumnCount(); i++ )
//      {
//        int finalI = i;
//        BaseElement be = new BaseElement()
//        {
//          @Override
//          public String getName()
//          {
//            try
//            {
//              return md.getColumnName( finalI );
//            }
//            catch( SQLException e )
//            {
//              throw new RuntimeException( e );
//            }
//          }
//
//          @Override
//          public int getPosition()
//          {
//            return 0;
//          }
//
//          @Override
//          public int getSize()
//          {
//            return 0;
//          }
//
//          @Override
//          public int getScale()
//          {
//            return 0;
//          }
//
//          @Override
//          public boolean isNullable()
//          {
//            return false;
//          }
//
//          @Override
//          public int getJdbcType()
//          {
//            try
//            {
//              return md.getColumnType( finalI );
//            }
//            catch( SQLException e )
//            {
//              throw new RuntimeException( e );
//            }
//          }
//
//          @Override
//          public String getSqlType()
//          {
//            try
//            {
//              return md.getColumnTypeName( finalI );
//            }
//            catch( SQLException e )
//            {
//              throw new RuntimeException( e );
//            }
//          }
//
//          @Override
//          public String getColumnClassName()
//          {
//            try
//            {
//              return md.getColumnClassName( finalI );
//            }
//            catch( SQLException e )
//            {
//              throw new RuntimeException( e );
//            }
//          }
//        };
//        String columnName = md.getColumnName( i );
//        sb.append( be.getName() ).append( " : " )
//          .append( be.getSqlType() ).append( " : " )
//          .append( be.getJdbcType() ).append( " : " )
//          .append( md.getColumnClassName( i ) ).append( " : " )
//          .append( vp.get( be.getJdbcType() ).getJavaType( be ).getTypeName() ).append( "\n" );
//      }
//      System.out.println( sb );
//    }
//  }
}