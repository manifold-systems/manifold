/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.sql.schema.api;

import manifold.rt.api.util.Pair;
import manifold.sql.api.Table;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SchemaTable extends Table
{
  SchemaColumn getId();
  Map<SchemaTable, List<SchemaForeignKey>> getForeignKeys();
  List<SchemaColumn> getPrimaryKey();
  Map<String, List<SchemaColumn>> getNonNullUniqueKeys();
  String getDescription();

  Kind getKind();

  @Override
  Map<String, SchemaColumn> getColumns();

  @Override
  SchemaColumn getColumn( String columnName );

  void resolveForeignKeys();
  void resolveFkRelations();

  List<SchemaColumn> getNonNullColumns();

  Set<SchemaForeignKey> getOneToMany();
  Set<Pair<SchemaColumn, SchemaColumn>> getManyToMany();

  enum Kind
  {
    Table, View, System;

    public static Kind get( String kind )
    {
      switch( kind )
      {
        case "VIEW":
          return View;
        case "SYSTEM TABLE":
          return System;
        case "TABLE":
        case "BASE TABLE":
        default:
          return Table;

      }
//      throw new IllegalStateException( "Unrecognized type kind: " + kind );
    }
  }
}
