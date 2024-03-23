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

import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.Pair;
import manifold.sql.query.api.ForeignKeyQueryRef;
import manifold.sql.query.api.QueryColumn;
import manifold.sql.api.Parameter;
import manifold.sql.query.api.QueryTable;
import manifold.sql.query.type.SqlIssueContainer;
import manifold.sql.query.type.SqlScope;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.util.DbUtil;
import manifold.sql.rt.util.DriverInfo;
import manifold.sql.schema.api.Schema;
import manifold.sql.schema.api.SchemaColumn;
import manifold.sql.schema.api.SchemaForeignKey;
import manifold.sql.schema.api.SchemaTable;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static manifold.sql.util.StatementUtil.replaceNamesWithQuestion;

public class JdbcQueryTable implements QueryTable
{
  private final SqlScope _scope;
  private final String _source;
  private final String _name;
  private final String _escapedName;
  private final Map<String, QueryColumn> _columns;
  private final List<Parameter> _parameters;
  private final SqlIssueContainer _issues;

  public JdbcQueryTable( SqlScope scope, String simpleName, String query )
  {
    _scope = scope;
    List<ParamInfo> paramNames = ParameterParser.getParameters( query );
    _source = replaceNamesWithQuestion( query, paramNames );
    _name = simpleName;
    _columns = new LinkedHashMap<>();
    _parameters = new ArrayList<>();
    Schema schema = _scope.getSchema();
    _issues = new SqlIssueContainer( schema == null ? DriverInfo.ERRANT : schema.getDriverInfo(),
      new ArrayList<>(), ManStringUtil.isCrLf( _source ) );

    if( _scope.isErrant() )
    {
      _escapedName = _name;
      return;
    }

    ConnectionProvider cp = Dependencies.instance().getConnectionProvider();
    String escapedName = _name;
    try( Connection c = cp.getConnection( scope.getDbconfig() ) )
    {
      escapedName = DbUtil.enquoteIdentifier( _name, c.getMetaData() );
      build( c, paramNames );
    }
    catch( SQLException e )
    {
      _issues.addIssues( Collections.singletonList( e ) );
    }
    _escapedName = escapedName;
  }

  private void build( Connection c, List<ParamInfo> paramNames ) throws SQLException
  {
    DatabaseMetaData metadata = c.getMetaData();
    try( PreparedStatement ps = c.prepareStatement( _source ) )
    {
      ParameterMetaData paramMetaData = ps.getParameterMetaData();
      int paramCount = paramMetaData.getParameterCount();
      if( !paramNames.isEmpty() && paramCount != paramNames.size() )
      {
        throw new SQLException( "Parameter name count does not match '?' param count. Query: " + _name + "\n" + _source );
      }
      for( int i = 1; i <= paramCount; i++ )
      {
        String name = paramNames.isEmpty() ? null : paramNames.get( i - 1 ).getName().substring( 1 );
        JdbcParameter param = new JdbcParameter( i, name, this, paramMetaData, metadata );
        _parameters.add( param );
      }
//todo: remove this code path?...
// executeQuery is an alternative to parsing the query when the driver does not provide the table name for the query column
// Going with parsing for now since executing the query involves shenanigans with parameters and such.
//      executeQueryIfRequired( metadata, ps );

      ResultSetMetaData rsMetaData = ps.getMetaData();
      int columnCount = rsMetaData.getColumnCount();
      for( int i = 1; i <= columnCount; i++ )
      {
        JdbcQueryColumn col = new JdbcQueryColumn( i, this, rsMetaData, metadata );
        _columns.put( col.getName(), col );
      }
    }
  }

  private void executeQueryIfRequired( DatabaseMetaData metadata, PreparedStatement ps ) throws SQLException
  {
    DriverInfo driverInfo = DriverInfo.lookup( metadata );
    if( driverInfo.requiresQueryExecForTableName() )
    {
      // most drivers do NOT require query exec to get the table corresponding with the query column,
      // so far only Oracle and SqlServer need to execute the query :\

      List<Parameter> parameters = getParameters();
      for( int i = 0; i < parameters.size(); i++ )
      {
        Parameter p = parameters.get( i );
        ps.setNull( i+1, p.getJdbcType() );
      }
      ps.executeQuery();
    }
  }

