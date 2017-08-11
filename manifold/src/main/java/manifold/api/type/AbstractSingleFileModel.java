package manifold.api.type;

import java.util.HashSet;
import java.util.Set;
import manifold.api.fs.IFile;

/**
 */
public abstract class AbstractSingleFileModel implements IModel
{
  private String _fqn;
  private Set<IFile> _files;

  public AbstractSingleFileModel( String fqn, Set<IFile> files )
  {
    _fqn = fqn;
    _files = new HashSet<>( files );
  }

  @Override
  public String getFqn()
  {
    return _fqn;
  }

  @Override
  public Set<IFile> getFiles()
  {
    return _files;
  }

  public IFile getFile()
  {
    return _files.iterator().next();
  }

  @Override
  public void addFile( IFile file )
  {
    // tolerate adding a file even though this is a single file model
    // the idea is to issue a warning during compilation if this model has more than one file
    _files.add( file );
  }

  @Override
  public void removeFile( IFile file )
  {
    // tolerate adding a file even though this is a sisngle file model
    // the idea is to issue a warning during compilation if this model has more than one file
    _files.remove( file );
  }

  @Override
  public void updateFile( IFile file )
  {
    _files.remove( file );
    _files.add( file );
  }
}
