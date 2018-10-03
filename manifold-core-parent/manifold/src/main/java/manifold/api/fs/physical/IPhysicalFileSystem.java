package manifold.api.fs.physical;

import java.util.List;
import manifold.api.fs.ResourcePath;

public interface IPhysicalFileSystem
{
  List<? extends IFileMetadata> listFiles( ResourcePath directoryPath );

  IFileMetadata getFileMetadata( ResourcePath filePath );

  boolean exists( ResourcePath filePath );

  boolean delete( ResourcePath filePath );

  boolean mkdir( ResourcePath dirPath );

  void clearDirectoryCaches( ResourcePath dirPath );

  void clearAllCaches();
}
