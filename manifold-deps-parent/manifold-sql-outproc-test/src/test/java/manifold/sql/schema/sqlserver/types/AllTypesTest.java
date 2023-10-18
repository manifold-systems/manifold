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

package manifold.sql.schema.sqlserver.types;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import com.microsoft.sqlserver.jdbc.Geography;
import com.microsoft.sqlserver.jdbc.SQLServerException;
import manifold.sql.schema.simple.sqlserver.SqlserverSakila;
import manifold.sql.schema.simple.sqlserver.SqlserverSakila.*;
import manifold.sql.schema.sqlserver.SqlserverDdlServerTest;
import microsoft.sql.DateTimeOffset;
import org.junit.Test;

import static org.junit.Assert.*;

public class AllTypesTest extends SqlserverDdlServerTest
{
  private static final microsoft.sql.DateTimeOffset Datetimeoffset_value = DateTimeOffset.valueOf( Timestamp.from( Instant.now() ), 2 );
  private static final LocalTime Time_value = LocalTime.now().truncatedTo( ChronoUnit.MINUTES );
  private static final String Xml_value = "<name>Foo</name>";
  private static final Object SqlVariant_value = 2.1;
  private static final String Uniqueidentifier_value = "6F9619FF-8B86-D011-B42D-00C04FC964FF";
  private static final String Ntext_value = "ntext";
  private static final String Nvarchar_value = "nvarchar";
  private static final String Nchar5_value = "nchar";
  private static final boolean Bit_value = true;
  private static final byte Tinyint_value = Byte.MAX_VALUE;
  private static final long Bigint_value = Long.MAX_VALUE;
  private static final byte[] Image_value = {1,2,3,4,5};
  private static final byte[] Varbinary_value = {1,2,3,4,5,6};
  private static final byte[] Binary4_value = {1,2,3,4};
  private static final String Text_value = "text";
  private static final String Char5_value = "abcde";
  private static final BigDecimal Numeric_value = BigDecimal.TEN;
  private static final BigDecimal Decimal_value = BigDecimal.TEN;
  private static final BigDecimal Money_value = BigDecimal.TEN;
  private static final BigDecimal Smallmoney_value = BigDecimal.TEN;
  private static final int Int_value = Integer.MAX_VALUE;
  private static final short Smallint_value = Short.MAX_VALUE;
  private static final double Float_value = Double.MAX_VALUE;
  private static final float Real_value = Float.MAX_VALUE;
  private static final String Varchar_value = "varchar";
  private static final LocalDate Date_value = LocalDate.now();
  private static final Instant Datetime2_value = Instant.now().truncatedTo( ChronoUnit.MINUTES );
  private static final Instant Datetime_value = Instant.now().truncatedTo( ChronoUnit.MINUTES );
  private static final Instant Smalldatetime_value = Instant.now().truncatedTo( ChronoUnit.MINUTES );
  private static final byte[] Geography_value; // todo: should be com.microsoft.sqlserver.jdbc.Geography, not byte[]
  static
  {
    try
    {
      // todo: make com.microsoft.sqlserver.jdbc.Geography (and others) work by a sqlserver BinaryValueAccessor override
      Geography_value = Geography.parse( "LINESTRING(-122.360 47.656, -122.343 47.656 )" ).serialize();
    }
    catch( SQLServerException e )
    {
      throw new RuntimeException( e );
    }
  }

