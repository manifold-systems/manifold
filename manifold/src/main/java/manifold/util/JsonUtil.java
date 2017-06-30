package manifold.util;

import java.util.List;
import javax.script.Bindings;

/**
 */
public class JsonUtil
{
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
    if( thisBindings.size() > 0 )
    {
      sb.append( "{\n" );
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
          listToJson( sb, indent, (List)value );
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
          toJson( (Bindings)comp, sb, indent + 4 );
        }
        else if( comp instanceof List )
        {
          listToJson( sb, indent + 4, (List)comp );
        }
        else
        {
          indent( sb, indent + 4 );
          appendValue( sb, comp );
        }
        appendCommaNewLine( sb, i < iSize - 1 );
        i++;
      }
    }
    indent( sb, indent + 2 );
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

  private static StringBuilder appendValue( StringBuilder sb, Object comp )
  {
    if( comp instanceof String )
    {
      sb.append( '\"' );
      sb.append( ManEscapeUtil.escapeForGosuStringLiteral( (String)comp ) );
      sb.append( '\"' );
    }
    else if( comp instanceof Integer ||
             comp instanceof Long ||
             comp instanceof Double ||
             comp instanceof Float ||
             comp instanceof Short ||
             comp instanceof Character ||
             comp instanceof Byte )
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
