/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.json;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import manifold.api.util.Pair;
import manifold.api.util.csv.CsvDataSet;
import manifold.api.util.csv.CsvField;
import manifold.api.util.csv.CsvHeader;
import manifold.api.util.csv.CsvParser;
import manifold.api.util.csv.CsvRecord;
import manifold.api.util.csv.CsvToken;
import manifold.ext.DataBindings;

public class Csv
{
  public static Object fromCsv( String csv )
  {
    return fromCsv( csv, false );
  }

  public static Object fromCsv( String csv, boolean withTokens )
  {
    try( InputStream inputStream = new BufferedInputStream( new ByteArrayInputStream( csv.getBytes() ) ) )
    {
      CsvDataSet dataSet = CsvParser.parse( inputStream );
      return withTokens ? transformType( dataSet ) : transformData( dataSet );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private static List<?> transformData( CsvDataSet dataSet )
  {
    CsvHeader header = dataSet.getHeader();

    List<?> names = new ArrayList<>();
    if( header != null )
    {
      names = header.getFields().stream().map( f -> f.getToken() ).collect( Collectors.toList() );
    }
    else
    {
      List<CsvRecord> records = dataSet.getRecords();
      if( !records.isEmpty() )
      {
        List<String> labels = new ArrayList<>();
        for( int i = 0; i < records.get( 0 ).getSize(); i++ )
        {
          labels.add( "Field" + (i + 1) );
        }
        names = labels;
      }
    }

    List<DataBindings> list = new ArrayList<>();
    for( CsvRecord record: dataSet.getRecords() )
    {
      DataBindings bindings = new DataBindings();
      List<CsvField> fields = record.getFields();
      for( int fieldNum = 0; fieldNum < fields.size(); fieldNum++ )
      {
        CsvField field = fields.get( fieldNum );
        Object name = names.get( fieldNum );
        bindings.put( name instanceof CsvToken ? ((CsvToken)name).getData() : name.toString(),
          field.getToken().getData() );
      }
      list.add( bindings );
    }
    return list;
  }

  private static DataBindings transformType( CsvDataSet dataSet )
  {
    CsvHeader header = dataSet.getHeader();

    List<?> names = new ArrayList<>();
    if( header != null )
    {
      names = header.getFields().stream().map( f -> f.getToken() ).collect( Collectors.toList() );
    }
    else
    {
      List<CsvRecord> records = dataSet.getRecords();
      if( !records.isEmpty() )
      {
        List<String> labels = new ArrayList<>();
        for( int i = 0; i < records.get( 0 ).getSize(); i++ )
        {
          labels.add( "Field" + (i + 1) );
        }
        names = labels;
      }
    }
    DataBindings typeBindings = new DataBindings();
    typeBindings.put( "\$schema", "http://json-schema.org/draft-04/schema#" );
    typeBindings.put( "synthetic", true ); // indicates this schema is not directly in the data file
    typeBindings.put( "type", "array" );
    DataBindings items = new DataBindings();
    typeBindings.put( "items", items );
    items.put( "type", "object" );
    DataBindings properties = new DataBindings();
    items.put( "properties", properties );
    List<Class> types = dataSet.getTypes();
    for( int i = 0; i < types.size(); i++ )
    {
      DataBindings property = new DataBindings();
      Object nameObj = names.get( i );
      if( nameObj instanceof CsvToken )
      {
        properties.put( ((CsvToken)nameObj).getData(), makeTokensValue( (CsvToken)nameObj, property ) );
      }
      else
      {
        properties.put( nameObj.toString(), property );
      }
      Class type = types.get( i );
      String t;
      String format = null;
      if( type == Boolean.class )
      {
        t = "boolean";
      }
      else if( type == Integer.class )
      {
        t = "integer";
      }
      else if( type == Double.class )
      {
        t = "number";
      }
      else
      {
        t = "string";
        if( type == Long.class )
        {
          format = "int64";
        }
        else if( type == BigInteger.class )
        {
          format = "big-integer";
        }
        else if( type == BigDecimal.class )
        {
          format = "big-decimal";
        }
        else if( type == LocalDateTime.class )
        {
          format = "date-time";
        }
        else if( type == LocalDate.class )
        {
          format = "date";
        }
        else if( type == LocalTime.class )
        {
          format = "time";
        }
      }

      property.put( "type", t );
      property.put( "nullable", true );
      if( format != null )
      {
        property.put( "format", format );
      }
    }
    return typeBindings;
  }


  private static Object makeTokensValue( CsvToken name, DataBindings property )
  {
    return new Pair<>( new Token[]{makeToken( name ), null}, property );
  }

  private static Token makeToken( CsvToken token )
  {
    return new Token( TokenType.STRING, token.getData(), token.getOffset(), token.getLine(), -1 );
  }
}
