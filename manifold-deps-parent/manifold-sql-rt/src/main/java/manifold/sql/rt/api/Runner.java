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

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class Runner<T extends SqlQueryResult>
{
//  private final SqlQuery<T> _query;
  private final Class<T> _queryClass;
  private final Bindings _params;
  private final String _querySource;
  private final String _configName;
  private final Function<Bindings, T> _makeResult;

  public Runner( Class<T> queryClass, Bindings params, String querySource, String configName, Function<Bindings, T> makeResult )
  {
    _queryClass = queryClass;
    _params = params;
    _querySource = querySource;
    _configName = configName;
    _makeResult = makeResult;
  }

  @SuppressWarnings( "unused" )
  public Iterable<T> run()
  {
    ConnectionProvider cp = ConnectionProvider.PROVIDERS.get().stream()
      .findFirst()
      .orElseThrow( () -> new RuntimeException( "Could not find SQL connection provider" ) );

    try( Connection c = cp.getConnection( _configName, _queryClass ) )
    {
      for( ConnectionNotifier p : ConnectionNotifier.PROVIDERS.get() )
      {
        p.init( c );
      }

      PreparedStatement ps = c.prepareStatement( _querySource );
      int i = 0;
      for( Object param : _params.values() )
      {
        ps.setObject( ++i, param );
      }
      ResultSet resultSet = ps.executeQuery();
      return new Result( resultSet, ps );
    }
    catch( SQLException e )
    {
      // todo: handle this
      throw ManExceptionUtil.unchecked( e );
    }
  }

  public class Result implements Iterable<T>
  {
    private final List<T> _results;

    public Result( ResultSet resultSet, PreparedStatement ps )
    {
      _results = new ArrayList<>();
      rip( resultSet, ps );
    }

    private void rip( ResultSet resultSet, PreparedStatement ps )
    {
      try
      {
        TypeMap typeMap = TypeMap.findFirst();

        ResultSetMetaData metaData = resultSet.getMetaData();
        for( boolean isOnRow = resultSet.next(); isOnRow; isOnRow = resultSet.next() )
        {
          DataBindings row = new DataBindings();
          for( int i = 1; i <= metaData.getColumnCount(); i++ )
          {
            String column = metaData.getColumnLabel( i );
            int datatype = metaData.getColumnType( i );
            Class<?> type = typeMap.getType( new ResultColumn( metaData, i ), datatype );
            if( type.isPrimitive() )
            {
              row.put( column, resultSet.getObject( i ) );
            }
            else
            {
              row.put( column, resultSet.getObject( i, type ) );
            }
          }
          _results.add( _makeResult.apply( row ) );
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

    private class ResultColumn implements RawElement
    {
      private final ResultSetMetaData _metaData;
      private final int _i;

      public ResultColumn( ResultSetMetaData metaData, int i )
      {
        _metaData = metaData;
        _i = i;
      }

      @Override
      public String getName()
      {
        try
        {
          return _metaData.getColumnLabel( _i );
        }
        catch( SQLException e )
        {
          throw ManExceptionUtil.unchecked( e );
        }
      }

      @Override
      public boolean isNullable()
      {
        try
        {
          return _metaData.isNullable( _i ) != ResultSetMetaData.columnNoNulls;
        }
        catch( SQLException e )
        {
          throw ManExceptionUtil.unchecked( e );
        }
      }
    }
  }
}
