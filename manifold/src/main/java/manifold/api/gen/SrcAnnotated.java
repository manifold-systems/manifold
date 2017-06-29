package manifold.api.gen;

import com.sun.tools.javac.code.Flags;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public abstract class SrcAnnotated<T extends SrcAnnotated<T>> extends SrcElement
{
  private List<SrcAnnotationExpression> _annotations = new ArrayList<>();
  private long _modifiers;
  private String _name;
  private List<SrcParameter> _parameters = new ArrayList<>();
  private Map<String, Object> _userData = Collections.emptyMap();

  public SrcAnnotated() {}
  public SrcAnnotated( SrcAnnotated owner )
  {
    super( owner );
  }

  public T addAnnotation( SrcAnnotationExpression anno )
  {
    _annotations.add( anno );
    return (T)this;
  }

  public T modifiers( long modifiers )
  {
    _modifiers = modifiers;
    return (T)this;
  }

  public T modifiers( Set<javax.lang.model.element.Modifier> modifiers )
  {
    _modifiers = modifiersFrom( modifiers );
    return (T)this;
  }

  public static long modifiersFrom( Set<javax.lang.model.element.Modifier> modifiers )
  {
    long mods = 0;
    for( javax.lang.model.element.Modifier mod: modifiers )
    {
      switch( mod )
      {
        case PUBLIC:
          mods |= Modifier.PUBLIC;
          break;
        case PROTECTED:
          mods |= Modifier.PROTECTED;
          break;
        case PRIVATE:
          mods |= Modifier.PRIVATE;
          break;
        case ABSTRACT:
          mods |= Modifier.ABSTRACT;
          break;
        case DEFAULT:
          mods |= Flags.DEFAULT;
          break;
        case STATIC:
          mods |= Modifier.STATIC;
          break;
        case FINAL:
          mods |= Modifier.FINAL;
          break;
        case TRANSIENT:
          mods |= Modifier.TRANSIENT;
          break;
        case VOLATILE:
          mods |= Modifier.VOLATILE;
          break;
        case SYNCHRONIZED:
          mods |= Modifier.SYNCHRONIZED;
          break;
        case NATIVE:
          mods |= Modifier.NATIVE;
          break;
        case STRICTFP:
          mods |= Flags.STRICTFP;
          break;
      }
    }
    return mods;
  }

  public T name( String simpleName )
  {
    _name = simpleName;
    return (T)this;
  }

  public T addParam( SrcParameter param )
  {
    _parameters.add( param );
    param.setOwner( this );
    return (T)this;
  }
  public T addParam( String name, Class type )
  {
    SrcParameter param = new SrcParameter( name, type );
    param.setOwner( this );
    _parameters.add( param );
    return (T)this;
  }
  public T addParam( String name, String type )
  {
    SrcParameter param = new SrcParameter( name, type );
    param.setOwner( this );
    _parameters.add( param );
    return (T)this;
  }
  public T addParam( String name, SrcType type )
  {
    SrcParameter param = new SrcParameter( name, type );
    param.setOwner( this );
    _parameters.add( param );
    return (T)this;
  }

  public List<SrcAnnotationExpression> getAnnotations()
  {
    return _annotations;
  }

  public long getModifiers()
  {
    return _modifiers;
  }

  public String getSimpleName()
  {
    return _name;
  }

  public List<SrcParameter> getParameters()
  {
    return _parameters;
  }

  public T withUserData( String tag, Object value )
  {
    if( _userData.isEmpty() )
    {
      _userData = new HashMap<>();
    }
    _userData.put( tag, value );
    return (T)this;
  }
  public Object getUserData( String tag )
  {
    return _userData.get( tag );
  }
  public Object removeUserData( String tag )
  {
    if( _userData.isEmpty() )
    {
      return null;
    }
    return _userData.remove( tag );
  }
  public void clearUserData()
  {
    _userData = Collections.emptyMap();
  }

  protected void renderAnnotations( StringBuilder sb, int indent, boolean sameLine )
  {
    for( SrcAnnotationExpression anno : _annotations )
    {
      anno.render( sb, indent, sameLine );
    }
  }

  protected String renderParameters( StringBuilder sb )
  {
    sb.append( '(' );
    for( int i = 0; i < _parameters.size(); i++ )
    {
      if( i > 0 )
      {
        sb.append( ", " );
      }
      SrcParameter param = _parameters.get( i );
      boolean isVarArgs = i == _parameters.size() - 1 && (getModifiers() & 0x00000080) != 0; // Modifier.VARARGS
      param.render( sb, 0, isVarArgs );
    }
    sb.append( ')' );
    return "";
  }

  public StringBuilder renderArgumenets( StringBuilder sb, List<SrcArgument> arguments, int indent, boolean sameLine )
  {
    sb.append( '(' );
    for( int i = 0; i < arguments.size(); i++ )
    {
      if( i > 0 )
      {
        sb.append( ", " );
      }
      SrcArgument arg = arguments.get( i );
      arg.render( sb, 0 );
    }
    sb.append( ')' ).append( sameLine ? "" : "\n" );
    return sb;
  }

  String renderTypeVars( List<SrcType> typeVars, StringBuilder sb )
  {
    if( typeVars.size() > 0 )
    {
      sb.append( '<' );
      for( int i = 0; i < typeVars.size(); i++ )
      {
        if( i > 0 )
        {
          sb.append( ", " );
        }
        typeVars.get( i ).render( sb, 0 );
      }
      sb.append( '>' );
    }
    return "";
  }

  protected String renderModifiers( StringBuilder sb, boolean isDefault, int defModifier )
  {
    return renderModifiers( sb, _modifiers, isDefault, defModifier );
  }
  protected String renderModifiers( StringBuilder sb, long modifiers, boolean isDefault, int defModifier )
  {
    if( isDefault )
    {
      sb.append( "default " );
    }

    if( (modifiers & Modifier.PUBLIC) != 0 )
    {
      sb.append( "public " );
    }
    else if( (modifiers & Modifier.PROTECTED) != 0 )
    {
      sb.append( "protected " );
    }
    else if( (modifiers & Modifier.PRIVATE) != 0 )
    {
      sb.append( "private " );
    }
    else if( defModifier != 0 )
    {
      renderModifiers( sb, defModifier, false, 0 );
    }

    // Canonical order
    if( (modifiers & Modifier.ABSTRACT) != 0 )
    {
      sb.append( "abstract " );
    }
    if( (modifiers & Modifier.STATIC) != 0 )
    {
      sb.append( "static " );
    }
    if( (modifiers & Modifier.FINAL) != 0 )
    {
      sb.append( "final " );
    }
    if( (modifiers & Modifier.TRANSIENT) != 0 )
    {
      sb.append( "transient " );
    }
    if( (modifiers & Modifier.VOLATILE) != 0 )
    {
      sb.append( "volatile " );
    }
    if( (modifiers & Modifier.SYNCHRONIZED) != 0 )
    {
      sb.append( "synchronized " );
    }
    if( (modifiers & Modifier.INTERFACE) != 0 )
    {
      sb.append( "interface " );
    }

    return "";
  }
}
