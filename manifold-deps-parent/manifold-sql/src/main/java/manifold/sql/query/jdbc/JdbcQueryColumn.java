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

package manifold.sql.query.jdbc;

import manifold.sql.query.api.QueryColumn;
import manifold.sql.query.api.QueryTable;
import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.api.TypeProvider;
import manifold.sql.rt.util.DbUtil;
import manifold.sql.schema.api.SchemaColumn;
import manifold.sql.schema.api.SchemaTable;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.jetbrains.annotations.Nullable;

import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JdbcQueryColumn implements QueryColumn
{
  private final QueryTable _queryTable;
  private final SchemaTable _schemaTable;
  private final SchemaColumn _schemaColumn;
  private final int _position;
  private final String _name;
  private final int _jdbcType;
  private final String _sqlType;
  private final int _size;
  private final int _scale;
  private final int _displaySize;
  private final boolean _isNullable;
  private final boolean _isCurrency;
  private final boolean _isReadOnly;
  private final boolean _isSigned;
  private final String _columnType;

  public JdbcQueryColumn( int colIndex, JdbcQueryTable queryTable, ResultSetMetaData rsMetaData, DatabaseMetaData dbMetadata ) throws SQLException
  {
    _position = colIndex;
    _queryTable = queryTable;
    _name = DbUtil.handleAnonQueryColumn( rsMetaData.getColumnLabel( colIndex ), colIndex );

    String tableName = getTableName( rsMetaData );
    _schemaTable = tableName == null || tableName.isEmpty()
      ? null // null if query column is not a table column eg. calculated
      : _queryTable.getSchema().getTable( tableName );

    _schemaColumn = _schemaTable == null ? null : _schemaTable.getColumn( rsMetaData.getColumnName( colIndex ) );

    if( _schemaColumn != null )
    {
      // ensure query and schema types are consistent

      _jdbcType = _schemaColumn.getJdbcType();
      _sqlType = _schemaColumn.getSqlType();
      _columnType = _schemaColumn.getColumnClassName();
    }
    else
    {
      TypeProvider typeProvider = Dependencies.instance().getTypeProvider();
      _jdbcType = typeProvider.getQueryColumnType( colIndex, rsMetaData, dbMetadata );
      _sqlType = rsMetaData.getColumnTypeName( colIndex );
      _columnType = rsMetaData.getColumnClassName( colIndex );
    }

    _isNullable = rsMetaData.isNullable( colIndex ) == ResultSetMetaData.columnNullable;

    _size = rsMetaData.getPrecision( colIndex );
    _scale = rsMetaData.getScale( colIndex );

    _displaySize = rsMetaData.getColumnDisplaySize( colIndex );
    _isCurrency = rsMetaData.isCurrency( colIndex );
    _isReadOnly = rsMetaData.isReadOnly( colIndex );
    _isSigned = rsMetaData.isSigned( colIndex );
  }

  @Nullable
  private String getTableName( ResultSetMetaData rsMetaData )
  {
    String tableName = null;
    try
    {
      tableName = rsMetaData.getTableName( getPosition() );
    }
    catch( SQLException ignore )
    {
    }
    if( tableName == null || tableName.isEmpty() )
    {
      // some drivers (SqlServer, Oracle) do not return the table name :\
      // low effort parse it
      tableName = parseTableName();
    }
    return tableName;
  }

  private String parseTableName()
  {
    try
    {
      Set<String> tables = TablesNamesFinder.findTables( _queryTable.getQuerySource() );
      if( tables.size() == 1 )
      {
        String tableName = tables.iterator().next();
        SchemaTable schemaTable = getSchemaTable( tableName );
        if( schemaTable != null && schemaTable.getColumn( _name ) != null )
        {
          return schemaTable.getName();
        }
      }
      else if( !tables.isEmpty() )
      {
        PlainSelect select = (PlainSelect)CCJSqlParserUtil.parse( _queryTable.getQuerySource() );
        SelectItem<?> selectItem = select.getSelectItem( getPosition()-1 );
        Expression expr = selectItem.getExpression();
        if( expr instanceof Column )
        {
          Table table = ((Column)expr).getTable();
          String tableName = table == null ? null : table.getName();
          if( tableName != null )
          {
            SchemaTable schemaTable = getSchemaTable( tableName );
            if( schemaTable != null )
            {
              return schemaTable.getName();
            }
          }
          if( _name.equalsIgnoreCase( ((Column)expr).getColumnName() ) )
          {
            return findColumnInTables( _name, tables );
          }
          else
          {
            LOGGER.error( "Column '" + _name + "' does not match selectItem: '" + ((Column)expr).getColumnName() + "'" );
          }
        }
      }
    }
    catch( Exception e )
    {
      LOGGER.warn( "Failed to find table name for column: '" + _name + "'" );
    }
    return null;
  }

  private String findColumnInTables( String columnName, Set<String> tables )
  {
    List<String> result = tables.stream()
      .filter( t -> {
        SchemaTable schemaTable = getSchemaTable( t );
        return schemaTable != null && schemaTable.getColumn( columnName ) != null;
      } )
      .collect( Collectors.toList() );
    if( result.size() == 1 )
    {
      return result.get( 0 );
    }
    return null;
  }

  private SchemaTable getSchemaTable( String tableName )
  {
    SchemaTable table = _queryTable.getSchema().getTable( tableName );
    if( table == null )
    {
      tableName = tableName.toLowerCase();
      table = _queryTable.getSchema().getTable( tableName );
      if( table == null )
      {
        tableName = tableName.toUpperCase();
        table = _queryTable.getSchema().getTable( tableName );
      }
    }
    return table;
  }

  @Override
  public QueryTable getTable()
  {
    return _queryTable;
  }

  public SchemaTable getSchemaTable()
  {
    return _schemaTable;
  }

  @Override
  public int getJdbcType()
  {
    return _jdbcType;
  }

  @Override
  public String getSqlType()
  {
    return _sqlType;
  }

  @Override
  public String getColumnClassName()
  {
    return _columnType;
  }

  public SchemaColumn getSchemaColumn()
  {
    return _schemaColumn;
  }

  @Override
  public int getPosition()
  {
    return _position;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public boolean isNullable()
  {
    return _isNullable;
  }

  @Override
  public int getSize()
  {
    return _size;
  }

  public int getScale()
  {
    return _scale;
  }

  public QueryTable getQueryTable()
  {
    return _queryTable;
  }

  public int getDisplaySize()
  {
    return _displaySize;
  }

  public boolean isCurrency()
  {
    return _isCurrency;
  }

  public boolean isReadOnly()
  {
    return _isReadOnly;
  }

  public boolean isSigned()
  {
    return _isSigned;
  }
}
