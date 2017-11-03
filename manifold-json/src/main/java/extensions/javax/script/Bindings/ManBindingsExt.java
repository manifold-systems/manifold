package extensions.javax.script.Bindings;

import extensions.java.net.URL.ManUrlExt;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import manifold.api.json.Json;
import manifold.ext.api.AbstractDynamicTypeProxy;
import manifold.ext.api.Extension;
import manifold.ext.api.This;
import manifold.util.JsonUtil;
import manifold.util.Pair;

/**
 */
@Extension
public class ManBindingsExt
{
  /**
   * Serializes this Bindings instance to a JSON formatted String
   */
  public static String toJson( @This Bindings thiz )
  {
    StringBuilder sb = new StringBuilder();
    toJson( thiz, sb, 0 );
    return sb.toString();
  }

  public static String toJson( Object obj )
  {
    Bindings bindings = getBindingsFrom( obj );
    if( bindings != null )
    {
      return toJson( bindings );
    }
    return null;
  }

  /**
   * Serializes this Bindings instance into a JSON formatted StringBuilder with the specified indent of spaces
   */
  public static void toJson( @This Bindings thiz, StringBuilder sb, int indent )
  {
    int iKey = 0;
    if( isNewLine( sb ) )
    {
      indent( sb, indent );
    }
    if( thiz.size() > 0 )
    {
      sb.append( "{\n" );
      for( String key : thiz.keySet() )
      {
        indent( sb, indent + 2 );
        sb.append( '\"' ).append( key ).append( '\"' ).append( ": " );
        Object value = thiz.get( key );
        if( value instanceof Pair )
        {
          value = ((Pair)value).getSecond();
        }
        if( value instanceof Bindings )
        {
          toJson( ((Bindings)value), sb, indent + 2 );
        }
        else if( value instanceof List )
        {
          listToJson( sb, indent, (List)value );
        }
        else
        {
          JsonUtil.appendValue( sb, value );
        }
        appendCommaNewLine( sb, iKey < thiz.size() - 1 );
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
        if( comp instanceof Pair )
        {
          comp = ((Pair)comp).getSecond();
        }
        if( comp instanceof Bindings )
        {
          toJson( ((Bindings)comp), sb, indent + 4 );
        }
        else if( comp instanceof List )
        {
          listToJson( sb, indent + 4, (List)comp );
        }
        else
        {
          indent( sb, indent + 4 );
          JsonUtil.appendValue( sb, comp );
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

  /**
   * Serializes this Bindings instance to XML
   */
  public static String toXml( @This Bindings thiz )
  {
    return toXml( thiz, "object" );
  }

  /**
   * Serializes this Bindings instance to XML
   */
  public static String toXml( @This Bindings thiz, String name )
  {
    StringBuilder sb = new StringBuilder();
    toXml( thiz, name, sb, 0 );
    return sb.toString();
  }

  public static String toXml( Object obj )
  {
    Bindings bindings = getBindingsFrom( obj );
    if( bindings != null )
    {
      return toXml( bindings );
    }
    return null;
  }

  public static String toXml( Object obj, String name )
  {
    Bindings bindings = getBindingsFrom( obj );
    if( bindings != null )
    {
      return toXml( bindings, name );
    }
    return null;
  }

  public static void toXml( @This Bindings thiz, String name, StringBuilder sb, int indent )
  {
    indent( sb, indent );
    sb.append( '<' ).append( name );
    if( thiz.size() > 0 )
    {
      sb.append( ">\n" );
      for( String key : thiz.keySet() )
      {
        Object value = thiz.get( key );
        if( value instanceof Pair )
        {
          value = ((Pair)value).getSecond();
        }
        if( value instanceof Bindings )
        {
          toXml( ((Bindings)value), key, sb, indent + 2 );
        }
        else if( value instanceof List )
        {
          int len = ((List)value).size();
          indent( sb, indent + 2 );
          sb.append( "<" ).append( key );
          if( len > 0 )
          {
            sb.append( ">\n" );
            for( Object comp : (List)value )
            {
              if( comp instanceof Pair )
              {
                comp = ((Pair)comp).getSecond();
              }

              if( comp instanceof Bindings )
              {
                toXml( ((Bindings)comp), "li", sb, indent + 4 );
              }
              else
              {
                indent( sb, indent + 4 );
                sb.append( "<li>" ).append( comp ).append( "</li>\n" );
              }
            }
            indent( sb, indent + 2 );
            sb.append( "</" ).append( key ).append( ">\n" );
          }
          else
          {
            sb.append( "/>\n" );
          }
        }
        else
        {
          indent( sb, indent + 2 );
          sb.append( '<' ).append( key ).append( ">" );
          sb.append( value );
          sb.append( "</" ).append( key ).append( ">\n" );
        }
      }
      indent( sb, indent );
      sb.append( "</" ).append( name ).append( ">\n" );
    }
    else
    {
      sb.append( "/>\n" );
    }
  }

  /**
   * Make a JSON-compatible URL with the arguments from the Bindings. URL encodes
   * the arguments in UTF-8 and appends them to the list using standard URL query
   * delimiters.
   * <p/>
   * If an argument is a javax.script.Bindings or a List, it is transformed to JSON.
   * Otherwise, the argument is coerced to a String and URL encoded.
   */
  public static URL makeUrl( @This Bindings thiz, String url )
  {
    return ManUrlExt.makeUrl( url, thiz );
  }

  /**
   * Use http POST to pass JSON bindings to this URL and get the full content as a JSON object.
   * <p>
   * If an argument is a javax.script.Bindings or a List, it is transformed to JSON.  Otherwise,
   * the argument is coerced to a String.  All arguments are URL encoded.
   *
   * @return The full content of this URL's stream as a JSON object.
   *
   * @see ManUrlExt#postForTextContent(URL, Bindings)
   */
  @SuppressWarnings("unused")
  public static Bindings postForJsonContent( @This Bindings thiz, String url )
  {
    try
    {
      return Json.fromJson( ManUrlExt.postForTextContent( new URL( url ), thiz ) );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Make a JSON-compatible URL with the arguments from the Bindings. URL encodes
   * the arguments in UTF-8 and appends them to the list using standard URL query
   * delimiters.
   * <p/>
   * If an argument is a javax.script.Bindings or a List, it is transformed to JSON.
   * Otherwise, the argument is coerced to a String and URL encoded.
   *<p/>
   * If the content of the resulting URL is a JSON document, returns a JSON bindings
   * reflecting the document.
   *
   * @return JSON bindings reflecting the content of the URL.
   *
   * @see manifold.api.json.Json#fromJson(String)
   */
  public static Bindings getJsonContent( @This Bindings thiz, String url )
  {
    return Json.fromJson( ManUrlExt.getTextContent( makeUrl( thiz, url ) ) );
  }

  /**
   * Convert this Json Bindings to an arguments String suitable for a Json Url.
   */
  public static String makeArguments( @This Bindings arguments )
  {
    try
    {
      StringBuilder sb = new StringBuilder();
      for( Map.Entry<String, Object> entry : arguments.entrySet() )
      {
        if( sb.length() != 0 )
        {
          sb.append( '&' );
        }
        sb.append( URLEncoder.encode( entry.getKey(), "UTF-8" ) )
          .append( '=' )
          .append( makeValue( entry.getValue() ) );
      }
      return sb.toString();
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Convert the Object to a String value suitable for a Json Url argument.
   * @param value A Json value.  On of: Bindings, List, or simple value.
   * @return Json formatted value String.
   */
  @Extension
  public static String makeValue( Object value )
  {
    if( value instanceof Bindings )
    {
      value = JsonUtil.toJson( (Bindings)value );
    }
    else if( value instanceof List )
    {
      value = JsonUtil.listToJson( (List)value );
    }

    try
    {
      return URLEncoder.encode( value.toString(), "UTF-8" );
    }
    catch( UnsupportedEncodingException e )
    {
      throw new RuntimeException( e );
    }
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

  /**
   * Generates a static type corresponding with this Bindings object.  The generated type is a nesting of structure types.
   * This nesting of types is intended to be placed in a .gs file as a top-level structure, or embedded as an inner type.
   * <p>
   * A structure type is a direct mapping of property members to name/value pairs in a Bindings.  A property has the same name as the key and follows these rules:
   * <ul>
   * <li> If the type of the value is a "simple" type, such as a String or Integer, the type of the property matches the simple type exactly
   * <li> Otherwise, if the value is a Bindings type, the property type is that of a child structure with the same name as the property and recursively follows these rules
   * <li> Otherwise, if the value is a List, the property is a List parameterized with the component type where the component type is the structural union inferred from the values of the List recursively following these rules for each value
   * </ul>
   */
  public static String toStructure( @This Bindings thiz, String nameForStructure )
  {
    return toStructure( thiz, nameForStructure, true );
  }

  public static String toStructure( @This Bindings thiz, String nameForStructure, boolean mutable )
  {
    return Json.makeStructureTypes( nameForStructure, thiz, mutable );
  }

  private static Bindings getBindingsFrom( Object obj )
  {
    Bindings bindings = null;
    if( obj instanceof Bindings )
    {
      bindings = (Bindings)obj;
    }
    else
    {
      while( obj instanceof AbstractDynamicTypeProxy )
      {
        final Object root = ((AbstractDynamicTypeProxy)obj).getRoot();
        if( root instanceof Bindings )
        {
          bindings = (Bindings)root;
          break;
        }
        else
        {
          obj = root;
        }
      }
    }
    return bindings;
  }
}
