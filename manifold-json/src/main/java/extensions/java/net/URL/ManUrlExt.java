package extensions.java.net.URL;

import extensions.javax.script.Bindings.ManBindingsExt;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import manifold.api.json.Json;
import manifold.ext.api.Extension;
import manifold.ext.api.This;
import manifold.util.StreamUtil;

/**
 */
@Extension
public class ManUrlExt
{
  /**
   * Make a JSON-compatible URL with the arguments from the Bindings. URL encodes
   * the arguments in UTF-8 and appends them to the list using standard URL query
   * delimiters.
   * <p/>
   * If an argument is a javax.script.Bindings or a List, it is transformed to JSON.
   * Otherwise, the argument is coerced to a String and URL encoded.
   */
  @Extension
  public static URL makeUrl( String url, Bindings arguments ) {
    StringBuilder sb = new StringBuilder();
    for( Map.Entry entry : arguments.entrySet() ) {
      sb.append( sb.length() == 0 ? '?' : '&' )
      .append( entry.getKey() )
      .append( '=' );
      Object value = entry.getValue();
      if( value instanceof Bindings ) {
        value = ManBindingsExt.toJson( ((Bindings)value) );
      }
      else if( value instanceof List ) {
        value = ManBindingsExt.listToJson( (List)value );
      }
      try
      {
        value = URLEncoder.encode( (String)value, "UTF-8" );
      }
      catch( UnsupportedEncodingException e )
      {
        throw new RuntimeException( e );
      }
      sb.append( value );
    }
    try
    {
      return new URL( url + sb );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @return The full content of this URL's stream coerced to a String.
   */
  public static String getTextContent( @This URL thiz ) {
    try( Reader reader = StreamUtil.getInputStreamReader( thiz.openStream() ) ) {
      return StreamUtil.getContent( reader );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @return If the content of this URL is a JSON document, a JSON object reflecting the document.
   *
   * @see manifold.api.json.Json#fromJson(String)
   */
  public static Bindings getJsonContent( @This URL thiz ) {
    return Json.fromJson( getTextContent( thiz ) );
  }
}
