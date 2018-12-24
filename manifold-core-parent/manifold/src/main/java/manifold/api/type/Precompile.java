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

package manifold.api.type;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use {@code @Precompile} to instruct Manifold to precompile classes from a specified type manifold.
 * This is useful for cases where a type manifold produces a static API for others to use.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Precompiles.class)
public @interface Precompile
{
  /**
   * The Type Manifold class defining the domain of types to compile from.
   */
  Class<? extends ITypeManifold> typeManifold();

  /**
   * A regular expression defining the range of types that should be compiled
   * from {@code typeManifold} via {@link ITypeManifold#getAllTypeNames()}.
   * The default value {@code "*."} compiles <i>all</i> types.
   */
  String typeNames() default ".*";
}
