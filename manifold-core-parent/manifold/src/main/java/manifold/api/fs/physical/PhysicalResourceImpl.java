package manifold.api.fs.physical;

import java.io.File;
import java.net.URI;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.IResource;
import manifold.api.fs.ResourcePath;

public class PhysicalResourceImpl implements IResource
{
  protected final ResourcePath _path;
  protected final IPhysicalFileSystem _backingFileSystem;
  private final IFileSystem _fs;

  protected PhysicalResourceImpl( IFileSystem fs, ResourcePath path, IPhysicalFileSystem backingFileSystem )
  {
    _fs = fs;
    _path = path;
    _backingFileSystem = backingFileSystem;
  }

  @Override
  public IFileSystem getFileSystem()
  {
    return _fs;
  }

  @Override
  public IDirectory getParent()
  {
    if( _path.getParent() == null )
    {
      return null;
    }
    else
    {
      return new PhysicalDirectoryImpl( getFileSystem(), _path.getParent(), _backingFileSystem );
    }
  }

  @Override
  public String getName()
  {
    return _path.getName();
  }

  @Override
  public boolean exists()
  {
    return getIFileMetadata().exists();
  }

  @Override
  public boolean delete()
  {
    return _backingFileSystem.delete( _path );
  }

  @Override
  public URI toURI()
  {
    return toJavaFile().toURI();
  }

  @Override
  public ResourcePath getPath()
  {
    return _path;
  }

  @Override
  public boolean isChildOf( IDirectory dir )
  {
    return dir.getPath().isChild( _path );
  }

  @Override
  public boolean isDescendantOf( IDirectory dir )
  {
    return dir.getPath().isDescendant( _path );
  }

  @Override
  public File toJavaFile()
  {
    return new File( _path.getPathString() );
  }

  @Override
  public boolean isJavaFile()
  {
    return true;
  }

  @Override
  public boolean isInJar()
  {
    return false;
  }

  @Override
  public boolean create()
  {
    return false;
  }

  @Override
  public boolean equals( Object obj )
  {
    if( obj instanceof IResource )
    {
      return _path.equals( ((IResource)obj).getPath() );
    }
    else
    {
      return false;
    }
  }

  @Override
  public String toString()
  {
    return getPath().getFileSystemPathString();
  }

  protected IFileMetadata getIFileMetadata()
  {
    return _backingFileSystem.getFileMetadata( _path );
  }
}
