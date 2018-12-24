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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;

public class JarEntryFileImpl extends JarEntryResourceImpl implements IFile
{

  public JarEntryFileImpl( IFileSystem fs, String name, IJarFileDirectory parent, JarFileDirectoryImpl jarFile )
  {
    super( fs, name, parent, jarFile );
  }

  @Override
  public InputStream openInputStream() throws IOException
  {
    if( _entry == null )
    {
      throw new IOException();
    }
    return _jarFile.getInputStream( _entry );
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
    int lastDot = _name.lastIndexOf( "." );
    if( lastDot != -1 )
    {
      return _name.substring( lastDot + 1 );
    }
    else
    {
      return "";
    }
  }

  @Override
  public String getBaseName()
  {
    int lastDot = _name.lastIndexOf( "." );
    if( lastDot != -1 )
    {
      return _name.substring( 0, lastDot );
    }
    else
    {
      return _name;
    }
  }

  @Override
  public boolean isInJar()
  {
    return true;
  }

  @Override
  public boolean create()
  {
    throw new RuntimeException( "Not supported" );
  }
}
