package manifold.api.json;

import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import manifold.util.JsonUtil;
import manifold.util.StreamUtil;

/**
 * Base class for generated *.impl.Foo json classes (primarily for Java use-case)
 */
public class JsonImplBase implements IJsonIO
{
  protected Bindings _bindings;

  @SuppressWarnings("unused")
  public JsonImplBase()
  {
    this( new SimpleBindings( new ConcurrentHashMap<>() ) );
  }

  public JsonImplBase( Bindings bindings )
  {
    _bindings = bindings;
  }

  protected Bindings getBindings()
  {
    return _bindings;
  }

  @SuppressWarnings("unused")
  protected Bindings getBindings( Object value )
  {
    if( value instanceof Bindings )
    {
      return (Bindings)value;
    }
    if( value instanceof JsonImplBase )
    {
      return ((JsonImplBase)value)._bindings;
    }
    throw new IllegalStateException( "Unhandled type: " + value.getClass() );
  }

  @SuppressWarnings("unused")
  protected List wrapList( List list, Function<Bindings, ? extends JsonImplBase> ctor )
  {
    List wrappedList = new ArrayList();
    for( Object e : list )
    {
      Object elem;
      if( e instanceof List )
      {
        elem = wrapList( (List)e, ctor );
      }
      else if( e instanceof Bindings )
      {
        elem = ctor.apply( (Bindings)e );
      }
      else
      {
        // throw?
        elem = e;
      }
      //noinspection unchecked
      wrappedList.add( elem );
    }
    return wrappedList;
  }

  @SuppressWarnings("unused")
  protected List unwrapList( List list )
  {
    List unwrappedList = new ArrayList();
    for( Object e : list )
    {
      Object elem;
      if( e instanceof List )
      {
        elem = unwrapList( (List)e );
      }
      else if( e instanceof JsonImplBase )
      {
        elem = ((JsonImplBase)e).getBindings();
      }
      else
      {
        // throw?
        elem = e;
      }
      //noinspection unchecked
      unwrappedList.add( elem );
    }
    return unwrappedList;
  }

  /**
   * Make a JSON-friendly URL with the arguments derived from this Json object.
   * <p>
   * If an argument is a Gosu Dynamic Expando or a javax.script.Bindings or a List,
   * it is transformed to JSON.  Otherwise, the argument is coerced to a String.  All
   * arguments are URL encoded.
   * <p>
   * Note the resulting URL is intended to be used for an http GET invocation via the
   * TextContent and JsonContent properties. Do not use the resulting URL for a POST
   * invocation, instead separately construct a URL and call postForTextContent() or
   * postForJsonContent().
   * <p>
   *
   * @see #postForTextContent(URL)
   * @see #postForJsonContent(URL)
   */
  public URL makeUrl( String url )
  {
    if( _bindings.size() > 0 )
    {
      url += '?';
    }
    try
    {
      return new URL( url + makeArguments( _bindings ) );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Using this Json as input fetch the content of the specified URL as a String.  If this is an http URL,
   * fetches the content using the GET method.
   *
   * @return The full content of the specified URL coerced to a String.
   */
  public String getTextContent( String url )
  {
    try( Reader reader = StreamUtil.getInputStreamReader( makeUrl( url ).openStream() ) )
    {
      return StreamUtil.getContent( reader );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Fetch the content of the specified URL as a Json object.  If this is an http URL,
   * fetches the content using the GET method.
   *
   * @return If the content of this URL is a Json document, a Json object reflecting the document.
   *
   * @see #getTextContent
   * @see #postForJsonContent(URL)
   */
  @SuppressWarnings("unused")
  public Bindings getJsonContent( String url )
  {
    return Json.fromJson( getTextContent( url ) );
  }

  /**
   * Use http POST to pass this json to the URL and get back the full content as a String.
   * <p>
   * If an argument is a Gosu Dynamic Expando or a javax.script.Bindings or a List,
   * it is transformed to JSON.  Otherwise, the argument is coerced to a String.  All
   * arguments are URL encoded.
   *
   * @return The full content of this URL coerced to a String.
   *
   * @see #postForJsonContent(URL)
   */
  public String postForTextContent( URL url )
  {
    try
    {
      byte[] bytes = makeArguments( _bindings ).getBytes( "UTF-8" );
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
   * Use http POST to pass arguments and get the full content of this URL as a JSON object.
   * <p>
   * If an argument is a Gosu Dynamic Expando or a javax.script.Bindings or a List,
   * it is transformed to JSON.  Otherwise, the argument is coerced to a String.  All
   * arguments are URL encoded.
   *
   * @return The full content of this URL's stream as a JSON object.
   *
   * @see #postForTextContent(URL)
   */
  @SuppressWarnings("unused")
  public Bindings postForJsonContent( URL url )
  {
    return Json.fromJson( postForTextContent( url ) );
  }

  private static String makeArguments( Bindings arguments )
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

  private static String makeValue( Object value ) throws UnsupportedEncodingException
  {
    if( value instanceof Bindings )
    {
      value = JsonUtil.toJson( (Bindings)value );
    }
    else if( value instanceof List )
    {
      value = JsonUtil.listToJson( (List)value );
    }
    return URLEncoder.encode( value.toString(), "UTF-8" );
  }

  @Override
  public void load( Bindings bindings )
  {
    _bindings = new SimpleBindings( bindings );
  }

  @Override
  public void save( Bindings bindings )
  {
    bindings.putAll( _bindings );
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    JsonImplBase that = (JsonImplBase)o;

    return _bindings.equals( that._bindings );
  }

  @Override
  public int hashCode()
  {
    return _bindings.hashCode();
  }
}
