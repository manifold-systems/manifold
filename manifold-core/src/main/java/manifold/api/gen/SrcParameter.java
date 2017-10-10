package manifold.api.gen;

/**
 */
public class SrcParameter extends SrcAnnotated<SrcParameter>
{
  private SrcType _type;

  public SrcParameter( String name )
  {
    name( name );
  }

  public SrcParameter( String name, Class type )
  {
    name( name );
    type( type );
  }

  public SrcParameter( String name, String type )
  {
    name( name );
    type( type );
  }

  public SrcParameter( String name, SrcType type )
  {
    name( name );
    type( type );
  }

  public SrcParameter type( SrcType type )
  {
    _type = type;
    return this;
  }

  public SrcParameter type( Class type )
  {
    _type = new SrcType( type );
    return this;
  }

  public SrcParameter type( String type )
  {
    _type = new SrcType( type );
    return this;
  }

  public SrcType getType()
  {
    return _type;
  }

  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, false );
  }
  public StringBuilder render( StringBuilder sb, int indent, boolean varArgs )
  {
    return render( sb, indent, varArgs, false );
  }
  public StringBuilder render( StringBuilder sb, int indent, boolean varArgs, boolean forSignature )
  {
    if( !forSignature )
    {
      renderAnnotations( sb, 0, true );
      renderModifiers( sb, false, 0 );
    }
    if( varArgs )
    {
      _type.getComponentType().render( sb, 0 ).append( "..." );
    }
    else
    {
      _type.render( sb, 0 );
    }
    if( !forSignature )
    {
      sb.append( ' ' ).append( getSimpleName() );
    }
    return sb;
  }
}
