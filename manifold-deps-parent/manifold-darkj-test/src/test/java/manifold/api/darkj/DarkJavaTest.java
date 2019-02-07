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

package manifold.api.darkj;

import junit.framework.TestCase;
import manifold.internal.runtime.Bootstrap;
import manifold.util.ReflectUtil;

/**
 */
public class DarkJavaTest extends TestCase
{
  public void testDarkness()
  {
    // Dark Java is only available at runtime, bootstrap runtime
    Bootstrap.init();

    // Use reflection to work with Dark Java
    Object darkness = ReflectUtil.constructor( "abc.Darkness", String.class ).newInstance( "hi" );
    assertEquals( "hi", ReflectUtil.method( darkness, "getName" ).invoke() );
    assertEquals( "bye", ReflectUtil.method(
      ReflectUtil.method( darkness, "makeStuff", String.class )
        .invoke( "bye" ), "getStuff" )
      .invoke() );
  }
}
