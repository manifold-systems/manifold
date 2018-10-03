package manifold.internal.host;

import java.util.List;
import manifold.api.fs.IDirectory;
import manifold.api.host.IManifoldHost;
import manifold.api.host.IModule;

/**
 */
public class DefaultSingleModule extends SimpleModule
{
  private static final String DEFAULT_NAME = "$default";

  DefaultSingleModule( IManifoldHost host, List<IDirectory> classpath, List<IDirectory> sourcePath, List<IDirectory> outputPath )
  {
    super( host, classpath, sourcePath, outputPath );
  }

  @Override
  public String getName()
  {
    return DEFAULT_NAME;
  }

  @Override
  public IModule getModule()
  {
    return this;
  }
}
