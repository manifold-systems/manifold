package manifold.internal.javac;

/**
 */
public class JavacPlugin_NoBootstrapping extends JavacPlugin
{
  @Override
  public String getName()
  {
    return "ManifoldNoBootstrapping";
  }

  @Override
  protected boolean decideIfNoBootstrapping()
  {
    return false;
  }
}
