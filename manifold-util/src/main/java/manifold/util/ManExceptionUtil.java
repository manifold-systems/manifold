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

public class ManExceptionUtil
{
  /**
   * Throws an unchecked exception without having to declare or catch it.
   *
   * @param t Any exception
   * @return The {@link RuntimeException} return type is here so you can do this:<br>
   * {@code throw ManExceptionUtil.uncheck(new SomeException())}
   */
  public static RuntimeException unchecked( Throwable t )
  {
    _unchecked( t );

    // above statement always throws, this is unreachable
    throw new IllegalStateException();
  }

  private static <T extends Throwable> void _unchecked( Throwable t ) throws T
  {
    //noinspection unchecked
    throw (T)t;
  }
}
