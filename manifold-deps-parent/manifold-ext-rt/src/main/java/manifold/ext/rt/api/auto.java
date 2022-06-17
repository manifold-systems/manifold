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

package manifold.ext.rt.api;

/**
 * Serves as the pseudo keyword {@code auto}, which declares a field, local, or method as having its type inferred from
 * its value/definition.
 * <p/>
 * A field or local variable declared as {@code auto} infers its type from the initializer. The behavior is similar to
 * Java's 'var'.
 * <p/>
 * A method with return type {@code auto} infers its return type from the return statements in its definition. As a
 * consequence, fields and methods declared in the inferred type are type-safely accessible to call-sites.
 */
public final class auto
{
  private auto() {}
}