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

package manifold.sql.rt.impl;

import manifold.ext.rt.api.IBindingsBacked;
import manifold.json.rt.api.DataBindings;
import manifold.rt.api.Bindings;
import manifold.sql.rt.api.*;
import manifold.util.ManExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class BasicCrudProvider implements CrudProvider
{
  private final Logger LOGGER = LoggerFactory.getLogger( BasicCrudProvider.class );

  public static final String SQLITE_LAST_INSERT_ROWID = "last_insert_rowid()";
  private static final String MYSQL_GENERATED_KEY = "GENERATED_KEY";

  @SuppressWarnings( "unused" )
  public <T extends TableRow> void create( Connection c, UpdateContext<T> ctx )
  {
    try
    {
      T table = ctx.getTable();
      String[] allColumnNames = ctx.getAllCols().keySet().toArray( new String[0] );

      Set<String> skipParams = new HashSet<>();
      String sql = makeInsertStmt( c.getMetaData(), ctx, skipParams );
      try( PreparedStatement ps = c.prepareStatement( sql, allColumnNames ) )
      {
        setInsertParameters( ctx, ps, skipParams );
        executeAndFetchRow( c, ctx, ps, table.getBindings() );
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private <T extends TableRow> void setInsertParameters( UpdateContext<T> ctx, PreparedStatement ps, Set<String> skipParams ) throws SQLException
  {
    int i = 0;
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    for( Map.Entry<String, Object> entry: ctx.getTable().getBindings().entrySet() )
    {
      if( skipParams.contains( entry.getKey() ) )
      {
        continue;
      }
      int jdbcType = ctx.getAllCols().get( entry.getKey() ).getJdbcType();
      ValueAccessor accessor = accProvider.get( jdbcType );
      Object value = entry.getValue();
      value = patchFk( value, entry.getKey(), ctx.getTable().getBindings() );
      accessor.setParameter( ps, ++i, value );
    }
  }

  private static Object patchFk( Object value, String colName, TxBindings bindings )
  {
    // We assign a Pair<TableRow, String> to an fk column when the tablerow is not yet inserted.
    // Normally this is resolved by TxScope commit logic, ordering inserts according to fk dependencies, however if there
    // is a cycle, the Pair value remains. In that case, if the fk is not nullable, we must assign a temporary value here.
    // The TxScope commit logic resolves the actual fk value.

    if( value instanceof KeyRef )
    {
      Object heldFkValue = bindings.getHeldValue( colName );
      if( heldFkValue != null )
      {
        // value obtained via ordered inserts from TxScope
        value = heldFkValue;
      }
      else
      {
        // temporary foreign key value for deferred constraint, to avoid NOT NULL enforcement that is not deferred
        //todo: handle other data types here
        value = 0;
      }
    }
    return value;
  }

  private <T extends TableRow> String makeInsertStmt( DatabaseMetaData metaData, UpdateContext<T> ctx, Set<String> skipParams )
  {
    StringBuilder sql = new StringBuilder();
    sql.append( "INSERT INTO " ).append( ctx.getDdlTableName() ).append( "(" );
    int i = 0;
    Set<Map.Entry<String, Object>> entries = ctx.getTable().getBindings().entrySet();
    for( Map.Entry<String, Object> entry: entries )
    {
      String colName = entry.getKey();
      if( i++ > 0 )
      {
        sql.append( ", " );
      }
      sql.append( enquote( colName, metaData ) );
    }
    sql.append( ")" ).append( " VALUES (" );
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    i = 0;
    for( Map.Entry<String, Object> entry: entries )
    {
      if( i++ > 0 )
      {
        sql.append( "," );
      }
      ValueAccessor accessor = accProvider.get( ctx.getAllCols().get( entry.getKey() ).getJdbcType() );
      String expr = accessor.getParameterExpression( metaData, entry.getValue(), ctx.getAllCols().get( entry.getKey() ) );
      sql.append( expr );
      if( !expr.contains( "?" ) )
      {
        skipParams.add( entry.getKey() );
      }
    }
    sql.append( ")" );
    return sql.toString();
  }

  @SuppressWarnings( "unused" )
  public <T extends TableRow> T readOne( QueryContext<T> ctx )
  {
    ConnectionProvider cp = Dependencies.instance().getConnectionProvider();
    try( Connection c = cp.getConnection( ctx.getConfigName(), ctx.getQueryClass() ) )
    {
      // todo: put a cache on this

      Set<String> skipParams = new HashSet<>();
      String sql = makeReadStatement( c.getMetaData(), ctx, skipParams );
      try( PreparedStatement ps = c.prepareStatement( sql ) )
      {
        setQueryParameters( ctx, ps, skipParams );
        try( ResultSet resultSet = ps.executeQuery() )
        {
          Result<T> ts = new Result<>( ctx.getTxScope(), resultSet, ctx.getRowMaker() );
          Iterator<T> iterator = ts.iterator();
          if( !iterator.hasNext() )
          {
            // not found
            return null;
          }
          T result = iterator.next();
          if( iterator.hasNext() )
          {
            throw new SQLException( "Results contain more than one row." );
          }
          return result;
        }
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @SuppressWarnings( "unused" )
  public <T extends TableRow> List<T> readMany( QueryContext<T> ctx )
  {
    ConnectionProvider cp = Dependencies.instance().getConnectionProvider();
    try( Connection c = cp.getConnection( ctx.getConfigName(), ctx.getQueryClass() ) )
    {
      // todo: put a cache on this

      Set<String> skipParams = new HashSet<>();
      String sql = makeReadStatement( c.getMetaData(), ctx, skipParams );
      try( PreparedStatement ps = c.prepareStatement( sql ) )
      {
        setQueryParameters( ctx, ps, skipParams );
        try( ResultSet resultSet = ps.executeQuery() )
        {
          Result<T> ts = new Result<>( ctx.getTxScope(), resultSet, ctx.getRowMaker() );
          List<T> result = new ArrayList<>();
          for( T t : ts )
          {
            result.add( t );
          }
          return result;
        }
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private <T extends TableRow> String makeReadStatement( DatabaseMetaData metaData, QueryContext<T> ctx, Set<String> skipParams )
  {
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    StringBuilder sql = new StringBuilder();
    sql.append( "SELECT * FROM " ).append( ctx.getDdlTableName() ).append( " WHERE " );
    int i = 0;
    for( Map.Entry<String, Object> entry : ctx.getParams().entrySet() )
    {
      if( i > 0 )
      {
        sql.append( " AND " );
      }
      ColumnInfo paramInfo = ctx.getParamInfo()[i];
      ValueAccessor accessor = accProvider.get( paramInfo.getJdbcType() );
      String expr = accessor.getParameterExpression( metaData, entry.getValue(), paramInfo );
      sql.append( enquote( entry.getKey(), metaData ) ).append( " = " ).append( expr );
      i++;
      if( !expr.contains( "?" ) )
      {
        skipParams.add( entry.getKey() );
      }
    }
    return sql.toString();
  }

  @SuppressWarnings( "unused" )
  public <T extends TableRow> void update( Connection c, UpdateContext<T> ctx )
  {
    try
    {
      T table = ctx.getTable();
      StringBuilder sql = new StringBuilder();
      sql.append( "UPDATE " ).append( ctx.getDdlTableName() ).append( " SET\n" );
      int i = 0;
      Map<String, Object> changeEntries = table.getBindings().uncommittedChangesEntrySet();
      if( changeEntries.isEmpty() )
      {
        throw new SQLException( "Expecting changed entries." );
      }
      Set<String> skipParams = new HashSet<>();
      ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
      for( Map.Entry<String, Object> entry : changeEntries.entrySet() )
      {
        if( i > 0 )
        {
          sql.append( ",\n" );
        }
        String colName = entry.getKey();
        ValueAccessor accessor = accProvider.get( ctx.getAllCols().get( colName ).getJdbcType() );
        String expr = accessor.getParameterExpression( c.getMetaData(), entry.getValue(), ctx.getAllCols().get( colName ) );
        String qcolName = enquote( colName, c.getMetaData() );
        sql.append( "$qcolName = " ).append( expr );
        i++;
        if( !expr.contains( "?" ) )
        {
          skipParams.add( entry.getKey() );
        }
      }
      sql.append( "\nWHERE " );

      Set<String> allColNames = ctx.getAllCols().keySet();

      Set<String> whereColumns;
      if( !ctx.getPkCols().isEmpty() )
      {
        whereColumns = ctx.getPkCols();
      }
      else if( !ctx.getUkCols().isEmpty() )
      {
        whereColumns = ctx.getUkCols();
      }
      else
      {
        whereColumns = allColNames;
      }
      if( !whereColumns.isEmpty() )
      {
        i = 0;
        for( String whereCol: whereColumns )
        {
          if( i++ > 0 )
          {
            sql.append( ", " );
          }
          ColumnInfo columnInfo = ctx.getAllCols().get( whereCol );
          ValueAccessor accessor = accProvider.get( columnInfo.getJdbcType() );
          String expr = accessor.getParameterExpression(
            c.getMetaData(), ctx.getTable().getBindings().getPersistedStateValue( whereCol ), columnInfo );
          String qwhereCol = enquote( whereCol, c.getMetaData() );
          sql.append( "$qwhereCol = " ).append( expr );
          if( !expr.contains( "?" ) )
          {
            skipParams.add( whereCol );
          }
        }
      }
      else
      {
        throw new SQLException( "Expecting primary key, unique key, or provided columns for WHERE clause." );
      }

      try( PreparedStatement ps = c.prepareStatement( sql.toString(), allColNames.toArray( new String[0] ) ) )
      {
        setUpdateParameters( ctx, whereColumns, ps, skipParams );
        executeAndFetchRow( c, ctx, ps, table.getBindings() );
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private <T extends TableRow> void setUpdateParameters( UpdateContext<T> ctx, Set<String> whereColumns, PreparedStatement ps, Set<String> skipParams ) throws SQLException
  {
    Map<String, Object> changeEntries = ctx.getTable().getBindings().uncommittedChangesEntrySet();
    if( changeEntries.isEmpty() )
    {
      throw new SQLException( "Expecting changed entries." );
    }
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    int i = 0;
    for( Map.Entry<String, Object> entry : changeEntries.entrySet() )
    {
      if( skipParams.contains( entry.getKey() ) )
      {
        continue;
      }
      ValueAccessor accessor = accProvider.get( ctx.getAllCols().get( entry.getKey() ).getJdbcType() );
      Object value = entry.getValue();
      accessor.setParameter( ps, ++i, value );
    }
    if( !whereColumns.isEmpty() )
    {
      for( String whereColumn : whereColumns )
      {
        if( skipParams.contains( whereColumn ) )
        {
          continue;
        }

        ValueAccessor accessor = accProvider.get( ctx.getAllCols().get( whereColumn ).getJdbcType() );
        Object value = ctx.getTable().getBindings().getPersistedStateValue( whereColumn );
        accessor.setParameter( ps, ++i, value );
      }
    }
    else
    {
      throw new SQLException( "Expecting primary key, unique key, or provided columns for WHERE clause." );
    }

  }

  private <T extends TableRow> void setDeleteParameters( UpdateContext<T> ctx, Set<String> whereColumns, PreparedStatement ps, Set<String> skipParams ) throws SQLException
  {
    int i = 0;
    if( !whereColumns.isEmpty() )
    {
      ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
      for( String whereColumn : whereColumns )
      {
        ValueAccessor accessor = accProvider.get( ctx.getAllCols().get( whereColumn ).getJdbcType() );
        Object value = ctx.getTable().getBindings().getPersistedStateValue( whereColumn );
        if( skipParams.contains( whereColumn ) )
        {
          continue;
        }
        accessor.setParameter( ps, ++i, value );
      }
    }
    else
    {
      throw new SQLException( "Expecting primary key, unique key, or provided columns for WHERE clause." );
    }
  }

  private <T extends TableRow> void executeAndFetchRow( Connection c, UpdateContext<T> ctx, PreparedStatement ps, TxBindings table ) throws SQLException
  {
    int result = ps.executeUpdate();
    if( result != 1 )
    {
      throw new SQLException( "Expecting a single row result for Update/Insert, got " + result );
    }

    // here getGeneratedKeys() returns ALL columns because PreparedStatement was created with all column names as gen keys
    // this is necessary to retrieve columns that are autoincrement, generated, default values, etc.
    Bindings reflectedRow = DataBindings.EMPTY_BINDINGS;
    try( ResultSet resultSet = ps.getGeneratedKeys() )
    {
      Result<IBindingsBacked> resultRow =
        new Result<>( ctx.getAllCols(), resultSet, rowBindings -> () -> rowBindings );
      Iterator<IBindingsBacked> iterator = resultRow.iterator();
      if( iterator.hasNext() )
      {
        reflectedRow = iterator.next().getBindings();
        if( iterator.hasNext() )
        {
          throw new SQLException( "Expecting a single row, found more." );
        }
      }
    }
    catch( SQLFeatureNotSupportedException e )
    {
      LOGGER.warn( "getGeneratedKeys() is not supported, attempting to fetch updated row.", e );
    }

    if( reflectedRow.isEmpty() && ctx.getPkCols().isEmpty() )
    {
      // no pk means there's no way to fetch the inserted row
      //todo: throw here instead?
      return;
    }

    // some drivers (sqlite) don't fetch the gen key columns supplied in the prepared statement, so we issue a Select
    reflectedRow = maybeFetchInsertedRow( c, ctx, table, reflectedRow );
    if( reflectedRow.isEmpty() )
    {
      throw new SQLException( "Failed to reflect newly inserted row." );
    }

    table.holdValues( reflectedRow );
  }

  private <T extends TableRow> Bindings maybeFetchInsertedRow(
    Connection c, UpdateContext<T> ctx, TxBindings bindings, Bindings reflectedRow ) throws SQLException
  {
    DataBindings params = new DataBindings();
    ColumnInfo[] ci;
    if( reflectedRow.containsKey( SQLITE_LAST_INSERT_ROWID ) )
    {
      // specific to sqlite :\
      // all sqlite tables (except those marked WITHOUT ROWID) have a built-in "rowid" column.
      // sqlite ignores all the columns we specify for generated keys when creating a PreparedStatement and instead sends
      // the "last_insert_rowid()" column. Thanks, sqlite.
      params.put( "_rowid_", reflectedRow.get( SQLITE_LAST_INSERT_ROWID ) );
      ci = new ColumnInfo[] {new ColumnInfo( "_rowid_", Types.INTEGER, "integer", null )};
    }
    else if( reflectedRow.isEmpty() ||
      // specific to mysql :\
      reflectedRow.containsKey( MYSQL_GENERATED_KEY ) )
    {
      // getGeneratedKeys() failed, lets hope the pk is provided manually...

      Set<String> pkCols = ctx.getPkCols();
      if( pkCols.isEmpty() )
      {
        // no pk, can't query
        return reflectedRow;
      }

      Map<String, ColumnInfo> allCols = ctx.getAllCols();
      ci = new ColumnInfo[pkCols.size()];
      int i = 0;
      for( String pkCol : pkCols )
      {
        Object pkValue = bindings.get( pkCol );
        if( pkValue == null )
        {
          pkValue = reflectedRow.get( MYSQL_GENERATED_KEY );
          if( pkValue == null )
          {
            // null pk value means we can't query for the inserted row, game over
            return reflectedRow;
          }
        }
        else if( pkValue instanceof TableRow ) // from a KeyRef, see BasicTxBindings#get()
        {
          // in this case an fk is part of the pk, e.g., many-many link tables
          pkValue = bindings.getHeldValue( pkCol );
          if( pkValue == null )
          {
            // null pk value means we can't query for the inserted row, game over
            return reflectedRow;
          }
        }
        params.put( pkCol, pkValue );
        ci[i] = allCols.get( pkCol );
        i++;
      }
    }
    else
    {
      return reflectedRow;
    }

    QueryContext<T> queryContext = new QueryContext<>( ctx.getTxScope(), null, ctx.getDdlTableName(), ci, params, ctx.getConfigName(), null );
    Set<String> skipParams = new HashSet<>();
    String sql = makeReadStatement( c.getMetaData(), queryContext, skipParams );
    try( PreparedStatement ps = c.prepareStatement( sql ) )
    {
      setQueryParameters( queryContext, ps, skipParams );
      try( ResultSet resultSet = ps.executeQuery() )
      {
        Result<IBindingsBacked> resultRow = new Result<>( ctx.getAllCols(), resultSet, rowBindings -> () -> rowBindings );
        Iterator<IBindingsBacked> iterator = resultRow.iterator();
        if( !iterator.hasNext() )
        {
          throw new SQLException( "Expecting a single row, found none." );
        }
        reflectedRow = iterator.next().getBindings();
        if( iterator.hasNext() )
        {
          throw new SQLException( "Expecting a single row, found more." );
        }
      }
    }
    return reflectedRow;
  }

  public <T extends TableRow> void delete( Connection c, UpdateContext<T> ctx )
  {  
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append( "DELETE FROM " ).append( ctx.getDdlTableName() ).append( " WHERE\n" );

      Set<String> allColNames = ctx.getAllCols().keySet();

      Set<String> whereColumns;
      if( !ctx.getPkCols().isEmpty() )
      {
        whereColumns = ctx.getPkCols();
      }
      else if( !ctx.getUkCols().isEmpty() )
      {
        whereColumns = ctx.getUkCols();
      }
      else
      {
        whereColumns = allColNames;
      }
      Set<String> skipParams = new HashSet<>();
      if( !whereColumns.isEmpty() )
      {
        ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
        int i = 0;
        for( String whereCol: whereColumns )
        {
          if( i++ > 0 )
          {
            sql.append( " AND " );
          }
          ValueAccessor accessor = accProvider.get( ctx.getAllCols().get( whereCol ).getJdbcType() );
          String expr = accessor.getParameterExpression(
            c.getMetaData(), ctx.getTable().getBindings().getPersistedStateValue( whereCol ), ctx.getAllCols().get( whereCol ) );
          String qwhereCol = enquote( whereCol, c.getMetaData() );
          sql.append( "$qwhereCol = " ).append( expr );
          if( !expr.contains( "?" ) )
          {
            skipParams.add( whereCol );
          }
        }
      }
      else
      {
        throw new SQLException( "Expecting primary key, unique key, or provided columns for WHERE clause." );
      }

      try( PreparedStatement ps = c.prepareStatement( sql.toString(), allColNames.toArray( new String[0] ) ) )
      {
        setDeleteParameters( ctx, whereColumns, ps, skipParams );
        int result = ps.executeUpdate();
        if( result != 1 )
        {
          throw new SQLException( "Expecting a single row result for Delete, got " + result );
        }
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private <T extends TableRow> void setQueryParameters( QueryContext<T> ctx, PreparedStatement ps, Set<String> skipParams ) throws SQLException
  {
    int i = 0;
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    for( Map.Entry<String, Object> entry : ctx.getParams().entrySet() )
    {
      if( skipParams.contains( entry.getKey() ) )
      {
        continue;
      }
      ValueAccessor accessor = accProvider.get( ctx.getParamInfo()[i].getJdbcType() );
      accessor.setParameter( ps, ++i, entry.getValue() );
    }
  }

  private String enquote( String id, DatabaseMetaData metaData )
  {
    // "foo" is a standard SQL identifier (column/table/etc)
    // [foo] is an identifier in MS SQL
    // `foo` is an identifier in MySQL

    try
    {
      char open;
      char close;
      String prod = metaData.getDatabaseProductName();
      switch( prod.toLowerCase() )
      {
        case "microsoft sql server":
          open = '[';
          close = ']';
          break;
        case "mysql":
          open = close = '`';
          break;
        default:
          open = close = '"';
      }
      return open + id + close;
    }
    catch( SQLException e )
    {
      throw new RuntimeException( e );
    }
  }
}
