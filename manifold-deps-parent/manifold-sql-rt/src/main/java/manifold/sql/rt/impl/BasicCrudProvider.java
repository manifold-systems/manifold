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
import manifold.sql.rt.api.*;
import manifold.util.ManExceptionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BasicCrudProvider implements CrudProvider
{
  @SuppressWarnings( "unused" )
  public <T extends TableRow> void create( Connection c, UpdateContext<T> ctx )
  {
    try
    {
      T table = ctx.getTable();
      String[] allColumnNames = ctx.getAllColsWithJdbcType().keySet().toArray( new String[0] );

      String sql = makeInsertStmt( ctx.getDdlTableName(), table );
      try( PreparedStatement ps = c.prepareStatement( sql, allColumnNames ) )
      {
        setInsertParameters( ctx, ps );
        executeAndFetchRow( ps, table.getBindings() );
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private <T extends TableRow> void setInsertParameters( UpdateContext<T> ctx, PreparedStatement ps ) throws SQLException
  {
    int i = 0;
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    for( Map.Entry<String, Object> entry: ctx.getTable().getBindings().entrySet() )
    {
      int jdbcType = ctx.getAllColsWithJdbcType().get( entry.getKey() );
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
        value = 0;
      }
    }
    return value;
  }

  private String makeInsertStmt( String ddlTableName, TableRow table )
  {
    StringBuilder sql = new StringBuilder();
    sql.append( "INSERT INTO " ).append( ddlTableName ).append( "(" );
    int i = 0;
    Set<Map.Entry<String, Object>> entries = table.getBindings().entrySet();
    for( Map.Entry<String, Object> entry: entries )
    {
      String colName = entry.getKey();
      if( i++ > 0 )
      {
        sql.append( ", " );
      }
      sql.append( colName );
    }
    sql.append( ")" ).append( " VALUES (" );
    for( i = 0; i < entries.size(); i++ )
    {
      if( i > 0 )
      {
        sql.append( "," );
      }
      sql.append( '?' );
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

      String sql = makeReadStatement( ctx );
      try( PreparedStatement ps = c.prepareStatement( sql ) )
      {
        setQueryParameters( ctx, ps );
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

      String sql = makeReadStatement( ctx );
      try( PreparedStatement ps = c.prepareStatement( sql ) )
      {
        setQueryParameters( ctx, ps );
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

  private <T extends TableRow> String makeReadStatement( QueryContext<T> ctx )
  {
    StringBuilder sql = new StringBuilder();
    sql.append( "SELECT * FROM " ).append( ctx.getDdlTableName() ).append( " WHERE " );
    int i = 0;
    for( String colName : ctx.getParams().keySet() )
    {
      if( i++ > 0 )
      {
        sql.append( " AND " );
      }
      sql.append( colName ).append( " = ?" );
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
      Set<Map.Entry<String, Object>> changeEntries = table.getBindings().uncommittedChangesEntrySet();
      if( changeEntries.isEmpty() )
      {
        throw new SQLException( "Expecting changed entries." );
      }
      for( Map.Entry<String, Object> entry : changeEntries )
      {
        if( i++ > 0 )
        {
          sql.append( ",\n" );
        }
        String colName = entry.getKey();
        sql.append( "\"$colName\" = ?" );
      }
      sql.append( "\nWHERE " );

      Set<String> allColNames = ctx.getAllColsWithJdbcType().keySet();

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
          sql.append( "\"$whereCol\" = ?" );
        }
      }
      else
      {
        throw new SQLException( "Expecting primary key, unique key, or provided columns for WHERE clause." );
      }

      try( PreparedStatement ps = c.prepareStatement( sql.toString(), allColNames.toArray( new String[0] ) ) )
      {
        setUpdateParameters( ctx, whereColumns, ps );
        executeAndFetchRow( ps, table.getBindings() );
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private <T extends TableRow> void setUpdateParameters( UpdateContext<T> ctx, Set<String> whereColumns, PreparedStatement ps ) throws SQLException
  {
    int paramIndex = 0;
    Set<Map.Entry<String, Object>> changeEntries = ctx.getTable().getBindings().uncommittedChangesEntrySet();
    if( changeEntries.isEmpty() )
    {
      throw new SQLException( "Expecting changed entries." );
    }
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    for( Map.Entry<String, Object> entry : changeEntries )
    {
      ValueAccessor accessor = accProvider.get( ctx.getAllColsWithJdbcType().get( entry.getKey() ) );
      Object value = entry.getValue();
      accessor.setParameter( ps, ++paramIndex, value );
    }
    if( !whereColumns.isEmpty() )
    {
      for( String whereColumn : whereColumns )
      {
        ValueAccessor accessor = accProvider.get( ctx.getAllColsWithJdbcType().get( whereColumn ) );
        Object value = ctx.getTable().getBindings().getPersistedStateValue( whereColumn );
        accessor.setParameter( ps, ++paramIndex, value );
      }
    }
    else
    {
      throw new SQLException( "Expecting primary key, unique key, or provided columns for WHERE clause." );
    }

  }

  private <T extends TableRow> void setDeleteParameters( UpdateContext<T> ctx, Set<String> whereColumns, PreparedStatement ps ) throws SQLException
  {
    int paramIndex = 0;
    if( !whereColumns.isEmpty() )
    {
      ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
      for( String whereColumn : whereColumns )
      {
        ValueAccessor accessor = accProvider.get( ctx.getAllColsWithJdbcType().get( whereColumn ) );
        Object value = ctx.getTable().getBindings().getPersistedStateValue( whereColumn );
        accessor.setParameter( ps, ++paramIndex, value );
      }
    }
    else
    {
      throw new SQLException( "Expecting primary key, unique key, or provided columns for WHERE clause." );
    }
  }

  private void executeAndFetchRow( PreparedStatement ps, TxBindings table ) throws SQLException
  {
    int result = ps.executeUpdate();
    if( result != 1 )
    {
      throw new SQLException( "Expecting a single row result for Update/Insert, got " + result );
    }

    // here getGeneratedKeys() returns ALL columns because PreparedStatement was created with all columns names as gen keys
    try( ResultSet resultSet = ps.getGeneratedKeys() )
    {
      Result<IBindingsBacked> resultRow = new Result<>( resultSet, rowBindings -> () -> rowBindings );
      Iterator<IBindingsBacked> iterator = resultRow.iterator();
      if( !iterator.hasNext() )
      {
        throw new SQLException( "Expecting a single row, found none." );
      }
      IBindingsBacked updatedRow = iterator.next();
      if( iterator.hasNext() )
      {
        throw new SQLException( "Expecting a single row, found more." );
      }
      table.holdValues( updatedRow.getBindings() );
    }
  }

  public <T extends TableRow> void delete( Connection c, UpdateContext<T> ctx )
  {  
    try
    {
      StringBuilder sql = new StringBuilder();
      sql.append( "DELETE FROM " ).append( ctx.getDdlTableName() ).append( " WHERE\n" );

      Set<String> allColNames = ctx.getAllColsWithJdbcType().keySet();

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
        int i = 0;
        //noinspection unused
        for( String whereCol: whereColumns )
        {
          if( i++ > 0 )
          {
            sql.append( ", " );
          }
          sql.append( "\"$whereCol\" = ?" );
        }
      }
      else
      {
        throw new SQLException( "Expecting primary key, unique key, or provided columns for WHERE clause." );
      }

      try( PreparedStatement ps = c.prepareStatement( sql.toString(), allColNames.toArray( new String[0] ) ) )
      {
        setDeleteParameters( ctx, whereColumns, ps );
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

  private <T extends TableRow> void setQueryParameters( QueryContext<T> ctx, PreparedStatement ps ) throws SQLException
  {
    int i = 0;
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    for( Object param : ctx.getParams().values() )
    {
      ValueAccessor accessor = accProvider.get( ctx.getJdbcParamTypes()[i] );
      accessor.setParameter( ps, ++i, param );
    }
  }
}
