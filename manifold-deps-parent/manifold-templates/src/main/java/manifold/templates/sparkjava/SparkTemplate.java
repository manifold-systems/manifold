package manifold.templates.sparkjava;

import manifold.templates.runtime.BaseTemplate;
import manifold.util.ManEscapeUtil;
import spark.Request;
import spark.Response;

import static spark.Spark.afterAfter;
import static spark.Spark.before;

/**
 * The base class for a Spark-based template.  Use the {@code extends} directive in your template to make this
 * class your template's base class.  Or, derive your own class from this one with features suitable for your
 * application and use that for your templates' base class.
 */
public abstract class SparkTemplate extends BaseTemplate
{
  private static ThreadLocal<Request> REQUEST = new ThreadLocal<>();
  private static ThreadLocal<Response> RESPONSE = new ThreadLocal<>();

  public static void init()
  {
    before( (request, response) -> {
      REQUEST.set( request );
      RESPONSE.set( response );
    } );

    afterAfter( (request, response) -> {
      REQUEST.set( null );
      RESPONSE.set( null );
    } );
  }

  /**
   * Access Spark's {@link Response} object.
   */
  public Response getResponse()
  {
    return RESPONSE.get();
  }

  /**
   * Access Spark's {@link Request} object;
   */
  public Request getRequest()
  {
    return REQUEST.get();
  }

  @Override
  public String toS( Object o )
  {
    if( o instanceof RawObject )
    {
      return super.toS( o.toString() );
    }
    else if( o == null )
    {
      return "";
    }
    else
    {
      return escapeHTML( o.toString() );
    }
  }

  private String escapeHTML( String str )
  {
    return ManEscapeUtil.escapeForHTML( str, false );
  }

  /**
   * Produce raw HTML
   */
  public Object raw( Object o )
  {
    return new RawObject( o );
  }

  private static class RawObject
  {
    private final Object in;

    RawObject( Object in )
    {
      this.in = in;
    }

    public String toString()
    {
      return in.toString();
    }
  }
}
