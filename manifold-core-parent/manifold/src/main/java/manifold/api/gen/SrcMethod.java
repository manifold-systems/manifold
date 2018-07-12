package manifold.api.gen;

/**
 */
public class SrcMethod extends AbstractSrcMethod<SrcMethod>
{
  public SrcMethod()
  {
    this( null );
  }

  public SrcMethod( SrcClass srcClass )
  {
    this( srcClass, false );
  }
  public SrcMethod( SrcClass srcClass, boolean isCtor )
  {
    super( srcClass );
    setConstructor( isCtor );
    if( !isCtor )
    {
      returns( "void" );
    }
    else
    {
      name( srcClass.getSimpleName() );
    }
  }
}
