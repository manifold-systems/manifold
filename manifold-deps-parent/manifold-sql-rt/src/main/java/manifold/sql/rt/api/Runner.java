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

import manifold.rt.api.Bindings;
import manifold.util.ManExceptionUtil;

import java.sql.*;
import java.util.function.Function;

public class Runner<T extends ResultRow>
{
  private final Class<T> _queryClass;
  private final int[] _jdbcParamTypes;
  private final Bindings _params;
  private final String _querySource;
  private final String _configName;
  private final Function<Bindings, T> _makeRow;

  public Runner( Class<T> queryClass, int[] jdbcParamTypes, Bindings params, String querySource, String configName, Function<Bindings, T> makeRow )
  {
    _queryClass = queryClass;
    _jdbcParamTypes = jdbcParamTypes;
    _params = params;
    _querySource = querySource;
    _configName = configName;
    _makeRow = makeRow;
  }

  @SuppressWarnings( "unused" )
  public Result<T> run()
  {
    ConnectionProvider cp = ConnectionProvider.findFirst();
    try( Connection c = cp.getConnection( _configName, _queryClass ) )
    {
      for( ConnectionNotifier p : ConnectionNotifier.PROVIDERS.get() )
      {
        p.init( c );
      }

      try( PreparedStatement ps = c.prepareStatement( _querySource ) )
      {
        setParameters( ps );
        try( ResultSet resultSet = ps.executeQuery() )
        {
          return new Result<T>( resultSet, _makeRow );
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
    for( Object param : _params.values() )
    {
      ValueAccessor accessor = ValueAccessor.get( _jdbcParamTypes[i] );
      accessor.setParameter( ps, ++i, param );
    }
  }
}
