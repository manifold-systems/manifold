package manifold.api.json;

import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import manifold.util.Pair;
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
    if( value instanceof Pair )
    {
      return getBindings( ((Pair)value).getSecond() );
    }
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
      if( e instanceof Pair )
      {
        e = ((Pair)e).getSecond();
      }

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
      return new URL( url + _bindings.makeArguments() );
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
    return url.postForTextContent( _bindings );
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
