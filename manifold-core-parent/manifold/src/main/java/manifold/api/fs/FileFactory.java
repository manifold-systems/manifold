package manifold.api.fs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import manifold.api.fs.jar.JarFileDirectoryImpl;
import manifold.api.fs.physical.IPhysicalFileSystem;
import manifold.api.fs.physical.PhysicalDirectoryImpl;
import manifold.api.fs.physical.PhysicalFileImpl;
import manifold.api.fs.physical.fast.FastPhysicalFileSystem;

public class FileFactory
{
  private IFileSystem _fs;
  private IPhysicalFileSystem _defaultPhysicalFileSystem;

  public FileFactory( IFileSystem fs )
  {
    _fs = fs;
    _defaultPhysicalFileSystem = createDefaultPhysicalFileSystem();
  }

  public IFileSystem getFileSystem()
  {
    return _fs;
  }

  public IDirectory getIDirectory( File f )
  {
    if( f.getName().endsWith( ".jar" ) && f.isFile() )
    {
      return new JarFileDirectoryImpl( getFileSystem(), f );
    }
    else
    {
      return new PhysicalDirectoryImpl( getFileSystem(), ResourcePath.parse( f.getAbsolutePath() ), _defaultPhysicalFileSystem );
    }
  }

  public IFile getIFile( File f )
  {
    return new PhysicalFileImpl( getFileSystem(), ResourcePath.parse( f.getAbsolutePath() ), _defaultPhysicalFileSystem );
  }

  public IDirectory getIDirectory( String absolutePath )
  {
    if( absolutePath.endsWith( ".jar" ) && new File( absolutePath ).isFile() )
    {
      return new JarFileDirectoryImpl( _fs, new File( absolutePath ) );
    }
    else
    {
      return new PhysicalDirectoryImpl( getFileSystem(), ResourcePath.parse( absolutePath ), _defaultPhysicalFileSystem );
    }
  }

  public IFile getIFile( String absolutePath )
  {
    return new PhysicalFileImpl( getFileSystem(), ResourcePath.parse( absolutePath ), _defaultPhysicalFileSystem );
  }

  public IFile getIFile( URL url )
  {
    try
    {
      return getIFile( url.toURI() );
    }
    catch( URISyntaxException e )
    {
      throw new RuntimeException( e );
    }
  }

  public IFile getIFile( URI uri )
  {
    return getIFile( uri, true );
  }

  public IFile getIFile( URL url, boolean bCreateIfNotExists )
  {
    try
    {
      return getIFile( url.toURI(), bCreateIfNotExists );
    }
    catch( URISyntaxException e )
    {
      throw new RuntimeException( e );
    }
  }

  public IFile getIFile( URI uri, boolean bCreateIfNotExists )
  {
    if( uri.getScheme().equals( "file" ) )
    {
      try
      {
        if( uri.getFragment() != null )
        {
          uri = new URI( uri.getScheme(), uri.getSchemeSpecificPart(), null );
        }
        return getIFile( new File( uri ) );
      }
      catch( URISyntaxException ex )
      {
        throw new RuntimeException( ex );
      }
      catch( IllegalArgumentException ex )
      {
        // debug getting IAE only in TH - unable to parse URL with fragment identifier
        throw new IllegalArgumentException( "Unable to parse URI " + uri, ex );
      }
    }
    else if( uri.getScheme().equals( "jar" ) )
    {
      String path = uri.getPath();
      path = path == null ? uri.getSchemeSpecificPart() : path;
      int idx = path.lastIndexOf( '!' );
      String filePath = path.substring( idx + 1 );
      String jarPath = path.substring( 0, idx );
      File jarFile;
      try
      {
        jarFile = getIFile( new URL( jarPath ) ).toJavaFile();
      }
      catch( MalformedURLException e )
      {
        throw new RuntimeException( e );
      }
      JarFileDirectoryImpl jarFileDirectory = new JarFileDirectoryImpl( _fs, jarFile );

      if( bCreateIfNotExists )
      {
        return jarFileDirectory.getOrCreateFile( filePath );
      }
      return jarFileDirectory.file( filePath );
    }
    else
    {
      throw new RuntimeException( "Unrecognized protocol: " + uri.getScheme() );
    }
  }

  public IPhysicalFileSystem getDefaultPhysicalFileSystem()
  {
    return _defaultPhysicalFileSystem;
  }

  public IPhysicalFileSystem getRootPhysicalFileSystem()
  {
    return _defaultPhysicalFileSystem;
  }

  public void setDefaultPhysicalFileSystem( IPhysicalFileSystem fileSystem )
  {
    _defaultPhysicalFileSystem = fileSystem;
  }

  // ---------------------------- Private Implementation Methods

  private IPhysicalFileSystem createDefaultPhysicalFileSystem()
  {
    return new FastPhysicalFileSystem();
  }
}
