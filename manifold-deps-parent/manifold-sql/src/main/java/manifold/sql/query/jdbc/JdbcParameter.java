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

import manifold.rt.api.util.Pair;
import manifold.sql.api.Statement;
import manifold.sql.api.Parameter;
import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.api.TypeProvider;

import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class JdbcParameter<S extends Statement> implements Parameter
{
  private final S _owner;
  private final int _position;
  private final String _name;
  private final int _jdbcType;
  private final String _sqlType;
  private final int _size;
  private final int _scale;
  private final boolean _isNullable;
  private final boolean _isSigned;
  private final String _javaClassNameForGetObject;

  public JdbcParameter( int paramIndex, String name, S owner, ParameterMetaData paramMetaData, DatabaseMetaData metaData ) throws SQLException
  {
    _position = paramIndex;
    _name = name == null ? "p" + paramIndex : name;
    _owner = owner;

    Pair<Integer, Boolean> types = getJdbcType( paramMetaData, paramIndex, metaData );
    _jdbcType = types.getFirst();
    boolean isFlakyDriver = types.getSecond();

    String sqlType;
    int size;
    int scale;
    boolean isNullable;
    boolean isSigned;
    String javaClassNameForGetObject;

    if( isFlakyDriver )
    {
      sqlType = "varchar";
      size = 0;
      scale = 0;
      isNullable = true;
      isSigned = true;
      javaClassNameForGetObject = Object.class.getTypeName();
    }
    else
    {
      sqlType = paramMetaData.getParameterTypeName( paramIndex );
      size = paramMetaData.getPrecision( paramIndex );
      scale = paramMetaData.getScale( paramIndex );
      isNullable = paramMetaData.isNullable( paramIndex ) != ParameterMetaData.parameterNoNulls;
      isSigned = paramMetaData.isSigned( paramIndex );
      javaClassNameForGetObject = paramMetaData.getParameterClassName( paramIndex );
    }
    
    _sqlType = sqlType;
    _size = size;
    _scale = scale;
    _isNullable = isNullable;
    _isSigned = isSigned;
    _javaClassNameForGetObject = javaClassNameForGetObject;
  }

  private Pair<Integer, Boolean> getJdbcType( ParameterMetaData paramMetaData, int paramIndex, DatabaseMetaData dbMetadata )
  {
    int jdbcType;
    boolean isFlakyDriver = false;
    try
    {
      TypeProvider typeProvider = Dependencies.instance().getTypeProvider();
      jdbcType = typeProvider.getQueryParameterType( paramIndex, paramMetaData, dbMetadata );
    }
    catch( SQLException se )
    {
      // (circus music)
      // Some drivers don't provide query parameter types when the parameter's value is not set. For instance, depending
      // on the version, SQLite will either return VARCHAR for all parameters or it will throw an exception when a parameter
      // is not set, hence this catch block. Mysql throws the borfin exception too unless the "generateSimpleParameterMetadata"
      // url parameter is set in which case it provides the same crap sandwich as sqlite (as of mysql driver 8.1).
      //
      jdbcType = Types.OTHER;
      isFlakyDriver = true;
    }

    return new Pair<>( jdbcType, isFlakyDriver );
  }

  @Override
  public int getJdbcType()
  {
    return _jdbcType;
  }

  @Override
  public String getSqlType()
  {
    return _sqlType;
  }

  @Override
  public String getColumnClassName()
  {
    return _javaClassNameForGetObject;
  }

  @Override
  public S getOwner()
  {
    return _owner;
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

  public boolean canBeNull()
  {
    return isNullable();
  }
}
