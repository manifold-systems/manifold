package manifold.api.fs.jar;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.jar.JarEntry;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IResource;
import manifold.api.fs.ResourcePath;

public abstract class JarEntryResourceImpl implements IResource
{

  protected JarEntry _entry;
  protected IJarFileDirectory _parent;
  protected JarFileDirectoryImpl _jarFile;
  protected String _name;
  private boolean _exists = false;

  protected JarEntryResourceImpl( String name, IJarFileDirectory parent, JarFileDirectoryImpl jarFile )
  {
    _name = name;
    _parent = parent;
    _jarFile = jarFile;
  }

  public void setEntry( JarEntry entry )
  {
    _entry = entry;
    setExists();
  }

  protected void setExists()
  {
    _exists = true;
    if( getParent() instanceof JarEntryResourceImpl )
    {
      ((JarEntryResourceImpl)getParent()).setExists();
    }
  }

  @Override
  public IDirectory getParent()
  {
    return _parent;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public boolean exists()
  {
    return _exists;
  }

  @Override
  public boolean delete() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI toURI()
  {
    try
    {
      return new URI( "jar:" + _jarFile.toURI().toString() + "!/" + getEntryName().replace( " ", "%20" ) );
    }
    catch( URISyntaxException e )
    {
      throw new RuntimeException( e );
    }
  }

  private String getEntryName()
  {
    if( _entry != null )
    {
      return _entry.getName();
    }
    else
    {
      String result = _name;
      IDirectory parent = _parent;
      while( !(parent instanceof JarFileDirectoryImpl) )
      {
        result = parent.getName() + "/" + result;
        parent = parent.getParent();
      }
      return result;
    }
  }

  @Override
  public ResourcePath getPath()
  {
    return _parent.getPath().join( _name );
  }

  @Override
  public boolean isChildOf( IDirectory dir )
  {
    return dir.equals( getParent() );
  }

  @Override
  public boolean isDescendantOf( IDirectory dir )
  {
    return dir.getPath().isDescendant( getPath() );
  }

  @Override
  public File toJavaFile()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isJavaFile()
  {
    return false;
  }

  @Override
  public String toString()
  {
    return getPath().toString();
  }

  @Override
  public boolean equals( Object obj )
  {
    if( obj == this )
    {
      return true;
    }

    if( obj instanceof JarEntryResourceImpl )
    {
      return getPath().equals( ((JarEntryResourceImpl)obj).getPath() );
    }
    else
    {
      return false;
    }
  }

  @Override
  public boolean create()
  {
    return false;
  }

  @Override
  public boolean isInJar()
  {
    return true;
  }
}
