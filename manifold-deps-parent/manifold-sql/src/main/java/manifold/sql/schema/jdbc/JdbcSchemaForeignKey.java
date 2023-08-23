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
  private final String _qualifiedName;
  private final String _actualName;
  private final SchemaTable _referencedTable;
  private final List<SchemaColumn> _columns;

  public JdbcSchemaForeignKey( String fkName, SchemaTable referencedTable, List<SchemaColumn> columns )
  {
    _referencedTable = referencedTable;
    _columns = columns;
    _actualName = fkName;
    String baseName = makeFkName( fkName );
    _name = baseName + "_ref";
    _qualifiedName = makeQualifiedName( baseName );
  }

  private String makeQualifiedName( String baseName )
  {
    // Note, qualified name is used exclusively for naming of fetch<FK>Refs methods,
    // thus table name is redundant, much better to remove it in this case. e.g.,
    // blog.fetchBlogCommentRefs()  vs.  blog.fetchCommentRefs()
    baseName = baseName.equalsIgnoreCase( _referencedTable.getName() ) ? "" : baseName + "_";
    return baseName + getOwnTable().getName() + "_ref";
  }

  private String makeFkName( String fkName )
  {
    if( fkName.toLowerCase().startsWith( "fk_" ) )
    {
      // remove 'fk_' prefix
      fkName = fkName.substring( "fk_".length() );
    }

    if( !isFkNameAcceptable( fkName ) )
    {
      fkName = assignName();
    }
    return fkName;
  }

  private boolean isFkNameAcceptable( String fkName )
  {
    // feels like we can always make more suitable name using the table name + the column name
    // for instance, store_staff vs. store_manager_staff, the latter is ours.
    // generally, fkName can be pretty bad, sometimes generated like: with H2 you get names like "Constraint432"
    return false;

//    if( fkName == null || fkName.isEmpty() )
//    {
//      return false;
//    }
//    char lastChar = fkName.charAt( fkName.length() - 1 );
//    // if the last char is a digit, it's probably a generated name like 'Constraint432', which is not acceptable
//    return !Character.isDigit( lastChar );
  }

  private String assignName()
  {
    // todo: make this configurable

    return _columns.size() == 1
      ? removeId( _columns.get( 0 ).getName() )
      : _referencedTable.getName();
  }

  public static String removeId( String name )
  {
    if( name.toLowerCase().endsWith( "_id" ) )
    {
      name = name.substring( 0, name.length() - "_id".length() );
    }
    return name;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  public String getQualifiedName()
  {
    return _qualifiedName;
  }

  @Override
  public String getActualName()
  {
    return _actualName;
  }

  @Override
  public SchemaTable getReferencedTable()
  {
    return _referencedTable;
  }

  @Override
  public List<SchemaColumn> getColumns()
  {
    return _columns;
  }
}
