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

package manifold.api.csv;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import manifold.api.json.DataBindings;
import manifold.api.json.parser.Token;
import manifold.api.json.parser.TokenType;
import manifold.api.util.Pair;
import manifold.api.csv.parser.CsvDataSet;
import manifold.api.csv.parser.CsvField;
import manifold.api.csv.parser.CsvHeader;
import manifold.api.csv.parser.CsvParser;
import manifold.api.csv.parser.CsvRecord;
import manifold.api.csv.parser.CsvToken;
import manifold.strings.api.DisableStringLiteralTemplates;

@DisableStringLiteralTemplates
public class Csv
{
  /**
   * Write the contents of the {@code jsonValue} to CSV formatted string following
   * <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>. Note data in all fields is enclosed in double quotes.
   * <p/>
   * Additionally, since CSV is a flat file format, nesting of data is not directly supported. That is, <i>field</i>
   * values having type {@code Bindings} or {@code List}, although legal Bindings value types, have no representation
   * in CSV. Currently, such values are simply converted to strings via {@code toString()}, however this may change in
   * a future revision.
   */
  public static String toCsv( Object jsonValue )
  {
    StringBuilder sb = new StringBuilder();
    if( jsonValue instanceof Map )
    {
      toCsv( jsonValue, null, sb, 0 );
    }
    else if( jsonValue instanceof Iterable )
    {
      toCsv( jsonValue, "list", sb, 0 );
    }
    else
    {
      toCsv( jsonValue, "item", sb, 0 );
    }
    return sb.toString();
  }

  /**
   * Write the contents of the {@code jsonValue} to CSV formatted string following
   * <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>. Note data in all fields is enclosed in double quotes.
   * <p/>
   * Additionally, since CSV is a flat file format, nesting of data is not directly supported. That is, <i>field</i>
   * values having type {@code Bindings} or {@code List}, although legal Bindings value types, have no representation
   * in CSV. Currently, such values are simply converted to strings via {@code toString()}, however this may change in
   * a future revision.
   */
  public static void toCsv( Object jsonValue, String name, StringBuilder target, int indent )
  {
    if( jsonValue instanceof Map )
    {
      if( name == null )
      {
        Map map = (Map)jsonValue;
        if( map.size() == 1 )
        {
          // single entry with no name implies root, defer to the root
          Object rootKey = map.keySet().iterator().next();
          Object rootValue = map.get( rootKey );
          if( rootValue instanceof Pair )
          {
            rootValue = ((Pair)rootValue).getSecond();
          }
          toCsv( rootValue, rootKey.toString(), target, indent );
          return;
        }
        else
        {
          //todo: factor out Csv.CSV_DEFAULT_ROOT
          name = "root_object";
        }
      }
      // a single row of data consisting of the name/value pairs in the map
      toCsv( Collections.singletonList( jsonValue ), name, target, indent );
    }
    else if( jsonValue instanceof Iterable )
    {
      // A list of data

      toCsv( (Iterable)jsonValue, name, target, indent );
    }
    else
    {
      // a single row of data consisting of just one column of the name/value pair

      toCsv( Collections.singletonList( jsonValue ), name, target, indent );
    }
  }

  private static void toCsv( Iterable value, String name, StringBuilder target, int indent )
  {
    Iterator iterator = value.iterator();
    if( iterator.hasNext() )
    {
      // Csv header

      Object comp = iterator.next();
      if( comp instanceof Pair )
      {
        comp = ((Pair)comp).getSecond();
      }

      if( comp instanceof Map )
      {
        // row of data

        int i = 0;
        for( Object key: ((Map)comp).keySet() )
        {
          if( i > 0 )
          {
            target.append( ',' );
          }
          appendCsvValue( target, key );
          i++;
        }
        target.append( '\n' );
      }
      else if( comp instanceof Iterable )
      {
        // single column of data

        appendCsvValue( target, name ).append( '\n' );
      }
    }
    else
    {
      return;
    }

    for( Object comp: value )
    {
      // Csv records

      if( comp instanceof Pair )
      {
        comp = ((Pair)comp).getSecond();
      }

      if( comp instanceof Map )
      {
        int i = 0;
        for( Object v: ((Map)comp).values() )
        {
          if( i > 0 )
          {
            target.append( ',' );
          }
          appendCsvValue( target, v );
          i++;
        }
        target.append( '\n' );
      }
      else if( comp instanceof Iterable )
      {
        // Lists of lists not supported with CSV, just dumping text for each element to a single value
        target.append( '"' );
        ((Iterable<?>)comp).forEach( e -> target.append( "\"\"" ).append( value ).append( "\"\"," ) );
        target.append( "\"\n" );
      }
      else
      {
        // single column of data
        appendCsvValue( target, value ).append( '\n' );
      }
    }
  }

  private static StringBuilder appendCsvValue( StringBuilder target, Object value )
  {
    target.append( '"' ).append( String.valueOf( value ).replace( "\"", "\"\"" ) ).append( '"' );
    return target;
  }

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
    typeBindings.put( "$schema", "http://json-schema.org/draft-04/schema#" );
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
