/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.json.rt.api;

import manifold.ext.rt.RuntimeMethods;

/**
 * Super interface for all Builder classes in JSON and JSON-derived manifolds, such as XML, YAML, and CSV.
 * @param <T>
 */
public interface JsonBuilder<T extends IJsonBindingsBacked> extends BuiltType<T>
{
  default T build()
  {
    //noinspection unchecked
    return (T)RuntimeMethods.coerceFromBindingsValue( getBindings(), findBuiltTypeFrom( JsonBuilder.class ) );
  }
}
