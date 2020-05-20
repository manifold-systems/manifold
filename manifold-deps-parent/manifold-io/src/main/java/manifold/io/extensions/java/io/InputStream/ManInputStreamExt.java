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

package manifold.io.extensions.java.io.InputStream;

import manifold.io.extensions.java.io.File.ManFileExt;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;


import static java.nio.charset.StandardCharsets.UTF_8;

/**
 */
@Extension
public class ManInputStreamExt
{
  /** Creates a reader on this input stream using UTF-8 or the specified {@code charset}. */
  public static InputStreamReader reader( @This InputStream thiz )
  {
    return thiz.reader( UTF_8 );
  }
  public static InputStreamReader reader( @This InputStream thiz, Charset charset )
  {
    return new InputStreamReader( thiz, charset );
  }

  /** Creates a buffered reader on this input stream using UTF-8 or the specified {@code charset}. */
  public static BufferedReader bufferedReader( @This InputStream thiz )
  {
    return thiz.bufferedReader( UTF_8 );
  }
  public static BufferedReader bufferedReader( @This InputStream thiz, Charset charset )
  {
    return thiz.reader( charset ).buffered();
  }

  /**
   * Copies this stream to the given output stream, returning the number of bytes copied
   * <p>
   * **Note** It is the caller's responsibility to close both of these resources.
   */
  public static long copyTo( @This InputStream thiz, OutputStream out )
  {
    return thiz.copyTo( out, ManFileExt.DEFAULT_BUFFER_SIZE );
  }
  /**
   * Copies this stream to the given output stream, returning the number of bytes copied
   * <p>
   * **Note** It is the caller's responsibility to close both of these resources.
   */
  public static long copyTo( @This InputStream thiz, OutputStream out, int bufferSize )
  {
    try
    {
      long bytesCopied = 0;
      byte[] buffer = new byte[bufferSize];
      int bytes = thiz.read( buffer );
      while( bytes >= 0 )
      {
        out.write( buffer, 0, bytes );
        bytesCopied += bytes;
        bytes = thiz.read( buffer );
      }
      return bytesCopied;
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

}
