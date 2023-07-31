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

package manifold.sql.schema.jdbc;

import manifold.sql.schema.api.SchemaColumn;
import manifold.sql.schema.api.SchemaForeignKey;
import manifold.sql.schema.api.SchemaTable;

import java.util.List;

public class JdbcSchemaForeignKey implements SchemaForeignKey
{
  private final String _name;
  private final SchemaTable _referencedTable;
  private final List<SchemaColumn> _columns;

  public JdbcSchemaForeignKey( String fkName, SchemaTable referencedTable, List<SchemaColumn> columns )
  {
    _referencedTable = referencedTable;
    _columns = columns;
//todo: fkName can be pretty bad, sometimes generated like with H2 you get names like "Constraint432"
//    if( fkName != null )
//    {
//      _name = fkName;
//    }
//    else
//    {
      _name = assignName();
//    }
  }

  private String assignName()
  {
    return _columns.size() == 1
      ? removeId( _columns.get( 0 ).getName() )
      : _referencedTable.getName() + "_ref";
  }

  private String removeId( String name )
  {
    if( name.toLowerCase().endsWith( "_id" ) )
    {
      name = name.substring( 0, name.length() - "_id".length() ) + "_ref";
    }
    return name;
  }

  public String getName()
  {
    return _name;
  }

  public SchemaTable getReferencedTable()
  {
    return _referencedTable;
  }

  public List<SchemaColumn> getColumns()
  {
    return _columns;
  }
}
