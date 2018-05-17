package manifold.internal.javac;

/**
 */
public class JavacPlugin_Static extends JavacPlugin
{
  @Override
  public String getName()
  {
    return "ManifoldStatic";
  }

  @Override
  protected boolean testForArg( String name, String[] args )
  {
    if( name.equals( "static" ) )
    {
      return true;
    }
    return super.testForArg( name, args );
  }
}
