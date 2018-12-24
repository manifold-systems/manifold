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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.IFileUtil;

public class JavaFileImpl extends JavaResourceImpl implements IFile
{
  public JavaFileImpl( IFileSystem fs, File file )
  {
    super( fs, file );
  }

  @Override
  public InputStream openInputStream() throws IOException
  {
    return new FileInputStream( _file );
  }

  @Override
  public OutputStream openOutputStream() throws IOException
  {
    return new FileOutputStream( _file );
  }

  @Override
  public OutputStream openOutputStreamForAppend() throws IOException
  {
    return new FileOutputStream( _file, true );
  }

  @Override
  public String getExtension()
  {
    return IFileUtil.getExtension( this );
  }

  @Override
  public String getBaseName()
  {
    return IFileUtil.getBaseName( this );
  }

  @Override
  public boolean create()
  {
    try
    {
      return _file.createNewFile();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  @Override
  public boolean exists()
  {
    return _file.isFile();
  }
}
