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

package manifold.sql.query.jdbc;

import manifold.sql.query.api.QueryParameter;

import java.sql.ParameterMetaData;
import java.sql.SQLException;

public class JdbcQueryParameter implements QueryParameter
{
  private final JdbcQueryTable _queryTable;
  private final int _position;
  private final String _name;
  private final Class<?> _type;
  private final int _size;
  private final int _scale;
  private final boolean _isNullable;
  private final boolean _isSigned;

  public JdbcQueryParameter( int paramIndex, String name, JdbcQueryTable queryTable, ParameterMetaData paramMetaData ) throws SQLException
  {
    _position = paramIndex;
    _name = name == null ? "p" + paramIndex : name;
    _queryTable = queryTable;

    int typeId = paramMetaData.getParameterType( paramIndex );
    _type = queryTable.getTypeMap().getType( this, typeId );
    _size = paramMetaData.getParameterType( paramIndex );
    _scale = paramMetaData.getScale( paramIndex );
    _isNullable = paramMetaData.isNullable( paramIndex ) != ParameterMetaData.parameterNoNulls;
    _isSigned = paramMetaData.isSigned( paramIndex );
  }

  public JdbcQueryTable getTable()
  {
    return _queryTable;
  }

  @Override
  public int getPosition()
  {
    return _position;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public Class<?> getType()
  {
    return _type;
  }

  @Override
  public int getSize()
  {
    return _size;
  }

  @Override
  public int getScale()
  {
    return _scale;
  }

  @Override
  public boolean isNullable()
  {
    return _isNullable;
  }

  @Override
  public boolean isSigned()
  {
    return _isSigned;
  }
}
