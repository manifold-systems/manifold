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

package manifold.api.json.schema;

/**
 */
public enum Type
{
  Object( "object" ),
  Array( "array" ),
  String( "string" ),
  Number( "number" ),
  Integer( "integer" ),
  Boolean( "boolean" ),
  Dynamic( "dynamic" ),
  Null( "null" ),
  Invalid( "invalid" );

  private final String _schemaName;

  Type( String name )
  {
    _schemaName = name;
  }

  public String getName()
  {
    return _schemaName;
  }

  public static Type fromName( String schemaName )
  {
    switch( schemaName )
    {
      case "object":
      case "Object":
        return Object;
      case "array":
      case "Array":
        return Array;
      case "string":
      case "String":
        return String;
      case "double":
      case "number":
        return Number;
      case "integer":
        return Integer;
      case "boolean":
        return Boolean;
      case "dynmaic":
        return Dynamic;
      case "null":
        return Null;
    }
    return Invalid;
  }
}
