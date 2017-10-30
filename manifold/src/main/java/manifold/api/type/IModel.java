package manifold.api.type;

import java.util.Set;
import manifold.api.fs.IFile;

/**
 */
public interface IModel
{
  String getFqn();

  Set<IFile> getFiles();

  void addFile( IFile file );

  void removeFile( IFile file );

  void updateFile( IFile file );
}
