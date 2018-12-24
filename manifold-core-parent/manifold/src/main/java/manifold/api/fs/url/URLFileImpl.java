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

package manifold.api.fs.url;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.ResourcePath;

/**
 * HTTP-backed file. The only supported operation is to open stream.
 */
public class URLFileImpl implements IFile
{
  private final IFileSystem _fileSystem;
  private URL _url;

  public URLFileImpl( IFileSystem fs, URL url )
  {
    _fileSystem = fs;
    _url = url;
  }

  @Override
  public IFileSystem getFileSystem()
  {
    return _fileSystem;
  }

  @Override
  public InputStream openInputStream() throws IOException
  {
    return _url.openStream();
  }

  @Override
  public OutputStream openOutputStream() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public OutputStream openOutputStreamForAppend() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getExtension()
  {
    int lastDot = getName().lastIndexOf( "." );
    if( lastDot != -1 )
    {
      return getName().substring( lastDot + 1 );
    }
    else
    {
      return "";
    }
  }

  @Override
  public String getBaseName()
  {
    int lastDot = getName().lastIndexOf( "." );
    if( lastDot != -1 )
    {
      return getName().substring( 0, lastDot );
    }
    else
    {
      return getName();
    }
  }

  @Override
  public IDirectory getParent()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName()
  {
    return getPath().getName();
  }

  @Override
  public boolean exists()
  {
    throw new UnsupportedOperationException();
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
      return _url.toURI();
    }
    catch( URISyntaxException e )
    {
      throw new RuntimeException( "Cannot convert to URI", e );
    }
  }

  @Override
  public ResourcePath getPath()
  {
    return ResourcePath.parse( _url.getPath() );
  }

  @Override
  public boolean isChildOf( IDirectory dir )
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDescendantOf( IDirectory dir )
  {
    throw new UnsupportedOperationException();
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
    return false;
  }

  @Override
  public boolean create()
  {
    throw new UnsupportedOperationException();
  }
}