  /**
   * Find the selected table object that has all its non-null columns selected in the query columns.
   * <p/>
   * This feature enables, for example, [SELECT * FROM foo ...] query results to consist of Entities instead of column
   * values.
   * <p/>
   * @return All query columns that correspond with the primary selected table, or null if no table is fully covered. The
   * resulting columns are sufficient to create a valid instance of the entity corresponding with the selected table.
   */
  public Pair<SchemaTable, List<QueryColumn>> findSelectedTable()
  {
    Map<SchemaTable, List<QueryColumn>> map = queryColumnsBySchemaTable();
    for( Map.Entry<SchemaTable, List<QueryColumn>> entry : map.entrySet() )
    {
      SchemaTable schemaTable = entry.getKey();
      List<QueryColumn> queryCols = entry.getValue();

      if( allNonNullColumnsRepresented( schemaTable, queryCols ) )
      {
        return new Pair<>( schemaTable, queryCols );
      }
    }
    return null;
  }

  /**
   * Of the query columns <i>not</i> corresponding with the selected table (if one exists, see findSelectedTable() above), finds
   * the columns fully covering foreign keys, represented as {@link JdbcForeignKeyQueryRef}. The idea is to provide {@code get<foreign-key-ref>()}
   * methods. For example, a {@code city_id} foreign key would result in a {@code getCityRef()} method return a {@code City}
   * entity.
   */
  public List<ForeignKeyQueryRef> findForeignKeyQueryRefs()
  {
    Map<String, QueryColumn> columns = getColumns();
    Pair<SchemaTable, List<QueryColumn>> coveredTable = findSelectedTable();
    if( coveredTable != null )
    {
      // remove selected table columns from search
      coveredTable.getSecond().forEach( c -> columns.remove( c.getName() ) );
    }

    List<ForeignKeyQueryRef> fkRefs = new ArrayList<>();
    Set<QueryColumn> taken = new HashSet<>();
    for( QueryColumn col: columns.values() )
    {
      if( taken.contains( col ) )
      {
        continue;
      }
      SchemaTable schemaTable = col.getSchemaTable();
      if( schemaTable != null )
      {
        findFkRefs( schemaTable, columns.values(), fkRefs, taken );
      }
    }
    return fkRefs;
  }

  private void findFkRefs( SchemaTable schemaTable, Collection<QueryColumn> columns,
                           List<ForeignKeyQueryRef> fkRefs, Set<QueryColumn> taken )
  {
    Collection<List<SchemaForeignKey>> foreignKeys = schemaTable.getForeignKeys().values();
    for( List<SchemaForeignKey> fks : foreignKeys )
    {
      List<QueryColumn> fkQueryCols = new ArrayList<>();
      for( SchemaForeignKey fk : fks )
      {
        for( QueryColumn queryCol : columns )
        {
          List<SchemaColumn> fkCols = fk.getColumns();
          SchemaColumn schemaColumn = queryCol.getSchemaColumn();
          if( schemaColumn != null && fkCols.contains( schemaColumn ) )
          {
            taken.add( queryCol );
            fkQueryCols.add( queryCol );
            if( fkQueryCols.size() == fkCols.size() )
            {
              // fk is covered by query cols, add it
              fkRefs.add( new JdbcForeignKeyQueryRef( fk, fkQueryCols ) );
              break;
            }
          }
        }
      }
    }
  }

  private Map<SchemaTable, List<QueryColumn>> queryColumnsBySchemaTable()
  {
    Map<SchemaTable, List<QueryColumn>> map = new LinkedHashMap<>();
    for( QueryColumn col: getColumns().values() )
    {
      SchemaTable schemaTable = col.getSchemaTable();
      if( schemaTable != null )
      {
        map.computeIfAbsent( schemaTable, __ -> new ArrayList<>() )
          .add( col );
      }
    }
    return map;
  }

  private boolean allNonNullColumnsRepresented( SchemaTable schemaTable, List<QueryColumn> queryCols )
  {
    Set<SchemaColumn> queriedSchemaCols = queryCols.stream()
      .map( c -> c.getSchemaColumn() )
      .filter( c -> c != null )
      .collect( Collectors.toSet() );
    return queriedSchemaCols.containsAll( schemaTable.getNonNullColumns() );
  }

  @Override
  public String getSqlSource()
  {
    return _source;
  }

  @Override
  public Schema getSchema()
  {
    return _scope.getSchema();
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public String getEscapedName()
  {
    return _escapedName;
  }

  @Override
  public Map<String, QueryColumn> getColumns()
  {
    return new LinkedHashMap<>( _columns );
  }

  @Override
  public QueryColumn getColumn( String columnName )
  {
    return _columns.get( columnName );
  }

  @Override
  public List<Parameter> getParameters()
  {
    return _parameters;
  }

  public SqlIssueContainer getIssues()
  {
    return _issues;
  }
}
