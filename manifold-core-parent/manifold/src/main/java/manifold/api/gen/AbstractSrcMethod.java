package manifold.api.gen;

import com.sun.tools.javac.code.Flags;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
public class AbstractSrcMethod<T extends AbstractSrcMethod<T>> extends SrcStatement<T>
{
  private SrcType _returns;
  private SrcStatementBlock _body;
  private List<SrcType> _typeVars;
  private List<SrcType> _throwTypes;
  private boolean _isConstructor;

  public AbstractSrcMethod( SrcClass srcClass )
  {
    super( srcClass );
    _typeVars = Collections.emptyList();
    _throwTypes = Collections.emptyList();
  }

  public boolean isConstructor()
  {
    return _isConstructor;
  }

  public void setConstructor( boolean isConstructor )
  {
    _isConstructor = isConstructor;
  }

  public T returns( SrcType returns )
  {
    _returns = returns;
    return (T)this;
  }

  public T returns( Class returns )
  {
    _returns = new SrcType( returns );
    return (T)this;
  }

  public T returns( String returns )
  {
    _returns = new SrcType( returns );
    return (T)this;
  }

  public void addTypeVar( SrcType typeVar )
  {
    if( _typeVars.isEmpty() )
    {
      _typeVars = new ArrayList<>();
    }
    _typeVars.add( typeVar );
  }

  public void addThrowType( SrcType type )
  {
    if( _throwTypes.isEmpty() )
    {
      _throwTypes = new ArrayList<>();
    }
    _throwTypes.add( type );
  }

  public T body( SrcStatementBlock body )
  {
    _body = body;
    return (T)this;
  }

  public SrcType getReturnType()
  {
    return _returns;
  }

  public List<SrcType> getTypeVariables()
  {
    return _typeVars;
  }

  public List<SrcType> getThrowTypes()
  {
    return _throwTypes;
  }

  private String renderThrowTypes( StringBuilder sb )
  {
    if( _throwTypes.size() > 0 )
    {
      sb.append( " throws " );
      for( int i = 0; i < _throwTypes.size(); i++ )
      {
        if( i > 0 )
        {
          sb.append( ", " );
        }
        sb.append( _throwTypes.get( i ) );
      }
    }
    return "";
  }

  public String signature()
  {
    StringBuilder sb = new StringBuilder();
    sb.append( getSimpleName() ).append( renderParameters( sb, true ) );
    return sb.toString();
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    renderAnnotations( sb, indent, false );
    indent( sb, indent );
    renderModifiers( sb, (getModifiers() & ~Modifier.TRANSIENT), (getModifiers() & Flags.DEFAULT) != 0, Modifier.PUBLIC );
    renderTypeVars( _typeVars, sb );
    if( _returns != null )
    {
      _returns.render( sb, indent ).append( ' ' ).append( getSimpleName() ).append( renderParameters( sb ) ).append( renderThrowTypes( sb ) );
    }
    else if( isConstructor() )
    {
      sb.append( getOwner().getSimpleName() ).append( renderParameters( sb ) ).append( renderThrowTypes( sb ) );
    }
    if( isAbstractMethod() )
    {
      sb.append( ";\n" );
    }
    else
    {
      if( _body != null )
      {
        _body.render( sb, indent );
      }
      else
      {
        throw new IllegalStateException( "Body of method is null" );
      }
    }
    return sb;
  }

  private boolean isAbstractMethod()
  {
    return Modifier.isAbstract( (int)getModifiers() ) ||
           isNonDefaultNonStaticInterfaceMethod();
  }

  private boolean isNonDefaultNonStaticInterfaceMethod()
  {
    return getOwner() instanceof SrcClass &&
           ((SrcClass)getOwner()).isInterface() &&
           (getModifiers() & Flags.DEFAULT) == 0 &&
           (getModifiers() & Flags.STATIC) == 0;
  }
}
