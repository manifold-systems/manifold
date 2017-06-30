package manifold.api.gen;

import java.lang.reflect.Array;
import manifold.util.ManEscapeUtil;

/**
 */
public abstract class SrcElement
{
  public static final int INDENT = 2;
  private SrcAnnotated _owner;

  public SrcElement()
  {
  }

  public SrcElement( SrcAnnotated owner )
  {
    _owner = owner;
  }

  public abstract StringBuilder render( StringBuilder sb, int indent );

  public SrcAnnotated getOwner()
  {
    return _owner;
  }

  public void setOwner( SrcAnnotated owner )
  {
    _owner = owner;
  }

  public String indent( StringBuilder sb, int indent )
  {
    for( int i = 0; i < indent; i++ )
    {
      sb.append( ' ' );
    }
    return "";
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    render( sb, 0 );
    return sb.toString();
  }

  public static String makeCompileTimeConstantValue( SrcType type, Object value )
  {
    String result;

    if( value == null )
    {
      result = "null";
    }
    else if( value instanceof Class )
    {
      result = ((Class)value).getName() + ".class";
    }
    else if( value instanceof String )
    {
      if( String.class.getName().equals( type.getName() ) )
      {
        result = "\"" + ManEscapeUtil.escapeForJava( value.toString() ) + "\"";
      }
      else if( type.isEnum() )
      {
        result = type.getName() + '.' + value;
      }
      else if( type.getName().equals( char.class.getName() ) )
      {
        result = "'" + ManEscapeUtil.escapeForJava( value.toString() ) + "'";
      }
      else
      {
        result = (String)value;
      }
    }
    else if( value instanceof Character )
    {
      result = "'" + ManEscapeUtil.escapeForJava( value.toString() ) + "'";
    }
    else if( value.getClass().isArray() )
    {
      StringBuilder sb = new StringBuilder();
      sb.append( "{" );
      int len = Array.getLength( value );
      for( int i = 0; i < len; i++ )
      {
        Object v = Array.get( value, i );
        sb.append( i > 0 ? ", " : "" ).append( makeCompileTimeConstantValue( type.getComponentType(), v ) );
      }
      sb.append( "}" );
      result = sb.toString();
    }
    else
    {
      result = value.toString();
    }
    return result;
  }
}
