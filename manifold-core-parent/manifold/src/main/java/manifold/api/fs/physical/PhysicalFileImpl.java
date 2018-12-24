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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.ResourcePath;

public class PhysicalFileImpl extends PhysicalResourceImpl implements IFile
{
  public PhysicalFileImpl( IFileSystem fs, ResourcePath path, IPhysicalFileSystem backingFileSystem )
  {
    super( fs, path, backingFileSystem );
  }

  @Override
  public InputStream openInputStream() throws IOException
  {
    return new FileInputStream( toJavaFile() );
  }

  @Override
  public OutputStream openOutputStream() throws IOException
  {
    return new FileOutputStream( toJavaFile() );
  }

  @Override
  public OutputStream openOutputStreamForAppend() throws IOException
  {
    return new FileOutputStream( toJavaFile(), true );
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
  public boolean create()
  {
    try
    {
      return toJavaFile().createNewFile();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }
}
