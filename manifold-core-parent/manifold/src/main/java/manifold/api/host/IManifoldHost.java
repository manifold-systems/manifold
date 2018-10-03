package manifold.api.host;

import manifold.api.fs.IFileSystem;
import manifold.api.service.IService;
import manifold.internal.javac.JavaParser;

/**
 * Implementors of this interface drive Manifold in a custom way based
 * on the environment employing Manifold's services.  These include:
 * <ul>
 * <li>Runtime class loaders - core Manifold</li>
 * <li>Compilers - the Manifold javac plugin</li>
 * <li>IDEs - the Manifold IntelliJ IDEA plugin</li>
 * </ul>
 */
public interface IManifoldHost extends IService
{
  ClassLoader getActualClassLoader();

  IModule getSingleModule();

  boolean isPathIgnored( String path );

  void addTypeLoaderListenerAsWeakRef( Object ctx, ITypeLoaderListener listener );

  IFileSystem getFileSystem();

  JavaParser getJavaParser();
}
