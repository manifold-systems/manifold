/*
 * Copyright (c) 2025 - Manifold Systems LLC
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

package manifold.extensions.java.lang.String;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.ExtensionMethodType;
import manifold.ext.rt.api.ExtensionSource;
import manifold.ext.rt.api.MethodSignature;

@Extension
@ExtensionSource(
  source = MyStringExtSource.class,
  type = ExtensionMethodType.EXCLUDE,
  overrideExistingMethods = true,
  methods = {
    @MethodSignature( name = "startsWith", paramTypes = { String.class, String.class, int.class } )
    // TODO: Should we include the first 'self' parameter (i.e., the method signature as in MyStringExtSource)?
    // Or should we define the methods as they appear in the String class?
  }
)
@ExtensionSource(
  source = MyStringExtSource2.class,
  type = ExtensionMethodType.INCLUDE,
  overrideExistingMethods = true,
  methods = {
    @MethodSignature( name = "substring", paramTypes = { String.class, int.class } )
  }
)
@ExtensionSource( source = MyStringExtSource3.class, overrideExistingMethods = true )
public class MyStringExt
{
}

