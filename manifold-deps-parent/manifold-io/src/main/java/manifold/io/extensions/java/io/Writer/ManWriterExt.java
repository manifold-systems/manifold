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

package manifold.io.extensions.java.io.Writer;

import java.io.BufferedWriter;
import java.io.Writer;
import manifold.ext.api.Extension;
import manifold.ext.api.This;


import static manifold.io.extensions.java.io.File.ManFileExt.DEFAULT_BUFFER_SIZE;

/**
 */
@Extension
public class ManWriterExt
{
  /** Returns a buffered reader wrapping this Writer, or this Writer itself if it is already buffered. */
  public static BufferedWriter buffered( @This Writer thiz )
  {
    return thiz.buffered( DEFAULT_BUFFER_SIZE );
  }
  /** Returns a buffered reader wrapping this Writer, or this Writer itself if it is already buffered. */
  public static BufferedWriter buffered( @This Writer thiz, int bufferSize )
  {
    return thiz instanceof BufferedWriter ? (BufferedWriter)thiz : new BufferedWriter( thiz, bufferSize );
  }
}
