package manifold.api.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class SrcType extends SrcElement
{
  private String _fqn;
  private SrcType _componentType;
  private SrcType _enclosingType;
  private String _superOrExtends;
  private List<SrcType> _bound;
  private List<SrcType> _typeParams = new ArrayList<>();
  private int _arrayDims;
  private boolean _isPrimitive;
  private boolean _isDiamond;
  private boolean _isEnum;
  private boolean _isInterface;
  private boolean _isAnnotation;

  public SrcType( Class type )
  {
    this( new TypeNameParser( type.getName() ).parse() );
  }

  public SrcType( String fqn )
  {
    this( new TypeNameParser( fqn ).parse() );
  }

  private SrcType( TypeNameParser.Type simpleType )
  {
    _fqn = simpleType._fqn;
    _isDiamond = simpleType._diamond;
    _arrayDims = simpleType._arrayDim;
    _componentType = _arrayDims > 0 ? new SrcType( simpleType.getComponentType() ) : null;
    for( TypeNameParser.Type param : simpleType._params )
    {
      addTypeParam( new SrcType( param ) );
    }
    if( simpleType._bound != null )
    {
      _superOrExtends = simpleType._superOrExtends;
      _bound = simpleType._bound.stream().map( SrcType::new ).collect( Collectors.toList() );
    }
  }

  public SrcType getEnclosingType()
  {
    return _enclosingType;
  }

  public void setEnclosingType( SrcType enclosingType )
  {
    _enclosingType = enclosingType;
  }

  public SrcType addTypeParam( SrcType srcType )
  {
    _typeParams.add( srcType );
    return this;
  }

  public SrcType addTypeParam( Class type )
  {
    SrcType srcType = new SrcType( type );
    _typeParams.add( srcType );
    return this;
  }

  public SrcType addTypeParam( String type )
  {
    SrcType srcType = new SrcType( type );
    _typeParams.add( srcType );
    return this;
  }

  public void setPrimitive( boolean primitive )
  {
    _isPrimitive = primitive;
  }

  public void setInterface( boolean isInterface )
  {
    _isInterface = isInterface;
  }

  public void setEnum( boolean isEnum )
  {
    _isEnum = isEnum;
  }

  public void setAnnotation( boolean isAnno )
  {
    _isAnnotation = isAnno;
  }

  public String getName()
  {
    return _fqn;
  }

  public SrcType diamond()
  {
    _isDiamond = true;
    return this;
  }

  public int getArrayDims()
  {
    return _arrayDims;
  }

  public boolean isPrimitive()
  {
    return _isPrimitive;
  }

  public boolean isArray()
  {
    return _arrayDims > 0;
  }

  public List<SrcType> getTypeParams()
  {
    return _typeParams;
  }

  public boolean isDiamond()
  {
    return _isDiamond;
  }

  public SrcType getComponentType()
  {
    if( _componentType != null )
    {
      return _componentType;
    }

    if( isArray() )
    {
      String type = _fqn.substring( 0, _fqn.lastIndexOf( '[' ) );
      return _componentType = new SrcType( type );
    }
    return null;
  }

  public StringBuilder render( StringBuilder sb, int indent )
  {
    if( _enclosingType != null )
    {
      _enclosingType.render( sb, indent );
      sb.append( '.' );
    }

    String fqn = _fqn; //!! ALWAYS USE FULLY QUALIFIED NAMES -- SOME USES CASES DEPEND ON FULLY QUALIFIED NAMES
    sb.append( fqn );
    if( _isDiamond )
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
        _bound.get( i ).render( sb, 0 );
      }
    }
    else if( _typeParams.size() > 0 )
    {
      sb.append( '<' );
      for( int i = 0; i < _typeParams.size(); i++ )
      {
        SrcType param = _typeParams.get( i );
        if( i > 0 )
        {
          sb.append( ", " );
        }
        param.render( sb, 0 );
      }
      sb.append( '>' );
    }
    for( int i = 0; i < _arrayDims; i++ )
    {
      sb.append( "[]" );
    }
    return sb;
  }

  public boolean isEnum()
  {
    return false;
  }
}
