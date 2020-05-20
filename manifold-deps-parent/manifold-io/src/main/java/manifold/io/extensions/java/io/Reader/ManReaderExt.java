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

package manifold.io.extensions.java.io.Reader;

import manifold.io.extensions.java.io.File.ManFileExt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

/**
 */
@Extension
public class ManReaderExt
{
  /** Returns a buffered reader wrapping this Reader, or this Reader itself if it is already buffered. */
  public static BufferedReader buffered( @This Reader thiz )
  {
    return thiz.buffered( ManFileExt.DEFAULT_BUFFER_SIZE );
  }
  public static BufferedReader buffered( @This Reader thiz, int bufferSize )
  {
    if( thiz instanceof BufferedReader )
    {
      return (BufferedReader)thiz;
    }
    else
    {
      return new BufferedReader( thiz, bufferSize );
    }
  }

  /**
   * Iterates through each line of this reader, calls {@code action} for each line read
   * and closes the {@code Reader} when it's completed.
   *
   * @param action function to process file lines.
   */
  public static void forEachLine( @This Reader thiz, Consumer<String> action )
  {
    thiz.useLines( it -> {it.forEach( action ); return null;} );
  }

  /**
   * Reads this reader content as a list of lines.
   *
   * Do not use this function for huge files.
   */
  public static List<String> readLines( @This Reader thiz )
  {
      ArrayList<String> result = new ArrayList<>();
      thiz.forEachLine( it -> result.add(it) );
      return result;
  }

  /**
   * Calls the {@code block} callback giving it a sequence of all the lines in this file and closes the reader once
   * the processing is complete.
   * @return the value returned by {@code block}.
   */
  public static <T> T useLines( @This Reader thiz, Function<Iterable<String>,T> block )
  {
    try( BufferedReader reader = thiz.buffered() )
    {
      return block.apply( reader.lineSequence() );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }
}
