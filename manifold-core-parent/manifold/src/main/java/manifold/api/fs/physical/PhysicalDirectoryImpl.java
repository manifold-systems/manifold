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

package manifold.api.fs.physical;

import java.util.ArrayList;
import java.util.List;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IDirectoryUtil;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.IResource;
import manifold.api.fs.ResourcePath;

public class PhysicalDirectoryImpl extends PhysicalResourceImpl implements IDirectory
{
  public PhysicalDirectoryImpl( IFileSystem fs, ResourcePath path, IPhysicalFileSystem backingFileSystem )
  {
    super( fs, path, backingFileSystem );
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
    return new PhysicalDirectoryImpl( getFileSystem(), absolutePath, _backingFileSystem );
  }

  @Override
  public IFile file( String path )
  {
    ResourcePath absolutePath = _path.join( path );
    return new PhysicalFileImpl( getFileSystem(), absolutePath, _backingFileSystem );
  }

  @Override
  public boolean mkdir()
  {
    return _backingFileSystem.mkdir( _path );
  }

  @Override
  public List<? extends IDirectory> listDirs()
  {
    List<IDirectory> dirs = new ArrayList<>();
    for( IFileMetadata fm : _backingFileSystem.listFiles( _path ) )
    {
      if( fm.isDir() )
      {
        dirs.add( new PhysicalDirectoryImpl( getFileSystem(), _path.join( fm.name() ), _backingFileSystem ) );
      }
    }

    return dirs;
  }

  @Override
  public List<? extends IFile> listFiles()
  {
    List<IFile> files = new ArrayList<>();
    for( IFileMetadata fm : _backingFileSystem.listFiles( _path ) )
    {
      if( fm.isFile() )
      {
        files.add( new PhysicalFileImpl( getFileSystem(), _path.join( fm.name() ), _backingFileSystem ) );
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
