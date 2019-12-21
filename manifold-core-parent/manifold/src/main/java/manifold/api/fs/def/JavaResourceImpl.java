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
import java.io.Serializable;
import java.net.URI;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.IResource;
import manifold.api.fs.ResourcePath;

public abstract class JavaResourceImpl implements IResource, Serializable
{
  private final IFileSystem _fileSystem;
  protected File _file;
  private String _name;
  private IDirectory _parent;
  private URI _uri;
  private ResourcePath _path;
  private int _hash;

  protected JavaResourceImpl( IFileSystem fileSystem, File file )
  {
    _fileSystem = fileSystem;
    _file = file.getAbsoluteFile();
  }

  @Override
  public IFileSystem getFileSystem()
  {
    return _fileSystem;
  }

  @Override
  public IDirectory getParent()
  {
    if( _parent == null )
    {
      File parentFile = _file.getParentFile();
      if( parentFile == null )
      {
        return null;
      }
      else
      {
        _parent = getFileSystem().getIDirectory( parentFile );
      }
    }
    return _parent;
  }

  @Override
  public String getName()
  {
    return _name == null ? _name = _file.getName() : _name;
  }

  @Override
  public boolean delete() throws IOException
  {
    return _file.delete();
  }

  @Override
  public URI toURI()
  {
    return _uri == null ? _uri = _file.toURI() : _uri;
  }

  @Override
  public ResourcePath getPath()
  {
    return _path == null ? _path = ResourcePath.parse( _file.getAbsolutePath() ) : _path;
  }

  @Override
  public boolean isChildOf( IDirectory dir )
  {
    return dir.equals( getParent() );
  }

  @Override
  public boolean isDescendantOf( IDirectory dir )
  {
    if( !(dir instanceof JavaDirectoryImpl) )
    {
      return false;
    }
    File javadir = ((JavaDirectoryImpl)dir)._file;
    File javafile = _file.getParentFile();
    while( javafile != null )
    {
      if( javafile.equals( javadir ) )
      {
        return true;
      }
      javafile = javafile.getParentFile();
    }
    return false;
  }

  @Override
  public File toJavaFile()
  {
    return _file;
  }

  @Override
  public boolean isJavaFile()
  {
    return true;
  }

  @Override
  public boolean equals( Object obj )
  {
    if( obj instanceof JavaResourceImpl )
    {
      return _file.equals( ((JavaResourceImpl)obj)._file );
    }
    else
    {
      return false;
    }
  }

  @Override
  public int hashCode()
  {
    return _hash == 0 ? _hash = _file.hashCode() : _hash;
  }

  @Override
  public String toString()
  {
    return _file.toString();
  }

  @Override
  public boolean create()
  {
    return false;
  }

  @Override
  public boolean isInJar()
  {
    return false;
  }
}
