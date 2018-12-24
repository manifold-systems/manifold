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

package manifold.api.fs.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileUtil;
import manifold.api.host.AbstractTypeSystemListener;
import manifold.api.host.IModule;
import manifold.api.host.RefreshRequest;
import manifold.util.JsonUtil;
import manifold.util.ManClassUtil;
import manifold.util.cache.FqnCache;
import manifold.util.concurrent.ConcurrentHashSet;

/**
 */
public class PathCache
{
  @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
  private CacheClearer _clearer;
  private IModule _module;
  private final Supplier<Collection<IDirectory>> _pathSupplier;
  private final Runnable _clearHandler;
  private Map<IFile, Set<String>> _reverseMap;
  private Map<String, FqnCache<IFile>> _filesByExtension;

  public PathCache( IModule module, Supplier<Collection<IDirectory>> pathSupplier, Runnable clearHandler )
  {
    _module = module;
    _pathSupplier = pathSupplier;
    _clearHandler = clearHandler;
    _reverseMap = new ConcurrentHashMap<>();
    init();
    _module.getHost().addTypeSystemListenerAsWeakRef( module, _clearer = new CacheClearer() );
  }

  private void init()
  {
    Map<String, FqnCache<IFile>> filesByExtension = new ConcurrentHashMap<>();
    for( IDirectory sourceEntry : _pathSupplier.get() )
    {
      if( IFileUtil.hasSourceFiles( sourceEntry ) )
      {
        addFilesInDir( "", sourceEntry, filesByExtension );
      }
    }
    _filesByExtension = filesByExtension;
  }

  @SuppressWarnings("unused")
  public Set<IFile> findFiles( String fqn )
  {
    Set<IFile> result = Collections.emptySet();
    for( String ext : _filesByExtension.keySet() )
    {
      IFile file = _filesByExtension.get( ext ).get( fqn );
      if( file != null )
      {
        if( result.isEmpty() )
        {
          result = new HashSet<>( 2 );
        }
        result.add( file );
      }
    }
    return result;
  }

  public FqnCache<IFile> getExtensionCache( String extension )
  {
    FqnCache<IFile> extCache = _filesByExtension.get( extension.toLowerCase() );
    if( extCache == null )
    {
      _filesByExtension.put( extension, extCache = new FqnCache<>() );
    }
    return extCache;
  }

  public Map<String, FqnCache<IFile>> getExtensionCaches()
  {
    return _filesByExtension;
  }

  public Set<String> getFqnForFile( IFile file )
  {
    return _reverseMap.get( file );
  }

  private void addFilesInDir( String relativePath, IDirectory dir, Map<String, FqnCache<IFile>> filesByExtension )
  {
    if( !_module.getHost().isPathIgnored( relativePath ) )
    {
      for( IFile file : dir.listFiles() )
      {
        String simpleName = file.getName();
        int iDot = simpleName.lastIndexOf( '.' );
        if( iDot > 0 )
        {
          simpleName = simpleName.substring( 0, iDot );
        }
        String fqn = appendResourceNameToPath( relativePath, simpleName );
        addToExtension( fqn, file, filesByExtension );
        addToReverseMap( file, fqn );
      }
      for( IDirectory subdir : dir.listDirs() )
      {
        if( isValidPackage( subdir ) )
        {
          String fqn = appendResourceNameToPath( relativePath, subdir.getName() );
          addFilesInDir( fqn, subdir, filesByExtension );
        }
      }
    }
  }

  private boolean isValidPackage( IDirectory subdir )
  {
    // Exclude directories that are not actual packages such as META-INF that exist in jar files
    //## todo: also consider excluding a user-defined list of directories e.g., the ../config directories for XCenter
    return ManClassUtil.isJavaIdentifier( subdir.getName() );
  }

  private void addToExtension( String fqn, IFile file, Map<String, FqnCache<IFile>> filesByExtension )
  {
    String ext = file.getExtension().toLowerCase();
    FqnCache<IFile> cache = filesByExtension.get( ext );
    if( cache == null )
    {
      filesByExtension.put( ext, cache = new FqnCache<>() );
    }
    if( !cache.contains( fqn ) )
    {
      // add only if absent; respect class/sourcepath order
      cache.add( fqn, file );
    }
  }

  private void removeFromExtension( String fqn, IFile file, Map<String, FqnCache<IFile>> filesByExtension )
  {
    String ext = file.getExtension().toLowerCase();
    FqnCache<IFile> cache = filesByExtension.get( ext );
    if( cache != null )
    {
      cache.remove( fqn );
    }
  }

  private String appendResourceNameToPath( String relativePath, String resourceName )
  {
    String path;
    if( relativePath.length() > 0 )
    {
      path = relativePath + '.' + JsonUtil.makeIdentifier( resourceName );
    }
    else
    {
      path = resourceName;
    }
    return path;
  }

  private void removeFromReverseMap( IFile file, String fqn )
  {
    Set<String> fqns = _reverseMap.get( file );
    if( fqns != null )
    {
      fqns.remove( fqn );
    }
  }

  private void addToReverseMap( IFile file, String fqn )
  {
    Set<String> fqns = _reverseMap.get( file );
    if( fqns == null )
    {
      _reverseMap.put( file, fqns = new ConcurrentHashSet<>() );
    }
    fqns.add( fqn );
  }

  public void clear()
  {
    _filesByExtension.clear();
    _reverseMap = new ConcurrentHashMap<>();
  }

  private class CacheClearer extends AbstractTypeSystemListener
  {
    @Override
    public boolean notifyEarly()
    {
      return true;
    }

    @Override
    public void refreshed()
    {
      clear();
      _clearHandler.run();
    }

    @Override
    public void refreshedTypes( RefreshRequest request )
    {
      IModule refreshModule = request.module;
      if( refreshModule != null && refreshModule != _module )
      {
        return;
      }

      switch( request.kind )
      {
        case CREATION:
        {
          Arrays.stream( request.types ).forEach(
            fqn ->
            {
              addToReverseMap( request.file, fqn );
              addToExtension( fqn, request.file, _filesByExtension );
            } );
          break;
        }

        case DELETION:
        {
          Arrays.stream( request.types ).forEach(
            fqn ->
            {
              removeFromReverseMap( request.file, fqn );
              removeFromExtension( fqn, request.file, _filesByExtension );
            } );
          break;
        }

        case MODIFICATION:
          break;
      }
    }

  }
}
