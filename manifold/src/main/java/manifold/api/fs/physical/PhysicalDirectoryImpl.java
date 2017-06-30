package manifold.api.fs.physical;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IDirectoryUtil;
import manifold.api.fs.IFile;
import manifold.api.fs.IResource;
import manifold.api.fs.ResourcePath;

public class PhysicalDirectoryImpl extends PhysicalResourceImpl implements IDirectory
{

  public PhysicalDirectoryImpl( ResourcePath path, IPhysicalFileSystem backingFileSystem )
  {
    super( path, backingFileSystem );
  }

  @Override
  public void clearCaches()
  {
    // No-op at this level
  }

  @Override
  public IDirectory dir( String relativePath )
  {
    ResourcePath absolutePath = _path.join( relativePath );
    return new PhysicalDirectoryImpl( absolutePath, _backingFileSystem );
  }

  @Override
  public IFile file( String path )
  {
    ResourcePath absolutePath = _path.join( path );
    return new PhysicalFileImpl( absolutePath, _backingFileSystem );
  }

  @Override
  public boolean mkdir() throws IOException
  {
    return _backingFileSystem.mkdir( _path );
  }

  @Override
  public List<? extends IDirectory> listDirs()
  {
    List<IDirectory> dirs = new ArrayList<IDirectory>();
    for( IFileMetadata fm : _backingFileSystem.listFiles( _path ) )
    {
      if( fm.isDir() )
      {
        dirs.add( new PhysicalDirectoryImpl( _path.join( fm.name() ), _backingFileSystem ) );
      }
    }

    return dirs;
  }

  @Override
  public List<? extends IFile> listFiles()
  {
    List<IFile> files = new ArrayList<IFile>();
    for( IFileMetadata fm : _backingFileSystem.listFiles( _path ) )
    {
      if( fm.isFile() )
      {
        files.add( new PhysicalFileImpl( _path.join( fm.name() ), _backingFileSystem ) );
      }
    }

    return files;
  }

  @Override
  public String relativePath( IResource resource )
  {
    return IDirectoryUtil.relativePath( this, resource );
  }

  @Override
  public boolean hasChildFile( String path )
  {
    IFile childFile = file( path );
    return childFile != null && childFile.exists();
  }

  @Override
  public boolean isAdditional()
  {
    return false;
  }
}
