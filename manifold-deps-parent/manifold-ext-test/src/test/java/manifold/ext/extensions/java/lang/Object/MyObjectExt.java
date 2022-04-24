/*
 * Copyright (c) 2022 - Manifold Systems LLC
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

package manifold.ext.extensions.java.lang.Object;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.Self;
import manifold.ext.rt.api.This;
import manifold.ext.rt.api.ThisClass;

import java.util.function.Predicate;

@Extension
public class MyObjectExt
{
  protected static String myProtectedMethod( @This Object thiz )
  {
    return "protected method";
  }

  @Extension
  public static @Self Object myStaticSelfMethod( Predicate<@Self Object> constraints )
  {
    // note, without employing @ThisClass the usage of @Self in a static method is still useful with code gen when
    // the static method is not really going to execute e.g., building a query model from the meta information in the
    // call.
    return null;
  }

  public static Class mySmartStaticSelfMethod( @ThisClass Class callingClass, Predicate<@Self Object> constraints ) throws Exception
  {
    if( !constraints.test( callingClass.newInstance() ) )
    {
      return null;
    }
    return callingClass;
  }
}
