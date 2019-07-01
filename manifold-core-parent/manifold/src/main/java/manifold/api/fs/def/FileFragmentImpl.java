/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;
import java.util.function.Supplier;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.ResourcePath;
import manifold.internal.javac.HostKind;

public class FileFragmentImpl implements IFileFragment
{
  private final String _name;
  private final String _ext;
  private final HostKind _hostKind;
  private final IFile _enclosingFile;
  private int _offset;
  private Supplier<Integer> _offsetSupplier;
  private final int _length;
  private final String _content;
  private Object _container;

  public FileFragmentImpl( String name, String ext, HostKind hostKind, IFile enclosingFile, int offset, int length, String content )
  {
    _name = name;
    _ext = ext.toLowerCase();
    _hostKind = hostKind;
    _enclosingFile = enclosingFile;
    _offset = offset;
    _length = length;
    _content = content;
  }

  public String getContent()
  {
    return _content;
  }

  @Override
  public Object getContainer()
  {
    return _container;
  }
  @Override
  public void setContainer( Object container )
  {
    _container = container;
  }

  public HostKind getHostKind()
  {
    return _hostKind;
  }

  @Override
  public IFile getEnclosingFile()
  {
    return _enclosingFile;
  }

  @Override
  public int getOffset()
  {
    return _offsetSupplier != null ? _offsetSupplier.get() : _offset;
  }
  @Override
  public void setOffset( Supplier<Integer> offset )
  {
    _offsetSupplier = offset;
  }

  @Override
  public int getLength()
  {
    return _length;
  }

  @Override
  public InputStream openInputStream()
  {
    return new ByteArrayInputStream( getContent().getBytes() );

//    try( InputStreamReader reader = new InputStreamReader( _enclosingFile.openInputStream() ) )
//    {
//      char[] fragment = new char[_length];
//      int length = reader.read( fragment, _offset, _length );
//      if( length != _length )
//      {
//        throw new IOException( "Expected fragment length " + _length + " but was " + length );
//      }
//      return new ByteArrayInputStream( new String( fragment ).getBytes() );
//    }
  }

  @Override
  public OutputStream openOutputStream()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public OutputStream openOutputStreamForAppend()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getExtension()
  {
    return _ext;
  }

  @Override
  public String getBaseName()
  {
    return _name;
//    return getEnclosingFile().getBaseName() + '#' + _name;
  }

  @Override
  public IFileSystem getFileSystem()
  {
    return getEnclosingFile().getFileSystem();
  }

  @Override
  public IDirectory getParent()
  {
    return _enclosingFile.getParent();
  }

  @Override
  public String getName()
  {
    return getBaseName() + '.' + getExtension();
  }

  @Override
  public boolean exists()
  {
    return getEnclosingFile().exists();
  }

  @Override
  public boolean delete()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public URI toURI()
  {
    // using enclosing file's uri for now because some type manifold Model's report() methods use the uri as file
    return getEnclosingFile().toURI();
//## todo: make a "filefragment" uri scheme like filefragment://C:/path/to/file.json#offset109length24
  }

  @Override
  public ResourcePath getPath()
  {
    return ResourcePath.parse( getParent().getPath().getPathString() + File.separatorChar + getName() );
  }

  @Override
  public boolean isChildOf( IDirectory dir )
  {
    return getEnclosingFile().isChildOf( dir );
  }

  @Override
  public boolean isDescendantOf( IDirectory dir )
  {
    return getEnclosingFile().isDescendantOf( dir );
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
  public boolean isInJar()
  {
    return getEnclosingFile().isInJar();
  }

  @Override
  public boolean create()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }
    FileFragmentImpl that = (FileFragmentImpl)o;
    return Objects.equals( _name, that._name ) &&
           Objects.equals( _ext, that._ext ) &&
           Objects.equals( _enclosingFile, that._enclosingFile );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _name, _ext, _enclosingFile );
  }
}
