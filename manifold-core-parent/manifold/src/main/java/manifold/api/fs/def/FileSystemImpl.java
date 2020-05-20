/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.api.fs.def;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.IResource;
import manifold.api.fs.jar.JarFileDirectoryImpl;
import manifold.api.fs.url.URLFileImpl;
import manifold.api.host.IManifoldHost;
import manifold.api.service.BaseService;
import manifold.rt.api.util.ManStringUtil;

public class FileSystemImpl extends BaseService implements IFileSystem
{
  private final IManifoldHost _host;
  private Map<File, IDirectory> _cachedDirInfo;
  private CachingMode _cachingMode;

  private FileSystemImpl.IDirectoryResourceExtractor _iDirectoryResourceExtractor;
  private FileSystemImpl.IFileResourceExtractor _iFileResourceExtractor;

  private final ReentrantLock _lock;

  public FileSystemImpl( IManifoldHost host, CachingMode cachingMode )
  {
    _host = host;
    _cachedDirInfo = new HashMap<>();
    _cachingMode = cachingMode;
    _iDirectoryResourceExtractor = new IDirectoryResourceExtractor();
    _iFileResourceExtractor = new IFileResourceExtractor();
    _lock = new ReentrantLock();
  }

  public IManifoldHost getHost()
  {
    return _host;
  }

  @Override
  public IDirectory getIDirectory( File dir )
  {
    if( dir == null )
    {
      return null;
    }

    dir = normalizeFile( dir );

    IDirectory directory = _cachedDirInfo.get( dir );
    if( directory == null )
    {
      _lock.lock();
      try
      {
        directory = _cachedDirInfo.get( dir );
        if( directory == null )
        {
          directory = createDir( dir );
          _cachedDirInfo.put( dir, directory );
        }
      }
      finally
      {
        _lock.unlock();
      }
    }
    return directory;
  }

  @Override
  public IFile getIFile( File file )
  {
    return file == null ? null : new JavaFileImpl( this, normalizeFile( file ) );
  }

  private static File normalizeFile( File file )
  {
    String absolutePath = file.getAbsolutePath();
    List<String> components = new ArrayList<>();

    boolean reallyNormalized = false;
    int lastIndex = 0;
    for( int i = 0; i < absolutePath.length(); i++ )
    {
      char c = absolutePath.charAt( i );
      if( c == '/' || c == '\\' )
      {
        String component = absolutePath.substring( lastIndex, i );
        if( component.equals( "." ) )
        {
          reallyNormalized = true;
        }
        else if( component.equals( ".." ) )
        {
          components.remove( components.size() - 1 );
          reallyNormalized = true;
        }
        else
        {
          components.add( component );
        }
        lastIndex = i + 1;
      }
    }

    String component = absolutePath.substring( lastIndex );
    if( component.equals( "." ) )
    {
      reallyNormalized = true;
    }
    else if( component.equals( ".." ) )
    {
      components.remove( components.size() - 1 );
      reallyNormalized = true;
    }
    else
    {
      components.add( component );
    }

    return reallyNormalized ? new File( ManStringUtil.join( components, "/" ) ) : file;
  }

  @Override
  public void setCachingMode( CachingMode cachingMode )
  {
    _lock.lock();
    try
    {
      _cachingMode = cachingMode;
      for( IDirectory dir: _cachedDirInfo.values() )
      {
        if( dir instanceof JavaDirectoryImpl )
        {
          ((JavaDirectoryImpl)dir).setCachingMode( cachingMode );
        }
      }
    }
    finally
    {
      _lock.unlock();
    }
  }

  private IDirectory createDir( File dir )
  {
    // PL-21817 in OSGi/Equinox JAR could be named as "bundlefile"
    if( (dir.getName().toLowerCase().endsWith( ".jar" ) || dir.getName().toLowerCase().endsWith( ".zip" ) || dir.getName().equals( "bundlefile" )) && dir.isFile() )
    {
      return new JarFileDirectoryImpl( this, dir );
    }
    else
    {
      return new JavaDirectoryImpl( this, dir, _cachingMode );
    }
  }

