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

import manifold.util.ManExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.Iterator;
import java.util.function.Function;

public class Runner<T extends ResultRow>
{
  private final QueryContext<T> _ctx;
  private final String _sqlQuery;

  public Runner( QueryContext<T> ctx, String sqlQuery )
  {
    _ctx = ctx;
    _sqlQuery = sqlQuery;
  }

  @SuppressWarnings( "unused" )
  public Result<T> fetch()
  {
    return runQueryWithConnection( c -> runQuery( c ) );
  }

  @SuppressWarnings( "unused" )
  public T fetchOne()
  {
    return runQueryWithConnection( c -> runQueryOne( c ) );
  }

  private <RT> RT runQueryWithConnection( Function<Connection, RT> query )
  {
    OperableTxScope txScope = (OperableTxScope)_ctx.getTxScope();
    Connection activeConnection = txScope.getActiveConnection();
    if( activeConnection != null )
    {
      try
      {
        TxScope.SqlChangeCtx ctx = txScope.newSqlChangeCtx( activeConnection );
        ctx.doCrud();
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
      try( Connection c = cp.getConnection( _ctx.getConfigName(), _ctx.getQueryClass() ) )
      {
        return query.apply( c );
      }
      catch( Exception e )
      {
        throw ManExceptionUtil.unchecked( e );
      }
    }
  }

  @NotNull
  private Result<T> runQuery( Connection c )
  {
    try
    {
      try( PreparedStatement ps = c.prepareStatement( _sqlQuery ) )
      {
        setParameters( ps );
        try( ResultSet resultSet = ps.executeQuery() )
        {
          return new Result<>( _ctx, resultSet );
        }
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @Nullable
  private T runQueryOne( Connection c )
  {
    try
    {
      try( PreparedStatement ps = c.prepareStatement( _sqlQuery ) )
      {
        setParameters( ps );
        try( ResultSet resultSet = ps.executeQuery() )
        {
          Result<T> rs = new Result<>( _ctx, resultSet );
          Iterator<T> iterator = rs.iterator();
          if( !iterator.hasNext() )
          {
            return null;
          }
          T one = iterator.next();
          if( iterator.hasNext() )
          {
            throw new SQLException( "Results contain more than one row." );
          }
          return one;
        }
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private void setParameters( PreparedStatement ps ) throws SQLException
  {
    int i = 0;
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    for( Object param : _ctx.getParams().values() )
    {
      ValueAccessor accessor = accProvider.get( _ctx.getParamInfo()[i].getJdbcType() );
      accessor.setParameter( ps, ++i, param );
    }
  }
}
