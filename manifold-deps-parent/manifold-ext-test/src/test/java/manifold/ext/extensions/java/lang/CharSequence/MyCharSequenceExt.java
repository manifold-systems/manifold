/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.ext.extensions.java.lang.CharSequence;

import manifold.ext.api.Extension;
import manifold.ext.api.Self;
import manifold.ext.api.This;

@Extension
public class MyCharSequenceExt
{
  public static @Self CharSequence something( @This CharSequence foo, @Self CharSequence arg )
  {
    return foo;
  }
}
