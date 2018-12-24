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

package manifold.util;

import manifold.util.concurrent.LocklessLazyVar;

public class PerfLogUtil
{
  private static final LocklessLazyVar<Boolean> PERF =
    LocklessLazyVar.make( () -> {
      String value = System.getProperty( "manifold.perf", "false" );
      return Boolean.valueOf( value );
    } );

  public static void log( String label, Runnable run )
  {
    long before = System.nanoTime();
    try
    {
      run.run();
    }
    finally
    {
      log( label, before );
    }
  }

  public static void log( String label, long nanosBefore )
  {
    //noinspection ConstantConditions
    if( !PERF.get() )
    {
      return;
    }

    System.out.println( label + ": " + ((System.nanoTime() - nanosBefore) / 1_000_000) + "ms" );
  }
}
