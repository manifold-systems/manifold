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

/**
 * todo: refactor {@code Json, Yaml, Xml, Csv} types to implement this interface so that there is only ONE
 *  type manifold for all such data types. This will improve areas in the codebase where knowledge
 *  of specific type manifolds are currently necessary (see classes Write, Load, JsonSchemaType, etc.)
 *  and will also eliminate having to create new type manifolds for such data types. Instead just
 *  implement this interface as a Service, which manifold discovers and includes in the collective
 *  "data type" manifold.
 */
public interface IBindingsProvider
{
  Names getNames();
  
  String toText( Object jsonValue );
  void toText( Object jsonValue, String name, StringBuilder target, int indent );
  Object fromText( String text );
  Object fromText( String text, boolean withTokens );

  class Names
  {
    String _shortName;
    String _fullName;
    String _primaryFileExtension;
    String[] _allFileExtensions;
  }
}
