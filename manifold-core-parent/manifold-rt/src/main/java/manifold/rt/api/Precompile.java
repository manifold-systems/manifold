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

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use {@code @Precompile} to instruct Manifold to precompile classes from a specified type manifold.
 * This is useful for cases where a type manifold produces a static API for others to use.  Note you
 * maybe provide more than one instance of {@code @Precompile} to precompile types from more than one
 * type manifold:
 * <pre><code>
 * &#64;Precompile(fileExtension = "json")
 * &#64;Precompile(fileExtension = "yml") </code></pre>
 * This instructs the Java compiler to precompile all {@code JSON} and {@code YAML} in the enclosing module.
 * <p/>
 * The default behavior:
 * <pre><code>
 * &#64;Precompile() </code></pre>
 * compiles all types from all type manifolds used in the module.
 *
 * @deprecated Use -Amanifold.resource.&lt;file-ext&gt;=[type-name-regex] javac command line arguments.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Precompiles.class)
@Deprecated
public @interface Precompile
{
  /**
   * The Type Manifold class defining the domain of types to compile from.
   * <p/>
   * Use {@link #fileExtension()} as a convenient alternative way to specify the type manifold via a file extension
   * it handles.
   */
  Class<?> typeManifold() default Object.class;

  /**
   * A file extension name e.g., {@code "json"}, handled by the Type Manifold class defining the domain of types to compile.
   * This value is an alternative to {@link #typeManifold()} as a simple way to indirectly specify the {@code ITypeManifold}.
   * If both arguments are present, {@link #typeManifold()} has precedence.
   * <p/>
   * The default wildcard value {@code "*"} precompiles types from <i>all</i> type manifolds used in the module
   */
  String fileExtension() default "*";

  /**
   * A regular expression defining the range of types that should be compiled from {@link #typeManifold} or
   * {@link #fileExtension} via {@code ITypeManifold#getAllTypeNames()}. The default value {@code ".*"} compiles
   * <i>all</i> types originating from the specified type manifold.
   */
  String typeNames() default ".*";
}
