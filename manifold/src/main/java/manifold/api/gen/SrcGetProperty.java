package manifold.api.gen;

/**
 */
public class SrcGetProperty extends AbstractSrcMethod<SrcGetProperty>
{
  public SrcGetProperty( String name, Class type )
  {
     this( null );
     name( "get"+name );
     returns( type );
  }
  public SrcGetProperty( String name, String type )
  {
     this( null );
     name( "get"+name );
     returns( type );
  }
  public SrcGetProperty( String name, SrcType type )
  {
     this( null );
     name( "get"+name );
     returns( type );
  }
  public SrcGetProperty( SrcClass srcClass )
  {
     super( srcClass );
  }
}
