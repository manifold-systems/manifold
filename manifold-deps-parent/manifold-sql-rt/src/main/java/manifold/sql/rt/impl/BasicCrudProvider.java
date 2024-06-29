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
import manifold.sql.rt.util.DbUtil;
import manifold.sql.rt.util.DriverInfo;
import manifold.util.ManExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.function.Function;

import static manifold.sql.rt.util.DriverInfo.DuckDB;
import static manifold.sql.rt.util.DriverInfo.Oracle;

public class BasicCrudProvider implements CrudProvider
{
  private final Logger LOGGER = LoggerFactory.getLogger( BasicCrudProvider.class );

  public static final String SQLITE_LAST_INSERT_ROWID = "last_insert_rowid()";

  @SuppressWarnings( "unused" )
  public <T extends Entity> void create( Connection c, UpdateContext<T> ctx )
  {
    try
    {
      Set<String> skipParams = new HashSet<>();
      String sql = makeInsertStmt( c.getMetaData(), ctx, skipParams );
      int[] reflectedColumnCount = {0};
      try( PreparedStatement ps = prepareStatement( c, ctx, sql, reflectedColumnCount ) )
      {
        setInsertParameters( ctx, ps, skipParams );
        executeAndFetchRow( c, ctx, ps, reflectedColumnCount[0] > 0 );
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  /**
   * Calls {@code c.prepareStatement(sql, columnNames)} with {@code columnNames} having all columns for the inserted/updated
   * row. If the driver does not handle that and throws an exception, we try calling {@code c.prepareStatement(...)} again
   * passing just the pk key column, if applicable. If not, we don't pass any column names. Note, if the driver doesn't
   * throw an exception from the former call and quietly ignores all the column names and instead just supplies the pk,
   * we handle that too when we process the call to {@code getGeneratedKeys()}.
   */
  private static <T extends Entity> PreparedStatement prepareStatement( Connection c, UpdateContext<T> ctx, String sql, int[] reflectedColumnCount ) throws SQLException
  {
    String[] reflectedColumns = reflectedColumns( c, ctx, true );
    try
    {
      return c.prepareStatement( sql, reflectedColumns );
    }
    catch( SQLException e )
    {
      if( DriverInfo.lookup( c.getMetaData() ) == DuckDB )
      {
        // sigh duckdb... no way to get inserted data from insert/update, have to add RETURNING clause :(
        if( ctx.getTable().getBindings().isForInsert() )
        {
          //todo: parse this better to handle trailing comments
          sql = sql.trim();
          if( sql.endsWith( ";" ) )
          {
            sql = sql.substring( 0, sql.length() - 1 );
          }
          sql = sql + " RETURNING *";
        }
        return c.prepareStatement( sql );
      }

      reflectedColumns = reflectedColumns( c, ctx, false );
      if( reflectedColumns.length == 0 )
      {
        return c.prepareStatement( sql );
      }
      return c.prepareStatement( sql, reflectedColumns );
    }
    finally
    {
      reflectedColumnCount[0] = reflectedColumns.length;
    }
  }

  /**
   * The names of columns that should be returned from the inserted row via {@code getGeneratedKeys()}, this array is passed
   * into {@code prepareStatement(sql, columnNames)}. Ideally, we want more than the "generated keys" the {@link Connection#prepareStatement(String, String[])}
   * method sort of documents; the parameter documentation is more loosely defined as <i>an array of column names indicating the
   * columns that should be returned from the inserted row</i>. Some (good) drivers adhere to this latter description and
   * return any and all columns asked for, others drivers vary in behavior here.
   */
  private static <T extends Entity> String[] reflectedColumns( Connection c, UpdateContext<T> ctx, boolean allColumns ) throws SQLException
  {
    String[] reflectedColumnNames = {};
    if( allColumns && DriverInfo.lookup( c.getMetaData() ) != Oracle )
    {
      // ask for all columns since we want the entire record to include whatever generated data that was not included in the insert

      reflectedColumnNames = ctx.getAllCols().keySet().toArray( new String[0] );
    }
    else
    {
      // ask for pk column, so we can query for the whole record

      if( ctx.getPkCols().size() == 1 )
      {
        ColumnInfo pkColumnInfo = ctx.getAllCols().get( ctx.getPkCols().iterator().next() );
        Boolean required = pkColumnInfo.isRequired();
        if( required != null && !required )
        {
          // some DBs (sql server) only reflect the pk column for getGeneratedKeys() and throw exception if we ask for more
          // in this case we use the pk to make a separate query for the full record
          reflectedColumnNames = ctx.getPkCols().toArray( new String[0] );
        }
      }
    }

    return reflectedColumnNames;
  }

  private <T extends Entity> void setInsertParameters( UpdateContext<T> ctx, PreparedStatement ps, Set<String> skipParams ) throws SQLException
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
      value = patchFk( value, entry.getKey(), ctx.getBindings() );
      accessor.setParameter( ps, ++i, value );
    }
  }

  private static Object patchFk( Object value, String colName, OperableTxBindings bindings )
  {
    // We assign a Pair<Entity, String> to an fk column when the entity is not yet inserted.
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

  private <T extends Entity> String makeInsertStmt( DatabaseMetaData metaData, UpdateContext<T> ctx, Set<String> skipParams ) throws SQLException
  {
    StringBuilder sql = new StringBuilder();
    sql.append( "INSERT INTO " ).append( DbUtil.enquoteIdentifier( ctx.getDdlTableName(), metaData ) ).append( "(" );
    int i = 0;
    Set<Map.Entry<String, Object>> entries = ctx.getTable().getBindings().entrySet();
    for( Map.Entry<String, Object> entry: entries )
    {
      String colName = entry.getKey();
      if( i++ > 0 )
      {
        sql.append( ", " );
      }
      sql.append( DbUtil.enquoteIdentifier( colName, metaData ) );
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
  public <T extends Entity> T readOne( QueryContext<T> ctx )
  {
    return runQueryWithConnection( ctx, c -> {
      try
      {
        Set<String> skipParams = new HashSet<>();
        String sql = makeReadStatement( c.getMetaData(), ctx, skipParams );
        try( PreparedStatement ps = c.prepareStatement( sql ) )
        {
          setQueryParameters( ctx, ps, skipParams );
          try( ResultSet resultSet = ps.executeQuery() )
          {
            Result<T> ts = new Result<>( ctx, resultSet );
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
    } );
  }

  @SuppressWarnings( "unused" )
  public <T extends Entity> List<T> readMany( QueryContext<T> ctx )
  {
    return runQueryWithConnection( ctx, c -> {
      try
      {
        Set<String> skipParams = new HashSet<>();
        String sql = makeReadStatement( c.getMetaData(), ctx, skipParams );
        try( PreparedStatement ps = c.prepareStatement( sql ) )
        {
          setQueryParameters( ctx, ps, skipParams );
          try( ResultSet resultSet = ps.executeQuery() )
          {
            Result<T> ts = new Result<>( ctx, resultSet );
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
    } );
  }

  private <T extends Entity, RT> RT runQueryWithConnection( QueryContext<T> ctx, Function<Connection, RT> query )
  {
    OperableTxScope txScope = (OperableTxScope)ctx.getTxScope();
    Connection activeConnection = txScope.getActiveConnection();
    if( activeConnection != null )
    {
      try
      {
        txScope.newSqlChangeCtx( activeConnection ).doCrud();
        return query.apply( activeConnection );
      }
      catch( Exception e )
      {
        throw ManExceptionUtil.unchecked( e );
      }
    }
    else
    {
      ConnectionProvider cp = Dependencies.instance().getConnectionProvider();
      try( Connection c = cp.getConnection( ctx.getConfigName(), ctx.getQueryClass() ) )
      {
        return query.apply( c );
      }
      catch( Exception e )
      {
        throw ManExceptionUtil.unchecked( e );
      }
    }
  }

  private <T extends Entity> String makeReadStatement( DatabaseMetaData metaData, QueryContext<T> ctx, Set<String> skipParams ) throws SQLException
  {
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    StringBuilder sql = new StringBuilder();
    sql.append( "SELECT * FROM " ).append( DbUtil.enquoteIdentifier(ctx.getDdlTableName(), metaData) ).append( " WHERE " );
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
      sql.append( DbUtil.enquoteIdentifier( entry.getKey(), metaData ) ).append( " = " ).append( expr );
      i++;
      if( !expr.contains( "?" ) )
      {
        skipParams.add( entry.getKey() );
      }
    }
    return sql.toString();
  }

  @SuppressWarnings( "unused" )
  public <T extends Entity> void update( Connection c, UpdateContext<T> ctx )
  {
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append( "UPDATE " ).append( DbUtil.enquoteIdentifier(ctx.getDdlTableName(), c.getMetaData()) ).append( " SET\n" );
      int i = 0;
      Map<String, Object> changeEntries = ctx.getBindings().uncommittedChangesEntrySet();
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
        String qcolName = DbUtil.enquoteIdentifier( colName, c.getMetaData() );
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
            c.getMetaData(), ctx.getBindings().getPersistedStateValue( whereCol ), columnInfo );
          String qwhereCol = DbUtil.enquoteIdentifier( whereCol, c.getMetaData() );
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

      int[] reflectedColumnCount = {0};
      try( PreparedStatement ps = prepareStatement( c, ctx, sql.toString(), reflectedColumnCount ) )
      {
        setUpdateParameters( ctx, whereColumns, ps, skipParams );
        executeAndFetchRow( c, ctx, ps, reflectedColumnCount[0] > 0 );
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private <T extends Entity> void setUpdateParameters( UpdateContext<T> ctx, Set<String> whereColumns, PreparedStatement ps, Set<String> skipParams ) throws SQLException
  {
    Map<String, Object> changeEntries = ctx.getBindings().uncommittedChangesEntrySet();
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
        Object value = ctx.getBindings().getPersistedStateValue( whereColumn );
        accessor.setParameter( ps, ++i, value );
      }
    }
    else
    {
      throw new SQLException( "Expecting primary key, unique key, or provided columns for WHERE clause." );
    }

  }

  private <T extends Entity> void setDeleteParameters( UpdateContext<T> ctx, Set<String> whereColumns, PreparedStatement ps, Set<String> skipParams ) throws SQLException
  {
    int i = 0;
    if( !whereColumns.isEmpty() )
    {
      ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
      for( String whereColumn : whereColumns )
      {
        ValueAccessor accessor = accProvider.get( ctx.getAllCols().get( whereColumn ).getJdbcType() );
        Object value = ctx.getBindings().getPersistedStateValue( whereColumn );
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

  private <T extends Entity> void executeAndFetchRow(
    Connection c, UpdateContext<T> ctx, PreparedStatement ps, boolean hasReflectedColumns ) throws SQLException
  {
    // here getGeneratedKeys() returns ALL columns because PreparedStatement was created with all column names as gen keys
    // this is necessary to retrieve columns that are autoincrement, generated, default values, etc.
    Bindings reflectedRow = DataBindings.EMPTY_BINDINGS;

    if( DriverInfo.lookup( c.getMetaData() ) == DuckDB )
    {
      // for duckdb we add a `RETURNING *` clause to get the reflected columns, must use p.execute() instead of executeUpdate() here
      boolean hasResultSet = ps.execute();
      if( hasResultSet )
      {
        try( ResultSet resultSet = ps.getResultSet() )
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
      }
    }
    else
    {
      int result = ps.executeUpdate();
      if( result != 1 )
      {
        throw new SQLException( "Expecting a single row result for Update/Insert, got " + result );
      }

      if( hasReflectedColumns )
      {
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
      }
    }

    if( isReflectedRowEmpty( reflectedRow ) && ctx.getPkCols().isEmpty() )
    {
      // no pk means there's no way to fetch the inserted row
      //todo: throw here instead?
      return;
    }

    // some drivers (sqlite) don't fetch the gen key columns supplied in the prepared statement, so we issue a Select
    reflectedRow = maybeFetchInsertedRow( c, ctx, reflectedRow );
    if( isReflectedRowEmpty( reflectedRow ) )
    {
      throw new SQLException( "Failed to reflect newly inserted row." );
    }

    ctx.getBindings().holdValues( reflectedRow );
  }

  private <T extends Entity> Bindings maybeFetchInsertedRow( Connection c, UpdateContext<T> ctx, Bindings reflectedRow ) throws SQLException
  {
    DataBindings params = new DataBindings();
    ColumnInfo[] ci = null;
    if( reflectedRow.containsKey( SQLITE_LAST_INSERT_ROWID ) )
    {
      // specific to sqlite :\
      // all sqlite tables (except those marked WITHOUT ROWID) have a built-in "rowid" column.
      // sqlite ignores all the columns we specify for generated keys when creating a PreparedStatement and instead sends
      // the "last_insert_rowid()" column, which is probably not the pk. Thanks, sqlite, thanks.
      params.put( "_rowid_", reflectedRow.get( SQLITE_LAST_INSERT_ROWID ) );
      ci = new ColumnInfo[] {new ColumnInfo( "_rowid_", Types.INTEGER, "integer", null )};
    }
    else if( isReflectedRowEmpty( reflectedRow ) && !ctx.getPkCols().isEmpty() )
    {
      // getGeneratedKeys() failed
      // let's hope the pk was required input

      Set<String> pkCols = ctx.getPkCols();
      Map<String, ColumnInfo> allCols = ctx.getAllCols();
      ci = new ColumnInfo[pkCols.size()];
      int i = 0;
      for( String pkCol : pkCols )
      {
        Object pkValue = ctx.getBindings().get( pkCol );
        if( pkValue == null )
        {
          // null pk value means we can't query for the inserted row, game over
          return reflectedRow;
        }
        else if( pkValue instanceof Entity ) // from a KeyRef, see BasicTxBindings#get()
        {
          // in this case an fk is part of the pk, e.g., many-many link tables
          pkValue = ctx.getBindings().getHeldValue( pkCol );
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
    else if( reflectedRow.size() == 1 && ctx.getAllCols().size() > 1 && ctx.getPkCols().size() == 1 )
    {
      // this block handles the case where getGeneratedKeys() only returns a single key that may have a db-specific name
      // like sql server's GENERATED_KEYS or mysql's GENERATED_KEY, etc.

      int i = 0;
      for( Map.Entry<String, Object> entry : reflectedRow.entrySet() )
      {
        String pkCol = ctx.getPkCols().iterator().next();
        ColumnInfo pkColumnInfo = ctx.getAllCols().get( pkCol );
        if( pkColumnInfo != null )
        {
          ci = new ColumnInfo[] {pkColumnInfo};
          params.put( pkCol, entry.getValue() );
          break;
        }
      }
      if( ci == null )
      {
        throw new SQLException( "Failed to retrieve generated primary key" );
      }
    }
    else
    {
      return reflectedRow;
    }

    QueryContext<T> queryContext = new QueryContext<>( ctx.getTxScope(), null, ctx.getDdlTableName(), null, ci, params, ctx.getConfigName(), null );
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

  private static boolean isReflectedRowEmpty( Bindings reflectedRow )
  {
    return reflectedRow.isEmpty() ||
      reflectedRow.size() == 1 && reflectedRow.entrySet().iterator().next().getValue() == null;
  }

  public <T extends Entity> void delete( Connection c, UpdateContext<T> ctx )
  {  
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append( "DELETE FROM " ).append( DbUtil.enquoteIdentifier( ctx.getDdlTableName(), c.getMetaData()) ).append( " WHERE\n" );

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
            c.getMetaData(), ctx.getBindings().getPersistedStateValue( whereCol ), ctx.getAllCols().get( whereCol ) );
          String qwhereCol = DbUtil.enquoteIdentifier( whereCol, c.getMetaData() );
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

      try( PreparedStatement ps = c.prepareStatement( sql.toString() ) )
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

  private <T extends Entity> void setQueryParameters( QueryContext<T> ctx, PreparedStatement ps, Set<String> skipParams ) throws SQLException
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
}
