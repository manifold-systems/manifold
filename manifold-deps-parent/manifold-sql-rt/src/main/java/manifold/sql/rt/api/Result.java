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

public class Result<T extends ResultRow> implements Iterable<T>
{
  private final List<T> _results;

  public Result( ResultSet resultSet, Function<Bindings, T> makeRow )
  {
    _results = new ArrayList<>();
    rip( resultSet, makeRow );
  }

  private void rip( ResultSet resultSet, Function<Bindings, T> makeRow )
  {
    try
    {
      ResultSetMetaData metaData = resultSet.getMetaData();
      for( boolean isOnRow = resultSet.next(); isOnRow; isOnRow = resultSet.next() )
      {
        DataBindings row = new DataBindings();
        for( int i = 1; i <= metaData.getColumnCount(); i++ )
        {
          String column = metaData.getColumnLabel( i );
          ValueAccessor accessor = ValueAccessor.get( metaData.getColumnType( i ) );
          Object value = accessor.getRowValue( resultSet, new ResultColumn( metaData, i ) );
          row.put( column, value );
        }
        _results.add( makeRow.apply( row ) );
      }
    }
    catch( SQLException e )
    {
      //## todo: handle
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @Override
  public Iterator<T> iterator()
  {
    return _results.iterator();
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
    for( T value: _results )
    {
      rows.append( value.display() ).append( "\n" );
    }
    return header.toString() + rows;
  }

}
