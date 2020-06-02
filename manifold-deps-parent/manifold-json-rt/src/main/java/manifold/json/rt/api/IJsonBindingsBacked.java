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

package manifold.json.rt.api;

import manifold.ext.rt.api.IBindingsBacked;

/**
 * A base interface for all common structured data types with methods to transform bindings to/from JSON, YAML, XML,
 * CSV etc. and to conveniently use the Bindings for Web services e.g., a JSON web service can use YAML etc.
 */
public interface IJsonBindingsBacked extends IBindingsBacked
{
  @Override
  DataBindings getBindings();

  /**
   * A fluent method to write this JSON object in various formats including JSON, YAML, XML, and CSV
   */
  default Writer write()
  {
    return new Writer( getBindings() );
  }
}
