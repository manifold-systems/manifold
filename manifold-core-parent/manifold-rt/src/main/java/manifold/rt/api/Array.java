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

package manifold.rt.api;

/**
 * Container for Java array type extensions. An extension class that extends this class effectively extends all Java
 * array types such as: {@code Object[]}, {@code String[]}, {@code int[]}, {@code int[][]}, etc. Note the {@code @This}
 * parameter must be declared as type {@code Object} since both reference and primitive component types apply.
 * <pre><code>
 *   package myproject.extensions.manifold.rt.api.Array;
 *
 *   import java.lang.reflect.Array;
 *   import manifold.ext.rt.api.Extension;
 *   import manifold.ext.rt.api.This;
 *
 *  {@literal @Extension}
 *   public class MyArrayExtension {
 *     public static final String myMethod(@This Object array) {
 *       return "Size of array: " + Array.getLength(array);
 *     }
 *   }
 *
 *   // usage
 *   String[] strings = new String[] {"a", "b", "c"};
 *   strings.myMethod();
 * </code></pre>
 * See {@code manifold.ext.rt.extensions.manifold.rt.api.Array.ManArrayExt} for implementations of builtin Array
 * extension methods.
 */
public final class Array
{
  private Array() {}
}
