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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class JdbcQueryParameter implements QueryParameter
{
  private final JdbcQueryTable _queryTable;
  private final int _position;
  private final String _name;
  private Class<?> _type;
  private final int _size;
  private final int _scale;
  private final boolean _isNullable;
  private final boolean _isSigned;

  public JdbcQueryParameter( int paramIndex, String name, JdbcQueryTable queryTable, ParameterMetaData paramMetaData, PreparedStatement preparedStatement ) throws SQLException
  {
    _position = paramIndex;
    _name = name == null ? "p" + paramIndex : name;
    _queryTable = queryTable;

    int typeId;
    try
    {
      typeId = paramMetaData.getParameterType( paramIndex );
    }
    catch( Exception e )
    {
      // (circus music)
      // some drivers (SQLite since 3.42) require the parameter value to be set in the prepared statement BEFORE the
      // call to getParameterType(), so the type can be obtained from the value (?!) instead of inferring the type from
      // the parameter's context
      preparedStatement.setString( paramIndex, "" );
      typeId = Types.OTHER;
      _type = Object.class;
    }
    _type = _type == null ? queryTable.getTypeMap().getType( this, typeId ) : _type;
    _size = paramMetaData.getPrecision( paramIndex );
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
