/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.util;

import java.util.List;
import javax.script.Bindings;

/**
 */
public class JsonUtil
{
  public static String makeIdentifier( String name )
  {
    String identifier = ReservedWordMapping.getIdentifierForName( name );
    if( !identifier.equals( name ) )
    {
      return identifier;
    }

    StringBuilder sb = new StringBuilder();
    for( int i = 0; i < name.length(); i++ )
    {
      char c = name.charAt( i );
      if( i == 0 && Character.isDigit( c ) )
      {
        sb.append( '_' ).append( c );
      }
      else if( c == '_' || c == '$' || Character.isLetterOrDigit( c ) )
      {
        sb.append( c );
      }
      else
      {
        sb.append( '_' );
      }
    }
    return sb.toString();
  }

  /**
   * Serializes this Bindings instance to a JSON formatted String
   */
  public static String toJson( Bindings thisBindings )
  {
    StringBuilder sb = new StringBuilder();
    toJson( thisBindings, sb, 0 );
    return sb.toString();
  }

  /**
   * Serializes this Bindings instance into a JSON formatted StringBuilder with the specified indent of spaces
   */
  public static void toJson( Bindings thisBindings, StringBuilder sb, int indent )
  {
    int iKey = 0;
    if( isNewLine( sb ) )
    {
      indent( sb, indent );
    }
    sb.append( "{\n" );
    if( thisBindings.size() > 0 )
    {
      for( String key : thisBindings.keySet() )
      {
        indent( sb, indent + 2 );
        sb.append( '\"' ).append( key ).append( '\"' ).append( ": " );
        Object value = thisBindings.get( key );
        if( value instanceof Bindings )
        {
          toJson( (Bindings)value, sb, indent + 2 );
        }
        else if( value instanceof List )
        {
          listToJson( sb, indent + 2, (List)value );
        }
        else
        {
          appendValue( sb, value );
        }
        appendCommaNewLine( sb, iKey < thisBindings.size() - 1 );
        iKey++;
      }
    }
    indent( sb, indent );
    sb.append( "}" );
  }

  /**
   * Build a JSON string from the specified {@code value}. The {@code value} must be a valid JSON value:
   * <lu>
   *   <li>primitive, boxed primitive, or {@code String}</li>
   *   <li>{@code List} of JSON values</li>
   *   <li>{@code Bindings} of JSON values</li>
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
   *   <li>{@code List} of JSON values</li>
   *   <li>{@code Bindings} of JSON values</li>
   * </lu>
   */
  public static void toJson( StringBuilder target, int margin, Object value )
  {
    if( value instanceof Pair )
    {
      value = ((Pair)value).getSecond();
    }
    if( value instanceof Bindings )
    {
      toJson( ((Bindings)value), target, margin );
    }
    else if( value instanceof List )
    {
      listToJson( target, margin, (List)value );
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

  public static void listToJson( StringBuilder sb, int indent, List value )
  {
    sb.append( '[' );
    if( value.size() > 0 )
    {
      sb.append( "\n" );
      int iSize = value.size();
      int i = 0;
      while( i < iSize )
      {
        Object comp = value.get( i );
        if( comp instanceof Bindings )
        {
          toJson( (Bindings)comp, sb, indent + 2 );
        }
        else if( comp instanceof List )
        {
          listToJson( sb, indent + 2, (List)comp );
        }
        else
        {
          indent( sb, indent + 2 );
          appendValue( sb, comp );
        }
        appendCommaNewLine( sb, i < iSize - 1 );
        i++;
      }
    }
    indent( sb, indent );
    sb.append( "]" );
  }

  /**
   * Serializes a JSON-compatible List into a JSON formatted StringBuilder with the specified indent of spaces
   */
  public static String listToJson( List list )
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

  private static void indent( StringBuilder sb, int indent )
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
}
