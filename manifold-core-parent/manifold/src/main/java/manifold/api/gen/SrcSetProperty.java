package manifold.api.gen;

/**
 */
public class SrcSetProperty extends AbstractSrcMethod<SrcSetProperty>
{
  public static final String VALUE_PARAM = "${'$'}value";

  public SrcSetProperty( String name, Class type )
  {
    this( null );
    name( "set" + name );
    type( type );
  }

  public SrcSetProperty( String name, String type )
  {
    this( null );
    name( "set" + name );
    type( type );
  }

  public SrcSetProperty( String name, SrcType type )
  {
    this( null );
    name( "set" + name );
    type( type );
  }

  public SrcSetProperty( SrcClass srcClass )
  {
    super( srcClass );
    returns( "void" );
  }

  public SrcSetProperty type( SrcType type )
  {
    addParam( new SrcParameter( "${'$'}value" ).type( type ) );
    return this;
  }

  public SrcSetProperty type( Class type )
  {
    addParam( new SrcParameter( "${'$'}value" ).type( type ) );
    return this;
  }

  public SrcSetProperty type( String type )
  {
    addParam( new SrcParameter( "${'$'}value" ).type( type ) );
    return this;
  }
}
