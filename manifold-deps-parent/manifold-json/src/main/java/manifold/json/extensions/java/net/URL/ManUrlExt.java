package manifold.json.extensions.java.net.URL;

import manifold.json.extensions.javax.script.Bindings.ManBindingsExt;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import manifold.api.json.Json;
import manifold.ext.api.Extension;
import manifold.ext.api.This;
import manifold.util.JsonUtil;
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
  public static URL makeUrl( String url, Bindings arguments )
  {
    StringBuilder sb = new StringBuilder();
    for( Map.Entry entry : arguments.entrySet() )
    {
      sb.append( sb.length() == 0 ? '?' : '&' )
        .append( entry.getKey() )
        .append( '=' );
      Object value = entry.getValue();
      if( value instanceof Bindings )
      {
        value = ManBindingsExt.toJson( ((Bindings)value) );
      }
      else if( value instanceof List )
      {
        value = ManBindingsExt.listToJson( (List)value );
      }
      value = encode( (String)value );
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
   * Convenience method to encode a URL string and not have to handle the
   * UnsupportedEncodingException.
   */
  @Extension
  public static String encode( String urlPart )
  {
    try
    {
      return URLEncoder.encode( urlPart, "UTF-8" );
    }
    catch( UnsupportedEncodingException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Use http POST to pass JSON bindings to this URL and get back the full content as a String.
   * <p>
   * If an argument is a javax.script.Bindings or a List, it is transformed to JSON.  Otherwise,
   * the argument is coerced to a String.  All arguments are URL encoded.
   *
   * @return The full content of this URL coerced to a String.
   *
   * @see #postForJsonContent(URL, Bindings)
   */
  public static String postForTextContent( @This URL url, Bindings bindings )
  {
    try
    {
      byte[] bytes = bindings.makeArguments().getBytes( "UTF-8" );
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setRequestMethod( "POST" );
      conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
      conn.setRequestProperty( "Content-Length", String.valueOf( bytes.length ) );
      conn.setDoOutput( true );
      try( OutputStream out = conn.getOutputStream() )
      {
        out.write( bytes );
      }
      try( Reader in = StreamUtil.getInputStreamReader( conn.getInputStream() ) )
      {
        return StreamUtil.getContent( in );
      }
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Use http POST to pass JSON bindings to this URL and get the full content as a JSON object.
   * <p>
   * If an argument is a javax.script.Bindings or a List, it is transformed to JSON.  Otherwise,
   * the argument is coerced to a String.  All arguments are URL encoded.
   *
   * @return The full content of this URL's stream as a JSON object.
   *
   * @see #postForTextContent(URL, Bindings)
   */
  @SuppressWarnings("unused")
  public static Bindings postForJsonContent( @This URL url, Bindings bindings )
  {
    return Json.fromJson( postForTextContent( url, bindings ) );
  }

  private static String makeValue( Object value )
  {
    if( value instanceof Bindings )
    {
      value = JsonUtil.toJson( (Bindings)value );
    }
    else if( value instanceof List )
    {
      value = JsonUtil.listToJson( (List)value );
    }
    return encode( value.toString() );
  }

  /**
   * @return The full text content of this URL's stream.
   */
  public static String getTextContent( @This URL thiz )
  {
    try( Reader reader = StreamUtil.getInputStreamReader( thiz.openStream() ) )
    {
      return StreamUtil.getContent( reader );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @return The full binary content of this URL's stream.
   */
  public static byte[] getBinaryContent( @This URL thiz )
  {
    try( InputStream stream = thiz.openStream() )
    {
      return StreamUtil.getContent( stream );
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
  public static Bindings getJsonContent( @This URL thiz )
  {
    return Json.fromJson( getTextContent( thiz ) );
  }
}
