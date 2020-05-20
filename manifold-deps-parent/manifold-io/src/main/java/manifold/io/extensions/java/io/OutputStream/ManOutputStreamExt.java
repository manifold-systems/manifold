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

package manifold.io.extensions.java.io.OutputStream;

import manifold.io.extensions.java.io.File.ManFileExt;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;


import static java.nio.charset.StandardCharsets.UTF_8;

/**
 */
@Extension
public class ManOutputStreamExt
{
  /**
   * Creates a buffered output stream wrapping this stream.
   */
  public static BufferedOutputStream buffered( @This OutputStream thiz )
  {
    return thiz.buffered( ManFileExt.DEFAULT_BUFFER_SIZE );
  }
  /**
   * Creates a buffered output stream wrapping this stream.
   * @param bufferSize the buffer size to use.
   */
  public static BufferedOutputStream buffered( @This OutputStream thiz, int bufferSize )
  {
    return thiz instanceof BufferedOutputStream
           ? (BufferedOutputStream)thiz
           : new BufferedOutputStream( thiz, bufferSize );
  }

  /** Creates a writer on this output stream using UTF-8 or the specified {@code charset}. */
  public static OutputStreamWriter writer( @This OutputStream thiz )
  {
    return thiz.writer( UTF_8 );
  }
  public static OutputStreamWriter writer( @This OutputStream thiz, Charset charset )
  {
    return new OutputStreamWriter( thiz, charset );
  }

  /** Creates a buffered writer on this output stream using UTF-8 or the specified {@code charset}. */
  public static BufferedWriter bufferedWriter( @This OutputStream thiz )
  {
    return thiz.bufferedWriter( UTF_8 );
  }
  public static BufferedWriter bufferedWriter( @This OutputStream thiz, Charset charset )
  {
    return thiz.writer( charset ).buffered();
  }

}
