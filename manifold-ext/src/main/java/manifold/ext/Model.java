package manifold.ext;

import java.util.HashSet;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.type.IModel;

/**
 */
public class Model implements IModel
{
  private final ExtensionManifold _sp;
  private final String _fqnExtended;
  private Set<IFile> _files;

  Model( String extendedFqn, Set<IFile> files, ExtensionManifold sp )
  {
    _fqnExtended = extendedFqn;
    _files = new HashSet<>( files );
    _sp = sp;
  }

  @Override
  public String getFqn()
  {
    return _fqnExtended;
  }

  @Override
  public Set<IFile> getFiles()
  {
    if( _files == null )
    {
      _files = new HashSet<>();
    }
    return _files;
  }

  @Override
  public void addFile( IFile file )
  {
    if( !getFiles().add( file ) )
    {
      // do not throw here, file creation events sometimes come in late after the
      // model is created with a newly created file e.g., the Create Extension Class dialog
      //
      // throw new IllegalStateException( "Model already contains " + file.getName() );
    }
  }

  @Override
  public void removeFile( IFile file )
  {
    if( !getFiles().remove( file ) )
    {
      throw new IllegalStateException( "Model does not contain " + file.getName() );
    }
  }

  @Override
  public void updateFile( IFile file )
  {
    getFiles().remove( file );
    getFiles().add( file );
  }

  ExtensionManifold getTypeManifold()
  {
    return _sp;
  }

  void report( DiagnosticListener<JavaFileObject> errorHandler )
  {

  }
}
