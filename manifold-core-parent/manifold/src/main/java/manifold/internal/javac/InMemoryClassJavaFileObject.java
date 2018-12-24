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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;

/**
 */
public class InMemoryClassJavaFileObject extends SimpleJavaFileObject
{
  private final ByteArrayOutputStream _outputStream;
  private final String _className;

  public InMemoryClassJavaFileObject( String className, Kind kind )
  {
    super( URI.create( "mem:///" + className.replace( '.', '/' ) + kind.extension ), kind );
    _className = className;
    _outputStream = new ByteArrayOutputStream();
  }

  @Override
  public OutputStream openOutputStream() throws IOException
  {
    return _outputStream;
  }

  @Override
  public InputStream openInputStream() throws IOException
  {
    if( _outputStream.size() > 0 )
    {
      return new ByteArrayInputStream( getBytes() );
    }
    return null;
  }

  public byte[] getBytes()
  {
    return _outputStream.toByteArray();
  }

  public String getClassName()
  {
    return _className;
  }
}
