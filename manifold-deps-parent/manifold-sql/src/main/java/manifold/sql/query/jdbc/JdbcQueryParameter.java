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
  private final int _jdbcType;
  private final int _size;
  private final int _scale;
  private final boolean _isNullable;
  private final boolean _isSigned;

  public JdbcQueryParameter( int paramIndex, String name, JdbcQueryTable queryTable, ParameterMetaData paramMetaData, PreparedStatement preparedStatement ) throws SQLException
  {
    _position = paramIndex;
    _name = name == null ? "p" + paramIndex : name;
    _queryTable = queryTable;

    int jdbcType;
    try
    {
      jdbcType = paramMetaData.getParameterType( paramIndex );
    }
    catch( SQLException se )
    {
      // (circus music)
      // Some drivers don't provide query parameter types when the parameter's value is not set. For instance, depending
      // on the version, SQLite will either return VARCHAR for all parameters or it will throw an exception when a parameter
      // is not set, hence this catch block.
      //
      preparedStatement.setString( paramIndex, "" );
      jdbcType = Types.OTHER;
    }

    _jdbcType = handleUnknownType( jdbcType );
    _size = paramMetaData.getPrecision( paramIndex );
    _scale = paramMetaData.getScale( paramIndex );
    _isNullable = paramMetaData.isNullable( paramIndex ) != ParameterMetaData.parameterNoNulls;
    _isSigned = paramMetaData.isSigned( paramIndex );
  }

  private int handleUnknownType( int jdbcType )
  {
    // Update: sqlite is flaky, see https://github.com/xerial/sqlite-jdbc/issues/928
    //
    String databaseProductName = _queryTable.getSchema().getDatabaseProductName();
    if( "SQLite".equalsIgnoreCase( databaseProductName ) )
    {
      // OTHER maps to java.lang.Object, which is less confusing than the VARCHAR type the sqlite driver assigns for all parameters.
      // For instance, java.lang.Object enables param values like integer foreign key ids to be passed directly in, instead
      // of confusing users with String type.
      return Types.OTHER; // maps to java.lang.Object
    }
    return jdbcType;
  }

  @Override
  public int getJdbcType()
  {
    return _jdbcType;
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
