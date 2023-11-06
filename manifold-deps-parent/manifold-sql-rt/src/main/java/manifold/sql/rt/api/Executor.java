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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * For internal use, called from generated code. Executes non-Select SQL.
 */
public class Executor
{
  private final String _sqlCommand;
  private final TxScope.RawChangeCtx _ctx;
  private final ColumnInfo[] _paramInfo;
  private final DataBindings _paramBindings;

  public Executor( TxScope.RawChangeCtx ctx, ColumnInfo[] paramInfo, DataBindings paramBindings, String sqlCommand )
  {
    _ctx = ctx;
    _paramInfo = paramInfo;
    _paramBindings = paramBindings;
    _sqlCommand = sqlCommand;
  }

  @SuppressWarnings( "unused" )
  public int execute() throws SQLException
  {
    Connection txConnextion = _ctx.getConnection();
    if( txConnextion == null )
    {
      throw new SQLException( "Connection is null. Raw commands must execute using `addRawChange()`." );
    }

    try( PreparedStatement ps = txConnextion.prepareStatement( _sqlCommand ) )
    {
      setParameters( ps );
      return ps.executeUpdate();
    }
  }

  private void setParameters( PreparedStatement ps ) throws SQLException
  {
    int i = 0;
    ValueAccessorProvider accProvider = Dependencies.instance().getValueAccessorProvider();
    for( Object param : _paramBindings.values() )
    {
      ValueAccessor accessor = accProvider.get( _paramInfo[i].getJdbcType() );
      accessor.setParameter( ps, ++i, param );
    }
  }
}
