package manifold.api.fs;

import java.io.File;
import java.net.URL;
import manifold.api.host.IManifoldHost;
import manifold.api.service.IService;

public interface IFileSystem extends IService
{
  IManifoldHost getHost();

  FileFactory getFileFactory();

  IDirectory getIDirectory( File dir );

  IFile getIFile( File file );

  void setCachingMode( CachingMode cachingMode );

  void clearAllCaches();

  IDirectory getIDirectory( URL url );

  IFile getIFile( URL url );

//  IFile getFakeFile( URL url, IModule module );

  enum CachingMode
  {
    NO_CACHING,
    CHECK_TIMESTAMPS,
    FUZZY_TIMESTAMPS,
    FULL_CACHING
  }
}
