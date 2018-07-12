package manifold.api.gen;

/**
 */
public class SrcConstructor extends AbstractSrcMethod<SrcConstructor>
{
  public SrcConstructor( SrcClass owner )
  {
    super( owner );
    if( owner != null )
    {
      name( owner.getSimpleName() );
    }
  }

  public SrcConstructor()
  {
    this( null );
  }

  @Override
  public boolean isConstructor()
  {
    return true;
  }

  @Override
  public void setOwner( SrcAnnotated owner )
  {
    super.setOwner( owner );
    if( owner != null )
    {
      name( owner.getSimpleName() );
    }
  }
}
