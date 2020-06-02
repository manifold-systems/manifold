/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.json.rt;

import manifold.ext.rt.api.IBindingsBacked;
import manifold.ext.rt.api.IListBacked;
import manifold.json.rt.api.IJsonParser;
import manifold.rt.api.util.ManEscapeUtil;
import manifold.rt.api.util.Pair;
import manifold.util.concurrent.LocklessLazyVar;

import manifold.rt.api.ScriptException;
import java.util.Iterator;
import java.util.Map;

public class Json
{
  private static String _parser = System.getProperty( "manifold.json.parser" );

  public static String getParserName()
  {
    return _parser;
  }

  @SuppressWarnings("UnusedDeclaration")
  public static void setParserName( String fqn )
  {
    _parser = fqn;
    PARSER.clear();
  }

  private static final LocklessLazyVar<IJsonParser> PARSER =
    new LocklessLazyVar<IJsonParser>()
    {

      @Override
      protected IJsonParser init()
      {
        String fqn = getParserName();
        return fqn == null ? IJsonParser.getDefaultParser() : makeParser( fqn );
      }

      private IJsonParser makeParser( String fqn )
      {
        try
        {
          return (IJsonParser)Class.forName( fqn ).newInstance();
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }
    };

  /**
   * Serializes this Map instance to a JSON formatted String
   */
  public static String toJson( Map thisMap )
  {
    StringBuilder sb = new StringBuilder();
    toJson( thisMap, sb, 0 );
    return sb.toString();
  }

  /**
   * Serializes this Map instance into a JSON formatted StringBuilder with the specified indent of spaces
   */
  public static void toJson( Map thisMap, StringBuilder sb, int indent )
  {
    int iKey = 0;
    if( isNewLine( sb ) )
    {
      indent( sb, indent );
    }
    sb.append( "{\n" );
    if( thisMap.size() > 0 )
    {
      for( Object key : thisMap.keySet() )
      {
        indent( sb, indent + 2 );
        sb.append( '\"' ).append( key ).append( '\"' ).append( ": " );
        Object value = thisMap.get( key );

        value = toBindings( value );

        if( value instanceof Map )
        {
          toJson( (Map)value, sb, indent + 2 );
        }
        else if( value instanceof Iterable )
        {
          listToJson( sb, indent + 2, (Iterable)value );
        }
        else
        {
          appendValue( sb, value );
        }
        appendCommaNewLine( sb, iKey < thisMap.size() - 1 );
        iKey++;
      }
    }
    indent( sb, indent );
    sb.append( "}" );
  }

  public static Object toBindings( Object value )
  {
    if( value instanceof IBindingsBacked )
    {
      value = ((IBindingsBacked) value).getBindings();
    }
    else if( value instanceof IListBacked )
    {
      value = ((IListBacked) value).getList();
    }
    return value;
  }

  public static void indent( StringBuilder sb, int indent )
  {
    int i = 0;
    while( i < indent )
    {
      sb.append( ' ' );
      i++;
    }
  }

  public static StringBuilder appendValue( StringBuilder sb, Object comp )
  {
    if( comp instanceof String )
    {
      sb.append( '\"' );
      sb.append( ManEscapeUtil.escapeForJavaStringLiteral( (String)comp ) );
      sb.append( '\"' );
    }
    else if( comp instanceof Integer ||
      comp instanceof Long ||
      comp instanceof Double ||
      comp instanceof Float ||
      comp instanceof Short ||
      comp instanceof Character ||
      comp instanceof Byte ||
      comp instanceof Boolean )
    {
      sb.append( comp );
    }
    else if( comp == null )
    {
      sb.append( "null" );
    }
    else
    {
      throw new IllegalStateException( "Unsupported expando type: " + comp.getClass() );
    }
    return sb;
  }

  /**
   * Build a JSON string from the specified {@code value}. The {@code value} must be a valid JSON value:
   * <lu>
   *   <li>primitive, boxed primitive, or {@code String}</li>
   *   <li>{@code Iterable} of JSON values</li>
   *   <li>{@code Map} of JSON values</li>
   * </lu>
   * @return A JSON String reflecting the specified {@code value}
   */
  public static String toJson( Object value )
  {
    StringBuilder target = new StringBuilder();
    toJson( target, 0, value );
    return target.toString();
  }
  /**
   * Build a JSON string in the specified {@code target} from the specified {@code value} with the provided left
   * {@code margin}. The {@code value} must be a valid JSON value:
   * <lu>
   *   <li>primitive, boxed primitive, or {@code String}</li>
   *   <li>{@code Iterable} of JSON values</li>
   *   <li>{@code Map} of JSON values</li>
   * </lu>
   */
  public static void toJson( StringBuilder target, int margin, Object value )
  {
    value = toBindings( value );

    if( value instanceof Pair )
    {
      value = ((Pair)value).getSecond();
    }

    value = toBindings( value );

    if( value instanceof Map )
    {
      toJson( ((Map)value), target, margin );
    }
    else if( value instanceof Iterable )
    {
      listToJson( target, margin, (Iterable)value );
    }
    else
    {
      appendValue( target, value );
    }
  }

  private static boolean isNewLine( StringBuilder sb )
  {
    return sb.length() > 0 && sb.charAt( sb.length() - 1 ) == '\n';
  }

  public static void listToJson( StringBuilder sb, int indent, Iterable value )
  {
    sb.append( '[' );
    int i = 0;
    for( Iterator iter = value.iterator(); iter.hasNext(); )
    {
      Object comp = iter.next();
      if( i == 0 )
      {
        sb.append( "\n" );
      }

      comp = toBindings( comp );

      if( comp instanceof Map )
      {
        toJson( (Map)comp, sb, indent + 2 );
      }
      else if( comp instanceof Iterable )
      {
        listToJson( sb, indent + 2, (Iterable)comp );
      }
      else
      {
        indent( sb, indent + 2 );
        appendValue( sb, comp );
      }
      appendCommaNewLine( sb, iter.hasNext() );
      i++;
    }
    indent( sb, indent );
    sb.append( "]" );
  }

  /**
   * Serializes a JSON-compatible List into a JSON formatted StringBuilder with the specified indent of spaces
   */
  public static String listToJson( Iterable list )
  {
    StringBuilder sb = new StringBuilder();
    listToJson( sb, 0, list );
    return sb.toString();
  }


  private static void appendCommaNewLine( StringBuilder sb, boolean bComma )
  {
    if( bComma )
    {
      sb.append( ',' );
    }
    sb.append( "\n" );
  }

  /**
   * Parse the JSON string as a manifold.rt.api.Bindings instance.
   *
   * @param json A Standard JSON formatted string
   *
   * @return A JSON value (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  @SuppressWarnings("UnusedDeclaration")
  public static Object fromJson( String json )
  {
    return fromJson( json, false, false );
  }
  public static Object fromJson( String json, boolean withBigNumbers, boolean withTokens )
  {
    try
    {
      return PARSER.get().parseJson( json, withBigNumbers, withTokens );
    }
    catch( ScriptException e )
    {
      throw new RuntimeException( e );
    }
  }
}
