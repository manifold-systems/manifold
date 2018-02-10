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
  protected boolean decideIfStatic( String[] args )
  {
    return true;
  }
}
