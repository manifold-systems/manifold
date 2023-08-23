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
import manifold.util.ManExceptionUtil;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static manifold.sql.rt.api.BasicTxBindings.TxKind.Update;

public class Result<R extends IBindingsBacked> implements Iterable<R>
{
  private final List<R> _results;

  public Result( TxScope txScope, ResultSet resultSet, Function<TxBindings, R> makeRow )
  {
    _results = new ArrayList<>();
    rip( resultSet, rowBindings -> new BasicTxBindings( txScope, Update, rowBindings ), makeRow );
  }

  public Result( ResultSet resultSet, Function<Bindings, R> makeRow )
  {
    _results = new ArrayList<>();
    rip( resultSet, rowBindings -> rowBindings, makeRow );
  }

  private <B extends Bindings> void rip( ResultSet resultSet, Function<DataBindings, B> makeBindings, Function<B, R> makeRow )
  {
    try
    {
      ResultSetMetaData metaData = resultSet.getMetaData();
      for( boolean isOnRow = resultSet.next(); isOnRow; isOnRow = resultSet.next() )
      {
        DataBindings row = new DataBindings();
        ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
        for( int i = 1; i <= metaData.getColumnCount(); i++ )
        {
          String column = metaData.getColumnLabel( i );
          ValueAccessor accessor = accProvider.get( metaData.getColumnType( i ) );
          Object value = accessor.getRowValue( resultSet, new ResultColumn( metaData, i ) );
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
