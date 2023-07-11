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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

class ResultColumn implements BaseElement
{
  private final ResultSetMetaData _metaData;
  private final int _pos;

  public ResultColumn( ResultSetMetaData metaData, int pos )
  {
    _metaData = metaData;
    _pos = pos;
  }

  @Override
  public String getName()
  {
    try
    {
      return _metaData.getColumnLabel( _pos );
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @Override
  public int getPosition()
  {
    return _pos;
  }

  @Override
  public boolean isNullable()
  {
    try
    {
      return _metaData.isNullable( _pos ) == ResultSetMetaData.columnNullable;
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @Override
  public int getSize()
  {
    try
    {
      return _metaData.getPrecision( _pos );
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }
}
