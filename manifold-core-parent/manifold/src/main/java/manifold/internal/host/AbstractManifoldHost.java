package manifold.internal.host;

import manifold.api.host.IManifoldHost;
import manifold.api.service.BaseService;

/**
 */
public abstract class AbstractManifoldHost extends BaseService implements IManifoldHost
{
  //## todo: move this to RuntimeManifoldHost after factoring ExtensionManifold#isInnerToJavaClass()
  public ClassLoader getActualClassLoader()
  {
//    if( JavacPlugin.instance() == null )
//    {
//      // runtime
//      return Thread.currentThread().getContextClassLoader();
//    }
//    // compile-time
    return RuntimeManifoldHost.class.getClassLoader();
  }

  public boolean isPathIgnored( String path )
  {
    return false;
  }
}