  @Test
  public void testAllTypesSetValues() throws SQLException
  {
    AllTypes a = AllTypes.create();

    a.setColDatetimeoffset( Datetimeoffset_value );
    a.setColTime( Time_value );
    a.setColXml( Xml_value );
    a.setColSqlVariant( SqlVariant_value );
    a.setColUniqueidentifier( Uniqueidentifier_value );
    a.setColNtext( Ntext_value );
    a.setColNvarchar( Nvarchar_value );
    a.setColNchar5( Nchar5_value );
    a.setColBit( Bit_value );
    a.setColTinyint( Tinyint_value );
    a.setColBigint( Bigint_value );
    a.setColImage( Image_value );
    a.setColVarbinary( Varbinary_value );
    a.setColBinary4( Binary4_value );
    a.setColText( Text_value );
    a.setColChar5( Char5_value );
    a.setColNumeric( Numeric_value );
    a.setColDecimal( Decimal_value );
    a.setColMoney( Money_value );
    a.setColSmallmoney( Smallmoney_value );
    a.setColInt( Int_value );
    a.setColSmallint( Smallint_value );
    a.setColFloat( Float_value );
    a.setColReal( Real_value );
    a.setColVarchar( Varchar_value );
    a.setColDate( Date_value );
    a.setColDatetime2( Datetime2_value );
    a.setColDatetime( Datetime_value );
    a.setColSmalldatetime( Smalldatetime_value );
    a.setColGeography( Geography_value );

    SqlserverSakila.commit();

    assertEquals( Datetimeoffset_value, a.getColDatetimeoffset() );
    assertEquals( Time_value, a.getColTime() );
    assertEquals( Xml_value, a.getColXml() );
    assertEquals( SqlVariant_value, a.getColSqlVariant() );
    assertEquals( Uniqueidentifier_value, a.getColUniqueidentifier() );
    assertEquals( Ntext_value, a.getColNtext() );
    assertEquals( Nvarchar_value, a.getColNvarchar() );
    assertEquals( Nchar5_value, a.getColNchar5() );
    assertEquals( Bit_value, a.getColBit() );
    assertEquals( Tinyint_value, (byte)a.getColTinyint() );
    assertEquals( Bigint_value, (long)a.getColBigint() );
    assertEquals( Image_value, a.getColImage() );
    assertEquals( Varbinary_value, a.getColVarbinary() );
    assertEquals( Binary4_value, a.getColBinary4() );
    assertEquals( Text_value, a.getColText() );
    assertEquals( Char5_value, a.getColChar5() );
    assertEquals( Numeric_value, a.getColNumeric() );
    assertEquals( Decimal_value, a.getColDecimal() );
    assertEquals( Money_value, a.getColMoney() );
    assertEquals( Smallmoney_value, a.getColSmallmoney() );
    assertEquals( Int_value, (int)a.getColInt() );
    assertEquals( Smallint_value, (short)a.getColSmallint() );
    assertEquals( 0, Float_value, (double)a.getColFloat() );
    assertEquals( 0, Real_value, (float)a.getColReal() );
    assertEquals( Varchar_value, a.getColVarchar() );
    assertEquals( Date_value, a.getColDate() );
    assertEquals( Datetime2_value, a.getColDatetime2() );
    assertEquals( Datetime_value, a.getColDatetime() );
    assertEquals( Smalldatetime_value, a.getColSmalldatetime() );
    assertEquals( Geography_value, a.getColGeography() );

    a = "[.sql:SqlserverSakila/] SELECT * FROM all_types".fetchOne();
    {
      assertEquals( Datetimeoffset_value, a.getColDatetimeoffset() );
      assertEquals( Time_value, a.getColTime() );
      assertEquals( Xml_value, a.getColXml() );
      assertEquals( SqlVariant_value, a.getColSqlVariant() );
      assertEquals( Uniqueidentifier_value, a.getColUniqueidentifier() );
      assertEquals( Ntext_value, a.getColNtext() );
      assertEquals( Nvarchar_value, a.getColNvarchar() );
      assertEquals( Nchar5_value, a.getColNchar5() );
      assertEquals( Bit_value, a.getColBit() );
      assertEquals( Tinyint_value, (byte)a.getColTinyint() );
      assertEquals( Bigint_value, (long)a.getColBigint() );
      assertArrayEquals( Image_value, a.getColImage() );
      assertArrayEquals( Varbinary_value, a.getColVarbinary() );
      assertArrayEquals( Binary4_value, a.getColBinary4() );
      assertEquals( Text_value, a.getColText() );
      assertEquals( Char5_value, a.getColChar5() );
      assertEquals( Numeric_value, a.getColNumeric() );
      assertEquals( Decimal_value, a.getColDecimal() );
      assertEquals( 0, Money_value.compareTo( a.getColMoney() ) );
      assertEquals( 0, Smallmoney_value.compareTo( a.getColSmallmoney() ) );
      assertEquals( Int_value, (int)a.getColInt() );
      assertEquals( Smallint_value, (short)a.getColSmallint() );
      assertEquals( 0, Float_value, (double)a.getColFloat() );
      assertEquals( 0, Real_value, (float)a.getColReal() );
      assertEquals( Varchar_value, a.getColVarchar() );
      assertEquals( Date_value, a.getColDate() );
      assertEquals( Datetime2_value, a.getColDatetime2() );
      assertEquals( Datetime_value, a.getColDatetime() );
      assertEquals( Smalldatetime_value, a.getColSmalldatetime() );
      assertArrayEquals( Geography_value, a.getColGeography() );
    }
  }
  
//  @Test
//  public void printAll() throws SQLException
//  {
//    Connection c = DriverManager.getConnection( "jdbc:sqlserver://localhost;database=sakila;integratedSecurity=true;encrypt=true;trustServerCertificate=true" );
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