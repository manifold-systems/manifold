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

package manifold.api.fs.jar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IDirectoryUtil;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.IResource;

public class JarEntryDirectoryImpl extends JarEntryResourceImpl implements IJarFileDirectory
{
  private Map<String, JarEntryDirectoryImpl> _directories = new HashMap<>();
  private Map<String, JarEntryFileImpl> _files = new HashMap<>();
  private List<IDirectory> _childDirs = new ArrayList<>();
  private List<IFile> _childFiles = new ArrayList<>();

  public JarEntryDirectoryImpl( IFileSystem fs, String name, IJarFileDirectory parent, JarFileDirectoryImpl jarFile )
  {
    super( fs, name, parent, jarFile );
  }

  @Override
  public JarEntryDirectoryImpl getOrCreateDirectory( String relativeName )
  {
    JarEntryDirectoryImpl result = _directories.get( relativeName );
    if( result == null )
    {
      result = new JarEntryDirectoryImpl( getFileSystem(), relativeName, this, _jarFile );
      _directories.put( relativeName, result );
      _childDirs.add( result );
    }
    return result;
  }

  @Override
  public JarEntryFileImpl getOrCreateFile( String relativeName )
  {
    JarEntryFileImpl result = _files.get( relativeName );
    if( result == null )
    {
      result = new JarEntryFileImpl( getFileSystem(), relativeName, this, _jarFile );
      _files.put( relativeName, result );
      _childFiles.add( result );
    }
    return result;
  }

  @Override
  public IDirectory dir( String relativePath )
  {
    return IDirectoryUtil.dir( this, relativePath );
  }

  @Override
  public IFile file( String path )
  {
    return IDirectoryUtil.file( this, path );
  }

  @Override
  public boolean mkdir()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<? extends IDirectory> listDirs()
  {
    List<IDirectory> results = new ArrayList<>();
    for( IDirectory child : _childDirs )
    {
      if( child.exists() )
      {
        results.add( child );
      }
    }
    return results;
  }

  @Override
  public List<? extends IFile> listFiles()
  {
    List<IFile> results = new ArrayList<>();
    for( IFile child : _childFiles )
    {
      if( child.exists() )
      {
        results.add( child );
      }
    }
    return results;
  }

  @Override
  public String relativePath( IResource resource )
  {
    return IDirectoryUtil.relativePath( this, resource );
  }

  @Override
  public void clearCaches()
  {
    // Do nothing
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
