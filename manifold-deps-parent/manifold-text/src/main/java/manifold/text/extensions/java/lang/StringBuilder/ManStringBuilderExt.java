/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.text.extensions.java.lang.StringBuilder;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

@Extension
public class ManStringBuilderExt
{
  /**
   * Implements the index operator for assignment such as {@code text[i] = 'x'}
   * <p>
   * The character at the specified {@code index} is set to {@code ch}.
   * <p>
   * The index argument must be greater than or equal to {@code 0}, and less than the length of this string.
   * @param      index   the index of the character to modify.
   * @param      ch      the new character.
   * @throws     IndexOutOfBoundsException  if {@code index} is negative or greater than or equal to {@code length()}.
   */
  public static void set( @This StringBuilder thiz, int index, char ch )
  {
    thiz.setCharAt( index, ch );
  }
}
