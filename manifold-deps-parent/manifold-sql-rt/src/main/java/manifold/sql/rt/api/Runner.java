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

import java.sql.*;

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
  public Result<T> run()
  {
    ConnectionProvider cp = ConnectionProvider.findFirst();
    try( Connection c = cp.getConnection( _ctx.getConfigName(), _ctx.getQueryClass() ) )
    {
      for( ConnectionNotifier p : ConnectionNotifier.PROVIDERS.get() )
      {
        p.init( c );
      }

      try( PreparedStatement ps = c.prepareStatement( _sqlQuery ) )
      {
        setParameters( ps );
        try( ResultSet resultSet = ps.executeQuery() )
        {
          return new Result<T>( _ctx.getTxScope(), resultSet, _ctx.getRowMaker() );
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
    for( Object param : _ctx.getParams().values() )
    {
      ValueAccessor accessor = ValueAccessor.get( _ctx.getJdbcParamTypes()[i] );
      accessor.setParameter( ps, ++i, param );
    }
  }
}
