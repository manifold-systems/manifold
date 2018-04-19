package manifold.internal.host;

import java.util.List;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFileSystem;
import manifold.api.host.IModule;

/**
 */
public class DefaultSingleModule extends SimpleModule
{
  private static final String DEFAULT_NAME = "${'$'}default";

  DefaultSingleModule( List<IDirectory> classpath, List<IDirectory> sourcePath, List<IDirectory> outputPath )
  {
    super( classpath, sourcePath, outputPath );
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

  @Override
  public IFileSystem getFileSystem()
  {
    return ManifoldHost.getFileSystem();
  }
}