  public void clearAllCaches()
  {
    _lock.lock();
    try
    {
      for( IDirectory dir: _cachedDirInfo.values() )
      {
        dir.clearCaches();
      }
    }
    finally
    {
      _lock.unlock();
    }
  }

  static boolean isDirectory( File f )
  {
    return f.isDirectory();
  }

  @Override
  public IDirectory getIDirectory( URL url )
  {
    if( url == null )
    {
      return null;
    }

    return _iDirectoryResourceExtractor.getClassResource( url );
  }

  @Override
  public IFile getIFile( URL url )
  {
    if( url == null )
    {
      return null;
    }

    return _iFileResourceExtractor.getClassResource( url );
  }

  @Override
  public ReentrantLock getLock()
  {
    return _lock;
  }

  private abstract class ResourceExtractor<J extends IResource>
  {
    J getClassResource( URL _url )
    {
      if( _url == null )
      {
        return null;
      }

      switch( _url.getProtocol() )
      {
        case "file":
          return getIResourceFromJavaFile( _url );

        case "jar":
        {
          JarURLConnection urlConnection;
          URL jarFileUrl;
          try
          {
            urlConnection = (JarURLConnection)_url.openConnection();
            jarFileUrl = urlConnection.getJarFileURL();
          }
          catch( IOException e )
          {
            throw new RuntimeException( e );
          }
          File dir = new File( jarFileUrl.getFile() );

          IDirectory jarFileDirectory;

          jarFileDirectory = _cachedDirInfo.get( dir );
          if( jarFileDirectory == null )
          {
            _lock.lock();
            try
            {
              jarFileDirectory = _cachedDirInfo.get( dir );
              if( jarFileDirectory == null )
              {
                jarFileDirectory = createDir( dir );
                _cachedDirInfo.put( dir, jarFileDirectory );
              }
            }
            finally
            {
              _lock.unlock();
            }
          }

          return getIResourceFromJarDirectoryAndEntryName( jarFileDirectory, urlConnection.getEntryName() );
        }

        case "http":
        {
          J res = getIResourceFromURL( _url );
          if( res != null )
          {
            return res;
          }
          break;
        }
      }
      throw new RuntimeException( "Unrecognized protocol: " + _url.getProtocol() );
    }

    abstract J getIResourceFromURL( URL location );

    abstract J getIResourceFromJarDirectoryAndEntryName( IDirectory jarFS, String entryName );

    abstract J getIResourceFromJavaFile( URL location );

    File getFileFromURL( URL url )
    {
      try
      {
        URI uri = url.toURI();
        if( uri.getFragment() != null )
        {
          uri = new URI( uri.getScheme(), uri.getSchemeSpecificPart(), null );
        }
        return new File( uri );
      }
      catch( URISyntaxException ex )
      {
        throw new RuntimeException( ex );
      }
      catch( IllegalArgumentException ex )
      {
        // debug getting IAE only in TH - unable to parse URL with fragment identifier
        throw new IllegalArgumentException( "Unable to parse URL " + url.toExternalForm(), ex );
      }
    }

  }

  private class IFileResourceExtractor extends ResourceExtractor<IFile>
  {

    IFile getIResourceFromJarDirectoryAndEntryName( IDirectory jarFS, String entryName )
    {
      return jarFS.file( entryName );
    }

    IFile getIResourceFromJavaFile( URL location )
    {
      return getHost().getFileSystem().getIFile( getFileFromURL( location ) );
    }

    @Override
    IFile getIResourceFromURL( URL location )
    {
      return new URLFileImpl( FileSystemImpl.this, location );
    }
  }

  private class IDirectoryResourceExtractor extends ResourceExtractor<IDirectory>
  {
    protected IDirectory getIResourceFromJarDirectoryAndEntryName( IDirectory jarFS, String entryName )
    {
      return jarFS.dir( entryName );
    }

    protected IDirectory getIResourceFromJavaFile( URL location )
    {
      return getHost().getFileSystem().getIDirectory( getFileFromURL( location ) );
    }

    @Override
    IDirectory getIResourceFromURL( URL location )
    {
      return null;
    }
  }
}
