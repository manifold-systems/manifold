package manifold.api.gen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 */
public class TypeNameParser
{
  private static final String TOKENS = "<>,[]?& ";

  private final StringTokenizer _tokenizer;
  private String _token;


  @SuppressWarnings("WeakerAccess")
  public TypeNameParser( String typeName )
  {
    _tokenizer = new StringTokenizer( typeName, TOKENS, true );
  }

  public Type parse()
  {
    nextToken();
    return parseType();
  }

  private Type parseType()
  {
    Type type;

    if( match( '?' ) )
    {
      String superOrExtends = _token;
      if( matchName() )
      {
        verifySuperOrExtends( superOrExtends );
        List<Type> bounds = parseCompoundType();
        type = new Type( "?", superOrExtends, bounds );
      }
      else
      {
        type = new Type( "?" );
      }
    }
    else
    {
      String name = _token;
      if( matchName() )
      {
        type = new Type( name );
        String superOrExtends = _token;
        if( matchName() )
        {
          verifySuperOrExtends( superOrExtends );
          List<Type> bounds = parseCompoundType();
          type = new Type( name, superOrExtends, bounds );
        }
      }
      else
      {
        return null;
      }
    }

    if( match( '<' ) )
    {
      parseParamList( type );
      if( !match( '>' ) )
      {
        throw new RuntimeException( "expecting '>" );
      }
      Type innerType = parseType();
      if( innerType != null )
      {
        //## note: ignoring type args for outer type of inner type e.g., abc.Outer<E>.Inner become abc.Outer.Inner
        innerType._fqn = type._fqn + innerType._fqn;
        type = innerType;
      }
    }
    while( match( '[' ) )
    {
      if( !match( ']' ) )
      {
        throw new RuntimeException( "expecting ']" );
      }
      type._arrayDim++;
    }
    return type;
  }

  private List<Type> parseCompoundType()
  {
    List<Type> bounds = new ArrayList<>();
    do
    {
      bounds.add( parseType() );
    } while( match( '&' ) );
    return bounds;
  }

  private void verifySuperOrExtends( String superOrExtends )
  {
    if( !superOrExtends.equals( "super" ) && !superOrExtends.equals( "extends" ) )
    {
      throw new RuntimeException( "expecting 'extends' or 'super'" );
    }
  }

  private void parseParamList( Type type )
  {
    Type param = parseType();
    if( param == null )
    {
      type._diamond = true;
      return;
    }

    type.addParam( param );
    while( match( ',' ) )
    {
      type.addParam( parseType() );
    }
  }

  private boolean match( char c )
  {
    if( _token.equals( String.valueOf( c ) ) )
    {
      nextToken();
      return true;
    }
    return false;
  }

  private boolean matchName()
  {
    if( _token.length() > 0 && !TOKENS.contains( _token ) )
    {
      nextToken();
      return true;
    }
    return false;
  }

  private void nextToken()
  {
    do
    {
      if( _tokenizer.hasMoreTokens() )
      {
        _token = _tokenizer.nextToken();
      }
      else
      {
        _token = "";
      }
    } while( _token.equals( " " ) );
  }

  public static class Type
  {
    String _fqn;
    List<Type> _params;
    String _superOrExtends;
    List<Type> _bound;
    boolean _diamond;
    int _arrayDim;

    public Type()
    {
    }

    public Type( String fqn )
    {
      _fqn = fqn;
      _params = new ArrayList<>();
    }

    public Type( String fqn, String superOrExtends, List<Type> bound )
    {
      _fqn = fqn;
      _superOrExtends = superOrExtends;
      _bound = bound;
      _params = Collections.emptyList();
    }

    void addParam( Type param )
    {
      _params.add( param );
    }

    Type getComponentType()
    {
      if( _arrayDim == 0 )
      {
        return null;
      }

      Type copy = new Type( _fqn );
      copy._params = _params;
      copy._superOrExtends = _superOrExtends;
      copy._bound = _bound;
      copy._diamond = _diamond;
      copy._arrayDim = _arrayDim - 1;
      return copy;
    }

    public String getFullName()
    {
      StringBuilder sb = new StringBuilder( _fqn );
      if( _diamond )
      {
        sb.append( "<>" );
      }
      else if( _superOrExtends != null )
      {
        sb.append( ' ' ).append( _superOrExtends ).append( ' ' );
        for( int i = 0; i < _bound.size(); i++ )
        {
          if( i > 0 )
          {
            sb.append( " & " );
          }
          sb.append( _bound.get( i ).getFullName() );
        }
      }
      else if( _params.size() > 0 )
      {
        sb.append( '<' );
        for( int i = 0; i < _params.size(); i++ )
        {
          Type param = _params.get( i );
          if( i > 0 )
          {
            sb.append( ", " );
          }
          sb.append( param.getFullName() );
        }
        sb.append( '>' );
      }
      for( int i = 0; i < _arrayDim; i++ )
      {
        sb.append( "[]" );
      }
      return sb.toString();
    }

    public String toString()
    {
      return getFullName();
    }
  }
}
