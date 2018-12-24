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

package manifold.internal.javac;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;
import manifold.api.fs.IDirectory;
import manifold.api.host.IManifoldHost;

/**
 * A utility for other compilers hosting Manifold, primarily for exposing class files as JavaFileObjects
 * where APIs require it.
 */
public class WriterJavaFileObject extends SimpleJavaFileObject
{
  private OutputStream _outputStream;

  public WriterJavaFileObject( IManifoldHost host, String fqn )
  {
    super( getUriFrom( host, fqn ), Kind.CLASS );
  }

  public WriterJavaFileObject( IManifoldHost host, String pkg, String filename )
  {
    super( getUriFrom( host, pkg, filename ), Kind.OTHER );
  }

  private static URI getUriFrom( IManifoldHost host, String fqn )
  {
    final String outRelativePath = fqn.replace( '.', File.separatorChar ) + ".class";
    IDirectory outputPath = host.getSingleModule().getOutputPath().stream().findFirst().orElse( null );
    File file = new File( outputPath.getPath().getFileSystemPathString(), outRelativePath );
    return file.toURI();
  }

  private static URI getUriFrom( IManifoldHost host, String fqn, String filename )
  {
    final String outRelativePath = fqn.replace( '.', File.separatorChar ) + File.separatorChar + filename;
    IDirectory outputPath = host.getSingleModule().getOutputPath().stream().findFirst().orElse( null );
    File file = new File( outputPath.getPath().getFileSystemPathString(), outRelativePath );
    return file.toURI();
  }

  @Override
  public OutputStream openOutputStream() throws IOException
  {
    throwIfInUse();
    synchronized( this )
    {
      throwIfInUse();
      File file = new File( toUri() );
      if( !file.isFile() )
      {
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
      }
      return _outputStream = new BufferedOutputStream( new FileOutputStream( file ) );
    }
  }

  private void throwIfInUse() throws IOException
  {
    if( _outputStream != null )
    {
      throw new IOException( "OutputStream in use" );
    }
  }

  @Override
  public InputStream openInputStream() throws IOException
  {
    throw new UnsupportedOperationException();
  }
}
