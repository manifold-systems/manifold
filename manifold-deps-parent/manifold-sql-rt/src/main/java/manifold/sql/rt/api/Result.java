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

package manifold.sql.rt.api;

import manifold.ext.rt.api.IBindingsBacked;
import manifold.json.rt.api.DataBindings;
import manifold.rt.api.Bindings;
import manifold.sql.rt.util.DbUtil;
import manifold.util.ManExceptionUtil;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

import static manifold.sql.rt.api.BasicTxBindings.TxKind.Update;

/**
 * Fetches all rows from a provided {@code ResultSet} into a {@code List}. The list is indirectly accessible this
 * class' {@code Iterable} implementation. It is also directly accessible via the {@link #toList()} method.
 *
 * @param <R> the formal type of the result set. For instance, a SQL schema table type such as {@code Customer}, or a
 * {@code Row} of a SQL query type derived from .sql resource file or embedded .sql resource.
 */
public class Result<R extends IBindingsBacked> implements Iterable<R>
{
  private final List<R> _results;

  public Result( QueryContext ctx, ResultSet resultSet )
  {
    _results = new ArrayList<>();
    rip( ctx.getAllCols(), resultSet, rowBindings -> new BasicTxBindings( ctx.getTxScope(), Update, rowBindings ), ctx.getRowMaker() );
  }

  public Result( Map<String, ColumnInfo> allCols, ResultSet resultSet, Function<Bindings, R> makeRow )
  {
    _results = new ArrayList<>();
    rip( allCols, resultSet, rowBindings -> rowBindings, makeRow );
  }

  private <B extends Bindings> void rip( Map<String, ColumnInfo> allCols, ResultSet resultSet, Function<DataBindings, B> makeBindings, Function<B, R> makeRow )
  {
    try
    {
      ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
      ResultSetMetaData metaData = resultSet.getMetaData();
      int columnCount = metaData.getColumnCount();
      ValueAccessor[] accessors = buildAccessors( allCols, accProvider, metaData, columnCount );
      for( boolean isOnRow = resultSet.next(); isOnRow; isOnRow = resultSet.next() )
      {
        DataBindings row = new DataBindings();
        for( int i = 1; i <= columnCount; i++ )
        {
          String column = DbUtil.handleAnonQueryColumn( metaData.getColumnLabel( i ), i );
          Object value = accessors[i-1].getRowValue( resultSet, new ResultColumn( metaData, i ) );
          row.put( column, value );
        }
        R resultRow = makeRow.apply( makeBindings.apply( row ) );
        if( resultRow instanceof TableRow )
        {
          ((TableRow)resultRow).getBindings().setOwner( (TableRow)resultRow );
        }
        _results.add( resultRow );
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private static ValueAccessor[] buildAccessors( Map<String, ColumnInfo> allCols, ValueAccessorProvider accProvider, ResultSetMetaData metaData, int columnCount ) throws SQLException
  {
    ValueAccessor[] accessors = new ValueAccessor[columnCount];
    for( int i = 0; i < columnCount; i++ )
    {
      Integer jdbcType = null;
      if( allCols != null )
      {
        // prefer the schema table's declared type for the queried column,
        // it is essential that the type is assignable to the corresponding property return / param types
        String colName = metaData.getColumnName( i+1 );
        if( colName != null )
        {
          ColumnInfo columnInfo = allCols.get( colName );
          // can be null e.g., sqlite's "last_insert_rowid()" bullshit
          jdbcType = columnInfo == null ? null : columnInfo.getJdbcType();
        }
      }

      if( jdbcType == null )
      {
        jdbcType = metaData.getColumnType( i+1 );
      }

      accessors[i] = accProvider.get( jdbcType );
    }
    return accessors;
  }

  @Override
  public Iterator<R> iterator()
  {
    return _results.iterator();
  }

  @SuppressWarnings( "unused" )
  public List<R> toList()
  {
    return _results;
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o ) return true;
    if( !(o instanceof Result) ) return false;
    Result<?> result = (Result<?>)o;
    return _results.equals( result._results );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _results );
  }

  @Override
  public String toString()
  {
    if( _results.isEmpty() )
    {
      return "<empty>";
    }

    StringBuilder header = new StringBuilder();
    for( String title: _results.get( 0 ).getBindings().keySet() )
    {
      if( header.length() > 0 )
      {
        header.append( ", " );
      }
      header.append( title ).append( "\n" );
    }
    StringBuilder rows = new StringBuilder();
    for( R value: _results )
    {
      rows.append( value.display() ).append( "\n" );
    }
    return header.toString() + rows;
  }

}
