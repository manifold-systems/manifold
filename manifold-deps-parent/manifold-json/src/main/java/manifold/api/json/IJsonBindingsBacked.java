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

package manifold.api.json;

import manifold.ext.api.IBindingsBacked;

/**
 * A base interface for all JSON and YAML types with methods to transform bindings to/from JSON and YAML
 * and to conveniently use the Bindings for JSON and YAML Web services.
 */
public interface IJsonBindingsBacked extends IBindingsBacked
{
  /** A fluent method to write this JSON object in various formats including JSON, YAML, and XML */
  default Writer write()
  {
    return new Writer( getBindings() );
  }

  /** A fluent method to send this JSON object via HTTP POST to field content as JSON bindings */
  default Poster post()
  {
    return new Poster( getBindings() );
  }
}
